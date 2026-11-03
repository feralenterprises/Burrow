package gg.feral.burrow.core;

import java.util.*;

/** Read-only positive gear matches against an expressly observed bank snapshot.
 * No automatic combat-stat ranking or invented ownership: an unobserved item is
 * NOT a confirmed missing item. Never alters Wiki Recommended loadouts. */
public final class BestBankMatches {
    private BestBankMatches() {}
    public enum Status { EXACT, REVIEWED_VARIANT, PARTIAL_STACK, NOT_OBSERVED, UNSPECIFIED }
    public record Match(VerifiedOutfitterCatalog.Item recommendation, int observedId,
                        int observedAmount, Status status) {
        public boolean confirmed() { return status == Status.EXACT || status == Status.REVIEWED_VARIANT; }
    }
    public static List<Match> gear(VerifiedOutfitterCatalog.Profile profile,
                                   Map<Integer,Integer> observed) {
        return gear(profile, observed, Map.of());
    }
    /** CP-1: as {@link #gear(VerifiedOutfitterCatalog.Profile,Map)} plus reviewed {@link NamedVariantRules}
     *  (locked, ornament, imbued, degraded Barrows) over names RuneLite reported for the observed IDs. */
    public static List<Match> gear(VerifiedOutfitterCatalog.Profile profile,
                                   Map<Integer,Integer> observed, Map<Integer,String> names) {
        Objects.requireNonNull(profile); Objects.requireNonNull(observed);
        if (names == null) names = Map.of();
        List<Match> matches = new ArrayList<>();
        for (var item : profile.gear()) {
            if (item == null || item.id() <= 0 || item.amount() < 1) {
                matches.add(new Match(item,-1,0,Status.UNSPECIFIED)); continue;
            }
            int rawId=item.id();
            int matched=rawId;
            int quantity=observed.getOrDefault(rawId,0);
            if (quantity <= 0 && ItemVariantPolicy.isGovernedVariant(rawId)) {
                ItemVariantPolicy.Resolution safe=ItemVariantPolicy.resolve(rawId,observed.keySet());
                if (safe.kind() == ItemVariantPolicy.Kind.EQUIVALENT_VARIANT) {
                    matched=safe.layoutId(); quantity=observed.getOrDefault(matched,0);
                }
            }
            if (quantity <= 0 && gg.feral.burrow.bib.MagicWeaponRules.isTrident(rawId)) {
                int trident=gg.feral.burrow.bib.MagicWeaponRules.ownedVariant(rawId,observed);
                if (trident>0) { matched=trident; quantity=observed.getOrDefault(matched,0); }
            }
            if (quantity <= 0) {
                var named=NamedVariantRules.usable(item,observed,names);
                if (named.isPresent()) { matched=named.get().itemId(); quantity=observed.getOrDefault(matched,0); }
            }
            Status status = quantity <= 0 ? Status.NOT_OBSERVED
                : quantity < item.amount() ? Status.PARTIAL_STACK
                : matched == rawId ? Status.EXACT : Status.REVIEWED_VARIANT;
            matches.add(new Match(item, matched, Math.max(0,quantity), status));
        }
        return List.copyOf(matches);
    }
}
