package gg.feral.burrow.ui;

import gg.feral.burrow.core.PathfinderAccountArchive;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.IntFunction;

/** PQA-W07 isolated read-only Bank Memory display model. No current-stock inference. */
public final class BankMemoryViewModel {
    private BankMemoryViewModel() {}
    public enum Freshness { NOT_SCANNED, UNDER_SEVEN_DAYS, SEVEN_DAYS_OR_OLDER, CLOCK_CONFLICT }
    public record Row(int exactItemId,int observedQuantity,String name,String variantState) {}
    public record Display(Freshness freshness,String asOf,boolean currentStockProven,
                          List<Row> rows,String notice,boolean suggestManualScan) {
        public Display {rows=List.copyOf(rows);}
    }
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    /**
     * Clear account-boundary convenience entrypoint. The host must also clear any
     * previously rendered view during logout or account switch before calling it.
     */
    public static Display renderForAccount(long activeAccountHash,long memoryOwnerHash,
                                           Optional<PathfinderAccountArchive.BankMemory> memory,long now,
                                           IntFunction<String> reviewedItemNames) {
        if(activeAccountHash==-1L || memoryOwnerHash!=activeAccountHash)
            return new Display(Freshness.NOT_SCANNED,"Account unavailable",false,List.of(),
                "No bank data is shown because the active account cannot be verified.",true);
        return render(memory,now,reviewedItemNames);
    }
    /** Missing names are ID-only; the original item ID is never silently normalized. */
    public static Display render(Optional<PathfinderAccountArchive.BankMemory> memory,long now,
                                 IntFunction<String> reviewedItemNames) {
        if(memory==null || memory.isEmpty() || now<=0)
            return new Display(Freshness.NOT_SCANNED,"Never scanned",false,List.of(),
                "Open your bank to capture an authenticated full-bank snapshot. Unknown is not zero.",true);
        var observation=memory.get();
        Freshness freshness=now<observation.observedAtMillis()?Freshness.CLOCK_CONFLICT:
            observation.reminderDue(now)?Freshness.SEVEN_DAYS_OR_OLDER:Freshness.UNDER_SEVEN_DAYS;
        List<Row> rows=new ArrayList<>();
        observation.bankItems().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{
            String reviewed=reviewedItemNames==null?null:reviewedItemNames.apply(e.getKey());
            String label=reviewed==null || reviewed.isBlank()?"Item ID "+e.getKey():reviewed;
            rows.add(new Row(e.getKey(),e.getValue(),label,"State not independently verified"));
        });
        String notice;
        if(observation.bankItems().isEmpty()) {
            notice="Bank was witnessed empty at scan time; current bank is unknown. "
                +"Best in Bank never treats this historical memory as withdrawable stock.";
        } else {
            notice="Historical bank snapshot only (as-of scan). Exact item IDs and quantities "
                +"are preserved as observed, not as live ownership. Best in Bank / green O "
                +"requires a fresh live open-bank validation before claiming withdrawable stock. "
                +(observation.fullTabWitness()
                    ? "Full-tab witness was recorded at scan."
                    : "Full-tab witness was NOT recorded for this memory.");
        }
        if(freshness==Freshness.SEVEN_DAYS_OR_OLDER) {
            notice+=" Reminder: this scan is 7+ days old — open the bank to refresh.";
        }
        String asOf;
        try { asOf=DATE.format(Instant.ofEpochMilli(observation.observedAtMillis())); }
        catch(RuntimeException badClock) {
            freshness=Freshness.CLOCK_CONFLICT;
            asOf="Unusable scan timestamp";
            notice="Historical scan timestamp cannot be verified. Never treat as live stock.";
        }
        // currentStockProven is always false — Display forbids promoting memory into live BiB claims.
        return new Display(freshness,asOf,false,rows,notice,freshness!=Freshness.UNDER_SEVEN_DAYS);
    }
}
