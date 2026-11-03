# Wave 6 · Settings / Final UI Shell Polish

**Input**: `TPT-UI-SHELL-R02-W05-STANDALONE-HOLD.zip` source snapshot, copied to a standalone `R02-W06` project. No writes to Project Burrow / RuneLite / the live TPT project.

| Scope | Change | Evidence |
| --- | --- | --- |
| Font preferences | Small, Normal, Large change local Swing text scale; exact stored values are Compact, Standard, Large | settings-default, settings-large-azure, home-large-azure previews |
| Accent preferences | Bronze, Azure, Moss alter UI accent tokens, active tab/buttons, headings and cards | settings-large-azure, settings-large-moss previews |
| Persistent settings | Atomic write of font/accent into standalone user-home appearance.properties; defaults on absent/invalid file | `--self-test` plus separate test file |
| Safe testing | Self-test uses randomized temp preference path when no override is provided | Java `main` implementation |
| Entry/return | Header gear opens Appearance subview; Back restores last view and scroll; tabs remain exactly four | `--self-test` |
| Render QA | Real 225px and 3× renders including Large + Azure in six views | `previews/` |
| No fabricated data | Sample XP and ratings labeled as demos; account/KC/bank remain unavailable or unknown | all UI previews |
| Art policy | Approved lean armored header portrait retained; rejected chunky mascot absent | JAR asset listing |

**Release status: HOLD.** Native Windows/RuneLite testing and owner's visual approval are not complete. Large OSRS boss-model renders remain unfulfilled. This shell does not integrate with TPT account, bank, or intelligence code.
