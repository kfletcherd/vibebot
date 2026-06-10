---
id: "004"
title: Server List Reference Pane
status: done
created: 2026-04-17
updated: 2026-04-17
---

## Summary

Adds a server list reference view as the first real use case for the content pane. A generic CSV parser reads a user-maintained CSV file (excluded from git) into typed records. A `ServerListView` implements `ContentView` and renders the servers in a themed `JTable`. An "Add Server" button opens a modal dialog that appends a new row to both the in-memory model and the CSV file on disk.

## Package Structure

```
com.example.vibebot
├── io/
│   └── csv/          # generic, reusable CSV reading and writing utilities
├── server/
│   ├── model/        # Server record and ServerStore (load/save/add)
│   └── ui/           # ServerListView (ContentView impl) and AddServerDialog
└── ui/               # (existing) — MainWindow wiring updated here
```

## Components

| Component | Type | Package | Purpose |
|---|---|---|---|
| `CsvRecord` | `record` | `io.csv` | Represents one parsed CSV row as an ordered list of string fields |
| `CsvParser` | `class` | `io.csv` | Generic stateless utility — reads a `Path` into `List<CsvRecord>` and writes `List<CsvRecord>` back to a `Path`; handles quoted fields and header row |
| `Server` | `record` | `server.model` | Immutable value object: `publicIp`, `pvlanIp`, `shortName`, `hostname`, `notes`, `environment`, `type` — all `String` |
| `ServerCsvMapper` | `class` | `server.model` | Converts between `CsvRecord` and `Server`; owns the column-index constants and the expected header row; columns are `public_ip`, `pvlan_ip`, `short_name`, `hostname`, `notes`, `environment`, `type` |
| `ServerStore` | `class` | `server.model` | Owns the runtime list of `Server` objects; loads from CSV via `CsvParser` + `ServerCsvMapper` on construction; exposes `List<Server> servers()`, `void add(Server)` (appends to list and persists to CSV); on any load failure (parse error, bad column count, malformed data) throws a checked `ServerStoreException` containing the failing file path so callers can surface a meaningful error message |
| `ServerStoreException` | `class` | `server.model` | Checked exception thrown by `ServerStore` when the CSV cannot be loaded; carries the resolved `Path` of the offending file |
| `ServerTableModel` | `class` | `server.ui` | Extends `AbstractTableModel`; backed by a `List<Server>`; seven columns displayed in order: `Public IP`, `PVLAN IP`, `Short Name`, `Environment`, `Type`, `Hostname`, `Notes`; read-only cells (no direct cell editing — adding is done through the dialog) |
| `AddServerDialog` | `class` | `server.ui` | Modal `JDialog` with labeled text fields for all seven server fields plus OK / Cancel buttons; validates that `publicIp`, `pvlanIp`, `shortName`, `environment`, and `type` are all non-blank before enabling OK; `hostname` and `notes` remain optional; calls a provided `Consumer<Server>` on confirmation |
| `ServerListView` | `class` | `server.ui` | Implements `ContentView`; on successful store load, constructs a `JScrollPane` wrapping a `JTable` backed by `ServerTableModel` plus a toolbar panel at the top containing an "Add Server" button; wires the button to open `AddServerDialog`, then delegates to `ServerStore.add` and calls `ServerTableModel.fireTableRowsInserted`; if the `ServerStore` constructor throws `ServerStoreException`, displays a plain error panel (no table, no toolbar) inside the content pane telling the user the CSV is corrupt and showing the full file path |

## Relationships

- `CsvParser` has no domain knowledge — it operates only on `Path`, `List<CsvRecord>`, and `String`. It is the only class that touches the file system directly for CSV data.
- `ServerCsvMapper` sits between `CsvParser` and `Server`. It knows column positions and the expected header row. `ServerStore` delegates all CSV serialization to `ServerCsvMapper` and all file I/O to `CsvParser`.
- `ServerStore` is constructed once (in `MainWindow`'s wiring code) and passed into `ServerListView`. This avoids the view owning I/O concerns. If construction throws `ServerStoreException`, `MainWindow` still creates a `ServerListView` — but passes the exception into it so the view can display the error panel instead of the table.
- `ServerListView` receives both a `ServerStore` and an optional `ServerStoreException` in its constructor. When the exception is non-null it renders the error panel; when null it renders the normal table + toolbar layout. The `ServerTableModel` is only constructed in the non-error path.
- `MainWindow.buildNavItems` must be updated to replace the `"chat"` nav item with a new `"servers"` nav item (label `"Servers"`). The `"home"` and `"settings"` nav items remain unchanged. `MainWindow.buildContentViews` must register the `ServerListView` under the `"servers"` id.

## Implementation Order

1. `CsvRecord` — value object, no dependencies
2. `CsvParser` — depends only on `CsvRecord` and `java.nio.file.Path`
3. `Server` — value object, no dependencies
4. `ServerCsvMapper` — depends on `CsvRecord` and `Server`
5. `ServerStoreException` — checked exception, no domain dependencies beyond `java.nio.file.Path`
6. `ServerStore` — depends on `CsvParser`, `ServerCsvMapper`, `Server`, and `ServerStoreException`; throws `ServerStoreException` on any load failure
7. `ServerTableModel` — depends on `Server`; extends `AbstractTableModel`
8. `AddServerDialog` — depends on `Server` and `DarkTheme`; pure UI, no store access
9. `ServerListView` — depends on all of the above; renders error panel when `ServerStoreException` is present, normal table + toolbar otherwise; implements `ContentView`
10. Update `MainWindow` — replace the `"chat"` nav item with a `"servers"` nav item (label `"Servers"`); construct `ServerStore` (catching `ServerStoreException`) and pass both store and exception into `ServerListView`; register the view under id `"servers"` in `buildContentViews`

## CSV File Location and Git Exclusion

The CSV file must live outside the compiled output directory so it is not clobbered by `make clean`. The recommended location is `~/.vibebot/servers.csv` (i.e., `System.getProperty("user.home") + "/.vibebot/servers.csv"`). This path is resolved at application startup and is not configurable in this plan.

`ServerStore` must create the file (and its parent directory) if it does not exist, writing only the header row so the application starts cleanly on a fresh machine.

The file is already excluded from git by virtue of living outside the repository. No `.gitignore` changes are needed.

## CSV Format

```
public_ip,pvlan_ip,short_name,hostname,notes,environment,type
10.0.0.1,192.168.1.1,web01,web01.example.com,primary web server,HV,api server
```

- First row is always the header. `CsvParser` reads it as a regular `CsvRecord`; `ServerCsvMapper` validates it against expected column names.
- Fields are comma-separated. Fields containing commas or double-quotes must be quoted per RFC 4180 (double-quote escaping). `CsvParser` must handle this.
- Blank `hostname` and `notes` fields are valid — the `Server` record stores them as empty strings, not null.
- `environment` and `type` are required fields; the dialog prevents saving a row with either blank, but `ServerStore` and `ServerCsvMapper` store whatever value is in the CSV without re-validating (existing files may predate this requirement).

## UI Layout

`ServerListView` should arrange its component as a `JPanel` with `BorderLayout`:

- `NORTH` — a thin toolbar `JPanel` (`FlowLayout`, right-aligned) containing the "Add Server" button styled with `DarkTheme.ACCENT` foreground.
- `CENTER` — a `JScrollPane` wrapping the `JTable`. The table header and cell colors must respect `DarkTheme` values (background `DarkTheme.SURFACE`, foreground `DarkTheme.FOREGROUND`, selection colors from `DarkTheme.SELECTION_BG` / `DarkTheme.SELECTION_FG`, grid color `DarkTheme.BORDER`).

Column widths do not need to be precisely controlled in this plan — default proportional sizing is acceptable.

### Error State

When `ServerListView` is constructed with a non-null `ServerStoreException` (meaning the CSV could not be loaded), the entire content area must be replaced with a plain error panel. The error panel is a `JPanel` with a centered `JLabel` (or multi-line `JTextArea` set to non-editable) whose text reads approximately:

> The server list could not be loaded because the CSV file is corrupt or unreadable.
> Please fix or delete the file and restart the application.
> File: &lt;resolved path from ServerStoreException&gt;

The error panel background should use `DarkTheme.SURFACE` and the text foreground `DarkTheme.FOREGROUND`. No table, toolbar, or "Add Server" button is shown in this state.

## Resolved Decisions

1. **Nav wiring (resolved).** The `"chat"` nav item is replaced by a new `"servers"` nav item (label `"Servers"`). The `"home"` and `"settings"` nav items remain unchanged. `MainWindow.buildNavItems` and `buildContentViews` must be updated accordingly (step 10 above).

2. **Corrupt/unreadable CSV (resolved).** `ServerStore` throws a checked `ServerStoreException` on any load failure (parse error, bad column count, malformed data). `ServerListView` catches this at construction time and displays an error panel in the content pane — no table or toolbar is shown. The error panel includes the full file path so the user knows where to look. See the Error State sub-section under UI Layout for exact wording and styling.

## Implementation Notes

### 2026-04-17 — All components complete

Built all ten implementation steps in a single session:

1. `CsvRecord` — immutable record with defensive copy and convenience accessors; `com.example.vibebot.io.csv`.
2. `CsvParser` — stateless RFC 4180 read/write utility; handles quoted fields, `""` escaping, blank-line skipping; `com.example.vibebot.io.csv`.
3. `Server` — seven-field immutable record with null guards; `com.example.vibebot.server.model`.
4. `ServerCsvMapper` — column-index constants, canonical header record, `isValidHeader`, `toServer`, `toRecord`; `com.example.vibebot.server.model`.
5. `ServerStoreException` — checked exception with `filePath()` accessor; `com.example.vibebot.server.model`.
6. `ServerStore` — resolves `~/.vibebot/servers.csv`, creates file+dir on first run, loads and validates header, throws `ServerStoreException` on any failure; `add()` appends in-memory and rewrites CSV; `com.example.vibebot.server.model`.
7. `ServerTableModel` — extends `AbstractTableModel`; presentation column order differs from CSV order (Environment/Type promoted to columns 3–4); `addServer()` fires insert event; `com.example.vibebot.server.ui`.
8. `AddServerDialog` — modal `JDialog`; OK enabled only when five required fields are non-blank; `DocumentListener` validation; `Consumer<Server>` callback; dark-themed; `com.example.vibebot.server.ui`.
9. `ServerListView` — implements `ContentView`; renders table+toolbar on success, error panel on `ServerStoreException`; `handleServerAdded` persists via store then updates model on EDT; `com.example.vibebot.server.ui`.
10. `MainWindow` — `"chat"` nav item replaced by `"servers"` (label `"Servers"`); `buildContentViews` now accepts `JFrame ownerFrame`; `ServerStore` constructed and `ServerStoreException` caught here; `buildViewForItem` helper dispatches to `ServerListView` for `"servers"` and `PlaceholderContentView` for all others.

All files compile clean (`make compile` zero errors).
