---
id: "011"
title: Move Nav Helpers Under Util.UI
status: done
created: 2026-05-12
updated: 2026-05-12
---

## Summary

Relocates the navigation helpers that currently live under the top-level `ui.nav` area into the existing shared `util.ui` area, alongside the other generic UI helpers (`pane`, `table`, `theme`). Both helpers are already domain-agnostic and have no ties to application-specific concerns, so they belong with the rest of the reusable UI toolkit rather than as a standalone top-level area.

## Goals

- Make the navigation helpers part of the shared UI utilities collection rather than a separate top-level area.
- Eliminate the standalone `ui.nav` area, since the nav helpers are its only occupants.
- Leave the application-specific UI screens that consume these helpers exactly where they are today.

## Acceptance Criteria

- The navigation helpers are reachable from inside the `util.ui` area instead of from the old `ui.nav` area.
- The old `ui.nav` area no longer exists in the source tree (no leftover empty folders).
- Every place in the app that used the navigation helpers continues to work exactly as before, with no user-visible change in behavior.
- Any documentation references to the old location are updated to point at the new location, so generated Javadoc resolves correctly with no broken links.
- The application-specific UI screens that use these helpers (the main window and the settings screen) stay in their current home in the application-specific UI area.
- The project still builds and runs through the existing make-based build with no warnings introduced by this change.

## Open Questions

- None. The new home for the navigation helpers is inside the existing `util.ui` area, in a sub-area named for navigation; the exact sub-area name is an architecture decision.

## Architecture

### Package Structure

```
src/
├── ui/                       # top-level UI area; nav/ subpackage is removed by this plan
│   ├── MainWindow.java       # consumer; import statements updated (unchanged content otherwise)
│   └── SettingsView.java     # consumer; Javadoc {@link} reference updated (unchanged content otherwise)
└── util/
    └── ui/                   # generic, reusable UI helpers
        ├── nav/              # navigation helpers (moved from ui.nav)
        │   ├── NavItem.java
        │   └── NavPane.java
        ├── pane/             # existing (unchanged)
        ├── table/            # existing (unchanged)
        └── theme/            # existing (unchanged)
```

The existing `src/ui/nav/` directory is removed entirely after the move, since
`NavItem` and `NavPane` are its only occupants.

### Components

| Component | Type | Package | Purpose |
|---|---|---|---|
| `NavItem` | `record` | `util.ui.nav` | Immutable nav entry; identifier and display label (relocated; package change only) |
| `NavPane` | `final class` | `util.ui.nav` | Scrollable nav list that fires a callback on selection (relocated; package change only) |
| `MainWindow` | `final class` (existing) | `ui` | Updated to import `util.ui.nav.NavItem` and `util.ui.nav.NavPane` instead of `ui.nav.*` |
| `SettingsView` | `final class` (existing) | `ui` | Updated to fix the `{@link ui.nav.NavItem}` Javadoc reference to `{@link util.ui.nav.NavItem}` |

### Signatures

Both relocated types keep their existing public API verbatim. Only their
`package` declaration changes.

```java
// util/ui/nav/NavItem.java
package util.ui.nav;

public record NavItem(String id, String label) {
    public NavItem;                // compact canonical constructor (validation)
    @Override public String toString();
}

// util/ui/nav/NavPane.java
package util.ui.nav;

public final class NavPane {
    public static final int PREFERRED_WIDTH = 220;
    public NavPane(List<NavItem> items, Consumer<NavItem> onSelect);
    public JComponent component();
}
```

Consumer updates (no behavior change):

```java
// ui/MainWindow.java
import util.ui.nav.NavItem;
import util.ui.nav.NavPane;

// ui/SettingsView.java
// Update the Javadoc {@link ui.nav.NavItem} reference (currently in the
// VIEW_ID constant docblock) to {@link util.ui.nav.NavItem}.
```

### Relationships

- `NavPane` depends on `NavItem`; both live together in `util.ui.nav`. `NavPane`
  also references `util.ui.pane.CardPane` in its Javadoc, which is unaffected
  (still in `util.ui.pane`).
- `MainWindow` is the only runtime consumer of `NavItem` and `NavPane`; its
  imports are updated to the new package. Behavior is unchanged.
- `SettingsView` does not use `NavItem` at runtime but has one Javadoc `{@link}`
  reference to it that must be updated so generated Javadoc resolves correctly.
- `NavItem` and `NavPane` remain domain-agnostic (standard library plus
  `util.ui.pane` only), satisfying the `util` package's "no application-specific
  dependencies" rule documented in `src/util/package-info.java`.
- The old `ui.nav` package is deleted entirely. No file in the source tree
  should reference `ui.nav` after this work.

### Implementation Order

Work through these one at a time. After each step, the project must still
compile via `make compile`.

1. `util/ui/nav/NavItem.java` — create by moving the file from
   `src/ui/nav/NavItem.java` (use `git mv`), changing its `package` declaration
   to `util.ui.nav`. No other content changes.
2. `util/ui/nav/NavPane.java` — create by moving the file from
   `src/ui/nav/NavPane.java` (use `git mv`), changing its `package` declaration
   to `util.ui.nav`. No other content changes.
3. `ui/MainWindow.java` — update the two `ui.nav.*` imports to `util.ui.nav.*`.
   The existing `{@link NavPane}`, `{@link NavPane#PREFERRED_WIDTH}`, and
   `{@link CardPane}` Javadoc references resolve via the updated imports and do
   not need to be rewritten.
4. `ui/SettingsView.java` — update the `{@link ui.nav.NavItem}` Javadoc
   reference (on the `VIEW_ID` constant) to `{@link util.ui.nav.NavItem}`. No
   import changes are required (the class does not use `NavItem` at runtime).
5. Delete the now-empty `src/ui/nav/` directory so no stray empty folder remains.
6. Verify with `make clean && make compile` (and a quick
   `grep -rn "ui\.nav\|ui/nav" src` to confirm zero remaining references) that
   the project still builds with no new warnings.

## Implementation Notes

<!-- java-coder appends progress notes here as it works through this plan -->

- 2026-05-12: Moved `src/ui/nav/NavItem.java` to `src/util/ui/nav/NavItem.java`
  via `git mv` and updated its package declaration to `util.ui.nav`. No other
  content changes.
- 2026-05-12: Moved `src/ui/nav/NavPane.java` to `src/util/ui/nav/NavPane.java`
  via `git mv` and updated its package declaration to `util.ui.nav`. No other
  content changes.
- 2026-05-12: Updated `src/ui/MainWindow.java` to import `util.ui.nav.NavItem`
  and `util.ui.nav.NavPane` instead of `ui.nav.*`. No other content changes.
- 2026-05-12: Updated the `{@link ui.nav.NavItem}` Javadoc reference on the
  `VIEW_ID` constant in `src/ui/SettingsView.java` to
  `{@link util.ui.nav.NavItem}`. No import changes needed.
- 2026-05-12: Removed the now-empty `src/ui/nav/` directory.
- 2026-05-12: Verified with `make clean && make compile` (clean build, no
  warnings) and a `grep -rn "ui\.nav\|ui/nav" src` that returns only the
  expected new `util.ui.nav` references. Plan complete; flipping status to
  `done`.
