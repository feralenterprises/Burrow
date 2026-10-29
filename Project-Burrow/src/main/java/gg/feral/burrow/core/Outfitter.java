package gg.feral.burrow.core;

import java.util.*;

/** Pure, host-independent plan, stock, and 8x7 bank-layout projection. */
public final class Outfitter {
    private Outfitter() { }

    public record Scope(String profile, long epoch) {
        public Scope {
            if (profile == null || profile.isBlank() || epoch < 0) throw new IllegalArgumentException("scope");
        }
    }
    public record Snapshot(Scope scope, long revision, Map<Integer, Integer> items) {
        public Snapshot {
            Objects.requireNonNull(scope, "scope");
            if (revision < 0) throw new IllegalArgumentException("revision");
            Objects.requireNonNull(items, "items");
            for (var e : items.entrySet()) if(e.getKey() <= 0 || e.getValue() < 0)
                throw new IllegalArgumentException("invalid bank count");
            items = Map.copyOf(items);
        }
    }
    public record Requirement(int itemId, String name, int amount) {
        public Requirement {
            if (itemId <= 0 || amount < 1 || name == null || name.isBlank())
                throw new IllegalArgumentException("requirement");
        }
    }
    public record Template(String boss, String style, List<Requirement> gear, List<Requirement> inventory) {
        public Template {
            if (boss == null || boss.isBlank() || style == null || style.isBlank() ||
                gear == null || gear.size() != 11 || inventory == null || inventory.size() > 28)
                throw new IllegalArgumentException("invalid template geometry");
            gear = Collections.unmodifiableList(new ArrayList<>(gear));
            inventory = List.copyOf(inventory);
        }
    }
    public enum Kind { SPACE, GEAR, INVENTORY }
    public enum Stock { UNVERIFIED, OWNED, MISSING, SPACE }
    public record Cell(int position, Kind kind, Requirement requirement, int allocated, Stock stock) {
        public Cell {
            if (position < 0 || position > 55 || allocated < 0) throw new IllegalArgumentException("cell");
        }
    }
    public record Plan(long planId, Scope scope, Template template, String mode,
                       long bankRevision, List<Cell> cells, boolean bankVerified) {
        public Plan {
            if (planId <= 0 || scope == null || template == null || mode == null ||
                cells == null || cells.size() != 56) throw new IllegalArgumentException("plan");
            cells = List.copyOf(cells);
        }
        public List<Cell> missing() {
            return cells.stream().filter(c -> c.stock() == Stock.MISSING).toList();
        }
        public List<Cell> gear() {
            return cells.stream().filter(c -> c.kind() == Kind.GEAR).toList();
        }
        public List<Cell> inventory() {
            return cells.stream().filter(c -> c.kind() == Kind.INVENTORY).toList();
        }
    }
    /** Historical L01 positions in an immutable, externally read-only list. */
    public static final List<Integer> GEAR_POSITIONS = List.of(1,8,9,10,16,17,18,25,32,33,34);
    public static int inventoryPosition(int slot) {
        if (slot < 0 || slot >= 28) throw new IllegalArgumentException("slot");
        return 4 + (slot % 4) + 8 * (slot / 4);
    }
    /** One immutable resolved plan is consumed by UI, bank projection, and checklist. */
    public static Plan resolve(long planId, Scope scope, Template template, String mode, Snapshot bank) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(template, "template");
        if (bank != null && !scope.equals(bank.scope()))
            throw new IllegalArgumentException("stale or foreign account bank snapshot");
        if (!"Recommended".equals(mode) && !"Best in Bank".equals(mode))
            throw new IllegalArgumentException("unknown mode");
        Map<Integer,Integer> remaining = bank == null ? new HashMap<>() : new HashMap<>(bank.items());
        Cell[] grid = new Cell[56];
        for (int i = 0; i < 56; i++) grid[i] = new Cell(i, Kind.SPACE, null, 0, Stock.SPACE);
        for (int i = 0; i < 11; i++)
            fill(grid, GEAR_POSITIONS.get(i), Kind.GEAR, template.gear().get(i), remaining, bank != null);
        for (int i = 0; i < 28; i++) {
            Requirement r = i < template.inventory().size() ? template.inventory().get(i) : null;
            fill(grid, inventoryPosition(i), Kind.INVENTORY, r, remaining, bank != null);
        }
        return new Plan(planId, scope, template, mode, bank == null ? -1 : bank.revision(),
                        Arrays.asList(grid), bank != null);
    }
    private static void fill(Cell[] grid, int p, Kind kind, Requirement r,
                             Map<Integer,Integer> remaining, boolean observed) {
        if (r == null) {
            // Keep the exact physical role even for unused slots. A missing gear
            // recommendation is UNKNOWN, never an observed empty bank slot.
            grid[p] = new Cell(p, kind, null, 0,
                kind == Kind.GEAR ? Stock.UNVERIFIED : Stock.SPACE);
            return;
        }
        int allocated = observed ? Math.min(r.amount(), remaining.getOrDefault(r.itemId(),0)) : 0;
        if (observed) remaining.put(r.itemId(), remaining.getOrDefault(r.itemId(), 0) - allocated);
        grid[p] = new Cell(p, kind, r, allocated, !observed ? Stock.UNVERIFIED
                               : allocated == r.amount() ? Stock.OWNED : Stock.MISSING);
    }
    /** Demonstration fixtures only. NOT verified wiki advice and NOT a live bank scan. */
    public static Template scurriusSample() {
        return new Template("Scurrius", "Melee reference", Arrays.asList(
            new Requirement(10828,"Helm of neitiznot",1),
            new Requirement(6570,"Fire cape",1),
            new Requirement(1704,"Amulet of glory",1),
            null,
            new Requirement(4151,"Abyssal whip",1),
            new Requirement(10551,"Fighter torso",1),
            new Requirement(12954,"Dragon defender",1),
            new Requirement(1079,"Rune platelegs",1),
            new Requirement(7462,"Barrows gloves",1),
            new Requirement(3105,"Climbing boots",1),
            new Requirement(6737,"Berserker ring",1)
        ), List.of(
            new Requirement(2434,"Prayer potion (4)",1),
            new Requirement(2434,"Prayer potion (4)",1),
            new Requirement(2434,"Prayer potion (4)",1),
            new Requirement(2434,"Prayer potion (4)",1),
            new Requirement(385,"Shark",1),
            new Requirement(385,"Shark",1),
            new Requirement(385,"Shark",1),
            new Requirement(385,"Shark",1)
        ));
    }
    public static Snapshot sampleBank(Scope scope) {
        // 3 potions for 4 distinct slots; the fourth must be flagged missing.
        return new Snapshot(scope, 1, Map.ofEntries(
            Map.entry(10828,1),Map.entry(6570,1),Map.entry(1704,1),Map.entry(4151,1),
            Map.entry(10551,1),Map.entry(12954,1),Map.entry(1079,1),Map.entry(7462,1),
            Map.entry(3105,1),Map.entry(6737,1),Map.entry(2434,3),Map.entry(385,2)));
    }
}
