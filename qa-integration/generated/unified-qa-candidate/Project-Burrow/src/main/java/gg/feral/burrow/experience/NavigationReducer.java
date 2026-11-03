package gg.feral.burrow.experience;

import gg.feral.burrow.core.BossRegistry;
import gg.feral.burrow.core.TieredOutfitterCatalog;
import gg.feral.burrow.experience.NavigationCommand.*;
import gg.feral.burrow.experience.NavigationState.*;
import java.util.List;
import java.util.Locale;

/** Pure transitions. An invalid command returns the same state with a notice. */
public final class NavigationReducer {
    public record Result(NavigationState state, String notice) {}
    private NavigationReducer() {}

    public static Result apply(NavigationState s, NavigationCommand command) {
        // Java 17 has no pattern switch, so this stays a plain chain.
        if (command instanceof OpenTab c) return ok(new NavigationState(c.tab(), null, s.browse(), s.chronicleBossId(), s.outfitter()));
        if (command instanceof OpenChronicleList) return ok(new NavigationState(s.topTab(), ChronicleRoute.BROWSE, s.browse(), s.chronicleBossId(), s.outfitter()));
        if (command instanceof UpdateBrowse c) return ok(new NavigationState(s.topTab(), s.chronicleRoute(), c.browse() == null ? BrowseMemory.DEFAULT : c.browse(), s.chronicleBossId(), s.outfitter()));
        if (command instanceof OpenBossDetail c) return openDetail(s, c);
        if (command instanceof BackToList) return ok(new NavigationState(s.topTab(), s.chronicleRoute() == ChronicleRoute.DETAIL ? ChronicleRoute.BROWSE : null, s.browse(), s.chronicleBossId(), s.outfitter()));
        if (command instanceof CloseChronicle) return ok(new NavigationState(s.topTab(), null, s.browse(), s.chronicleBossId(), s.outfitter()));
        if (command instanceof OpenBossInOutfitter) return openInOutfitter(s);
        if (command instanceof SelectInOutfitter c) return selectOutfitter(s, c.selection());
        if (command instanceof AccountChanged) return ok(NavigationState.initial());
        return new Result(s, "Unknown command");
    }

    private static Result ok(NavigationState s) { return new Result(s, ""); }

    private static Result openDetail(NavigationState s, OpenBossDetail c) {
        var boss = ChronicleProvider.resolve(c.boss());
        if (boss.isEmpty()) return new Result(s, "Unknown boss");
        var b = s.browse();
        var browse = new BrowseMemory(b.query(), b.group(), b.sort(), c.scrollKey() == null ? b.scrollKey() : c.scrollKey());
        return ok(new NavigationState(s.topTab(), ChronicleRoute.DETAIL, browse, boss.get(), s.outfitter()));
    }

    private static Result openInOutfitter(NavigationState s) {
        if (s.chronicleBossId() == null) return new Result(s, "No boss selected");
        var intent = ChronicleProvider.outfitterIntent(s.chronicleBossId());
        if (!intent.canOpen()) return new Result(s, intent.reason());
        String boss = intent.bossId();
        List<String> methods = TieredOutfitterCatalog.methods(boss);
        var old = s.outfitter();
        String method = old.method() != null && methods.contains(old.method()) ? old.method() : sameStyle(boss, old, methods);
        var tier = old.tier();
        if (TieredOutfitterCatalog.find(boss, method, tier) == null) {
            tier = null;
            for (var t : TieredOutfitterCatalog.Tier.values()) if (TieredOutfitterCatalog.find(boss, method, t) != null) tier = t;
            if (tier == null) return new Result(s, "No gear plan for this method");
        }
        var chosen = new OutfitterSelection(boss, method, tier, old.fill(), old.bestBank() && BossRegistry.bestBankAllowed(boss));
        return new Result(new NavigationState(TopTab.OUTFITTER, null, s.browse(), s.chronicleBossId(), chosen), intent.reason());
    }

    private static String sameStyle(String boss, OutfitterSelection old, List<String> methods) {
        if (old.bossId() != null && old.method() != null) {
            String style = TieredOutfitterCatalog.style(old.bossId(), old.method());
            for (String m : methods)
                if (style != null && style.toLowerCase(Locale.ROOT).equals(String.valueOf(TieredOutfitterCatalog.style(boss, m)).toLowerCase(Locale.ROOT))) return m;
        }
        return methods.get(0);
    }

    private static Result selectOutfitter(NavigationState s, OutfitterSelection selection) {
        if (selection == null || selection.bossId() == null || BossRegistry.find(selection.bossId()).isEmpty()) return new Result(s, "Unknown boss");
        var safe = new OutfitterSelection(selection.bossId(), selection.method(), selection.tier(), selection.fill(),
            selection.bestBank() && BossRegistry.bestBankAllowed(selection.bossId()));
        return ok(new NavigationState(s.topTab(), s.chronicleRoute(), s.browse(), s.chronicleBossId(), safe));
    }
}
