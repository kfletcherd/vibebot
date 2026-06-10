---
id: "003"
title: Split-Pane Layout with Nav and Content Areas
status: done
created: 2026-04-16
updated: 2026-04-16
---

## Summary

Adds a two-pane layout to `MainWindow`: a fixed-width left navigation pane presenting a list of menu items, and a right content pane whose displayed view updates whenever the selected nav item changes. The event model is a simple observer: the nav pane fires a selection-changed notification; the content pane reacts by swapping its visible view. No third-party libraries are used; the entire implementation relies on `javax.swing` from the standard library.

## Package Structure

```
com.example.vibebot
├── ui/
│   ├── MainWindow.java          # existing — gains a JSplitPane child; modified
│   ├── nav/                     # new package
│   │   ├── NavItem.java         # value object representing one menu entry
│   │   ├── NavPane.java         # left pane: renders the item list, fires selection events
│   │   └── NavSelectionListener.java  # functional interface for selection callbacks
│   └── content/                 # new package
│       ├── ContentPane.java     # right pane: owns a CardLayout and swaps views
│       └── ContentView.java     # interface a displayable view must satisfy
```

## Components

| Component | Type | Package | Purpose |
|---|---|---|---|
| `NavItem` | `record` | `ui.nav` | Immutable value object holding an item's unique ID and display label |
| `NavSelectionListener` | `interface` | `ui.nav` | Functional interface with a single `onNavItemSelected(NavItem)` callback |
| `NavPane` | `class` | `ui.nav` | Wraps a themed `JList<NavItem>` inside a `JScrollPane`; accepts a list of `NavItem`s and a `NavSelectionListener` at construction; fires the listener on list-selection changes |
| `ContentView` | `interface` | `ui.content` | Contract for a displayable content view; exposes `viewId()` (returns a `String` matching its `NavItem` ID) and `component()` (returns the `JComponent` to embed) |
| `ContentPane` | `class` | `ui.content` | Wraps a `JPanel` with `CardLayout`; accepts a collection of `ContentView`s at construction; exposes `showView(String viewId)` to swap the visible card |
| `MainWindow` | `class` (modified) | `ui` | Existing class — updated to build a `JSplitPane` containing `NavPane` on the left and `ContentPane` on the right, wire the `NavSelectionListener` so that nav selections call `ContentPane.showView`, and add a fixed divider position |

## Relationships

`NavItem` is a plain record with no dependencies. `NavSelectionListener` is a functional interface that depends only on `NavItem`. `NavPane` depends on both of these and on `DarkTheme` for its custom cell renderer colors. `ContentView` has no dependencies — it is the contract that concrete view classes will satisfy in future plans. `ContentPane` depends on `ContentView`. `MainWindow` depends on `NavPane`, `ContentPane`, `NavItem`, `NavSelectionListener`, and the existing `DarkTheme`; it constructs all collaborators and wires the listener so that a selection in `NavPane` calls `ContentPane.showView`.

## Layout Details

- `JSplitPane.HORIZONTAL_SPLIT` with the divider set to a fixed pixel position (220 px is the default; it must be a named constant, not a magic number).
- The divider should not be user-draggable in this initial implementation (`setEnabled(false)` on the divider, or `setResizable(false)` equivalent). This keeps the layout predictable until a future plan explicitly adds resize support.
- `NavPane` sets a preferred width of 220 px and stretches vertically to fill available height.
- `ContentPane` takes all remaining horizontal space and fills height.
- Both panes use `DarkTheme.SURFACE` as their background to distinguish them from the window's `DarkTheme.BACKGROUND`.

## Selection Event Flow

1. User clicks an item in `NavPane`'s `JList`.
2. `JList`'s `ListSelectionListener` (internal to `NavPane`) fires.
3. `NavPane` retrieves the selected `NavItem` and calls `NavSelectionListener.onNavItemSelected(item)`.
4. `MainWindow`'s lambda (registered as the listener) calls `contentPane.showView(item.id())`.
5. `ContentPane` calls `CardLayout.show(container, viewId)`, swapping the visible panel.

The listener is registered at construction time in `MainWindow` and is never null — `NavPane` must guard against this with a constructor-level null check.

## Implementation Order

1. `NavItem` — record, no dependencies
2. `NavSelectionListener` — functional interface, depends only on `NavItem`
3. `ContentView` — interface, no dependencies
4. `NavPane` — class, depends on `NavItem`, `NavSelectionListener`, `DarkTheme`
5. `ContentPane` — class, depends on `ContentView`
6. `MainWindow` (modification) — wires `NavPane` and `ContentPane` into a `JSplitPane`, registers the selection listener, adds placeholder `ContentView` stubs for each initial nav item so the layout is visible and testable

## Open Questions

None — the scope is fully defined for this plan.

## Implementation Notes

<!-- java-coder appends progress notes here as it works through this plan -->

### 2026-04-16 — NavItem implemented

Created `src/main/java/com/example/vibebot/ui/nav/NavItem.java` as a `record` with fields `id` and `label`. The compact constructor rejects null or blank values for both fields. `toString()` is overridden to return `label` so `JList`'s default renderer displays a human-readable name without a custom cell renderer at this stage.

### 2026-04-16 — NavSelectionListener implemented

Created `src/main/java/com/example/vibebot/ui/nav/NavSelectionListener.java` as a `@FunctionalInterface` with a single method `onNavItemSelected(NavItem)`. The Javadoc specifies that implementations are invoked on the EDT and that the passed `NavItem` is never null.

### 2026-04-16 — ContentView implemented

Created `src/main/java/com/example/vibebot/ui/content/ContentView.java` as an interface with two methods: `viewId()` returns the card name (must match the `NavItem` id), and `component()` returns the `JComponent` to embed. The Javadoc calls out that `ContentPane` retains the component instance returned at registration time, so implementations must not swap it out after the fact.

### 2026-04-16 — NavPane implemented

Created `src/main/java/com/example/vibebot/ui/nav/NavPane.java`. Key decisions:
- Preferred width is exposed as `public static final int PREFERRED_WIDTH = 220` (named constant; used by `MainWindow` when setting the split-pane divider location).
- A private `NavCellRenderer` (extends `DefaultListCellRenderer`) applies `DarkTheme.SURFACE` / `DarkTheme.FOREGROUND` for normal rows and `DarkTheme.SELECTION_BG` / `DarkTheme.SELECTION_FG` for the selected row, plus `EmptyBorder` padding.
- The `ListSelectionListener` guards against adjusting events and null selection before invoking `NavSelectionListener`.
- The scroll pane's viewport background is also set to `DarkTheme.SURFACE` so no flash of the default color shows when the list is shorter than the pane height.
- `component()` returns the `JScrollPane` root so callers have no dependency on the internal `JList`.

### 2026-04-16 — ContentPane implemented

Created `src/main/java/com/example/vibebot/ui/content/ContentPane.java`. Key decisions:
- Constructor accepts `Collection<? extends ContentView>` (upper-bounded wildcard) so callers can pass any `List`, `Set`, or other collection of `ContentView` subtypes.
- Rejects a null collection or an empty collection with `NullPointerException` / `IllegalArgumentException` at construction time, before any Swing state is touched.
- Each view is added via a private `addView` helper that also null-checks the `JComponent` returned by `view.component()`, so a misbehaving `ContentView` implementation produces a clear `NullPointerException` rather than a cryptic NPE inside `CardLayout`.
- `showView(String)` delegates directly to `cardLayout.show(container, viewId)` — `CardLayout` silently ignores unknown keys, and the Javadoc documents that behaviour so callers know not to expect an exception.
- `component()` returns the `JPanel` container, matching the same pattern as `NavPane.component()`; callers have no dependency on the internal `CardLayout` instance.

### 2026-04-16 — MainWindow modified (plan complete)

Modified `src/main/java/com/example/vibebot/ui/MainWindow.java`. Key decisions:
- `buildSplitPane()` is extracted as a private helper to keep the constructor a single delegation call (`configureFrame()` + `buildSplitPane()`), preserving the constructor's readability.
- The three initial nav items (`home`, `chat`, `settings`) are defined in `buildNavItems()`. A matching `buildContentViews()` streams over them to produce `PlaceholderContentView` instances, ensuring the two lists are always aligned and a future developer knows exactly where to add a new section.
- `PlaceholderContentView` is a `private static final` inner class of `MainWindow`. Keeping the stubs co-located with the wiring makes it obvious they are scaffolding and prevents them from being imported anywhere else in the codebase.
- The `JSplitPane` divider is set to `NavPane.PREFERRED_WIDTH` (the named constant from `NavPane`), `setEnabled(false)` prevents dragging, and `setDividerSize(1)` gives a thin visual separator using `DarkTheme.BORDER` as the split-pane background, matching the plan's requirement.
- The `NavSelectionListener` is wired as a lambda: `item -> contentPane.showView(item.id())`, which is the shortest expression of the event flow described in the plan.
