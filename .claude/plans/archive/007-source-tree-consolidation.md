---
id: "007"
title: Source Tree Consolidation
status: done
created: 2026-05-05
updated: 2026-05-05
---

## Summary

The project currently has two separate source folders. The newer folder is where recent work has been going; the older folder still holds the bulk of the application. This plan brings all source code together into one folder so there is a single, consistent place to find and add code going forward.

## Goals

- All source code lives under one folder.
- The project builds cleanly from that single folder with no references to the old location.
- The old folder is removed entirely once the migration is complete.

## Acceptance Criteria

- Opening any source file leads to the same root folder, regardless of which part of the application it belongs to.
- The application launches and behaves identically after the migration — no functional changes.
- The old source folder no longer exists in the project.
- The build produces no errors or warnings related to missing or misplaced files.

## Open Questions

None.

## Architecture

### Package Structure

```
src/
├── app/                        — bootstrap: entry point, launcher, settings singleton
├── io/
│   └── csv/                    — domain-agnostic CSV parser and record type
├── server/
│   ├── model/                  — Server value object, column enum, CSV mapper, store
│   └── ui/                     — ServerListView, ServerTableModel, AddServerDialog
├── settings/                   — SettingsKeys enum, file reader and writer
├── ui/
│   ├── nav/                    — NavItem record, NavPane (kept from new tree)
│   └── MainWindow.java         — primary application window (migrated from old tree)
└── util/
    └── ui/
        ├── pane/               — PaneView interface, CardPane (kept from new tree)
        ├── table/              — Table, TableModel (kept from new tree)
        └── theme/              — StandardColor enum, StandardTheme (kept from new tree)
```

### Components

| Component | Type | Package | Purpose |
|---|---|---|---|
| `Main` | `final class` | `app` | JVM entry point; delegates to `AppLauncher.launch()` |
| `AppLauncher` | `final class` | `app` | Loads settings, applies theme, schedules EDT window creation |
| `AppSettings` | `final class` | `app` | Immutable settings singleton; `load`/`loadTyped`/`instance`/`setInstance` |
| `CsvParser` | `final class` | `io.csv` | Stateless RFC-4180 CSV reader and writer |
| `CsvRecord` | `record` | `io.csv` | Ordered, immutable list of string fields for one CSV row |
| `Server` | `record` | `server.model` | Immutable value object for a single server entry |
| `ServerColumn` | `enum` | `server.model` | Single source of truth for column index, CSV header, and display name |
| `ServerCsvMapper` | `final class` | `server.model` | Converts between `CsvRecord` and `Server`; owns CSV schema logic |
| `ServerStore` | `final class` | `server.model` | In-memory server repository backed by `servers.csv` |
| `ServerStoreException` | `final class` | `server.model` | Checked exception carrying the offending `Path` |
| `ServerTableModel` | `final class` | `server.ui` | `AbstractTableModel` backed by a `List<Server>`; uses `PRESENTATION_ORDER` |
| `ServerListView` | `final class` | `server.ui` | `PaneView` showing the server table or an error panel |
| `AddServerDialog` | `final class` | `server.ui` | Modal dialog for adding a new `Server`; validates required fields |
| `SettingsKeys` | `enum` | `settings` | Typed enumeration of every settings-file key; exposes `key()` for I/O boundaries |
| `SettingsFileReader` | `final class` | `settings` | Reads `key=value` settings file; creates with defaults on first run |
| `SettingsFileWriter` | `final class` | `settings` | Writes a `Map<String,String>` to a `key=value` file |
| `SettingsView` | `final class` | `server.ui` → `ui` (see note) | `PaneView` form for editing `dataDir` and `sshKeyPath` |
| `MainWindow` | `final class` | `ui` | Primary `JFrame` wrapper; builds nav+content split-pane layout |
| `NavItem` | `record` | `ui.nav` | Nav entry with `id` and `label`; kept from new tree |
| `NavPane` | `final class` | `ui.nav` | `JList`-based nav pane with `Consumer<NavItem>` selection callback; kept from new tree |
| `CardPane` | `final class` | `util.ui.pane` | `CardLayout` host for `PaneView` instances; kept from new tree |
| `PaneView` | `interface` | `util.ui.pane` | Contract for views managed by `CardPane`; replaces old `ContentView` |
| `Table` | `final class` | `util.ui.table` | Generic `JTable`+`JScrollPane` wrapper; kept from new tree |
| `TableModel` | `class` | `util.ui.table` | Package-private `AbstractTableModel` backing `Table`; kept from new tree |
| `StandardColor` | `enum` | `util.ui.theme` | Palette of named `Color` constants; kept from new tree |
| `StandardTheme` | `final class` | `util.ui.theme` | Applies palette to `UIManager` defaults; replaces old `DarkTheme`/`ThemeApplicator` |

> Note: `SettingsView` currently lives under `ui/settings/` in the old tree; it migrates to `src/ui/` as package `ui` alongside `MainWindow`, keeping a flat `ui` package rather than introducing a `ui.settings` sub-package.

### Signatures

#### `app.Main`
```java
public final class Main {
    public static void main(String[] args);
}
```

#### `app.AppLauncher`
```java
public final class AppLauncher {
    public static void launch();
}
```

#### `app.AppSettings`
```java
public final class AppSettings {
    public static final String DEFAULT_DATA_DIR;
    public static final String DEFAULT_SSH_KEY_PATH;
    public final Path dataDir;
    public final String sshKeyPath;
    public final Path settingsFilePath;
    public static AppSettings load(Map<String, String> raw);
    public static AppSettings loadTyped(Map<SettingsKeys, String> typed);
    public static AppSettings instance();
    public static void setInstance(AppSettings settings);
    public Path dataDir();
    public String sshKeyPath();
    public Path settingsFilePath();
    public Map<String, String> toMap();
}
```

#### `io.csv.CsvParser`
```java
public final class CsvParser {
    public static List<CsvRecord> read(Path path) throws IOException;
    public static void write(Path path, List<CsvRecord> records) throws IOException;
}
```

#### `io.csv.CsvRecord`
```java
public record CsvRecord(List<String> fields) {
    public CsvRecord { /* compact constructor — validates and defensively copies */ }
    public int size();
    public String get(int index);
}
```

#### `server.model.Server`
```java
public record Server(
    String publicIp, String pvlanIp, String shortName,
    String hostname, String notes, String environment, String type
) {
    public Server { /* compact constructor — null-checks all fields */ }
}
```

#### `server.model.ServerColumn`
```java
public enum ServerColumn {
    PUBLIC_IP, PVLAN_IP, SHORT_NAME, HOSTNAME, NOTES, ENVIRONMENT, TYPE;
    public static final int COUNT;
    public int csvIndex();
    public String csvHeader();
    public String displayName();
}
```

#### `server.model.ServerCsvMapper`
```java
public final class ServerCsvMapper {
    public static CsvRecord headerRecord();
    public static boolean isValidHeader(CsvRecord record);
    public static Server toServer(CsvRecord record);
    public static CsvRecord toRecord(Server server);
}
```

#### `server.model.ServerStore`
```java
public final class ServerStore {
    public ServerStore() throws ServerStoreException;
    public ServerStore(Path dataDir) throws ServerStoreException;
    public List<Server> servers();
    public void add(Server server) throws IOException;
    public Path csvPath();
    public void reload(Path newDataDir) throws ServerStoreException;
}
```

#### `server.model.ServerStoreException`
```java
public final class ServerStoreException extends Exception {
    public ServerStoreException(String message, Path filePath);
    public ServerStoreException(String message, Path filePath, Throwable cause);
    public Path filePath();
}
```

#### `server.ui.ServerTableModel`
```java
final class ServerTableModel extends AbstractTableModel {
    ServerTableModel(List<Server> servers);
    public void addServer(Server server);
    // getRowCount, getColumnCount, getColumnName, getValueAt, isCellEditable — overrides
}
```

#### `server.ui.ServerListView`
```java
public final class ServerListView implements PaneView {
    public ServerListView(JFrame ownerFrame, ServerStore store, Optional<ServerStoreException> loadError);
    @Override public String id();
    @Override public JComponent component();
}
```

#### `server.ui.AddServerDialog`
```java
public final class AddServerDialog {
    public AddServerDialog(JFrame owner, Consumer<Server> onConfirm);
}
```

#### `settings.SettingsKeys`
```java
public enum SettingsKeys {
    DATA_DIR, SSH_KEY_PATH;
    public String key();
}
```

#### `settings.SettingsFileReader`
```java
public final class SettingsFileReader {
    public static Map<String, String> load(Path path, Map<String, String> defaults) throws IOException;
}
```

#### `settings.SettingsFileWriter`
```java
public final class SettingsFileWriter {
    public static void write(Path path, Map<String, String> settings) throws IOException;
}
```

#### `ui.SettingsView`
```java
public final class SettingsView implements PaneView {
    public SettingsView(AppSettings appSettings, ServerStore serverStore);
    @Override public String id();
    @Override public JComponent component();
}
```

#### `ui.MainWindow`
```java
public final class MainWindow {
    public MainWindow(AppSettings appSettings);
    public void show();
    public Dimension getPreferredWindowSize();
}
```

### Relationships

- `Main` delegates entirely to `AppLauncher.launch()`.
- `AppLauncher` reads settings via `SettingsFileReader`, builds an `AppSettings` singleton via `AppSettings.load`, applies `StandardTheme.apply()`, then schedules `MainWindow` construction on the EDT.
- `MainWindow` constructs `NavPane` and `CardPane` side by side in a `JSplitPane`. It creates one `PaneView` per `NavItem`: `ServerListView` for `"servers"`, `SettingsView` for `"settings"`, and a private `PlaceholderContentView` inner class for any other id. A `Consumer<NavItem>` passed to `NavPane` calls `CardPane.show(item.id())` to flip views.
- `ServerListView` and `SettingsView` both implement `PaneView` (replacing the old `ContentView` interface). Their `id()` method returns the string that matches the owning `NavItem`'s id.
- `ServerListView` holds a `ServerStore` and a `ServerTableModel`. On "Add Server", it opens `AddServerDialog` and on confirm calls `ServerStore.add` then `ServerTableModel.addServer`.
- `SettingsView` reads and writes settings through `SettingsFileWriter`, replaces the `AppSettings` singleton, and calls `ServerStore.reload` on a virtual thread when the data directory changes.
- `ServerStore` delegates CSV I/O to `CsvParser` and row mapping to `ServerCsvMapper`, which in turn consults `ServerColumn` for all column metadata.
- All UI classes obtain colors exclusively through `StandardColor` and rely on `StandardTheme.apply()` having been called before any component is constructed.
- The old `ui/content/ContentView`, `ui/content/ContentPane`, `ui/nav/NavItem`, `ui/nav/NavPane`, `ui/nav/NavSelectionListener`, and `ui/theme/DarkTheme`+`ThemeApplicator` classes are entirely retired; their roles are covered by `PaneView`, `CardPane`, `ui.nav.NavItem`, `ui.nav.NavPane` (new tree), and `StandardTheme`/`StandardColor`.

### Implementation Order

1. `io.csv.CsvParser` — migrate to `src/io/csv/CsvParser.java`; update package declaration only.
2. `io.csv.CsvRecord` — migrate to `src/io/csv/CsvRecord.java`; update package declaration only.
3. `settings.SettingsKeys` — migrate to `src/settings/SettingsKeys.java`; update package declaration only.
4. `settings.SettingsFileWriter` — migrate to `src/settings/SettingsFileWriter.java`; update package declaration only.
5. `settings.SettingsFileReader` — migrate to `src/settings/SettingsFileReader.java`; update package declaration and import of `SettingsFileWriter`.
6. `server.model.ServerColumn` — migrate to `src/server/model/ServerColumn.java`; update package declaration only.
7. `server.model.Server` — migrate to `src/server/model/Server.java`; update package declaration only.
8. `server.model.ServerCsvMapper` — migrate to `src/server/model/ServerCsvMapper.java`; update package declaration and imports (`io.csv.*`, `server.model.*`).
9. `server.model.ServerStoreException` — migrate to `src/server/model/ServerStoreException.java`; update package declaration only.
10. `server.model.ServerStore` — migrate to `src/server/model/ServerStore.java`; update package declaration and imports (`app.AppSettings`, `io.csv.*`, `server.model.*`).
11. `server.ui.ServerTableModel` — migrate to `src/server/ui/ServerTableModel.java`; update package declaration and imports (`server.model.*`).
12. `server.ui.AddServerDialog` — migrate to `src/server/ui/AddServerDialog.java`; update package declaration and imports (`server.model.Server`); replace all `DarkTheme.*` color references with `StandardColor.*`.
13. `server.ui.ServerListView` — migrate to `src/server/ui/ServerListView.java`; implement `PaneView` instead of `ContentView`; rename `viewId()` to `id()`; update all imports and replace `DarkTheme.*` with `StandardColor.*`.
14. `app.AppSettings` — migrate to `src/app/AppSettings.java`; update package declaration and imports (`settings.SettingsKeys`).
15. `ui.SettingsView` — migrate to `src/ui/SettingsView.java` (package `ui`, not `ui.settings`); implement `PaneView` instead of `ContentView`; rename `viewId()` to `id()`; update all imports and replace `DarkTheme.*` with `StandardColor.*`.
16. `ui.MainWindow` — migrate to `src/ui/MainWindow.java`; update package declaration and all imports to flat package names; replace `ContentPane`/`ContentView` with `CardPane`/`PaneView`; replace `ThemeApplicator`/`DarkTheme` usages with `StandardColor`; use `ui.nav.NavItem` and `ui.nav.NavPane` from the new tree.
17. `app.AppLauncher` — migrate to `src/app/AppLauncher.java`; update package declaration and imports; replace `ThemeApplicator.apply()` with `StandardTheme.apply()`; update `MainWindow` import to `ui.MainWindow`.
18. `app.Main` — migrate to `src/app/Main.java`; update package declaration and import of `AppLauncher`.
19. `Makefile` — update `SRC_DIR` to `src/`, update `SOURCES` find expression to cover the single `src/` tree, update `MAIN_CLASS` to `app.Main`; verify `make run` produces a clean build.
20. Delete `src/main/` — remove the old source tree after confirming the build is clean and the application launches correctly.

## Implementation Notes

All 20 steps completed in a single session (2026-05-05).

Steps 1–18: Migrated every class from `src/main/java/com/example/vibebot/` to the
flat `src/` tree with package names matching the plan's Architecture table. Key
substitutions applied throughout:

- `DarkTheme.BACKGROUND` → `StandardColor.BACKGROUND_PRIMARY.color`
- `DarkTheme.SURFACE` → `StandardColor.BACKGROUND_ELEVATED.color`
- `DarkTheme.FOREGROUND` → `StandardColor.TEXT_PRIMARY.color`
- `DarkTheme.TEXT_MUTED` → `StandardColor.TEXT_SECONDARY.color`
- `DarkTheme.ACCENT` → `StandardColor.ACCENT.color`
- `DarkTheme.BORDER` → `StandardColor.BORDER.color`
- `DarkTheme.SELECTION_BG` → `StandardColor.BACKGROUND_SELECTED.color`
- `DarkTheme.SELECTION_FG` → `StandardColor.FOREGROUND_SELECTED.color`
- `ContentView` interface → `PaneView`; `viewId()` → `id()`
- `ContentPane` → `CardPane`
- Old `ui.nav.NavItem`/`ui.nav.NavPane`/`NavSelectionListener` → new-tree `ui.nav.NavItem`/`ui.nav.NavPane` with `Consumer<NavItem>` callback
- `ThemeApplicator.apply()` → `StandardTheme.apply()` in `AppLauncher`
- `PlaceholderContentView` renamed to `PlaceholderPaneView` in `MainWindow`

Step 19: Updated `Makefile` — `SRC_DIR` to `src/`, `SOURCES` find to `find $(SRC_DIR) -name "*.java"`,
`MAIN_CLASS` to `app.Main`.

Step 20: Deleted `src/main/` after confirming `make compile` and `make jar` produce zero errors.
The build now compiles 26 source files, all from `src/`.
