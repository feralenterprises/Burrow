package gg.feral.burrow.core;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Per-account preference. New verified accounts default to Ironman without persisting a guess. */
public final class AccountModePreference {
    public enum Mode { IRONMAN, MAIN }
    public static final String GROUP = "trashpanda";
    private static final String PREFIX = "account_mode_";
    private final Function<String, String> reader;
    private final BiConsumer<String, String> writer;

    public AccountModePreference(Function<String, String> reader, BiConsumer<String, String> writer) {
        this.reader = Objects.requireNonNull(reader);
        this.writer = Objects.requireNonNull(writer);
    }

    /** A RuneLite account hash is required; -1 means unavailable. */
    public static String key(long accountHash) {
        if (accountHash == -1L) throw new IllegalArgumentException("Authenticated account required");
        return PREFIX + Long.toUnsignedString(accountHash, 16);
    }
    public Optional<Mode> read(long accountHash) {
        if (accountHash == -1L) return Optional.empty();
        String saved = reader.apply(key(accountHash));
        if (saved == null || saved.isBlank()) return Optional.empty();
        try {return Optional.of(Mode.valueOf(saved.trim().toUpperCase(Locale.ROOT)));}
        catch (IllegalArgumentException malformed) {return Optional.empty();}
    }
    /** Default is a product preference, never an authenticated OSRS account classification. */
    public Mode resolve(long accountHash) {return read(accountHash).orElse(Mode.IRONMAN);}
    public void save(long accountHash, Mode mode) {
        writer.accept(key(accountHash), Objects.requireNonNull(mode).name());
    }
}
