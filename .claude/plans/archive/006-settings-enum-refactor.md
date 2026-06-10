---
id: "006"
title: Settings Enum Refactor — SettingsKeys enum and Immutable AppSettings
status: done
created: 2026-04-17
updated: 2026-04-17
---

## Summary

This plan refactors the settings subsystem in two coordinated steps. First,
`SettingsKeys` is converted from a constants class to an enum whose constants carry
a `key()` accessor returning the raw file string. Second, `AppSettings` is made
fully immutable by replacing `volatile` mutable fields with `public final` fields,
removing `update()`, and replacing all mutation call sites with construction of a
new `AppSettings` instance via `AppSettings.load(Map<String, String>)`.

The two changes are tightly coupled — most call sites that change for one also
change for the other — so they are delivered in a single plan with a clear
implementation order.

## Package Structure

No new packages are introduced. All changes are modifications to existing files
within the packages already established by plan-005.

```
com.example.vibebot
├── app/
│   └── AppSettings        # modified: immutable public final fields, load() factory
├── settings/
│   └── SettingsKeys       # modified: constants class → enum with key() accessor
└── ui/settings/
    └── SettingsView       # modified: replace update() calls with new instance + setInstance()
```

`AppLauncher` is also modified (in `app/`) to use `SettingsKeys` enum constants
where it currently passes raw strings.

## Components

| Component | Type | Package | Change |
|---|---|---|---|
| `SettingsKeys` | `enum` | `settings` | Convert from `final class` with `static final String` constants to an `enum` with a `key()` accessor |
| `AppSettings` | `class` | `app` | Remove `volatile` mutable fields and `update()`; add `public final` fields; rename constructor or add `load()` factory |
| `SettingsView` | `class` | `ui.settings` | Replace `appSettings.update(...)` calls with fresh `AppSettings.load(map)` construction and `AppSettings.setInstance(newSettings)` |
| `AppLauncher` | `class` | `app` | Replace `SettingsKeys.DATA_DIR` / `SettingsKeys.SSH_KEY_PATH` string references with `SettingsKeys.DATA_DIR.key()` / `SettingsKeys.SSH_KEY_PATH.key()` |

`SettingsFileReader` and `SettingsFileWriter` do not need changes — they deal only
in raw `String` keys as map keys, and `AppSettings.toMap()` remains responsible for
producing that map.

## SettingsKeys Enum Design

```
public enum SettingsKeys {
    DATA_DIR("dataDir"),
    SSH_KEY_PATH("sshKeyPath");

    private final String key;

    SettingsKeys(String key) { this.key = key; }

    /** Returns the raw string written to and read from the settings file. */
    public String key() { return key; }
}
```

### Call-site decisions

The goal is to use the enum type directly wherever the API can be changed.
`.key()` is used only where the string form is externally fixed and cannot
realistically be replaced with the enum type.

| Call site | Before | After | Rationale |
|---|---|---|---|
| `AppSettings.load()` internal lookup | `raw.getOrDefault(SettingsKeys.DATA_DIR, ...)` | `raw.getOrDefault(SettingsKeys.DATA_DIR.key(), ...)` | `raw` is a `Map<String, String>` produced by `SettingsFileReader` which parses an on-disk file; its keys are raw strings that cannot be changed to enum types without rewriting the file format — `.key()` is the correct boundary crossing here |
| `AppSettings.toMap()` | `SettingsKeys.DATA_DIR` as map key | `SettingsKeys.DATA_DIR.key()` as map key | Same — the map is passed to `SettingsFileWriter` which writes raw `key=value` lines; the string form is required |
| `AppSettings.update()` switch | `case SettingsKeys.DATA_DIR ->` | removed entirely | `update()` is deleted by this plan |
| `AppLauncher.buildDefaults()` | `defaults.put(SettingsKeys.DATA_DIR, ...)` | `defaults.put(SettingsKeys.DATA_DIR.key(), ...)` | `defaults` is a `Map<String, String>` passed to `SettingsFileReader.load` and ultimately written to disk; `.key()` is required at this file-format boundary |
| `SettingsView.buildSettingsMap()` | `map.put(SettingsKeys.DATA_DIR, ...)` | `map.put(SettingsKeys.DATA_DIR.key(), ...)` | Same — this map is passed directly to `SettingsFileWriter.write` |
| `SettingsView` class-level Javadoc references | `{@link SettingsKeys#DATA_DIR}` | remains; the field still exists on the enum | No change needed |

In this particular refactor every existing internal call site turns out to involve a
`Map<String, String>` that feeds the file parser or writer — a genuine external
boundary. If any future internal method is added that accepts a settings key without
touching the file format, it must accept `SettingsKeys` directly, not `String`.

## AppSettings Immutability Design

### Fields

Remove the three `private volatile` instance fields. Replace with:

```java
public final Path dataDir;
public final String sshKeyPath;
public final Path settingsFilePath;
```

`public final` is preferred over private + accessor methods for a value-object-style
class. The existing accessor methods (`dataDir()`, `sshKeyPath()`, `settingsFilePath()`)
may be removed or retained as thin forwarding methods — java-coder's call. If
retained, their Javadoc should note that they delegate to the public field.

The `DEFAULT_DATA_DIR` and `DEFAULT_SSH_KEY_PATH` constants remain unchanged.

### Constructor vs. static factory

Rename the existing constructor to a **static factory `AppSettings.load(Map<String, String> raw)`**
that returns a new `AppSettings`. The actual construction becomes a private
constructor. This matches the style used by `SettingsFileReader.load` and clearly
expresses intent: "produce a fresh instance from raw data."

Signature:

```java
public static AppSettings load(Map<String, String> raw)
```

All existing callers that wrote `new AppSettings(raw)` (only `AppLauncher`) become
`AppSettings.load(raw)`.

### Removing `update()`

`AppSettings.update(String, String)` is deleted. Its Javadoc reference in the class
Javadoc is also removed.

### SettingsView save flow

The current `onSave()` method in `SettingsView`:
1. Calls `SettingsFileWriter.write(...)` — unchanged.
2. Calls `appSettings.update(SettingsKeys.DATA_DIR, rawDataDir)` — replaced.
3. Calls `appSettings.update(SettingsKeys.SSH_KEY_PATH, rawSshKey)` — replaced.

After this plan the replacement for steps 2-3 is:

```
Map<String, String> newRaw = buildSettingsMap(rawDataDir, rawSshKey);
AppSettings newSettings = AppSettings.load(newRaw);
AppSettings.setInstance(newSettings);
```

The `appSettings` field on `SettingsView` is currently `final`. After this change it
must either:
- Become non-final so `onSave()` can reassign it to the new instance (so that
  `previousDataDir` comparisons in future saves are against the latest saved value), OR
- Be read fresh from `AppSettings.instance()` at the top of `onSave()` each time.

The recommended approach is to read `AppSettings.instance()` at the top of
`onSave()` (removing the stored `appSettings` field entirely, or keeping it only for
field pre-population at construction time). This keeps the field final and avoids
stale-instance bugs if `setInstance` is called from another path in the future.

Concretely, `onSave()` should capture `AppSettings current = AppSettings.instance()`
at the start of the method and use `current` for `previousDataDir` comparison — not
the constructor-time snapshot.

## Relationships

`SettingsKeys` has no dependencies on other project classes; it is the lowest-level
component. `AppSettings` depends on `SettingsKeys` for its `load()` factory. `SettingsView`
depends on `AppSettings` (for `load` and `setInstance`) and on `SettingsKeys` (for
`.key()` string lookups). `AppLauncher` depends on both.

## Implementation Order

1. **`SettingsKeys`** — convert to enum; no other files need to change yet because
   the public surface is `String` constants used in raw string comparisons; after
   this step every call site that currently compiles against the old string constants
   will fail to compile, making the remaining work discoverable.
2. **`AppSettings`** — introduce `public final` fields and `static AppSettings.load(Map)`
   factory; remove `update()` and the three `volatile` fields; update the `toMap()`
   method to use `SettingsKeys.DATA_DIR.key()` etc.
3. **`AppLauncher`** — update `buildDefaults()` to use `.key()` and update
   `loadSettings()` to call `AppSettings.load(...)` instead of `new AppSettings(...)`.
4. **`SettingsView`** — remove `update()` call sites; replace with
   `AppSettings.load(newRaw)` + `AppSettings.setInstance(newSettings)`; update
   `buildSettingsMap()` to use `.key()`; update `onSave()` to read the current
   instance from `AppSettings.instance()` at the top of the method.

## Open Questions

None. All decisions are resolved in this plan.

## Implementation Notes

### 2026-04-20 — Step 1 complete: SettingsKeys converted to enum

`SettingsKeys` is now an `enum` with `DATA_DIR("dataDir")` and `SSH_KEY_PATH("sshKeyPath")`
constants and a `key()` accessor. The old `final class` with `static final String` constants
and the private no-arg constructor are gone. A compile run confirmed 9 expected errors at
`Map<String,String>` call sites in `AppSettings`, `AppLauncher`, and `SettingsView` — exactly
the breakage predicted by the plan. Steps 2–4 resolve those errors.

### 2026-04-20 — Step 2 complete: AppSettings made immutable with static factory

`AppSettings` is now fully immutable. The three `private volatile` instance fields have been
replaced with `public final Path dataDir`, `public final String sshKeyPath`, and
`public final Path settingsFilePath`. The public constructor `AppSettings(Map<String,String>)`
has been replaced with a `private` three-argument constructor and a
`public static AppSettings load(Map<String,String>)` static factory that calls
`SettingsKeys.DATA_DIR.key()` and `SettingsKeys.SSH_KEY_PATH.key()` when looking up values
in the raw map. The `update(String, String)` method and its class-level Javadoc reference
are removed. `toMap()` now uses `SettingsKeys.DATA_DIR.key()` and `SettingsKeys.SSH_KEY_PATH.key()`
as map keys. The three accessor methods (`dataDir()`, `sshKeyPath()`, `settingsFilePath()`)
are retained as thin delegates to their corresponding public fields.

Next: Step 3 — update `AppLauncher` to use `SettingsKeys.DATA_DIR.key()` / `SettingsKeys.SSH_KEY_PATH.key()`
in `buildDefaults()` and call `AppSettings.load(...)` instead of `new AppSettings(...)`.

### 2026-04-20 — Step 3 complete: AppLauncher updated

Two changes made to `AppLauncher`:

1. `buildDefaults()` — both `defaults.put(SettingsKeys.DATA_DIR, ...)` and
   `defaults.put(SettingsKeys.SSH_KEY_PATH, ...)` changed to use `.key()` so the
   enum constants are converted to their raw string form before being used as
   `Map<String, String>` keys (a file-format boundary).
2. `loadSettings()` — both `new AppSettings(mergeWithDefaults(...))` and
   `new AppSettings(defaults)` replaced with `AppSettings.load(...)` to match the
   new static factory.

A compile run confirmed `AppLauncher` is now error-free. The 4 remaining errors
are all in `SettingsView` (missing `update()` call sites + raw enum-as-string put
calls) — exactly the Step 4 work the plan describes.

### 2026-04-20 — Step 4 complete: SettingsView updated; plan complete

Three targeted changes to `SettingsView`:

1. `buildSettingsMap()` — both `map.put(SettingsKeys.DATA_DIR, ...)` and
   `map.put(SettingsKeys.SSH_KEY_PATH, ...)` updated to use `.key()`, crossing
   the file-format boundary correctly as specified by the plan.
2. `onSave()` — the two `appSettings.update(...)` calls removed and replaced
   with `AppSettings.load(newRaw)` to construct a fresh immutable instance and
   `AppSettings.setInstance(newSettings)` to publish it globally.
3. `onSave()` — a fresh capture `AppSettings current = AppSettings.instance()`
   added at the top of the method; `previousDataDir` comparison now uses
   `current.dataDir()` instead of the constructor-time snapshot stored in the
   `appSettings` field. The field itself is retained for construction-time
   text-field pre-population only.

Class-level Javadoc updated to remove the `AppSettings#update` reference and
document the new `load`/`setInstance` flow.

Stray `.class` files that had accumulated in `src/main/java` from prior direct
`javac` invocations were also deleted. All future compile/build/test steps use
`make` only.

`make` confirmed the entire project compiles and jars cleanly with zero errors.
