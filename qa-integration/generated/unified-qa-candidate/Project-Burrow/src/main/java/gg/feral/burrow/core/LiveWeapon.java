package gg.feral.burrow.core;

/** One observed equipment slot, never a recommended item or a bank ownership claim. */
public record LiveWeapon(State state, int itemId, String itemName) {
    public enum State { OFFLINE, WAITING, EMPTY, EQUIPPED }

    public LiveWeapon {
        if (state == null) throw new IllegalArgumentException("state");
        if (state == State.EQUIPPED && itemId <= 0) throw new IllegalArgumentException("itemId");
        if (state != State.EQUIPPED && itemId != -1) throw new IllegalArgumentException("non-weapon ID");
        itemName = itemName == null ? "" : itemName.strip();
        if (state != State.EQUIPPED && !itemName.isEmpty()) throw new IllegalArgumentException("non-weapon name");
    }

    public static LiveWeapon offline() { return new LiveWeapon(State.OFFLINE, -1, ""); }
    public static LiveWeapon waiting() { return new LiveWeapon(State.WAITING, -1, ""); }
    public static LiveWeapon empty() { return new LiveWeapon(State.EMPTY, -1, ""); }
    public static LiveWeapon equipped(int id, String name) { return new LiveWeapon(State.EQUIPPED, id, name); }

    public String displayName() {
        return switch (state) {
            case OFFLINE -> "Log in to see equipped weapon";
            case WAITING -> "Waiting for equipment data...";
            case EMPTY -> "No weapon equipped";
            case EQUIPPED -> itemName.isBlank() || "null".equalsIgnoreCase(itemName)
                    ? "Unknown weapon (#" + itemId + ")" : itemName;
        };
    }
    public String detail() {
        return state == State.EQUIPPED ? "Observed slot ID #" + itemId + "  /  not bank stock"
                : "Read-only RuneLite equipment slot";
    }
}
