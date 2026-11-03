package gg.feral.burrow.experience;

import gg.feral.burrow.core.*;
import gg.feral.burrow.core.CompletedSessionHistory.FeedState;
import gg.feral.burrow.core.CompletedSessionHistory.Summary;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import gg.feral.burrow.experience.AccountEvidenceContext.BankState;
import gg.feral.burrow.experience.AccountEvidenceContext.Spellbook;
import gg.feral.burrow.experience.BanditBossCandidate.Readiness;
import gg.feral.burrow.experience.BanditBossCandidate.Reason;
import gg.feral.burrow.experience.NavigationCommand.*;
import gg.feral.burrow.experience.NavigationState.*;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import gg.feral.burrow.pathfinder.SuggestionEngine.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Acceptance cases 1 to 17 from the UI data handoff. All fixtures are synthetic. */
public final class ExperienceFoundationJUnitTest {
    static final List<String> SKILLS = List.of("Attack","Strength","Defence","Ranged","Magic","Hitpoints","Prayer","Slayer","Agility","Herblore",
        "Mining","Smithing","Crafting","Fletching","Thieving","Firemaking","Woodcutting","Fishing","Cooking","Farming","Runecraft","Hunter","Construction");

    static Map<String,Integer> levels(int level) { Map<String,Integer> m = new HashMap<>(); for (String s : SKILLS) m.put(s, level); return m; }
    static Map<String,Quest> quests(Quest state) {
        Map<String,Quest> q = new HashMap<>();
        for (var e : BossRegistry.all()) for (String n : e.quests()) q.put(n, state);
        for (var e : gg.feral.burrow.pathfinder.GuideData.questPrerequisites().entrySet()) { q.put(e.getKey(), state); for (String n : e.getValue()) q.put(n, state); }
        return q;
    }
    static BankEvidence live(Map<Integer,Integer> own) { return new BankEvidence(own, Map.of(), Map.of(), BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT, 1L, true, false); }
    static AccountContext gear(Map<String,Integer> levels, Map<String,Quest> quests) {
        Map<String,String> q = new HashMap<>();
        for (var e : quests.entrySet()) q.put(e.getKey(), e.getValue().name());
        return new AccountContext(levels, q);
    }
    static AccountEvidenceContext account(String key, String login, BankEvidence bank, int level, Quest state, Map<String,Boolean> diaries, Spellbook book, Map<String,GoalMark> goals) {
        var l = levels(level); var q = quests(state);
        var snapshot = new Snapshot(true, l, q, BankStatus.VERIFIED_SAME_ACCOUNT, bank == null ? Set.of() : bank.combined().keySet(), Mode.IRONMAN, Map.of(), goals);
        return new AccountEvidenceContext(key, login, 1, bank, snapshot, gear(l, q), diaries, book);
    }
    static AccountEvidenceContext account(BankEvidence bank) { return account("a", "login-1", bank, 99, Quest.FINISHED, Map.of(), Spellbook.UNKNOWN, Map.of()); }
    static Map<Integer,Integer> kit(String boss, String method, Tier tier) {
        Map<Integer,Integer> own = new HashMap<>();
        var p = TieredOutfitterCatalog.find(boss, method, tier).profile();
        for (Item it : p.gear()) if (it != null && it.id() > 0) own.put(it.id(), Math.max(1000, it.amount()));
        for (Item it : p.inventory()) if (it != null && it.id() > 0) own.merge(it.id(), Math.max(1000, it.amount()), Integer::sum);
        return own;
    }

    static final List<BestBankPlanner.GearCell> GEAR = Collections.nCopies(11, new BestBankPlanner.GearCell(null, null, BestBankPlanner.GearStatus.EMPTY, ""));
    static final List<BestBankPlanner.SupplyCell> INV = Collections.nCopies(28, new BestBankPlanner.SupplyCell(null, null, BestBankPlanner.SupplyStatus.EMPTY, ""));

    // 1
    @Test void searchIsCaseInsensitiveUsesAliasesAndSortsStably() {
        var hits = ChronicleProvider.browse(new ChronicleBrowseRequest("sCuRRiUs", null, false, null, 0, 10));
        assertEquals(List.of("Scurrius"), hits.rows().stream().map(BossPreview::bossId).toList());
        assertTrue(hits.rows().get(0).wikiUrl().startsWith("https://oldschool.runescape.wiki/"));
        var aliased = BossRegistry.all().stream().filter(e -> !e.aliases().isEmpty()).findFirst().orElseThrow();
        var byAlias = ChronicleProvider.browse(new ChronicleBrowseRequest(aliased.aliases().get(0), null, false, null, 0, 100));
        assertTrue(byAlias.rows().stream().anyMatch(b -> b.bossId().equals(aliased.id())));
        var az = ChronicleProvider.browse(ChronicleBrowseRequest.all()).rows().stream().map(BossPreview::bossId).toList();
        var za = ChronicleProvider.browse(new ChronicleBrowseRequest("", null, true, ChronicleBrowseRequest.Sort.Z_TO_A, 0, 100)).rows().stream().map(BossPreview::bossId).toList();
        assertEquals(az.get(0), za.get(za.size() - 1));
        assertEquals(az, ChronicleProvider.browse(ChronicleBrowseRequest.all()).rows().stream().map(BossPreview::bossId).toList());
    }
    @Test void pagingIsBoundedAndDifficultySortIsNotFaked() {
        var far = ChronicleProvider.browse(new ChronicleBrowseRequest("", null, true, null, Integer.MAX_VALUE, 100));
        assertTrue(far.rows().isEmpty());
        var hard = ChronicleProvider.browse(new ChronicleBrowseRequest("", null, true, ChronicleBrowseRequest.Sort.DIFFICULTY, 0, 5));
        assertEquals(ChronicleBrowseRequest.Sort.A_TO_Z, hard.sortApplied());
        assertFalse(hard.note().isEmpty());
    }
    // 2
    @Test void publishedRateStaysSeparateFromCumulativeOdds() {
        var drop = new RareDropTable.Drop("test_drop", 1, "Test drop", 1, 33, "tertiary", 1, null);
        var encounter = new RareDropTable.Encounter("Test", "Test", "https://oldschool.runescape.wiki/w/Test", List.of(drop), List.of(), List.of(), "", 0, null);
        var published = PublishedDrop.of(encounter, drop);
        assertEquals(1 / 33d, published.perKillChance().getAsDouble(), 1e-9);
        var noKc = ChronicleProvider.encounterDrops(encounter, null).rows().get(0);
        assertTrue(noKc.atLeastOne().isEmpty());
        assertEquals(EvidenceStatus.UNKNOWN, noKc.oddsStatus());
        var withKc = ChronicleProvider.encounterDrops(encounter, evidence(33)).rows().get(0);
        assertEquals(1 - Math.pow(32 / 33d, 33), withKc.atLeastOne().getAsDouble(), 1e-9);
        assertEquals(EvidenceStatus.OBSERVED, withKc.oddsStatus());
    }
    static PersonalDropEvidence evidence(long kc) {
        return new PersonalDropEvidence() {
            @Override public RareDropStore.Effective kc(String encounter) { return new RareDropStore.Effective(OptionalLong.of(kc), RareDropStore.Source.COLLECTION_LOG, 1L); }
            @Override public Optional<String> unlockSource(int itemId) { return Optional.empty(); }
        };
    }
    // 3
    @Test void unknownRatesAndConditionalOddsStayUnknown() {
        var unknown = PublishedDrop.unknown("x", 1, "Mystery", "No sourced rate", "https://oldschool.runescape.wiki/w/Test");
        assertTrue(unknown.perKillChance().isEmpty());
        assertEquals(PublishedDrop.RateType.UNKNOWN, unknown.rateType());
        var conditional = new RareDropTable.Drop("c", 1, "Conditional", 1, 10, "g", 1, "only on task");
        var encounter = new RareDropTable.Encounter("T", "T", "u", List.of(conditional), List.of(), List.of(), "", 0, null);
        assertTrue(ChronicleProvider.encounterDrops(encounter, evidence(100)).rows().get(0).atLeastOne().isEmpty());
        assertEquals(PublishedDrop.RateType.CONDITIONAL, PublishedDrop.of(encounter, conditional).rateType());
        for (var row : ChronicleProvider.browse(ChronicleBrowseRequest.all()).rows()) assertNull(row.imageRef());
    }
    // 4
    @Test void browseIsRestoredAndSelectionsStayIndependent() {
        var nav = NavigationState.initial();
        nav = NavigationReducer.apply(nav, new SelectInOutfitter(new OutfitterSelection("Scurrius", null, Tier.ENTRY, "MATCH", true))).state();
        nav = NavigationReducer.apply(nav, new OpenChronicleList()).state();
        var browse = new BrowseMemory("sara", BossRegistry.Group.REGULAR, ChronicleBrowseRequest.Sort.Z_TO_A, "");
        nav = NavigationReducer.apply(nav, new UpdateBrowse(browse)).state();
        nav = NavigationReducer.apply(nav, new OpenBossDetail("sarachnis", "row-7")).state();
        assertEquals(ChronicleRoute.DETAIL, nav.chronicleRoute());
        assertEquals("Sarachnis", nav.chronicleBossId());
        assertEquals("Scurrius", nav.outfitter().bossId(), "viewing a boss must not change Outfitter");
        nav = NavigationReducer.apply(nav, new BackToList()).state();
        assertEquals(ChronicleRoute.BROWSE, nav.chronicleRoute());
        assertEquals("sara", nav.browse().query());
        assertEquals("row-7", nav.browse().scrollKey());
        assertEquals(ChronicleBrowseRequest.Sort.Z_TO_A, nav.browse().sort());
        assertEquals("Scurrius", NavigationReducer.apply(nav, new OpenTab(TopTab.SESSIONS)).state().outfitter().bossId());
        nav = NavigationReducer.apply(nav, new OpenBossDetail("Sarachnis", null)).state();
        var opened = NavigationReducer.apply(nav, new OpenBossInOutfitter()).state();
        assertEquals("Sarachnis", opened.outfitter().bossId());
        assertEquals(TopTab.OUTFITTER, opened.topTab());
        assertFalse(opened.chronicleOpen());
        assertNotNull(TieredOutfitterCatalog.find(opened.outfitter().bossId(), opened.outfitter().method(), opened.outfitter().tier()));
    }
    @Test void unknownBossLeavesStateAlone() {
        var nav = NavigationReducer.apply(NavigationState.initial(), new OpenChronicleList()).state();
        var result = NavigationReducer.apply(nav, new OpenBossDetail("Not a boss", "x"));
        assertEquals(nav, result.state());
        assertFalse(result.notice().isEmpty());
    }
    @Test void topTabsStayFour() { assertEquals(4, TopTab.values().length); }
    // 5
    @Test void raidIsReferenceOnly() {
        var detail = ChronicleProvider.detail("Verzik Vitur").orElseThrow();
        assertTrue(detail.preview().raidReference());
        assertEquals(OutfitterNavigationIntent.Availability.UNSUPPORTED_RAID, detail.outfitter().availability());
        assertTrue(detail.tasks().isEmpty());
        var nav = NavigationReducer.apply(NavigationState.initial(), new OpenBossDetail("Verzik Vitur", null)).state();
        var result = NavigationReducer.apply(nav, new OpenBossInOutfitter());
        assertEquals(nav, result.state(), "a raid never opens Outfitter");
        assertFalse(result.notice().isEmpty());
    }
    // 6
    @Test void bankEvidenceStatesStaySeparate() {
        var empty = new BankEvidence(Map.of(), Map.of(), Map.of(), BankEvidence.Population.STABLE_READ, 1L, true, false);
        assertEquals(BankState.CONFIRMED_EMPTY, AccountEvidenceContext.classify(empty));
        assertEquals(BankState.STALE, AccountEvidenceContext.classify(empty.markStale()));
        assertEquals(BankState.UNKNOWN, AccountEvidenceContext.classify(null));
        assertEquals(BankState.UNKNOWN, AccountEvidenceContext.classify(BankEvidence.fixture(Map.of())));
        assertEquals(Reason.NO_BANK_SNAPSHOT, BanditAssessor.withPlanner(account(null)).assess("Scurrius").reasonCode());
        assertEquals(Reason.BANK_STALE, BanditAssessor.withPlanner(account(empty.markStale())).assess("Scurrius").reasonCode());
        var confirmed = BanditAssessor.withPlanner(account(empty)).assess("Scurrius");
        assertEquals(Reason.CONFIRMED_EMPTY_BANK, confirmed.reasonCode());
        assertEquals(Readiness.BLOCKED, confirmed.readiness());
    }
    @Test void missingDragonfireProtectionIsBlockedNeverReady() {
        String method = "Ranged crossbow";
        Map<Integer,Integer> own = new HashMap<>();
        for (Tier t : Tier.values()) own.putAll(kit("Vorkath", method, t));
        for (Tier t : Tier.values()) own.remove(TieredOutfitterCatalog.find("Vorkath", method, t).profile().gear().get(6).id());
        var a = account(live(own));
        var plan = BestBankPlanner.plan("Vorkath", method, Tier.OPTIMAL, a.bank(), a.gearContext(), ItemFacts.catalogDerived());
        assertTrue(plan.blockers().stream().anyMatch(x -> x.contains("Shield")), plan.blockers().toString());
        var c = new BanditAssessor(a, (b, m, t) -> plan).assess("Vorkath");
        assertTrue(c.readiness() == Readiness.BLOCKED || c.readiness() == Readiness.UNKNOWN, "no false-safe dragonfire recommendation");
    }
    @Test void aProtectionBlockerAlwaysBecomesBlocked() {
        // Every method supplies a matching identity and selected tier; otherwise unmatched
        // alternative styles intentionally return UNKNOWN, which is still not a safe recommendation.
        var c = new BanditAssessor(account(live(Map.of(1, 1))), (b, m, t) ->
            new BestBankPlanner.Plan(b, m, t, Tier.ENTRY, BestBankPlanner.Verdict.NOT_READY,
                GEAR, INV, List.of("CORE Shield: Dragonfire ward not observed"),
                List.of(), List.of(), 0, 28, List.of(), "", null, List.of(), List.of(), List.of())
        ).assess("Scurrius");
        assertEquals(Readiness.BLOCKED, c.readiness());
        assertEquals(Reason.PROTECTION_MISSING, c.reasonCode());
    }
    @Test void aMissingWeaponIsNotClaimedAsNoSuitableWeapon() {
        var plan = planWithBlockers(List.of("CORE Weapon: Armadyl crossbow not observed"), BestBankPlanner.Verdict.NOT_READY);
        var c = new BanditAssessor(account(live(Map.of(1, 1))), (b, m, t) -> plan).assess("Scurrius");
        assertEquals(Readiness.UNKNOWN, c.readiness());
        assertEquals(Reason.NO_REVIEWED_WEAPON_OWNED, c.reasonCode());
    }
    static BestBankPlanner.Plan planWithBlockers(List<String> blockers, BestBankPlanner.Verdict verdict) {
        return new BestBankPlanner.Plan("Scurrius", TieredOutfitterCatalog.methods("Scurrius").get(0), Tier.OPTIMAL, Tier.ENTRY, verdict, GEAR, INV, blockers, List.of(), List.of(), 0, 28, List.of(), "", null, List.of(), List.of(), List.of());
    }
    // 7
    @Test void weakButCompleteGearIsStretchOrHardNotBlocked() {
        String method = TieredOutfitterCatalog.methods("Sarachnis").get(0);
        var c = BanditAssessor.withPlanner(account(live(kit("Sarachnis", method, Tier.ENTRY)))).assess("Sarachnis");
        assertTrue(c.readiness() == Readiness.STRETCH || c.readiness() == Readiness.HARD_BUT_DOABLE, c.readiness() + " " + c.reason());
        assertEquals(EvidenceStatus.INFERRED_WITH_LIMITS, c.status());
    }
    @Test void ownerNamedFightsAreNeverComfortable() {
        for (String boss : List.of("General Graardor", "K'ril Tsutsaroth")) {
            var plan = new BestBankPlanner.Plan(boss, TieredOutfitterCatalog.methods(boss).get(0), Tier.OPTIMAL, Tier.OPTIMAL, BestBankPlanner.Verdict.READY, GEAR, INV, List.of(), List.of(), List.of(), 0, 28, List.of(), "", null, List.of(), List.of(), List.of());
            var c = new BanditAssessor(account(live(Map.of(1, 1))), (b, m, t) -> plan).assess(boss);
            assertEquals(Readiness.STRETCH, c.readiness(), boss);
        }
    }
    @Test void readyAboveEntryIsComfortableForOrdinaryBosses() {
        var plan = new BestBankPlanner.Plan("Scurrius", TieredOutfitterCatalog.methods("Scurrius").get(0), Tier.OPTIMAL, Tier.RECOMMENDED, BestBankPlanner.Verdict.READY, GEAR, INV, List.of(), List.of(), List.of(), 0, 28, List.of(), "", null, List.of(), List.of(), List.of());
        assertEquals(Readiness.COMFORTABLE, new BanditAssessor(account(live(Map.of(1, 1))), (b, m, t) -> plan).assess("Scurrius").readiness());
    }
    // 8
    @Test void wildernessNeverReachesThePlannerOrBestBank() {
        String boss = BossRegistry.all().stream().filter(BossRegistry.Entry::wilderness).findFirst().orElseThrow().id();
        var a = new BanditAssessor(account(live(Map.of(1, 1))), (b, m, t) -> {
            assertFalse(BossRegistry.wilderness(b), "planner reached for " + b);
            return planWithBlockers(List.of(), BestBankPlanner.Verdict.READY);
        });
        var c = a.assess(boss);
        assertEquals(Reason.WILDERNESS_NOT_ASSESSED, c.reasonCode());
        assertFalse(c.warning().isEmpty());
        assertTrue(a.hunts(100).stream().noneMatch(x -> BossRegistry.wilderness(x.bossId())));
        for (var e : BossRegistry.all()) if (e.wilderness()) {
            var intent = ChronicleProvider.outfitterIntent(e.id());
            assertFalse(intent.bestBankAllowed(), e.id());
            var nav = NavigationReducer.apply(NavigationState.initial(), new SelectInOutfitter(new OutfitterSelection("Scurrius", null, Tier.ENTRY, "MATCH", true))).state();
            nav = NavigationReducer.apply(nav, new OpenBossDetail(e.id(), null)).state();
            var opened = NavigationReducer.apply(nav, new OpenBossInOutfitter()).state();
            assertFalse(opened.outfitter().bestBank(), e.id());
            var chosen = NavigationReducer.apply(nav, new SelectInOutfitter(new OutfitterSelection(e.id(), null, Tier.ENTRY, "MATCH", true))).state();
            assertFalse(chosen.outfitter().bestBank(), e.id());
        }
    }
    // 9
    static boolean eligible(String id, AccountEvidenceContext a) { return SpoonsTipCatalog.find(id).orElseThrow().eligible(a); }
    @Test void spoonsGatesNeedRealEvidence() {
        Map<Integer,Integer> own = Map.of(1521, 1, 9075, 2, 561, 1, 557, 15, 995, 175, 3142, 1);
        var unknown = account(live(own));
        assertFalse(eligible("plank_make", unknown), "spellbook unknown");
        assertTrue(eligible("plank_make", account("a", "l", live(own), 99, Quest.FINISHED, Map.of(), Spellbook.LUNAR, Map.of())));
        assertFalse(eligible("plank_make", account("a", "l", live(own), 99, Quest.FINISHED, Map.of(), Spellbook.STANDARD, Map.of())));
        assertFalse(eligible("plank_make", account("a", "l", live(own), 80, Quest.FINISHED, Map.of(), Spellbook.LUNAR, Map.of())));
        assertFalse(eligible("zaff_battlestaves", unknown), "diary unknown");
        assertFalse(eligible("zaff_battlestaves", account("a", "l", live(own), 99, Quest.FINISHED, Map.of("Varrock Easy", false), Spellbook.UNKNOWN, Map.of())));
        assertFalse(eligible("zaff_battlestaves", account("a", "l", live(own), 99, Quest.FINISHED, Map.of("Varrock Easy", true), Spellbook.UNKNOWN, Map.of())), "daily barrel reward/stock proof not observed");
        assertFalse(eligible("cook_karambwan", unknown), "quest unknown to the fixture");
        Map<String,Quest> q = new HashMap<>(quests(Quest.FINISHED)); q.put("Tai Bwo Wannai Trio", Quest.FINISHED);
        var l = levels(30);
        var snap = new Snapshot(true, l, q, BankStatus.VERIFIED_SAME_ACCOUNT, own.keySet(), Mode.IRONMAN, Map.of(), Map.of());
        assertTrue(eligible("cook_karambwan", new AccountEvidenceContext("a", "l", 1, live(own), snap, gear(l, q), Map.of(), Spellbook.UNKNOWN)));
        var low = new Snapshot(true, levels(29), q, BankStatus.VERIFIED_SAME_ACCOUNT, own.keySet(), Mode.IRONMAN, Map.of(), Map.of());
        assertFalse(eligible("cook_karambwan", new AccountEvidenceContext("a", "l", 1, live(own), low, gear(l, q), Map.of(), Spellbook.UNKNOWN)));
    }
    @Test void unknownAccountOnlyGetsTheGenericReminder() {
        var s = SpoonsTipService.select(account(null), null);
        var tip = s.tip().orElseThrow();
        assertEquals("check_farms", tip.tipId());
        assertEquals(EvidenceStatus.GENERIC_REMINDER, tip.status());
        assertNull(tip.minMinutes());
        for (var t : SpoonsTipCatalog.tips()) if (!t.generic()) assertFalse(t.eligible(account(null)), t.id());
    }
    // 10
    @Test void oneTipPerLoginEvenAcrossRedrawsAndHops() {
        Map<Integer,Integer> own = Map.of(526, 1, 1755, 1, 1623, 1, 199, 1, 561, 1, 554, 1);
        var a = account(live(own));
        var first = SpoonsTipService.select(a, null);
        for (int i = 0; i < 5; i++) assertEquals(first.tip().orElseThrow().tipId(), SpoonsTipService.select(a, first.state()).tip().orElseThrow().tipId());
        var done = SpoonsTipService.close(first.state(), SpoonsTipService.Closed.MANUAL_DONE);
        assertTrue(SpoonsTipService.select(a, done).tip().isEmpty(), "Done closes it for this login");
        assertTrue(SpoonsTipService.select(a, done).tip().isEmpty(), "no replacement tip this login");
        var dismissed = SpoonsTipService.close(first.state(), SpoonsTipService.Closed.DISMISSED);
        assertTrue(SpoonsTipService.select(a, dismissed).tip().isEmpty());
        var next = SpoonsTipService.select(account("a", "login-2", live(own), 99, Quest.FINISHED, Map.of(), Spellbook.UNKNOWN, Map.of()), done);
        assertTrue(next.tip().isPresent());
        assertNotEquals(first.tip().get().tipId(), next.tip().get().tipId(), "cooldown skips the last tip");
        assertEquals(SpoonsTipService.select(a, null).tip(), first.tip(), "deterministic for an account and login");
    }
    @Test void cooldownNeverRepeatsATipOnBackToBackLogins() {
        Map<Integer,Integer> own = Map.of(526, 1, 1755, 1, 1623, 1, 199, 1, 561, 1, 554, 1);
        SpoonsTipService.State state = null;
        String last = null;
        for (int n = 0; n < 40; n++) {
            var s = SpoonsTipService.select(account("a", "login-" + n, live(own), 99, Quest.FINISHED, Map.of(), Spellbook.STANDARD, Map.of()), state);
            String id = s.tip().orElseThrow().tipId();
            assertNotEquals(last, id, "login " + n);
            last = id;
            state = s.state();
        }
    }
    @Test void staleBankProvesNeitherOwnershipNorAbsence() {
        var a = account(live(Map.of(526, 1)).markStale());
        assertEquals(AccountEvidenceContext.Check.UNKNOWN, a.owns(526));
        assertEquals(AccountEvidenceContext.Check.UNKNOWN, a.owns(527));
        assertEquals("check_farms", SpoonsTipService.select(a, null).tip().orElseThrow().tipId());
    }
    // 11
    @Test void noodlesReadsGoalsAndNeverChangesThem() {
        var rule = gg.feral.burrow.pathfinder.GuideData.rules().stream().filter(r -> !r.quests().isEmpty()).findFirst().orElseThrow();
        Map<String,GoalMark> goals = new LinkedHashMap<>(); goals.put(rule.id(), GoalMark.SAVED);
        var a = account("a", "l", live(Map.of(1, 1)), 99, Quest.NOT_STARTED, Map.of(), Spellbook.UNKNOWN, goals);
        var o = NoodlesAdvisor.opportunity(a);
        assertEquals(rule.id(), o.pinnedGoalId());
        assertEquals(Map.of(rule.id(), GoalMark.SAVED), a.snapshot().goals());
        assertFalse(o.status() == EvidenceStatus.VERIFIED);
        assertEquals(EvidenceStatus.UNAVAILABLE, NoodlesAdvisor.opportunity(AccountEvidenceContext.signedOut()).status());
        var noQuests = new AccountEvidenceContext("a", "l", 1, null, new Snapshot(true, Map.of(), Map.of(), BankStatus.UNAVAILABLE, Set.of()), null, null, null);
        assertEquals(EvidenceStatus.UNAVAILABLE, NoodlesAdvisor.opportunity(noQuests).status());
    }
    // 12
    @Test void weeklyXpIsNeverInventedOrDoubleCounted() throws Exception {
        LocalDate week = LocalDate.of(2026, 11, 2);
        assertEquals(EvidenceStatus.UNAVAILABLE, ProgressProjections.weeklyXp(week, Optional.empty()).status());
        assertTrue(ProgressProjections.weeklyXp(week, Optional.empty()).xp().isEmpty());
        var dir = Files.createTempDirectory("burrow-weekly");
        var service = new SessionHistoryService(dir);
        assertEquals(EvidenceStatus.UNAVAILABLE, ProgressProjections.weeklyXp(service, "a", week, null, null).status(), "unbound");
        service.bind("a");
        assertTrue(ProgressProjections.weeklyXp(service, "a", week, null, null).xp().isEmpty(), "no rows yet");
        var contribution = new WeeklyProgress.Contribution(Map.of("Magic", 100L), Map.of(), 0, 0, Map.of(), Set.of());
        assertTrue(service.saveWeekly("a", "login-1", Map.of(week, contribution)));
        assertEquals(100L, ProgressProjections.weeklyXp(service, "a", week, null, null).xp().getAsLong());
        var later = new WeeklyProgress.Contribution(Map.of("Magic", 150L), Map.of(), 0, 0, Map.of(), Set.of());
        assertEquals(150L, ProgressProjections.weeklyXp(service, "a", week, "login-1", later).xp().getAsLong(), "live replaces the saved login");
        assertEquals(EvidenceStatus.UNAVAILABLE, ProgressProjections.weeklyXp(service, "b", week, null, null).status(), "other account");
    }
    // 13
    static Summary session(int n, List<CompletedSessionHistory.Drop> drops, Map<String,Long> kc, Map<String,Long> xp) {
        return new Summary(CompletedSessionHistory.SCHEMA, "login-" + n, n * 1000L, n * 1000L + 500, CompletedSessionHistory.CONFIRMED_LOGOUT, FeedState.OBSERVED, xp, FeedState.OBSERVED, kc, Map.of(), drops, FeedState.UNAVAILABLE, List.of(), List.of());
    }
    static CompletedSessionHistory.Drop tableDrop() {
        var encounter = RareDropTable.encounters("Vorkath").get(0);
        var d = encounter.drops().get(0);
        return new CompletedSessionHistory.Drop(d.itemId(), d.name(), "Vorkath", 1);
    }
    @Test void biggestWinUsesOnlyTheLastTenCompletedSessions() {
        List<Summary> sessions = new ArrayList<>();
        sessions.add(session(1, List.of(tableDrop()), Map.of(), Map.of()));
        for (int n = 2; n <= 12; n++) sessions.add(session(n, List.of(), Map.of("Vorkath", (long) n), Map.of("Magic", 5L)));
        var win = ProgressProjections.biggestWin(sessions, "a", null).win().orElseThrow();
        assertEquals(BiggestWin.Kind.VERIFIED_KILLS, win.kind(), "the drop is outside the last ten");
        assertEquals(12, win.amount());
        var withDrop = new ArrayList<>(sessions); withDrop.add(session(13, List.of(tableDrop()), Map.of(), Map.of()));
        assertEquals(BiggestWin.Kind.RARE_DROP, ProgressProjections.biggestWin(withDrop, "a", null).win().orElseThrow().kind());
        Collections.reverse(withDrop);
        assertEquals(BiggestWin.Kind.RARE_DROP, ProgressProjections.biggestWin(withDrop, "a", null).win().orElseThrow().kind(), "input order does not matter");
    }
    @Test void unknownRarityCannotRankAndNoEvidenceGivesNothing() {
        var mystery = new CompletedSessionHistory.Drop(999999, "Mystery", "Vorkath", 1);
        assertTrue(ProgressProjections.events(List.of(session(1, List.of(mystery), Map.of(), Map.of()))).isEmpty());
        var none = ProgressProjections.biggestWin(List.of(), "a", null);
        assertTrue(none.win().isEmpty());
        assertEquals(EvidenceStatus.UNKNOWN, none.status());
    }
    @Test void pinHoldsUntilEvictedAndOnlyForItsAccount() {
        List<Summary> sessions = List.of(session(2, List.of(), Map.of(), Map.of("Magic", 5L)), session(1, List.of(tableDrop()), Map.of(), Map.of()));
        var events = ProgressProjections.events(sessions);
        var xp = events.stream().filter(e -> e.kind() == BiggestWin.Kind.XP_GAIN).findFirst().orElseThrow();
        var pin = new ProgressProjections.Pin("a", xp.eventId());
        var pinned = ProgressProjections.biggestWin(sessions, "a", pin);
        assertEquals(xp.eventId(), pinned.win().orElseThrow().eventId());
        assertTrue(pinned.win().get().pinned());
        var other = ProgressProjections.biggestWin(sessions, "b", pin);
        assertEquals(BiggestWin.Kind.RARE_DROP, other.win().orElseThrow().kind(), "another account ignores the pin");
        var evicted = ProgressProjections.biggestWin(List.of(sessions.get(1)), "a", pin);
        assertTrue(evicted.pinCleared());
        assertEquals(BiggestWin.Kind.RARE_DROP, evicted.win().orElseThrow().kind());
    }
    // 14
    @Test void taskCompletionIsUnknownWithoutGameEvidence() {
        int tasks = 0, tips = 0;
        for (var e : BossRegistry.all()) for (var t : ChronicleProvider.detail(e.id()).orElseThrow().tasks()) {
            tasks++; if (t.hasTip()) tips++;
            assertEquals(ChronicleDetail.Completion.UNKNOWN, t.completion(), t.name());
        }
        assertEquals(CombatTaskGuide.taskCount() + 2 * CombatTaskGuide.forBoss("Blood Moon").size(), tasks, "the other Moons repeat shared tasks");
        assertEquals(290, CombatTaskGuide.taskCount());
        assertTrue(tips >= 77);
        var claimed = new TaskCompletionEvidence(Map.of("Unnecessary Optimisation", "game varp"));
        assertEquals(ChronicleDetail.Completion.UNKNOWN, ChronicleProvider.detail("Kraken", AccountEvidenceContext.signedOut(), null, claimed).orElseThrow().tasks().stream()
            .filter(t -> t.name().equals("Unnecessary Optimisation")).findFirst().orElseThrow().completion(), "no account, no completion");
        assertEquals(ChronicleDetail.Completion.COMPLETE, ChronicleProvider.detail("Kraken", account(null), null, claimed).orElseThrow().tasks().stream()
            .filter(t -> t.name().equals("Unnecessary Optimisation")).findFirst().orElseThrow().completion());
    }
    // 15
    @Test void accountBDoesNotInheritAccountA() {
        Map<Integer,Integer> own = Map.of(526, 1);
        var a = SpoonsTipService.select(account(live(own)), null);
        var closed = SpoonsTipService.close(a.state(), SpoonsTipService.Closed.DISMISSED);
        var b = SpoonsTipService.select(account("b", "login-1", live(own), 99, Quest.FINISHED, Map.of(), Spellbook.UNKNOWN, Map.of()), closed);
        assertTrue(b.tip().isPresent(), "A's dismissal does not reach B");
        assertEquals("b", b.state().accountKey());
        var nav = NavigationReducer.apply(NavigationState.initial(), new OpenBossDetail("Kraken", "9")).state();
        assertEquals(NavigationState.initial(), NavigationReducer.apply(nav, new AccountChanged()).state());
        var out = AccountEvidenceContext.signedOut();
        assertEquals(EvidenceStatus.UNAVAILABLE, SpoonsTipService.select(out, closed).status());
        assertEquals(AccountEvidenceContext.Check.UNKNOWN, out.owns(1));
        var detail = ChronicleProvider.detail("Vorkath", out, evidence(500), TaskCompletionEvidence.none()).orElseThrow();
        assertTrue(detail.encounters().stream().allMatch(e -> e.eligibleKc().isEmpty()), "no KC without a signed-in account");
        var feed = HomeFeedProvider.build(out, null, LocalDate.of(2026, 11, 2), null, null, closed, null, (b2, m, t) -> { throw new AssertionError("planner reached"); });
        assertTrue(feed.spoons().isEmpty());
        assertTrue(feed.banditHunts().isEmpty());
        assertEquals(EvidenceStatus.UNAVAILABLE, feed.weeklyXp().status());
        assertFalse(account(null).toString().contains("login-1"));
    }
    // 16
    @Test void staleWorkCannotOverwriteALaterLogin() {
        var gate = new ExperienceSnapshotGate<String>();
        var old = gate.begin("a");
        var next = gate.begin("b");
        assertFalse(gate.publish(old, "stale"));
        assertTrue(gate.current("b").isEmpty());
        assertTrue(gate.publish(next, "fresh"));
        assertEquals("fresh", gate.current("b").orElseThrow());
        assertTrue(gate.current("a").isEmpty(), "another account reads nothing");
        var again = gate.begin("b");
        assertFalse(gate.publish(next, "late"), "same account, older generation");
        assertTrue(gate.current("b").isEmpty(), "new generation requires fresh evidence even on the same account");
        assertTrue(gate.publish(again, "newest"));
        gate.clear();
        assertTrue(gate.current("b").isEmpty());
    }
    // 17
    @Test void cp9DataIsUntouched() {
        assertEquals(60, BossRegistry.selectableIds().size());
        assertEquals("165f0781ab4e0e738235833e51dee04c21f30825cc8b3ab421c051e48e6dbe98", TieredOutfitterCatalog.DATASET_SHA256);
        assertEquals(290, CombatTaskGuide.taskCount());
    }
}
