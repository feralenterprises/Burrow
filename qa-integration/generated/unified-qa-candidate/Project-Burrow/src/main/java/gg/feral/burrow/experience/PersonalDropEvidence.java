package gg.feral.burrow.experience;

import gg.feral.burrow.core.RareDropStore;
import java.util.Optional;

/** Per-account drop evidence for one signed-in account. Adapter over the existing rare drop store state. */
public interface PersonalDropEvidence {
    RareDropStore.Effective kc(String encounter);
    /** Where an unlock was observed, or empty when there is no evidence. Ownership today is never implied. */
    Optional<String> unlockSource(int itemId);

    static PersonalDropEvidence of(RareDropStore.State state) {
        return new PersonalDropEvidence() {
            @Override public RareDropStore.Effective kc(String encounter) { return state.effective(encounter); }
            @Override public Optional<String> unlockSource(int itemId) { return state.unlockEvidence(itemId).map(RareDropStore.State.UnlockEvidence::source); }
        };
    }
}
