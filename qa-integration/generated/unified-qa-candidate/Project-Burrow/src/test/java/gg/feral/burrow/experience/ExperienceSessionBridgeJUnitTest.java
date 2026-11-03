package gg.feral.burrow.experience;

import gg.feral.burrow.core.AccountContext;
import gg.feral.burrow.core.BankEvidence;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExperienceSessionBridgeJUnitTest {
    private static SuggestionEngine.Snapshot snapshot() {
        return new SuggestionEngine.Snapshot(true,Map.of("Crafting",80),Map.of(),
            SuggestionEngine.BankStatus.UNAVAILABLE,Set.of());
    }
    private static BankEvidence bank() { return BankEvidence.fixture(Map.of(1623,2)); }
    @Test void rejectsUnknownIdentityOrUnverifiedSnapshot() {
        var b=new ExperienceSessionBridge();
        assertFalse(b.begin("", "first"));
        assertFalse(b.observe("acct","first",snapshot(),bank(),AccountContext.UNKNOWN));
        assertTrue(b.begin("acct","first"));
        assertFalse(b.observe("acct","first",new SuggestionEngine.Snapshot(false,Map.of(),Map.of(),
            SuggestionEngine.BankStatus.UNAVAILABLE,Set.of()),bank(),AccountContext.UNKNOWN));
        assertTrue(b.current("acct","first").isEmpty());
    }
    @Test void sameLoginHopRetainsLoginButRevokesSnapshotUntilRefreshed() {
        var b=new ExperienceSessionBridge();
        b.begin("acct","first");
        assertTrue(b.observe("acct","first",snapshot(),bank(),AccountContext.UNKNOWN));
        long before=b.current("acct","first").orElseThrow().generation();
        assertFalse(b.begin("acct","first"));
        assertTrue(b.current("acct","first").isPresent());
        b.pause();
        assertTrue(b.current("acct","first").isEmpty());
        assertTrue(b.resume("acct","first"));
        assertTrue(b.current("acct","first").isEmpty());
        assertTrue(b.observe("acct","first",snapshot(),null,AccountContext.UNKNOWN));
        var current=b.current("acct","first").orElseThrow();
        assertTrue(current.generation()>before);
        assertEquals(AccountEvidenceContext.BankState.UNKNOWN,current.bankState());
    }
    @Test void secondLoginSameAccountNeverInheritsFirstLogin() {
        var b=new ExperienceSessionBridge();
        b.begin("acct","first");b.observe("acct","first",snapshot(),bank(),AccountContext.UNKNOWN);
        b.end();
        assertTrue(b.begin("acct","second"));
        assertTrue(b.current("acct","second").isEmpty());
        assertFalse(b.observe("acct","first",snapshot(),bank(),AccountContext.UNKNOWN));
        assertTrue(b.observe("acct","second",snapshot(),null,AccountContext.UNKNOWN));
        assertTrue(b.current("acct","first").isEmpty());
    }
    @Test void accountSwitchAndBankMutationInvalidateImmediately() {
        var b=new ExperienceSessionBridge();
        b.begin("A","loginA");b.observe("A","loginA",snapshot(),bank(),AccountContext.UNKNOWN);
        b.invalidateEvidence();
        assertTrue(b.current("A","loginA").isEmpty());
        b.begin("B","loginB");
        assertFalse(b.observe("A","loginA",snapshot(),bank(),AccountContext.UNKNOWN));
        assertTrue(b.current("A","loginA").isEmpty());
        assertTrue(b.current("B","loginB").isEmpty());
        b.observe("B","loginB",snapshot(),null,AccountContext.UNKNOWN);
        assertEquals(AccountEvidenceContext.BankState.UNKNOWN,b.current("B","loginB").orElseThrow().bankState());
    }
    @Test void unavailableBankStatusCannotPublishOwnedItemsFromOldBankEvidence() {
        var b=new ExperienceSessionBridge();
        b.begin("acct","login");
        // Deliberately inconsistent inputs: the host has not established same-account
        // bank provenance, but an older bank object still contains a positive item.
        assertTrue(b.observe("acct","login",snapshot(),bank(),AccountContext.UNKNOWN));
        var result=b.current("acct","login").orElseThrow();
        assertEquals(AccountEvidenceContext.BankState.UNKNOWN,result.bankState());
        assertEquals(AccountEvidenceContext.Check.UNKNOWN,result.owns(1623));
        assertNull(result.bank());
    }
    @Test void verifiedBankStatusMayPublishSameAccountObservedItem() {
        var b=new ExperienceSessionBridge();
        b.begin("acct","login");
        var authenticated=new SuggestionEngine.Snapshot(true,Map.of("Crafting",80),Map.of(),
            SuggestionEngine.BankStatus.VERIFIED_SAME_ACCOUNT,Set.of(1623));
        assertTrue(b.observe("acct","login",authenticated,bank(),AccountContext.UNKNOWN));
        var result=b.current("acct","login").orElseThrow();
        assertEquals(AccountEvidenceContext.BankState.LIVE_OBSERVED,result.bankState());
        assertEquals(AccountEvidenceContext.Check.MET,result.owns(1623));
        assertEquals(AccountEvidenceContext.Check.UNMET,result.owns(1621));
    }
    @Test void mutationAfterAuthenticatedBankRetractsPositiveOwnership() {
        var b=new ExperienceSessionBridge();
        b.begin("acct","login");
        var authenticated=new SuggestionEngine.Snapshot(true,Map.of(),Map.of(),
            SuggestionEngine.BankStatus.VERIFIED_SAME_ACCOUNT,Set.of(1623));
        b.observe("acct","login",authenticated,bank(),AccountContext.UNKNOWN);
        assertEquals(AccountEvidenceContext.Check.MET,b.current("acct","login").orElseThrow().owns(1623));
        b.invalidateEvidence();
        assertTrue(b.current("acct","login").isEmpty());
        assertTrue(b.observe("acct","login",snapshot(),bank(),AccountContext.UNKNOWN));
        assertEquals(AccountEvidenceContext.Check.UNKNOWN,b.current("acct","login").orElseThrow().owns(1623));
    }
    @Test void gateRetiresSameAccountSnapshotOnBegin() {
        var g=new ExperienceSnapshotGate<String>();
        var t=g.begin("A");assertTrue(g.publish(t,"old"));
        var newer=g.begin("A");
        assertTrue(g.current("A").isEmpty());
        assertFalse(g.publish(t,"late"));
        assertTrue(g.publish(newer,"new"));
        assertEquals("new",g.current("A").orElseThrow());
    }
    @Test void scopedHomeHasNoOwnerReadyClaimWithoutPreparedPlan() {
        var b=new ExperienceSessionBridge();
        b.begin("acct","first");b.observe("acct","first",snapshot(),bank(),AccountContext.UNKNOWN);
        var account=b.current("acct","first").orElseThrow();
        var feed=HomeFeedProvider.buildScoped(account,null,java.time.LocalDate.of(2026,11,2),null,null,
            null,null,(boss,method,tier,ticket) -> null);
        assertTrue(feed.banditHunts().isEmpty());
        assertEquals(EvidenceStatus.UNAVAILABLE,feed.weeklyXp().status());
        assertEquals(EvidenceStatus.UNAVAILABLE,feed.biggestWin().status());
        assertEquals(account.generation(),feed.generation());
    }
    @Test void signedOutCannotPublishOrReturnPlayerAccount() {
        var b=new ExperienceSessionBridge();
        b.begin("A","loginA"); b.observe("A","loginA",snapshot(),bank(),AccountContext.UNKNOWN);
        b.end();
        assertTrue(b.current("A","loginA").isEmpty());
        assertFalse(b.observe("A","loginA",snapshot(),bank(),AccountContext.UNKNOWN));
        assertFalse(b.toString().contains("loginA"));
    }
}
