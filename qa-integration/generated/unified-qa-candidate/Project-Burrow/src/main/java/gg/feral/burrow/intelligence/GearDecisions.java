package gg.feral.burrow.intelligence;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** W02: deterministic, PURE reasoning. Neither observations nor recommendations are read from live RuneLite. */
public final class GearDecisions {
    private GearDecisions() {}
    public enum Signal { YES, NO, UNKNOWN }
    public enum Verdict { USABLE, NOT_OWNED, BLOCKED, UNKNOWN, RESEARCH_ONLY, NOT_APPLICABLE }
    public enum EffectMode { PER_PIECE, FULL_SET }

    /** Quantities/states must be supplied by a future fresh, authenticated bank+equipment observer. */
    public record Observation(Integer quantity, Map<String,Signal> states, Map<String,Integer> skills) {
        public Observation {
            if (quantity != null && quantity < 0) throw new IllegalArgumentException("Negative quantity");
            states = Map.copyOf(Objects.requireNonNull(states));
            skills = Map.copyOf(Objects.requireNonNull(skills));
            for (var e: skills.entrySet())
                if (e.getValue() < 1 || e.getValue() > 99) throw new IllegalArgumentException("Invalid observed level");
        }
    }

    /** VERIFIED_SOURCE is necessary but not sufficient; exact owned state and skill evidence also matter. */
    public static Verdict usability(GearKnowledge.Item item, Observation observation) {
        if (item==null || observation==null) return Verdict.UNKNOWN;
        if (!item.approved()) return Verdict.RESEARCH_ONLY;
        if (observation.quantity()==null) return Verdict.UNKNOWN;
        if (observation.quantity()==0) return Verdict.NOT_OWNED;
        if (item.slot()==GearKnowledge.Slot.NONE) return Verdict.BLOCKED;
        if (item.slot()==GearKnowledge.Slot.UNKNOWN) return Verdict.UNKNOWN;
        for (var skill: item.minimumSkills().entrySet()) {
            Integer observed=observation.skills().get(skill.getKey());
            if (observed==null) return Verdict.UNKNOWN;
            if (observed<skill.getValue()) return Verdict.BLOCKED;
        }
        for (String flag: item.requiredStateFlags()) {
            Signal signal=observation.states().getOrDefault(flag,Signal.UNKNOWN);
            if (signal==Signal.NO) return Verdict.BLOCKED;
            if (signal==Signal.UNKNOWN) return Verdict.UNKNOWN;
        }
        return Verdict.USABLE;
    }

    /** An unknown ammo edge remains UNKNOWN, not automatically compatible or incompatible. */
    public static Verdict ammunition(GearKnowledge.Pack pack,int weaponId,int ammoId) {
        Objects.requireNonNull(pack);
        GearKnowledge.Item weapon=pack.find(weaponId),ammo=pack.find(ammoId);
        if (weapon==null || ammo==null) return Verdict.UNKNOWN;
        if (!weapon.approved() || !ammo.approved()) return Verdict.RESEARCH_ONLY;
        if (weapon.slot()!=GearKnowledge.Slot.WEAPON || ammo.slot()!=GearKnowledge.Slot.AMMO) return Verdict.BLOCKED;
        GearKnowledge.Relation bad=pack.relation(weaponId,ammoId,GearKnowledge.RelationKind.AMMO_INCOMPATIBLE);
        GearKnowledge.Relation good=pack.relation(weaponId,ammoId,GearKnowledge.RelationKind.AMMO_COMPATIBLE);
        if (bad!=null && bad.approved() && good!=null && good.approved()) return Verdict.UNKNOWN; // conflict cannot be resolved by preference
        if (bad!=null && bad.approved()) return Verdict.BLOCKED;
        if (good!=null && good.approved()) return Verdict.USABLE;
        return Verdict.UNKNOWN;
    }

    /** Exact, directed substitution only; god-specific protection must not be silently lost. */
    public static Verdict substitute(GearKnowledge.Pack pack,int originalId,int candidateId,
                                     String requiredGod) {
        Objects.requireNonNull(pack);
        GearKnowledge.Item from=pack.find(originalId),to=pack.find(candidateId);
        if (from==null || to==null) return Verdict.UNKNOWN;
        if (!from.approved() || !to.approved()) return Verdict.RESEARCH_ONLY;
        if (from.slot()==GearKnowledge.Slot.UNKNOWN || to.slot()==GearKnowledge.Slot.UNKNOWN) return Verdict.UNKNOWN;
        if (from.slot()!=to.slot()) return Verdict.BLOCKED;
        if (requiredGod!=null && !requiredGod.isBlank()) {
            if (to.godAlignment().isBlank() || to.godAlignment().equals("UNKNOWN")) return Verdict.UNKNOWN;
            if (!requiredGod.equalsIgnoreCase(to.godAlignment())) return Verdict.BLOCKED;
        }
        for (GearKnowledge.Relation rel:pack.relations()) {
            if (rel.fromId()!=originalId || rel.toId()!=candidateId) continue;
            if (rel.kind()!=GearKnowledge.RelationKind.COSMETIC_PEER &&
                rel.kind()!=GearKnowledge.RelationKind.CONTEXTUAL_ALTERNATIVE &&
                rel.kind()!=GearKnowledge.RelationKind.DIRECTED_UPGRADE) continue;
            if (rel.approved()) {
                // A context-dependent edge is not a global equivalence; Wave 3 supplies context gates.
                if (!rel.requiredContext().isBlank()) return Verdict.UNKNOWN;
                return Verdict.USABLE;
            }
        }
        return Verdict.UNKNOWN;
    }

    /** Weapon/offhand conflict ONLY when two-handed status is proven, never guessed. */
    public static Verdict hands(GearKnowledge.Item weapon,GearKnowledge.Item shield) {
        if (weapon==null || shield==null) return Verdict.UNKNOWN;
        if (!weapon.approved() || !shield.approved()) return Verdict.RESEARCH_ONLY;
        if (weapon.slot()!=GearKnowledge.Slot.WEAPON || shield.slot()!=GearKnowledge.Slot.SHIELD) return Verdict.BLOCKED;
        return switch (weapon.hands()) {
            case TWO -> Verdict.BLOCKED;
            case ONE -> Verdict.USABLE;
            default -> Verdict.UNKNOWN;
        };
    }

    /** A true set effect checks each exact slot/member. Per-piece effects count individual usable pieces. */
    public record SetRule(String id,Set<Integer> memberIds,EffectMode mode,int requiredPieces,
                          IntelligenceCatalog.Review review,IntelligenceCatalog.Source source) {
        public SetRule {
            if (id==null || id.isBlank() || memberIds==null || memberIds.isEmpty())
                throw new IllegalArgumentException("Invalid set rule");
            memberIds=Set.copyOf(memberIds);
            Objects.requireNonNull(mode);
            Objects.requireNonNull(review);
            if (requiredPieces<1 || requiredPieces>memberIds.size())
                throw new IllegalArgumentException("Invalid required piece count");
            if (mode==EffectMode.FULL_SET && requiredPieces!=memberIds.size())
                throw new IllegalArgumentException("FULL_SET must enumerate complete required set");
            if (review==IntelligenceCatalog.Review.VERIFIED_SOURCE &&
                (source==null || !GearKnowledge.immutableSource(source))) throw new IllegalArgumentException("Unpinned set rule");
        }
        public boolean approved() {return review==IntelligenceCatalog.Review.VERIFIED_SOURCE && source!=null && GearKnowledge.immutableSource(source);}
    }
    public record SetResult(Verdict verdict,int usablePieceCount) {}
    public static SetResult setEffect(SetRule rule,Map<Integer,Signal> equippedUsable) {
        Objects.requireNonNull(rule);
        Objects.requireNonNull(equippedUsable);
        if (!rule.approved()) return new SetResult(Verdict.RESEARCH_ONLY,0);
        int count=0;
        boolean unknown=false, definiteMissing=false;
        for (int id:rule.memberIds()) {
            Signal v=equippedUsable.getOrDefault(id,Signal.UNKNOWN);
            if (v==Signal.YES) count++;
            else if (v==Signal.UNKNOWN) unknown=true;
            else definiteMissing=true;
        }
        if (rule.mode()==EffectMode.PER_PIECE) {
            if (count>=rule.requiredPieces()) return new SetResult(Verdict.USABLE,count);
            return new SetResult(unknown?Verdict.UNKNOWN:Verdict.NOT_APPLICABLE,count);
        }
        if (count==rule.requiredPieces()) return new SetResult(Verdict.USABLE,count);
        if (definiteMissing) return new SetResult(Verdict.NOT_APPLICABLE,count);
        return new SetResult(unknown?Verdict.UNKNOWN:Verdict.NOT_APPLICABLE,count);
    }
}
