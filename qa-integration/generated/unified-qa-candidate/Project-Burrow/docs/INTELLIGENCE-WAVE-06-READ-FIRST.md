# TrashPanda Tracker | Intelligence Expansion Wave 06 | Integration Freeze

**Development checkpoint only. Owner's first combined gameplay QA begins after this handoff.**

## Wave 06: only these logic integrations

1. **Pathfinder:** `PathfinderIntelligenceBridge` runs after existing suggestions are generated. A UNIQUE, exact activity-name match and an approved, immutable-source activity entry are required. It can lower a falsely optimistic readiness based on verified requirements, but cannot increase readiness, infer a teleport, turn unobserved stock into zero, alter existing time scores, or route the player.
2. **Chronicle:** `ChronicleIntelligenceBridge` matches exact `(boss, task name, tier)`. Only an independently approved task with pinned provenance can replace the legacy condition and its tip. With no verified task rows, all 290 legacy entries stay unchanged. Expanded Combat Tasks now shows a `RESEARCH CHECK` status above the existing reference content.
3. **Best in Bank:** W03's existing gated gear intelligence stays connected, unchanged. The bank observation, Bank O temporary tag tab, 4x7 inventory, whole-loadout planner ranking and withdrawal workflow are untouched. No new bank automation.
4. **Home / Sessions:** existing source and UI unchanged. Four tabs remain. No gameplay action, KC inference, diary inference or CA completion inference added.

## Current activation truth

- W02 gear: 80 staged IDs, **0 independently approved**. No W02 equipment relationship activated.
- W04 activities: 44 staged records, **0 independently approved**. No changed current Pathfinder recommendations.
- W05 CA: 290 legacy research references and 77 tips, **0 verified canonical records**. Rejected BQA-906 placeholder rows remain excluded. Existing Chronicle strings still display as references, visibly marked CHECK.
- A research review / source pin is necessary before any staged rule can affect a player's live recommendation. The integration hooks exist but **do not imply that the submitted research was verified or that the player has an item/unlock**.

## Protection boundary

Compared to clean W05: only `BurrowPanel.java` and the quick-launch banner are changed; two small new intelligence bridges, one queued JUnit fixture, and this document are added. All other previous source bytes, including HoverLite v0.2.8, BestBankPlanner, Bank R05 and other main sources, remain unchanged.

## Development checks actually performed

- W05 archive integrity, SHA256 base and member parity checked during packaging.
- New bridge source and `BurrowPanel.java` compiled in isolation via OpenJDK21 `javac --release 17`.
- Small synthetic development probe verified W04 44 CHECK items, W05 290 CHECK tasks, zero activated research, unchanged legacy task wording, untouched Pathfinder suggestions.
- Complete JUnit, Gradle with JDK 17, Windows RuneLite, both plugin startup, gameplay QA, bank workflow: **NOT RUN in this environment**. No native gameplay pass claimed.

## Owner's one combined Windows QA

1. Extract into a **fresh folder**; retain the known-working Bank R05 snapshot separately for rollback.
2. Install/select JDK 17. Set `JAVA17_HOME` if needed.
3. Run `00-START-BOTH-NO-REPEAT-QA.cmd`. A single RuneLite development client should start and load both TrashPanda Tracker and HoverLite v0.2.8. This is source compilation plus launch, **not a prebuilt signed JAR**.
4. Confirm Bank O opens once and stays open after four Best in Bank withdrawals, and W03 readiness statuses stay correct.
5. Check Pathfinder suggestions and the Chronicle combat-task expander. Chronicle should show `RESEARCH CHECK`; no CA completion should be inferred from KC.
6. Verify HoverLite independently in the same client, without changed right-click actions. Report actual failure evidence only if a step fails.

**Release status:** HOLD until owner decides after Windows RuneLite QA. No Plugin Hub submission or production promotion performed.
