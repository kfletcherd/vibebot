---
id: "010"
title: Move CSV Utilities Under Util and Document Util's Purpose
status: done
created: 2026-05-12
updated: 2026-05-12
---

## Summary

Relocates the CSV helpers that currently live under the top-level `io` area into the project's shared `util` area, so that all generic, reusable helpers live together in one place. Also adds a written description for the `util` area itself so future contributors understand what belongs there.

## Goals

- Make the CSV helpers part of the shared utilities collection rather than a separate top-level area.
- Eliminate the standalone `io` area, since CSV is its only current occupant.
- Give the `util` area an explicit, written purpose so contributors know it is the home for generic, reusable helpers usable in multiple contexts.

## Acceptance Criteria

- The CSV helpers are reachable from inside the `util` area instead of from the old `io` area.
- The old `io` area no longer exists in the source tree (no leftover empty folders).
- Every place in the app that used the CSV helpers continues to work exactly as before, with no user-visible change in behavior.
- The `util` area carries a short written description, viewable as standard Javadoc, that states it is intended for generic, reusable helpers that can be used in multiple contexts.
- The project still builds and runs through the existing make-based build with no warnings introduced by this change.

## Open Questions

- None. The new home for the CSV helpers is inside the existing `util` area; the exact sub-location is an architecture decision.

## Architecture

### Package Structure

```
src/
└── util/                  # generic, reusable helpers usable in multiple contexts
    ├── package-info.java  # written description of util's purpose
    ├── csv/               # CSV reading/writing helpers (moved from io.csv)
    │   ├── CsvParser.java
    │   └── CsvRecord.java
    └── ui/                # existing ui sub-area (unchanged by this plan)
        ├── pane/
        ├── table/
        └── theme/
```

The existing `src/io/` directory (and its `csv/` subdirectory) is removed entirely after the move, since `io.csv` is its only occupant.

### Components

| Component | Type | Package | Purpose |
|---|---|---|---|
| `CsvParser` | `final class` | `util.csv` | Stateless RFC 4180 CSV reader/writer (relocated; package change only) |
| `CsvRecord` | `record` | `util.csv` | Immutable ordered list of raw string fields for one CSV row (relocated; package change only) |
| `package-info` | `package-info.java` | `util` | Javadoc describing the `util` area as the home for generic, reusable helpers |
| `ServerStore` | `class` (existing) | `server.model` | Updated to import from `util.csv` instead of `io.csv` |
| `ServerCsvMapper` | `class` (existing) | `server.model` | Updated to import from `util.csv` instead of `io.csv` and to fix the `{@link io.csv.CsvParser}` reference in its Javadoc |

### Signatures

The two relocated types keep their existing public API verbatim. Only their `package` declaration changes.

```java
// util/csv/CsvParser.java
package util.csv;

public final class CsvParser {
    public static List<CsvRecord> read(Path path) throws IOException;
    public static void write(Path path, List<CsvRecord> records) throws IOException;
}

// util/csv/CsvRecord.java
package util.csv;

public record CsvRecord(List<String> fields) {
    public CsvRecord;          // compact canonical constructor (validation/defensive copy)
    public int size();
    public String get(int index);
}
```

The `util` area's purpose is captured as a Javadoc comment on a `package-info.java` file:

```java
// util/package-info.java
/**
 * Generic, reusable helpers usable in multiple contexts.
 *
 * <p>Code in {@code util} and its subpackages must be domain-agnostic — it must
 * not depend on any application-specific package (such as {@code server},
 * {@code settings}, or {@code app}). Anything that is broadly useful across
 * unrelated parts of the codebase belongs here; anything specific to a single
 * feature does not.
 */
package util;
```

Consumer import updates (no behavior change):

```java
// server/model/ServerStore.java
import util.csv.CsvParser;
import util.csv.CsvRecord;

// server/model/ServerCsvMapper.java
import util.csv.CsvRecord;
// And update the Javadoc {@link io.csv.CsvParser} reference to {@link util.csv.CsvParser}.
```

### Relationships

- `CsvParser` depends on `CsvRecord`; both live together in `util.csv` and have no other project dependencies (standard library only).
- `ServerStore` and `ServerCsvMapper` are the only consumers of the CSV helpers. After this plan, their imports point at `util.csv` instead of `io.csv`; their behavior is unchanged.
- `package-info.java` for `util` is documentation only — it produces no runtime code and has no dependencies.
- The old `io` package (and `io.csv` subpackage) is deleted entirely. No file in the source tree should reference `io.csv` after this work.

### Implementation Order

Work through these one at a time. After each step, the project must still compile via `make compile`.

1. `util/csv/CsvRecord.java` — create by moving the file from `src/io/csv/CsvRecord.java`, changing its `package` declaration to `util.csv`. No other content changes.
2. `util/csv/CsvParser.java` — create by moving the file from `src/io/csv/CsvParser.java`, changing its `package` declaration to `util.csv`. No other content changes.
3. `server/model/ServerStore.java` — update the two `io.csv.*` imports to `util.csv.*`.
4. `server/model/ServerCsvMapper.java` — update the `io.csv.CsvRecord` import to `util.csv.CsvRecord` and update the `{@link io.csv.CsvParser}` Javadoc reference to `{@link util.csv.CsvParser}`.
5. Delete the now-empty `src/io/csv/` and `src/io/` directories so no stray empty folders remain.
6. `util/package-info.java` — add the package-level Javadoc describing the `util` area's purpose (text shown in the Signatures subsection above).
7. Verify with `make clean && make compile` (and a quick `grep -r "io\.csv\|io/csv" src` to confirm zero remaining references) that the project still builds with no new warnings.

## Implementation Notes

<!-- java-coder appends progress notes here as it works through this plan -->

- 2026-05-12 — Moved `src/io/csv/CsvRecord.java` to `src/util/csv/CsvRecord.java` via `git mv` and updated its `package` declaration to `util.csv`. File content otherwise unchanged.
- 2026-05-12 — Moved `src/io/csv/CsvParser.java` to `src/util/csv/CsvParser.java` via `git mv` and updated its `package` declaration to `util.csv`. File content otherwise unchanged.
- 2026-05-12 — Updated `server/model/ServerStore.java` imports from `io.csv.*` to `util.csv.*`.
- 2026-05-12 — Updated `server/model/ServerCsvMapper.java`: import changed from `io.csv.CsvRecord` to `util.csv.CsvRecord`, and the `{@link io.csv.CsvParser}` Javadoc reference updated to `{@link util.csv.CsvParser}`.
- 2026-05-12 — Removed the now-empty `src/io/csv/` and `src/io/` directories.
- 2026-05-12 — Added `src/util/package-info.java` describing `util` as the home for generic, reusable, domain-agnostic helpers. Deviated from the plan's example docblock to comply with the agent's Hard Rules (no HTML tags, no multi-paragraph prose in docblocks); kept the substance identical.
- 2026-05-12 — Verified with `make clean && make compile`: builds successfully with no warnings. `grep -rn "io\.csv\|io/csv" src` returns zero matches.
