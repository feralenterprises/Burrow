package gg.feral.burrow.core;

import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

/** Small bounded, single-thread, low-priority planner. NEVER calls RuneLite APIs.
 * One supported profile every 250ms; stops preloading whenever Outfitter is visible.
 * A selected loadout is computed on demand ahead of background preparation.
 * Each snapshot has a generation fence; previous-account responses cannot publish.
 *
 * <p>Build 19: each prepared entry carries a complete {@link BestBankPlanner.Plan} computed from a
 * coherent {@link BankEvidence}, the observed {@link AccountContext} and {@link ItemFacts}. A new
 * snapshot, context change, stale mark or clear increments the generation and drops the cache. */
public final class BestBankPreloader implements AutoCloseable {
    public record Key(String boss,String method,Tier tier) {}
    public record Prepared(Key key,long generation,List<BestBankMatches.Match> matches,
                           Map<Integer,Integer> positiveOwned,TierAwareBankResolver.Result tierResult,
                           BestBankPlanner.Plan plan,List<BestBankDiagnosis.Finding> diagnosis) {
        public Prepared { diagnosis=diagnosis==null?List.of():List.copyOf(diagnosis); }
        public Prepared(Key key,long generation,List<BestBankMatches.Match> matches,
                        Map<Integer,Integer> positiveOwned,TierAwareBankResolver.Result tierResult,BestBankPlanner.Plan plan) {
            this(key,generation,matches,positiveOwned,tierResult,plan,List.of());
        }
        public Prepared(Key key,long generation,List<BestBankMatches.Match> matches,
                        Map<Integer,Integer> positiveOwned,TierAwareBankResolver.Result tierResult) {
            this(key,generation,matches,positiveOwned,tierResult,null);
        }
    }
    private final ScheduledExecutorService worker;
    private final List<Key> all;
    private final ConcurrentMap<Key,Prepared> cache=new ConcurrentHashMap<>();
    private final AtomicLong generation=new AtomicLong();
    private final AtomicInteger computations=new AtomicInteger();
    private volatile BankEvidence evidence;
    private volatile Map<Integer,String> observedNames=Map.of();
    /** CP-7: RuneLite equipment stats for observed IDs only, and the owner's fill focus. */
    private volatile Map<Integer,OwnedGearStats.Stats> observedStats=Map.of();
    private volatile OwnedGearStats.Focus focus=OwnedGearStats.Focus.MATCH;
    /** Step 2: live Slayer task. Cleared with the account; UNKNOWN never grants an on-task bonus. */
    private volatile gg.feral.burrow.bib.SlayerTask slayerTask=gg.feral.burrow.bib.SlayerTask.UNKNOWN;
    public gg.feral.burrow.bib.SlayerTask slayerTask() { return slayerTask; }
    /** Step 2: a changed task re-plans the same evidence. Kill-count changes on the same task do not. */
    public synchronized long updateSlayerTask(gg.feral.burrow.bib.SlayerTask next) {
        if(closed || next==null || next.identity("").equals(slayerTask.identity("")))return generation.get();
        slayerTask=next;
        if(!active)return generation.get();
        generation.incrementAndGet();cache.clear();cursor=0;
        return generation.get();
    }
    public synchronized long observe(BankEvidence observed, AccountContext ctx, ItemFacts itemFacts, Map<Integer,String> names,
                                     Map<Integer,OwnedGearStats.Stats> stats) {
        observedStats=stats==null?Map.of():Map.copyOf(stats);
        return observe(observed,ctx,itemFacts,names);
    }
    public OwnedGearStats.Focus focus() { return focus; }
    /** CP-7: changing what "best" means re-plans the same evidence. */
    public synchronized long setFocus(OwnedGearStats.Focus next) {
        if(closed || next==null || next==focus)return generation.get();
        focus=next;
        if(!active)return generation.get();
        generation.incrementAndGet();cache.clear();cursor=0;
        return generation.get();
    }
    /** R05: same as {@link #observe(BankEvidence,AccountContext,ItemFacts)} plus client item names for diagnosis only. */
    public synchronized long observe(BankEvidence observed, AccountContext ctx, ItemFacts itemFacts, Map<Integer,String> names) {
        observedNames=names==null?Map.of():Map.copyOf(names);
        return observe(observed,ctx,itemFacts);
    }
    private volatile AccountContext context=AccountContext.UNKNOWN;
    private volatile ItemFacts facts=ItemFacts.catalogDerived();
    private volatile boolean active;
    private volatile boolean outfitterVisible;
    private volatile boolean closed;
    private int cursor;
    public BestBankPreloader(List<Key> profileKeys) {
        all=List.copyOf(profileKeys);
        worker=Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t=new Thread(r,"TrashPanda-Bank-Preload");
            t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;
        });
        worker.scheduleWithFixedDelay(this::prepareOne,250,250,TimeUnit.MILLISECONDS);
    }
    public static List<Key> knownProfiles() {
        var out=new ArrayList<Key>();
        for(var boss:BurrowModel.availableBosses()) {
            if(!BossRegistry.bestBankAllowed(boss))continue; // Build 20: never preload Wilderness plans
            for(var method:TieredOutfitterCatalog.methods(boss))
                for(var tier:Tier.values())
                    if(TieredOutfitterCatalog.find(boss,method,tier)!=null &&
                       !StyleMethodPicker.isProvisional(boss,method,tier))
                        out.add(new Key(boss,method,tier));
        }
        return List.copyOf(out);
    }
    /** Legacy intake (single-read fixture evidence). */
    public synchronized long observe(Map<Integer,Integer> positivelyOwned) {
        return observe(positivelyOwned.isEmpty()?null:BankEvidence.fixture(positivelyOwned),context,facts);
    }
    /** Publish immutable client-thread evidence without retaining account names. */
    public synchronized long observe(BankEvidence observed, AccountContext ctx, ItemFacts itemFacts) {
        if(closed)return generation.get();
        generation.incrementAndGet();
        evidence=observed;
        if(ctx!=null)context=ctx;
        if(itemFacts!=null)facts=itemFacts;
        // BQA-901 BF-04: a coherent read of an EMPTY bank (inventory/worn only) is valid evidence.
        // Only "no observation" (null) keeps the planner inactive; the legacy Map intake still maps {} to null.
        cache.clear();cursor=0;active=observed!=null && observed.planningInput();
        return generation.get();
    }
    /** Levels/quests changed: recompute plans for the same evidence. Returns new generation. */
    public synchronized long updateContext(AccountContext ctx) {
        if(closed || ctx==null || ctx.equals(context))return generation.get();
        context=ctx;
        if(!active)return generation.get();
        generation.incrementAndGet();cache.clear();cursor=0;
        return generation.get();
    }
    /** Inventory/worn changed after bank close: keep evidence but label it stale. */
    public synchronized long markStale() {
        BankEvidence current=evidence;
        if(closed || current==null || current.stale())return generation.get();
        return observe(current.markStale(),context,facts);
    }
    /**
     * CP-1: inventory/worn changed while the bank is closed and the client gave a complete live read of both.
     * Re-plans on as-of-close bank + live carried gear. Names for newly carried IDs are merged (diagnosis/variants).
     */
    public synchronized long refreshCarried(Map<Integer,Integer> liveInventory, Map<Integer,Integer> liveEquipment,
                                            long now, Map<Integer,String> newNames) {
        return refreshCarried(liveInventory,liveEquipment,now,newNames,Map.of());
    }
    public synchronized long refreshCarried(Map<Integer,Integer> liveInventory, Map<Integer,Integer> liveEquipment,
                                            long now, Map<Integer,String> newNames, Map<Integer,OwnedGearStats.Stats> newStats) {
        BankEvidence current=evidence;
        if(closed || current==null || current.bankStillOpen())return generation.get();
        if(current.inventory().equals(liveInventory) && current.equipment().equals(liveEquipment) && !current.stale())
            return generation.get();
        Map<Integer,String> merged=new HashMap<>(observedNames);
        if(newNames!=null) newNames.forEach(merged::putIfAbsent);
        observedNames=Map.copyOf(merged);
        Map<Integer,OwnedGearStats.Stats> mergedStats=new HashMap<>(observedStats);
        if(newStats!=null) newStats.forEach(mergedStats::putIfAbsent);
        observedStats=Map.copyOf(mergedStats);
        return observe(current.withLiveCarried(liveInventory,liveEquipment,now),context,facts);
    }
    public synchronized long markBankClosed() {
        BankEvidence current=evidence;
        if(closed || current==null || !current.bankStillOpen())return generation.get();
        return observe(current.closed(),context,facts);
    }
    public synchronized void clear() {
        generation.incrementAndGet();active=false;evidence=null;observedNames=Map.of();observedStats=Map.of();cache.clear();cursor=0;
        slayerTask=gg.feral.burrow.bib.SlayerTask.UNKNOWN;
    }
    public BankEvidence evidence() { return evidence; }
    public AccountContext context() { return context; }
    public void pauseForOutfitter(boolean visible) {outfitterVisible=visible;}
    public boolean pausedForOutfitter() {return outfitterVisible;}
    public int preparedCount() {return cache.size();}
    public int profileCount() {return all.size();}
    public int computations() {return computations.get();}
    public long generation() {return generation.get();}
    public boolean isClosed() {return closed;}
    public Optional<Prepared> cached(Key key) {
        if(key==null || !BossRegistry.bestBankAllowed(key.boss()))return Optional.empty();
        Prepared p=cache.get(key);
        return p==null || p.generation()!=generation.get()?Optional.empty():Optional.of(p);
    }
    /** On-demand priority task, without blocking the Swing EDT. */
    public void request(Key key, Consumer<Prepared> ready) {
        if(closed || !active || key==null || !BossRegistry.bestBankAllowed(key.boss()))return;
        var cached=cached(key);
        if(cached.isPresent()){ready.accept(cached.get());return;}
        worker.execute(()-> {
            Prepared result=compute(key);
            if(result!=null && !closed && result.generation()==generation.get())ready.accept(result);
        });
    }
    private void prepareOne() {
        if(closed || !active || outfitterVisible)return;
        // There is at most one background profile per 250ms. No loops over all boss gear.
        Key key;
        synchronized(this) {if(cursor>=all.size())return;key=all.get(cursor++);}
        if(cached(key).isPresent())return;
        compute(key);
    }
    private Prepared compute(Key key) {
        if(closed || !active || !BossRegistry.bestBankAllowed(key.boss()))return null;
        long stamp=generation.get();
        BankEvidence observed=evidence;
        AccountContext ctx=context;
        ItemFacts itemFacts=facts;
        if(observed==null || !observed.planningInput())return null;
        var selected=TieredOutfitterCatalog.find(key.boss(),key.method(),key.tier());
        if(selected==null)return null;
        Map<Integer,Integer> combined=observed.combined();
        computations.incrementAndGet();
        Map<Integer,String> names=observedNames;
        List<BestBankMatches.Match> matches=BestBankMatches.gear(selected.profile(),combined,names);
        if(closed || stamp!=generation.get())return null;
        TierAwareBankResolver.Result tierResult=TierAwareBankResolver.resolve(key.boss(),key.method(),key.tier(),combined,names);
        if(closed || stamp!=generation.get())return null;
        BestBankPlanner.Plan plan=BestBankPlanner.plan(key.boss(),key.method(),key.tier(),observed,ctx,itemFacts,names,observedStats,focus,slayerTask);
        if(closed || stamp!=generation.get())return null;
        var diagnosis=BestBankDiagnosis.diagnose(plan,combined,observedNames);
        Prepared ready=new Prepared(key,stamp,matches,combined,tierResult,plan,diagnosis);
        cache.putIfAbsent(key,ready);
        Prepared stored=cache.get(key);
        return stored!=null && stored.generation()==stamp?stored:ready;
    }
    /** R09: an item that would improve a prepared Best-in-Bank plan. Evidence-gated, never ownership. */
    public record Upgrade(int itemId, String itemName, String slot, Kind kind, List<String> bosses) {
        public enum Kind { MISSING, LOWER_TIER_IN_USE }
        public Upgrade { bosses=List.copyOf(bosses); }
    }
    public enum UpgradeEvidence { CURRENT, STALE, NONE }
    public UpgradeEvidence upgradeEvidence() {
        BankEvidence ev=evidence;
        if(ev==null || !ev.planningInput()) return UpgradeEvidence.NONE;
        return ev.stale()?UpgradeEvidence.STALE:UpgradeEvidence.CURRENT;
    }
    private static final String[] UPGRADE_SLOT={"Head","Cape","Neck","Ammo","Weapon","Body","Shield","Legs","Gloves","Boots","Ring"};
    /**
     * R09: target gear the current, non-stale bank read did NOT show (MISSING) or that is covered only by a lower-tier
     * substitute (LOWER_TIER_IN_USE), across the plans prepared for THIS generation. Ranked by how many bosses want it.
     * Empty when the evidence is missing or stale: the caller must say "unknown", not "no upgrades".
     */
    public List<Upgrade> upgradeOpportunities(int max) {
        if(upgradeEvidence()!=UpgradeEvidence.CURRENT) return List.of();
        long g=generation.get();
        Map<Integer,Upgrade> by=new LinkedHashMap<>();
        Map<Integer,Set<String>> bosses=new HashMap<>();
        List<Prepared> ps=new ArrayList<>(cache.values());
        ps.sort(Comparator.comparing((Prepared p)->p.key().boss()).thenComparing(p->p.key().method()).thenComparing(p->p.key().tier()));
        for(Prepared p:ps) {
            if(p.generation()!=g || p.plan()==null) continue;
            var gear=p.plan().gear();
            for(int i=0;i<gear.size() && i<UPGRADE_SLOT.length;i++) {
                var c=gear.get(i);
                if(c.planned()==null || c.planned().id()<=0) continue;
                Upgrade.Kind k=c.status()==BestBankPlanner.GearStatus.NOT_OBSERVED||c.status()==BestBankPlanner.GearStatus.OPTIONAL_MISSING?Upgrade.Kind.MISSING
                    :c.status()==BestBankPlanner.GearStatus.OWNED_SUBSTITUTE?Upgrade.Kind.LOWER_TIER_IN_USE:null;
                if(k==null) continue;
                int id=c.planned().id();
                bosses.computeIfAbsent(id,x->new TreeSet<>()).add(p.key().boss());
                Upgrade prior=by.get(id);
                // LOWER_TIER_IN_USE wins over MISSING only if every plan had a substitute; keep MISSING if any plan lacks it entirely.
                Upgrade.Kind kind=prior==null?k:(prior.kind()==Upgrade.Kind.MISSING||k==Upgrade.Kind.MISSING?Upgrade.Kind.MISSING:k);
                by.put(id,new Upgrade(id,c.planned().name(),UPGRADE_SLOT[i],kind,List.copyOf(bosses.get(id))));
            }
        }
        List<Upgrade> out=new ArrayList<>(by.values());
        out.sort(Comparator.comparingInt((Upgrade u)->-u.bosses().size()).thenComparing(Upgrade::itemName));
        return out.size()>max?List.copyOf(out.subList(0,max)):List.copyOf(out);
    }
    /** Best prepared verdict per boss for the CURRENT generation only (Pathfinder input). */
    public Map<String,String> bestHeadlineByBoss() {
        long g=generation.get();
        Map<String,BestBankPlanner.Plan> best=new HashMap<>();
        for(Prepared p:cache.values()) {
            if(p.generation()!=g || p.plan()==null)continue;
            var cur=best.get(p.key().boss());
            if(cur==null || BestBankPlanner.rank(p.plan().verdict())>BestBankPlanner.rank(cur.verdict())) best.put(p.key().boss(),p.plan());
        }
        Map<String,String> out=new HashMap<>();
        best.forEach((boss,plan)->out.put(boss,plan.headline()+" ("+plan.method()+")"));
        return out;
    }
    @Override public synchronized void close() {closed=true;clear();worker.shutdownNow();}
}
