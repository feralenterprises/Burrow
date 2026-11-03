package gg.feral.burrow.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** Bounded Iron Ledger quest state observer, all data sourced from client states. */
public final class IronLedgerQuestSurveyJUnitTest {
  @Test void surveyCountsOnlyActualObservedStates() {
    var q=new IronLedgerQuestSurvey();q.reset(3);
    q.observe("Cook's Assistant","FINISHED");
    q.observe("Dragon Slayer II","IN_PROGRESS");
    q.observe("Monkey Madness II","NOT_STARTED");
    q.finish();
    var v=q.snapshot();
    assertTrue(v.complete());
    assertEquals(1L,v.completed());assertEquals(1L,v.inProgress());
    assertEquals(3,v.scanned());assertEquals(3,v.total());
  }
  @Test void unknownQuestStateFailsClosedAndCannotBecomeCompletion() {
    var q=new IronLedgerQuestSurvey();q.reset(2);
    q.observe("Unknown Quest",null);q.observe("Cook's Assistant","FINISHED");q.finish();
    var v=q.snapshot();assertFalse(v.complete());
    assertEquals(1,v.failures());assertEquals(1L,v.completed());
    assertFalse(v.quests().containsKey("Unknown Quest"));
    q.reset(1);q.observe("Dragon Slayer II","NOT_STARTED");q.finish();
    assertEquals(0L,q.snapshot().completed());
    assertEquals(1,q.snapshot().total());
  }
  @Test void unfinishedSurveyRemainsNotComplete() {
    var q=new IronLedgerQuestSurvey();q.reset(2);
    q.observe("First","FINISHED");
    assertFalse(q.snapshot().complete());
    assertThrows(IllegalStateException.class,q::finish);
  }
}
