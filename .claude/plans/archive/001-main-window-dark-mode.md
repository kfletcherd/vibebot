---
id: "001"
title: Main Application Window with Dark Mode Theme
status: done
created: 2026-04-13
updated: 2026-04-13
decision: window opens at a fixed 800x600; adjustable later
---

## Summary

This plan establishes the entry point for the local desktop application by creating a main window that opens on launch. The window uses a dark mode theme applied consistently across all UI surfaces. This is the foundational plan — all future UI features will be built on top of the window and theme infrastructure defined here.

## Package Structure

```
com.example.vibebot
├── app/           # application entry point and lifecycle management
├── ui/            # top-level window and scene construction
└── ui/theme/      # dark mode color palette, stylesheet constants, and theming utilities
```

## Components

| Component | Type | Package | Purpose |
|---|---|---|---|
| `Main` | `class` | `app` | Application entry point; contains `main(String[] args)` and bootstraps the UI |
| `AppLauncher` | `class` | `app` | Owns application lifecycle; creates and shows the primary window |
| `MainWindow` | `class` | `ui` | Encapsulates the primary `JFrame`; sets title, size, close behavior, and delegates theming |
| `DarkTheme` | `class` | `ui.theme` | Holds the dark mode color palette as named constants (background, foreground, accent, border, etc.) |
| `ThemeApplicator` | `class` | `ui.theme` | Applies `DarkTheme` values to Swing `UIManager` defaults so all components inherit the theme automatically |

## Relationships

- `Main` delegates immediately to `AppLauncher.launch()`, keeping the entry point thin.
- `AppLauncher` calls `ThemeApplicator.apply()` before constructing any Swing components, ensuring the `UIManager` defaults are set prior to window creation.
- `MainWindow` is constructed by `AppLauncher` after theming is applied; it builds and displays the `JFrame`.
- `ThemeApplicator` reads its color values exclusively from `DarkTheme` constants — it has no hardcoded colors of its own.
- `DarkTheme` is a pure data class with no dependencies on any other component.

## Implementation Order

1. `DarkTheme` — pure constants, no dependencies; establishes the color palette first
2. `ThemeApplicator` — depends only on `DarkTheme`; sets `UIManager` defaults for dark mode
3. `MainWindow` — depends on the themed `UIManager` being applied; constructs and configures the `JFrame`
4. `AppLauncher` — depends on `ThemeApplicator` and `MainWindow`; orchestrates launch sequence on the Event Dispatch Thread
5. `Main` — entry point, depends on `AppLauncher`; kept minimal

## Decisions

- **Window size:** Fixed default of 800×600. This can be adjusted in a later plan if needed.

## Implementation Notes

<!-- java-coder appends progress notes here as it works through this plan -->

### [2026-04-13] DarkTheme — complete
Created `src/main/java/com/example/vibebot/ui/theme/DarkTheme.java`. Pure constants class; nine named `Color` fields (BACKGROUND, SURFACE, FOREGROUND, TEXT_MUTED, ACCENT, BORDER, SELECTION_BG, SELECTION_FG, FOCUS). Non-instantiable. No dependencies.

### [2026-04-13] ThemeApplicator — complete
Created `src/main/java/com/example/vibebot/ui/theme/ThemeApplicator.java`. Single static `apply()` entry point; populates `UIManager` defaults for panels, windows, buttons, labels, text inputs, scroll bars, lists, tables, menus, tooltips, and borders — all colors sourced exclusively from `DarkTheme`. Non-instantiable. No hardcoded colors.

### [2026-04-13] MainWindow — complete
Created `src/main/java/com/example/vibebot/ui/MainWindow.java`. Encapsulates a `JFrame` configured with a fixed 800x600 size (`DEFAULT_WIDTH`/`DEFAULT_HEIGHT` constants), title "vibebot", `EXIT_ON_CLOSE` policy, centered on screen, and content pane background set to `DarkTheme.BACKGROUND`. Exposes `show()` (caller must be on EDT) and `getPreferredWindowSize()`. Does not invoke `ThemeApplicator` — relies on the caller applying the theme before construction.

### [2026-04-13] AppLauncher — complete
Created `src/main/java/com/example/vibebot/app/AppLauncher.java`. Non-instantiable utility class with a single static `launch()` entry point. Calls `ThemeApplicator.apply()` before any Swing construction, then schedules `MainWindow` creation and display on the EDT via `SwingUtilities.invokeLater`. Window creation is extracted into a private `openWindow()` helper to keep `launch()` readable. Also created the `app` package directory.

### [2026-04-13] Main — complete
Created `src/main/java/com/example/vibebot/app/Main.java`. Intentionally minimal entry-point class; `main(String[] args)` contains a single call to `AppLauncher.launch()`. Non-instantiable. Keeping the class thin makes `AppLauncher` independently testable and allows alternative bootstrap paths without touching the JVM entry point. All five components are now implemented — plan is complete.
