package gg.feral.burrow.pathfinder;

import gg.feral.burrow.core.BankEvidence;
import java.util.Map;

/**
 * PQA-W04 read-only bridge from the existing accepted RuneLite bank witness into Pathfinder.
 * Uses exact account and session fences; never reads the client, writes to game state, or persists data.
 * COMPLETE is limited to a currently open, stable, recent whole-bank observation.
 * Closed or uncertain evidence cannot prove either current presence or confirmed absence.
 */
public final class PathfinderAccountTruth {
    private PathfinderAccountTruth() {}
    /** Local engineering safety limit, NOT a Wiki fact or OSRS mechanic. */
    public static final long MAX_OBSERVATION_AGE_MS = 120_000L;

    public record Context(long liveAccount, long sourceAccount, Object liveSession, Object sourceSession,
                          boolean sessionActive, boolean bankOpen, boolean recapturePending,
                          boolean revalidationPending, boolean carriedDirty, boolean sameOpenEpoch,
                          long nowMillis) {}

    public enum Verdict { ACCEPTED, NO_EVIDENCE, NOT_AUTHENTICATED, WRONG_ACCOUNT,
        WRONG_SESSION, BANK_CLOSED, CHANGING_OR_UNVERIFIED, OLD_OR_FUTURE_READ }

    public record Result(Verdict verdict, SuggestionEngine.ResourceEvidence resources) {
        public boolean accepted() { return verdict == Verdict.ACCEPTED && resources != null; }
    }

    public static Result capture(BankEvidence bank, Context c) {
        if(bank==null || c==null) return new Result(Verdict.NO_EVIDENCE,null);
        if(!c.sessionActive() || c.liveAccount()==-1L || c.sourceAccount()==-1L ||
            c.liveSession()==null || c.sourceSession()==null)
            return new Result(Verdict.NOT_AUTHENTICATED,null);
        if(c.liveAccount()!=c.sourceAccount()) return new Result(Verdict.WRONG_ACCOUNT,null);
        if(c.liveSession()!=c.sourceSession()) return new Result(Verdict.WRONG_SESSION,null);
        if(!c.bankOpen() || !bank.bankStillOpen()) return new Result(Verdict.BANK_CLOSED,null);
        if(c.recapturePending() || c.revalidationPending() || c.carriedDirty() || !c.sameOpenEpoch() ||
           bank.stale() || bank.population()==BankEvidence.Population.SINGLE_READ || !bank.planningInput())
            return new Result(Verdict.CHANGING_OR_UNVERIFIED,null);
        if(bank.observedAtMillis()<=0 || c.nowMillis()<bank.observedAtMillis() ||
           c.nowMillis()-bank.observedAtMillis()>MAX_OBSERVATION_AGE_MS)
            return new Result(Verdict.OLD_OR_FUTURE_READ,null);
        Map<Integer,Integer> combined;
        try { combined=bank.combined(); }
        catch(ArithmeticException overflow) { // stacked counts must never wrap around to a misleading number
            return new Result(Verdict.CHANGING_OR_UNVERIFIED,null);
        }
        // STABLE_READ lacks an independent tab-count completeness witness. Its positive
        // entries are evidence of presence, but ABSENCE must remain UNKNOWN.
        boolean complete=bank.population()==BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT;
        // No price conversion. Coin denomination ID 995 only. 'Food' means only three
        // independently identified cooked-food IDs, not a combat trip budget.
        Integer gp=complete?combined.getOrDefault(995,0):combined.get(995);
        long food=(long)combined.getOrDefault(3144,0)+combined.getOrDefault(7946,0)+combined.getOrDefault(385,0);
        Integer foodCount=(complete||food>0)&&food<=Integer.MAX_VALUE?(int)food:null;
        // An undifferentiated "runes" total is misleading: spell requirements are per-rune-ID.
        return new Result(Verdict.ACCEPTED,
            new SuggestionEngine.ResourceEvidence(true,true,complete,combined,gp,foodCount,null));
    }
}
