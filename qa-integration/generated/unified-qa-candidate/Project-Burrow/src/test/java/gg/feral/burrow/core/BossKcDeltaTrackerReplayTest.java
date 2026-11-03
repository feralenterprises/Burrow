package gg.feral.burrow.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import gg.feral.burrow.core.BossKcDeltaTracker.Observation;
import gg.feral.burrow.core.BossKcDeltaTracker.Outcome;
import gg.feral.burrow.core.BossKcDeltaTracker.Reliability;
import gg.feral.burrow.core.BossKcDeltaTracker.Status;

/** No JUnit or RuneLite dependency. Run with java -ea after javac --release 17. */
public final class BossKcDeltaTrackerReplayTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        Path fixture = args.length == 0
                ? Path.of("src/test/resources/gg/feral/burrow/core/bur-0025-replay.tsv")
                : Path.of(args[0]);
        int rows = replay(fixture);
        edgeCases();
        rejectedResponseIsolation();
        System.out.printf("PASS: BUR-0025 isolated KC core: %d fixture events, %d assertions (%s)%n",
                rows, checks, fixture.getFileName());
    }

    private static int replay(Path fixture) throws Exception {
        BossKcDeltaTracker t = new BossKcDeltaTracker();
        List<String> lines = Files.readAllLines(fixture);
        int executed = 0;
        for (int lineNo = 1; lineNo <= lines.size(); lineNo++) {
            String line = lines.get(lineNo - 1);
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] x = line.split("\\|", -1);
            if (x.length != 14) throw new AssertionError("fixture columns at line " + lineNo + ": " + x.length);
            String step = x[0], action = x[1], account = x[2], session = x[3];
            long sg = Long.parseLong(x[4]);
            String epoch = x[5];
            long eg = Long.parseLong(x[6]);
            String boss = x[7];
            long seq = Long.parseLong(x[8]);
            String quality = x[9], kcText = x[10], expected = x[11];
            long delta = Long.parseLong(x[12]), total = Long.parseLong(x[13]);
            switch (action) {
                case "LOGIN" -> check(t.openSession(account, session, sg) == Boolean.parseBoolean(expected), step);
                case "EPOCH" -> check(t.openEpoch(account, session, sg, epoch, eg) == Boolean.parseBoolean(expected), step);
                case "LOGOUT" -> check(t.logout(account, session, sg) == Boolean.parseBoolean(expected), step);
                case "SAMPLE" -> {
                    Long kc = kcText.equals("-") ? null : Long.valueOf(kcText);
                    Outcome o = t.observe(new Observation(account, session, sg, epoch, eg, boss, seq,
                            Reliability.valueOf(quality), kc));
                    check(o.status() == Status.valueOf(expected), step + " status " + o.status());
                    check(o.delta() == delta, step + " delta " + o.delta());
                    check(o.sessionTotal() == total, step + " total " + o.sessionTotal());
                    check(o.bossId().equals(boss), step + " boss identity");
                    System.out.printf("EVENT\t%s\t%s\t%s\tdelta=%d\tsessionTotal=%d%n",
                            step, boss, o.status(), o.delta(), o.sessionTotal());
                }
                default -> throw new AssertionError("Unknown action: " + action);
            }
            executed++;
        }
        check(t.sessionTotal("Scurrius") == 0, "logout clears remembered totals");
        check(!t.isSessionOpen(), "fixture ends offline");
        return executed;
    }

    private static void edgeCases() {
        BossKcDeltaTracker t = new BossKcDeltaTracker();
        check(t.openSession("A", "login#1", 1), "start login");
        check(t.openEpoch("A", "login#1", 1, "world-301", 1), "start epoch");
        Outcome odd = t.observe(new Observation("A", "login#1", 1, "world-301", 1,
                "Boss: test/EXACT", 1, Reliability.RELIABLE, 3L));
        check(odd.bossId().equals("Boss: test/EXACT"), "exact boss ID preserved");
        check(odd.status() == Status.BASELINED, "first observation never counts historical KC");
        check(!t.openEpoch("A", "login#1", 1, "world-301", 2), "old epoch ID never resurrected");
        check(!t.openEpoch("A", "login#1", 1, "world-302", 1), "stale epoch generation rejected");
        check(!t.openSession("A", "login#1", 1), "replayed login cannot clear state");
        check(t.logout("A", "login#1", 1), "logout success");
        check(!t.logout("A", "login#1", 1), "repeated logout rejected");
        check(!t.openSession("A", "login#1", 1), "old login cannot resurrect after logout");
        expectBad(() -> new Observation("A", "S", 1, "E", 1, "B", 1, Reliability.RELIABLE, -1L));
        expectBad(() -> new Observation("A", "S", 1, "E", 1, "B", 1, Reliability.RELIABLE, null));
        expectBad(() -> new Observation("A", "S", 1, "E", 1, " ", 1, Reliability.UNKNOWN, null));
        expectBad(() -> new Observation("A", "S", 1, "E", 1, "B", 0, Reliability.UNKNOWN, null));
    }

    private static void rejectedResponseIsolation() {
        BossKcDeltaTracker t = new BossKcDeltaTracker();
        check(t.openSession("account-A", "login-A", 1), "privacy test login");
        check(t.openEpoch("account-A", "login-A", 1, "world-A", 1), "privacy test epoch");
        Outcome initial = t.observe(new Observation("account-A", "login-A", 1,
                "world-A", 1, "Scurrius", 1, Reliability.RELIABLE, 100L));
        check(initial.status() == Status.BASELINED, "privacy test baseline");
        Outcome gain = t.observe(new Observation("account-A", "login-A", 1,
                "world-A", 1, "Scurrius", 2, Reliability.RELIABLE, 105L));
        check(gain.status() == Status.GAIN && gain.delta() == 5 && gain.sessionTotal() == 5,
                "accepted Scurrius gain retained");

        assertRejected(t.observe(new Observation("account-B", "login-B", 1,
                "world-A", 1, "Scurrius", 3, Reliability.RELIABLE, 900L)),
                Status.WRONG_IDENTITY, "foreign account after nonzero gain");
        assertRejected(t.observe(new Observation("account-A", "wrong-login", 1,
                "world-A", 1, "Scurrius", 3, Reliability.RELIABLE, 901L)),
                Status.WRONG_IDENTITY, "foreign login after nonzero gain");
        assertRejected(t.observe(new Observation("account-A", "login-A", 2,
                "world-A", 1, "Scurrius", 3, Reliability.RELIABLE, 902L)),
                Status.WRONG_IDENTITY, "stale/foreign login generation after nonzero gain");
        assertRejected(t.observe(new Observation("account-A", "login-A", 1,
                "stale-world", 1, "Scurrius", 3, Reliability.RELIABLE, 903L)),
                Status.WRONG_EPOCH, "old epoch ID after nonzero gain");
        assertRejected(t.observe(new Observation("account-A", "login-A", 1,
                "world-A", 9, "Scurrius", 3, Reliability.RELIABLE, 904L)),
                Status.WRONG_EPOCH, "foreign epoch generation after nonzero gain");
        assertRejected(t.observe(new Observation("account-A", "login-A", 1,
                "world-A", 1, "Scurrius", 2, Reliability.RELIABLE, 105L)),
                Status.OLD_OR_REPLAYED, "same-epoch replay after nonzero gain");

        check(t.sessionTotal("Scurrius") == 5, "rejected events do not mutate active totals");
        Outcome next = t.observe(new Observation("account-A", "login-A", 1,
                "world-A", 1, "Scurrius", 3, Reliability.RELIABLE, 106L));
        check(next.status() == Status.GAIN && next.delta() == 1 && next.sessionTotal() == 6,
                "good event still counts after rejection");
        check(t.openEpoch("account-A", "login-A", 1, "world-B", 2), "open real new epoch");
        assertRejected(t.observe(new Observation("account-A", "login-A", 1,
                "world-A", 1, "Scurrius", 4, Reliability.RELIABLE, 999L)),
                Status.WRONG_EPOCH, "old-epoch leak after legitimate world hop");
        check(t.sessionTotal("Scurrius") == 6, "world-hop rejects preserve totals");
        Outcome fresh = t.observe(new Observation("account-A", "login-A", 1,
                "world-B", 2, "Scurrius", 1, Reliability.RELIABLE, 250L));
        check(fresh.status() == Status.BASELINED && fresh.sessionTotal() == 6,
                "real new epoch baseline keeps current-login total");
        check(t.logout("account-A", "login-A", 1), "privacy logout");
        assertRejected(t.observe(new Observation("account-A", "login-A", 1,
                "world-B", 2, "Scurrius", 2, Reliability.RELIABLE, 251L)),
                Status.NO_SESSION, "post-logout observation");
        check(t.sessionTotal("Scurrius") == 0, "logout clears live-session total");
    }

    private static void assertRejected(Outcome outcome, Status status, String name) {
        check(outcome.status() == status, name + " status");
        check(outcome.delta() == 0, name + " delta");
        check(outcome.sessionTotal() == 0, name + " must not expose any active-session total");
    }

    private static void expectBad(Runnable test) {
        try { test.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Expected invalid observation to fail closed");
    }

    private static void check(boolean pass, String reason) {
        checks++;
        if (!pass) throw new AssertionError(reason);
    }
}
