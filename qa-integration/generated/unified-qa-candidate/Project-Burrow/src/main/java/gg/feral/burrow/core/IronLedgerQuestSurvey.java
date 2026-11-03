package gg.feral.burrow.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Port of Iron Ledger's bounded quest-discovery loop state. Client supplies observed statuses. */
public final class IronLedgerQuestSurvey {
    public record Snapshot(Map<String, String> quests, boolean complete, int failures,
                           int scanned, int total) {
        public Snapshot { quests = Map.copyOf(quests); }
        public long completed() { return quests.values().stream().filter("FINISHED"::equals).count(); }
        public long inProgress() { return quests.values().stream().filter("IN_PROGRESS"::equals).count(); }
    }
    private final Map<String,String> quests = new LinkedHashMap<>();
    private int scanned, total, failures;
    private boolean complete;
    public void reset(int total) {
        if (total < 0) throw new IllegalArgumentException("total");
        this.total=total;scanned=0;failures=0;complete=false;quests.clear();
    }
    public void observe(String name, String state) {
        if (complete || scanned >= total) throw new IllegalStateException("quest survey closed");
        scanned++;
        if (name==null || name.isBlank() || state==null ||
            !(state.equals("FINISHED") || state.equals("IN_PROGRESS") || state.equals("NOT_STARTED")))
            failures++;
        else quests.put(name,state);
    }
    public void finish() {
        if (scanned!=total) throw new IllegalStateException("incomplete survey");
        complete=true;
    }
    public Snapshot snapshot() { return new Snapshot(quests,complete && failures==0,failures,scanned,total); }
    public boolean scannedAll() { return complete; }
    public int scanned() { return scanned; }
}
