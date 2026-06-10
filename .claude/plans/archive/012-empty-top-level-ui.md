---
id: "012"
title: Empty the Top-Level UI Area by Moving Main Window and Settings View Into Their Features
status: done
created: 2026-05-12
updated: 2026-05-12
---

## Summary

Finishes draining the top-level UI area by relocating its last two screens into the feature areas they belong to. The main application window moves into the application area, and the settings screen moves into the settings area. After this work the standalone top-level UI area no longer exists, and each feature-specific screen lives next to the rest of its feature's code, matching the convention already established for the server feature.

## Goals

- Place the settings screen inside the settings feature, alongside the rest of the settings code, so that everything settings-related lives in one area.
- Place the main application window inside the application feature, so that the program's top-level shell lives in its own area instead of in a generic catch-all.
- Eliminate the standalone top-level UI area entirely, since after this move it has no remaining occupants.
- Mirror the `<feature>/ui` convention already used by the server feature consistently across the settings and application features.

## Acceptance Criteria

- The settings screen is reachable from inside the settings feature's UI sub-area instead of from the old top-level UI area.
- The main application window is reachable from inside the application feature's UI sub-area instead of from the old top-level UI area.
- The old top-level UI area no longer exists in the source tree (no leftover empty folders).
- Launching the application produces the exact same window, navigation, and settings screen as before, with no user-visible change in behavior.
- The single place that opens the main window is updated to reference its new location.
- The single place inside the main window that opens the settings screen is updated to reference its new location.
- The project still builds and runs through the existing make-based build with no warnings introduced by this change.

## Open Questions

- None. Both new homes (a UI sub-area inside the settings feature and a UI sub-area inside the application feature) are fixed by this plan; only the exact sub-area names are an architecture decision, and the expectation is that they mirror the existing `<feature>/ui` pattern.

## Sequencing

This plan must be picked up only after plan 011 (Move Nav Helpers Under Util.UI) has reached `done`, or at minimum has been merged. Plan 011 is what empties the `ui/nav` sub-area; this plan assumes that work is already in place so that the only remaining occupants of the top-level UI area are the main window and the settings screen. The architect and the implementing coder should both verify plan 011's status before starting work here. If 011 is still outstanding, pause and finish it first.

A related note for the implementer: the settings screen's Javadoc currently links to the navigation item type at its old location. Plan 011 will have already updated that link to its new location, so this plan does not need to touch it.

## Architecture

This plan assumes plan 011 is complete: `NavItem` and `NavPane` already live in
`util.ui.nav`, `src/ui/nav/` no longer exists, and the `{@link util.ui.nav.NavItem}`
Javadoc reference in `SettingsView` already points at the post-011 location.

### Package Structure

```
src/
├── app/
│   ├── AppLauncher.java     # consumer; import statement updated
│   ├── AppSettings.java     # unchanged
│   ├── Main.java            # unchanged
│   └── ui/                  # new sub-area for the application's top-level shell
│       └── MainWindow.java  # moved from ui/MainWindow.java
├── settings/
│   ├── SettingsFileReader.java   # unchanged
│   ├── SettingsFileWriter.java   # unchanged
│   ├── SettingsKeys.java         # unchanged
│   └── ui/                       # new sub-area for the settings feature's UI
│       └── SettingsView.java     # moved from ui/SettingsView.java
├── server/                  # unchanged (already follows the <feature>/ui convention)
└── util/                    # unchanged
```

After this plan, the top-level `src/ui/` directory no longer exists (no leftover
empty folders).

### Components

| Component | Type | Package | Purpose |
|---|---|---|---|
| `MainWindow` | `final class` | `app.ui` | Primary application window (relocated; package change only) |
| `SettingsView` | `final class` | `settings.ui` | `PaneView` for the settings nav item (relocated; package change only) |
| `AppLauncher` | `final class` (existing) | `app` | Updated to import `app.ui.MainWindow` instead of `ui.MainWindow` |

The inner `PlaceholderPaneView` defined inside `MainWindow` moves with it; it is
a `private static final` nested class and requires no separate consideration.

### Signatures

Both relocated types keep their existing public API verbatim. Only their
`package` declaration changes; `MainWindow` additionally gains one new import
because it constructs `SettingsView`, which now lives in a different package.

```java
// app/ui/MainWindow.java
package app.ui;

public final class MainWindow {
    public MainWindow(AppSettings appSettings);
    public void show();
    public Dimension getPreferredWindowSize();
}

// settings/ui/SettingsView.java
package settings.ui;

public final class SettingsView implements PaneView {
    public SettingsView(AppSettings appSettings, ServerStore serverStore);
    @Override public String id();
    @Override public JComponent component();
}
```

Consumer and same-file import updates (no behavior change):

```java
// app/AppLauncher.java
import app.ui.MainWindow;          // replaces import ui.MainWindow;

// app/ui/MainWindow.java
import settings.ui.SettingsView;   // new — SettingsView is no longer same-package
// (other existing imports such as app.AppSettings, server.model.*,
//  server.ui.ServerListView, util.ui.nav.*, util.ui.pane.*,
//  util.ui.theme.* remain unchanged)
```

Javadoc references that resolve via these imports (no body changes needed):

- The `{@link SettingsView}` references inside `MainWindow`'s Javadoc resolve
  via the new `settings.ui.SettingsView` import.
- The `{@link util.ui.nav.NavItem}` reference inside `SettingsView`'s Javadoc
  (placed there by plan 011) continues to resolve correctly; this plan does
  not touch it.

### Relationships

- `Main` calls `AppLauncher.launch()`; `AppLauncher` constructs
  `app.ui.MainWindow`; `MainWindow` (now in `app.ui`) constructs
  `settings.ui.SettingsView` for the `"settings"` nav item and
  `server.ui.ServerListView` for the `"servers"` nav item.
- `SettingsView` (now in `settings.ui`) depends on `app.AppSettings`,
  `server.model.ServerStore`, `server.model.ServerStoreException`,
  `settings.SettingsFileWriter`, `settings.SettingsKeys`, `util.ui.pane.PaneView`,
  and `util.ui.theme.StandardColor`. None of those imports change; only the
  declaring package of `SettingsView` itself changes.
- `MainWindow` (now in `app.ui`) depends on the same set of types it depends on
  today plus the newly required `settings.ui.SettingsView` import.
- The old `ui` package is deleted entirely. No file in the source tree should
  reference `ui.MainWindow` or `ui.SettingsView` after this work.

### Implementation Order

Work through these one at a time. After each step, the project must still
compile via `make compile`. Confirm plan 011 is `done` (or merged) before
starting; if it is not, pause this plan and finish 011 first.

1. `settings/ui/SettingsView.java` — create by moving the file from
   `src/ui/SettingsView.java` (use `git mv`), changing its `package` declaration
   to `settings.ui`. No other content changes. After this step the project does
   not yet compile because `MainWindow` still references the old location; that
   is fixed in step 2.
2. `app/ui/MainWindow.java` — create by moving the file from
   `src/ui/MainWindow.java` (use `git mv`), changing its `package` declaration
   to `app.ui`, and adding `import settings.ui.SettingsView;` to the import
   block. No other content changes.
3. `app/AppLauncher.java` — update the `import ui.MainWindow;` line to
   `import app.ui.MainWindow;`. No other content changes.
4. Delete the now-empty `src/ui/` directory so no stray empty folder remains.
5. Verify with `make clean && make compile` (and a quick
   `grep -rn "^package ui\|import ui\.\|\\bui/MainWindow\\b\|\\bui/SettingsView\\b" src`
   to confirm zero remaining references to the old location) that the project
   still builds with no new warnings. Launch the application and confirm the
   window, nav, and settings screen behave exactly as before.

## Implementation Notes

<!-- java-coder appends progress notes here as it works through this plan -->

- 2026-05-12: Confirmed plan 011 status `done` before starting (NavItem/NavPane
  already in `util.ui.nav`).
- 2026-05-12: Moved `src/ui/SettingsView.java` to
  `src/settings/ui/SettingsView.java` via `git mv` and updated its package
  declaration to `settings.ui`. No other content changes.
- 2026-05-12: Moved `src/ui/MainWindow.java` to `src/app/ui/MainWindow.java`
  via `git mv`, updated its package declaration to `app.ui`, and added
  `import settings.ui.SettingsView;` to the import block (kept in
  alphabetical order between `server.ui.ServerListView` and `util.ui.nav.NavItem`).
- 2026-05-12: Updated `src/app/AppLauncher.java` to import `app.ui.MainWindow`
  instead of `ui.MainWindow`. No other content changes.
- 2026-05-12: Removed the now-empty `src/ui/` directory.
- 2026-05-12: Found and fixed one stale Javadoc reference the plan did not
  call out: the `{@link ui.SettingsView}` reference inside the
  `loadTyped(Map)` docblock in `src/app/AppSettings.java` was updated to
  `{@link settings.ui.SettingsView}`. No runtime imports changed.
- 2026-05-12: Verified with `make clean && make compile` (clean build, no
  warnings) and a grep of `ui\.MainWindow|ui\.SettingsView|^package ui;|import ui\.|ui/MainWindow|ui/SettingsView`
  that returns only the expected new locations (`app.ui.MainWindow`,
  `settings.ui.SettingsView`). Plan complete; flipping status to `done`.
