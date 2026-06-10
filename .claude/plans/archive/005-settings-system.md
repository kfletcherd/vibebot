---
id: "005"
title: Global Settings System
status: done
created: 2026-04-17
updated: 2026-04-17
---

## Summary

Introduces a persistent, extensible settings system for the application. A
key=value settings file is stored on disk alongside `servers.csv`. At startup,
the file is read and an `AppSettings` singleton is populated. All code that
previously hardcoded `~/.vibebot` reads the directory from `AppSettings`
instead. A real `SettingsView` replaces the placeholder, letting users inspect
and change `dataDir` without restarting.

## Package Structure

```
com.example.vibebot
├── app/
│   ├── AppLauncher.java      # existing — updated to orchestrate settings load
│   └── AppSettings.java      # NEW — global singleton holding all config values
├── settings/
│   ├── SettingsFileReader.java  # NEW — reads key=value file from disk
│   ├── SettingsFileWriter.java  # NEW — writes key=value file to disk
│   └── SettingsKeys.java        # NEW — constants for every settings key name
├── server/
│   └── model/
│       └── ServerStore.java  # existing — updated to accept dataDir via constructor
└── ui/
    └── settings/
        └── SettingsView.java # NEW — ContentView implementation for the Settings nav item
```

## Components

| Component | Type | Package | Purpose |
|---|---|---|---|
| `SettingsKeys` | `class` (constants) | `settings` | Single source of truth for all key name strings used in the settings file. Currently defines two constants: `DATA_DIR` and `SSH_KEY_PATH`. Adding a new setting means adding one constant here. |
| `SettingsFileReader` | `class` | `settings` | Reads a `key=value` file from a given `Path` and returns a `Map<String,String>`. Creates the file with defaults if absent — on first run this means writing both `dataDir` and `sshKeyPath` entries. Stateless utility — all methods static. |
| `SettingsFileWriter` | `class` | `settings` | Writes a `Map<String,String>` to a `key=value` file at a given `Path`, one entry per line. Stateless utility — all methods static. |
| `AppSettings` | `class` (mutable singleton) | `app` | Holds the live, in-memory view of all settings. Loaded once at startup. Provides typed accessors (`dataDir()` → `Path`, `sshKeyPath()` → `String`) and a single `update(String key, String value)` mutator that updates the in-memory value. Responsible for knowing the default for each key: `dataDir` defaults to `<user.home>/.vibebot`; `sshKeyPath` defaults to `~/.ssh/id_rsa`. `sshKeyPath` is stored and returned as a plain `String` (not `Path`) because its value is not used by any current feature — callers that eventually consume it will resolve it to a `Path` themselves at that point. |
| `SettingsView` | `class` | `ui.settings` | `ContentView` implementation for the `"settings"` nav item. Reads current values from `AppSettings`, renders a labeled form with two text fields — one for `dataDir` and one for `sshKeyPath` (below `dataDir`) — and a single Save button that covers both fields. On save: validates `dataDir` as a path, calls `SettingsFileWriter` to persist both values, calls `AppSettings.update(...)` twice (once per key) to update in-memory state, then instructs `ServerStore` to reload from the new `dataDir`. |
| `ServerStore` (modified) | `class` | `server.model` | Remove the hardcoded `resolveCsvPath()` call. Add a new constructor overload that accepts a `Path dataDir` and resolves `<dataDir>/servers.csv`. The no-arg constructor remains but is updated to read `dataDir` from `AppSettings` (so existing call sites in tests or other code continue to work). |

## Relationships

`AppSettings` is a mutable singleton — one instance is created by `AppLauncher`
before any Swing code runs and then passed to `MainWindow`, which forwards it to
`SettingsView` and to the `ServerStore` constructor. `AppSettings` does not know
about `SettingsFileReader` or `SettingsFileWriter`; those are used only by
`AppLauncher` (to load on startup) and `SettingsView` (to persist on save).
`SettingsKeys` is imported by `AppSettings`, `SettingsFileReader`,
`SettingsFileWriter`, and `SettingsView` — it is the sole place where a key
string like `"dataDir"` is defined.

`ServerStore` depends on `AppSettings` only at construction time: the caller
resolves `appSettings.dataDir()` and passes it in as a `Path`. `ServerStore`
does not hold a reference to `AppSettings`.

`SettingsView` holds references to both `AppSettings` (to read/write the
in-memory state) and `ServerStore` (to call `reload(Path)` after a `dataDir`
change). It also calls `SettingsFileWriter` directly to persist the change.

## Implementation Order

1. `SettingsKeys` — constants only, no dependencies
2. `SettingsFileReader` — depends on `SettingsKeys`; reads and creates a
   key=value file using only `java.nio.file` and `java.util`
3. `SettingsFileWriter` — depends on `SettingsKeys`; writes a key=value file
4. `AppSettings` — depends on `SettingsKeys`; holds typed in-memory values with
   defaults; exposes `dataDir()` (returns `Path`), `sshKeyPath()` (returns `String`,
   default `"~/.ssh/id_rsa"`), `settingsFilePath()`, and `update(String, String)`
5. `ServerStore` (modify) — add a `Path dataDir` constructor parameter;
   remove the hardcoded `APP_DIR_NAME` constant and `resolveCsvPath()` static
   method; the resolved path becomes `dataDir.resolve(CSV_FILE_NAME)`; update
   the no-arg constructor to read `AppSettings.instance().dataDir()` so that
   nothing outside this class needs to change if only the default path is needed
6. `AppLauncher` (modify) — before `ThemeApplicator.apply()`, call
   `SettingsFileReader.load(defaultSettingsPath)` to get a raw map, then
   construct `AppSettings` from that map and store it on `AppSettings` as the
   singleton; pass the resulting `AppSettings` instance into `MainWindow`
7. `MainWindow` (modify) — accept `AppSettings` as a constructor parameter;
   pass it to `ServerStore` (via `appSettings.dataDir()`) and to `SettingsView`;
   update `buildViewForItem` to construct `SettingsView` for the `"settings"` id
   instead of a `PlaceholderContentView`
8. `SettingsView` — implement `ContentView` for `"settings"`; constructor takes
   `AppSettings` and `ServerStore`; renders a form with two labeled text fields
   (`dataDir` first, `sshKeyPath` directly below it) and a single Save button
   that persists both values via `SettingsFileWriter`, updates both in `AppSettings`,
   and reloads `ServerStore` if `dataDir` changed

## File Format

The settings file is a plain `key=value` text file (UTF-8), one entry per line.
Lines beginning with `#` are treated as comments and ignored on read. Blank
lines are ignored. This format is readable and editable without tooling, requires
no parsing library, and is trivially extensible — adding a new setting is one
new constant in `SettingsKeys` and one new default in `AppSettings`.

Example file content:

```
# vibebot settings
dataDir=/Users/alice/.vibebot
sshKeyPath=~/.ssh/id_rsa
```

The settings file is always stored at `<dataDir>/vibebot.settings`. On first
run, `dataDir` defaults to `<user.home>/.vibebot`; both `servers.csv` and
`vibebot.settings` are created there automatically.

## ServerStore Reload

`ServerStore` needs a `reload(Path newDataDir)` method that:
1. Clears `serverList`.
2. Recomputes `csvPath` as `newDataDir.resolve(CSV_FILE_NAME)`.
3. Calls `initializeFile()` then `loadFromDisk()` against the new path.
4. Throws `ServerStoreException` if the new path cannot be initialized or read.

`SettingsView` calls this method on a background thread (not the EDT) after
persisting the settings change, then updates the UI back on the EDT.

## AppSettings Singleton Contract

`AppSettings` is NOT a static-field singleton in the traditional sense. Instead:
- `AppLauncher` constructs the single instance and calls a static
  `AppSettings.setInstance(AppSettings)` method before any Swing code runs.
- All other code retrieves it via `AppSettings.instance()`, which throws
  `IllegalStateException` if called before `setInstance`.
- This approach keeps the class testable (a test can call `setInstance` with a
  custom instance) and avoids the pitfalls of lazy initialization.

## Open Questions

None. All design decisions are resolved above.

## Implementation Notes

### 2026-04-17 — All components implemented

Worked through all 8 steps in order in a single session. Each file compiled
cleanly at the end of the session; `make compile` reports zero errors.

1. `SettingsKeys` — `com.example.vibebot.settings`; two public constants
   (`DATA_DIR`, `SSH_KEY_PATH`); non-instantiable utility class.
2. `SettingsFileReader` — `com.example.vibebot.settings`; static `load(Path,
   Map)` that creates the file with defaults on first run, then parses
   key=value lines (skipping comments and blanks); returns `LinkedHashMap`.
3. `SettingsFileWriter` — `com.example.vibebot.settings`; static `write(Path,
   Map)` that prefixes output with `# vibebot settings` and writes one
   `key=value` line per entry.
4. `AppSettings` — `com.example.vibebot.app`; mutable singleton with
   `setInstance`/`instance` pattern; `dataDir()` returns `Path`;
   `sshKeyPath()` returns `String`; `settingsFilePath()` tracks
   `<dataDir>/vibebot.settings`; `update()` dispatches via `switch`; `toMap()`
   for convenient serialisation.
5. `ServerStore` (modified) — removed `APP_DIR_NAME` constant and
   `resolveCsvPath()` static method; added `ServerStore(Path dataDir)`
   constructor; no-arg constructor delegates to it via
   `AppSettings.instance().dataDir()`; added `reload(Path)` method that
   clears the list, resets `csvPath`, and re-initializes and re-loads from
   the new directory.
6. `AppLauncher` (modified) — resolves default settings path, builds defaults
   map, calls `SettingsFileReader.load`, merges defaults for missing keys,
   constructs `AppSettings`, calls `AppSettings.setInstance`, then passes the
   instance into `MainWindow`.
7. `MainWindow` (modified) — constructor now accepts `AppSettings`; passes
   `appSettings.dataDir()` to `ServerStore(Path)`; passes `appSettings` and
   `store` to `SettingsView`; `buildViewForItem` uses a `switch` expression
   and routes `"settings"` to `SettingsView` instead of
   `PlaceholderContentView`.
8. `SettingsView` — `com.example.vibebot.ui.settings`; two labeled text fields
   (dataDir first, sshKeyPath below); single Save button; validates dataDir as
   a `Path`; writes to disk via `SettingsFileWriter`; updates in-memory state
   via `AppSettings.update`; reloads `ServerStore` on a virtual thread when
   dataDir changes, then posts result back to EDT via `SwingUtilities.invokeLater`.
