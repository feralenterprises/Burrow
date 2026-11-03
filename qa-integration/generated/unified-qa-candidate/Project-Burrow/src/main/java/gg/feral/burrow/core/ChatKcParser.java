package gg.feral.burrow.core;

import java.util.*;
import java.util.regex.*;

/** Build 20: absolute KC from the game's own "Your X kill count is: N." message. Nothing else is accepted. */
public final class ChatKcParser {
    private ChatKcParser() {}
    public record Kc(String encounter, long value) {}
    private static final Pattern P = Pattern.compile("^Your (.+?) (?:kill|chest) count is: ([0-9][0-9,]*)\\.?$");
    public static Optional<Kc> parse(String message) {
        String m=CollectionLogParser.clean(message);
        Matcher x=P.matcher(m);
        if(!x.matches()) return Optional.empty();
        long v;
        try { v=Long.parseLong(x.group(2).replace(",","")); } catch(NumberFormatException e) { return Optional.empty(); }
        String name=x.group(1).trim();
        return Optional.of(new Kc(CollectionLogParser.encounterName(name,name),v));
    }
}
