# Java Coder Feedback Log

This file is maintained by the human. The `java-coder` and `architect-bot` agents read it; neither writes to it.

<!-- New entries go at the bottom, newest last. -->

## [2026-04-17] ServerColumn enum — table presentation order is separate from CSV storage order

**Context:** Extracting `ServerColumn` enum as the single source of truth for column metadata shared by `ServerCsvMapper` and `ServerTableModel`.
**Lesson:** The CSV mapper and the table model may legitimately differ in column order (e.g., the table promotes Environment and Type for readability). The enum defines CSV storage order via `csvIndex()` and the display name via `displayName()`. The table model defines its own `PRESENTATION_ORDER` array of `ServerColumn` values, which is allowed to differ from CSV order. The switch expression in `getValueAt` dispatches on the enum constant at `PRESENTATION_ORDER[columnIndex]`, so adding a column only requires updating the enum and the presentation array — no raw integer constants anywhere.
**Apply when:** Extracting a column-metadata enum for a data entity that has both a CSV representation and a UI table representation with potentially different orderings.

## [2026-04-17] Codebase-wide @since removal — strip from all files in one pass

**Context:** Removing `@since 1.0` tags from every class, interface, method, and field Javadoc across the entire source tree.
**Lesson:** When a no-`@since` rule is established mid-project, every existing file will have violations. The correct approach is a single coordinated pass that rewrites all affected files — not incremental removal file-by-file across sessions. After the pass, verify with a grep for `@since` to confirm zero matches remain before finishing.
**Apply when:** Any project-wide Javadoc style rule change that affects previously written files.

## [2026-05-12] Wrap docblock prose at ~80 characters

**Context:** `src/util/package-info.java` was written with the entire description on a single long line, which is hard to read in a normal editor gutter.
**Lesson:** Hard-wrap Javadoc prose at roughly an 80-character gutter. This applies to every docblock — class, method, field, and `package-info.java` — not just long-form descriptions. Wrap on word boundaries; do not pad to align. Tag lines (`@param`, `@return`, etc.) that exceed ~80 characters should also wrap, with continuation lines indented under the description text.
**Apply when:** Writing or editing any Javadoc block. If an existing block is on one long line, rewrap it as part of the change.

## [2026-05-12] Package moves must grep `{@link}` references, not just `import` statements

**Context:** Plan 012 (move `SettingsView` from `ui` to `settings.ui`) scoped two consumers — `AppLauncher` and `MainWindow` — based on imports alone. java-coder's verification grep caught a third site the plan missed: a `{@link ui.SettingsView}` reference inside `AppSettings.java`'s Javadoc. Without that catch the project would have shipped a broken doc link.
**Lesson:** When a plan relocates or renames a type, "find all consumers" must include Javadoc cross-references (`{@link ...}`, `{@linkplain ...}`, `@see`, and bare fully-qualified-name mentions), not just `import` statements. architect-bot should list these sites explicitly in the Architecture section's consumer-update table; java-coder should grep for both the old fully-qualified name and the old simple name in `*.java` files as part of post-move verification, before declaring the plan done.
**Apply when:** Any plan that moves, renames, or deletes a public type or package. The check is cheap (one `grep -rn`) and the failure mode (silently broken Javadoc) is easy to miss in a build that only checks compilation.
