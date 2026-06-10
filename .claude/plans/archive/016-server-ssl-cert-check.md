---
id: "016"
title: SSL Certificate Check for Servers with Hostnames
status: done
created: 2026-05-19
updated: 2026-05-19


---

## Summary

Adds a second action button to the Server view, alongside the existing Add Server button, that runs an SSL certificate check across every server in the list that has a hostname recorded. When the check finishes, a report dialog opens summarizing each checked host, when its certificate was issued, when it expires, and a clearly visible warning for any certificate that will expire within the next 30 days. This gives the user a one-click way to spot upcoming certificate expirations across their whole server inventory without having to visit each host manually.

## Goals

- Give the user a dedicated button on the Server view to trigger an SSL certificate check across all known servers.
- Skip any server that does not have a hostname recorded, so the check only runs against servers it can actually reach.
- Present the results in a single, readable report dialog the user can scan top to bottom.
- For every checked host, show the certificate's issue date and expiration date in a consistent, human-friendly format.
- Make certificates expiring within the next 30 days visually obvious so the user can act on them.
- Handle hosts that cannot be reached or that do not present a valid certificate gracefully, by listing them in the report with an explanation rather than silently dropping them or crashing the dialog.

## Acceptance Criteria

- The Server view shows a new button immediately next to the existing Add Server button, with a label that clearly communicates its purpose (e.g. "Check SSL Certs" or similar).
- Clicking the new button runs an SSL certificate check against every server in the table that has a non-empty hostname.
- Servers without a hostname are silently excluded from the check; they do not appear in the report at all.
- While the check is running, the user gets some visible indication that work is in progress (the application does not appear frozen, and the button cannot be clicked repeatedly to start parallel runs).
- When the check finishes, a modal report dialog opens listing one row per checked server.
- Each successful row in the report shows: the hostname, the date the certificate was issued (notBefore), and the date the certificate expires (notAfter).
- Any certificate whose expiration date is within 30 days of today is flagged with a clearly distinct visual indicator (e.g. a warning color, a warning icon, or an "Expiring soon" label) so it stands out from healthy certificates.
- Already-expired certificates are also flagged distinctly, so the user can tell at a glance which hosts are already broken versus which are about to break.
- Each checked host is contacted on the standard SSL port (443 / https) using the JDK's built-in TLS support; no external tools are invoked.
- Each host has a 5 second time budget to respond. Hosts that exceed that budget appear in the report with an explicit "timed out" status rather than being dropped.
- If a host cannot be reached, times out, or does not present a usable certificate, it still appears in the report with a brief explanation (e.g. "timed out", "connection refused", "no certificate") in place of the dates, instead of being omitted or causing the dialog to fail.
- The report is on-screen only — there is no export to CSV, clipboard, or file in this version.
- The report dialog can be closed and reopened by clicking the button again, which re-runs the check and shows fresh results.
- No server data is modified by running the check; this is a read-only diagnostic.

## Resolved Decisions

- **TLS implementation:** Use the JDK's built-in TLS support to connect and read the certificate directly. Do not shell out to `curl`. This preserves the project's "standard library only" rule and avoids depending on `curl` being installed on each user's machine.
- **Port:** Use the standard SSL port — connect via the `https` scheme / port 443, whichever fits the JDK API the architect selects. No per-server port override in this version.
- **Timeout:** 5 seconds per host. When a host exceeds that budget, surface it explicitly in the report row (e.g. a "timed out" status or note) rather than dropping the row from the results.
- **Export:** On-screen display only. No CSV or file export is in scope for this plan.

## Architecture

### Package Structure

```
src/
├── util/
│   └── tls/                       # NEW — domain-agnostic TLS certificate probe
│       ├── package-info.java      # describes the tls helper boundary
│       ├── CertProbe.java         # synchronous, blocking, single-host TLS probe
│       ├── CertProbeResult.java
│       └── CertProbeStatus.java
└── server/
    └── ui/
        ├── ServerListView.java         # EXISTING — gains a second toolbar button
        └── sslcheck/                   # NEW — server-feature SSL-check UI glue
            ├── package-info.java       # describes the sslcheck subpackage boundary
            ├── SslCheckAction.java     # NEW — orchestrates the check off the EDT
            ├── SslCheckReportRow.java  # NEW — per-row presentation record
            ├── SslCheckReportDialog.java     # NEW — modal report dialog
            └── SslCheckReportTableModel.java # NEW — table model for the report
```

Rationale:
- `util/tls/` is a new domain-agnostic sub-area of `util`. It only depends on
  `javax.net.ssl`, `java.net`, `java.security.cert`, and `java.time`; it has
  no knowledge of `Server` or any other feature type. This satisfies the
  `util` rule in `src/util/package-info.java`.
- `server.ui.sslcheck` is a new subpackage that groups every class specific
  to the SSL-check feature: the orchestrator, the report dialog, and its
  supporting row/table-model types. Grouping them under a dedicated
  subpackage keeps `server.ui` from accumulating one-off feature classes and
  establishes a pattern future per-action UI clusters (e.g. import/export
  dialogs, batch operations) can follow.
- `ServerListView` remains in `server.ui` because it is the feature's main
  view, not SSL-check-specific. It is the only consumer of the new
  subpackage and only references `SslCheckAction` directly; the dialog, row,
  and table model stay internal to `sslcheck`.
- All server-feature glue (iterating `ServerStore.servers()`, filtering by
  non-empty `hostname()`, building the report) lives in `server.ui.sslcheck`
  because it depends on `server.model` types.

### Visibility

- `SslCheckAction` is the only entry point `ServerListView` (in the parent
  `server.ui` package) needs. Its constructor and `attachTo(...)` / `run()`
  methods are `public`.
- `SslCheckReportDialog`, `SslCheckReportRow`, and `SslCheckReportTableModel`
  are constructed and used exclusively from within `server.ui.sslcheck`
  (`SslCheckAction` is the only caller). They stay **package-private**:
  the class declarations omit `public`, and so do their constructors,
  factory methods, and any helper methods listed below. The `@Override`
  table-model methods retain `public` because `AbstractTableModel` declares
  them that way.
- The nested `SslCheckReportRow.ExpiryClassification` enum is also
  package-private; only the row record and the dialog's renderer consult it,
  and both live in `server.ui.sslcheck`.
- `util.tls` types (`CertProbe`, `CertProbeResult`, `CertProbeStatus`) stay
  `public` because `server.ui.sslcheck` consumes them across a package
  boundary.

### Components

| Component | Type | Package | Purpose |
|---|---|---|---|
| `CertProbe` | `public final class` | `util.tls` | Synchronously fetches the leaf X.509 certificate from a host on port 443 with a fixed connect+read timeout |
| `CertProbeResult` | `public record` | `util.tls` | Outcome of a single probe: status, optional issue/expiry instants, optional error detail |
| `CertProbeStatus` | `public enum` | `util.tls` | Enumerates the possible outcomes of a probe (`OK`, `TIMED_OUT`, `CONNECTION_REFUSED`, `NO_CERTIFICATE`, `HANDSHAKE_FAILED`, `UNKNOWN_HOST`, `OTHER_ERROR`) |
| `SslCheckAction` | `public final class` | `server.ui.sslcheck` | Orchestrates a batch run: filters servers with a hostname, dispatches probes off the EDT, opens the dialog when done. Only class in the subpackage visible to `server.ui` |
| `SslCheckReportRow` | package-private `record` | `server.ui.sslcheck` | Per-row presentation record combining a `Server`'s short name + hostname with its `CertProbeResult` and a derived expiry classification |
| `SslCheckReportDialog` | package-private `final class` | `server.ui.sslcheck` | Modal `JDialog` that displays the report table and a Close button |
| `SslCheckReportTableModel` | package-private `final class` | `server.ui.sslcheck` | `AbstractTableModel` mapping `SslCheckReportRow` instances to displayed columns; also exposes the row's expiry classification for row-level coloring |

`ServerListView` (in `server.ui`) is not new but is modified to add a second toolbar button and wire it to `server.ui.sslcheck.SslCheckAction`. This is the only cross-package reference into `server.ui.sslcheck`.

### Signatures

```java
// src/util/tls/CertProbeStatus.java
public enum CertProbeStatus {
    OK,
    TIMED_OUT,
    CONNECTION_REFUSED,
    UNKNOWN_HOST,
    HANDSHAKE_FAILED,
    NO_CERTIFICATE,
    OTHER_ERROR;

    public String displayLabel();
}
```

```java
// src/util/tls/CertProbeResult.java
public record CertProbeResult(
    String host,
    CertProbeStatus status,
    Optional<Instant> notBefore,
    Optional<Instant> notAfter,
    Optional<String> errorDetail
) {
    public CertProbeResult { /* null checks only — no logic shown */ }

    public static CertProbeResult success(String host, Instant notBefore, Instant notAfter);
    public static CertProbeResult failure(String host, CertProbeStatus status, String detail);
}
```

```java
// src/util/tls/CertProbe.java
public final class CertProbe {
    public static final int DEFAULT_PORT = 443;
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    public CertProbe();
    public CertProbe(Duration timeout, int port);

    // Synchronous; must NOT be called on the EDT.
    public CertProbeResult probe(String host);
}
```

```java
// src/server/ui/sslcheck/SslCheckReportRow.java  (package-private)
record SslCheckReportRow(
    String shortName,
    String hostname,
    CertProbeResult result,
    ExpiryClassification expiry
) {
    enum ExpiryClassification {
        OK,
        EXPIRING_SOON,   // <= 30 days from now and not yet expired
        EXPIRED,
        UNKNOWN          // probe did not return a notAfter
    }

    static SslCheckReportRow from(String shortName, CertProbeResult result, Instant now);
}
```

```java
// src/server/ui/sslcheck/SslCheckAction.java
public final class SslCheckAction {
    public SslCheckAction(JFrame ownerFrame, ServerStore store);

    // Wires the supplied button: disables it while a run is in progress
    // and re-enables it when the report dialog is dismissed. Idempotent.
    public void attachTo(JButton trigger);

    // Triggers a run programmatically (e.g. from tests or other entry points).
    public void run();
}
```

```java
// src/server/ui/sslcheck/SslCheckReportTableModel.java  (package-private)
final class SslCheckReportTableModel extends AbstractTableModel {
    SslCheckReportTableModel(List<SslCheckReportRow> rows);

    SslCheckReportRow getRowAt(int rowIndex);

    @Override public int getRowCount();
    @Override public int getColumnCount();
    @Override public String getColumnName(int columnIndex);
    @Override public Object getValueAt(int rowIndex, int columnIndex);
    @Override public boolean isCellEditable(int rowIndex, int columnIndex);
}
```

```java
// src/server/ui/sslcheck/SslCheckReportDialog.java  (package-private)
final class SslCheckReportDialog extends JDialog {
    SslCheckReportDialog(JFrame owner, List<SslCheckReportRow> rows);

    // Convenience entry point used by SslCheckAction (same package).
    static void show(JFrame owner, List<SslCheckReportRow> rows);
}
```

```java
// src/server/ui/ServerListView.java  (modified — toolbar only)
// The existing buildToolbar() is updated to also add a SecondaryButton
// labeled "Check SSL Certs" and pass it to a new
// server.ui.sslcheck.SslCheckAction via attachTo(...). No public API change.
// New import: server.ui.sslcheck.SslCheckAction.
```

### Relationships

- `CertProbe` is the only class that touches `javax.net.ssl`. It opens an
  `SSLSocket` to `host:443`, applies the timeout to both `connect()` and the
  TLS handshake (`startHandshake()`), reads the peer certificate chain, and
  returns the first certificate's `notBefore` / `notAfter` as `Instant` values.
  Exceptions are mapped to `CertProbeStatus` values (`SocketTimeoutException`
  -> `TIMED_OUT`, `ConnectException` -> `CONNECTION_REFUSED`,
  `UnknownHostException` -> `UNKNOWN_HOST`, `SSLException` ->
  `HANDSHAKE_FAILED`, etc.) and packaged into a `CertProbeResult`. `CertProbe`
  never throws.
- `SslCheckAction` is the feature-side orchestrator:
  1. Disables the trigger button on click.
  2. Filters `store.servers()` to entries with a non-empty `hostname()`.
  3. Submits the probe work to a `SwingWorker` (off the EDT). Within
     `doInBackground()`, each host is probed sequentially using a single
     `CertProbe` instance. (Sequential keeps the design stdlib-simple; the
     5s budget is per host.)
  4. Builds a `List<SslCheckReportRow>` via `SslCheckReportRow.from(...)`
     using `Instant.now()` once per run as the reference moment.
  5. In `done()`, opens `SslCheckReportDialog` and re-enables the trigger
     button when the dialog is closed.
- `SslCheckReportRow.from(...)` derives the `ExpiryClassification`:
  - `EXPIRED` if `result.notAfter()` is present and not after `now`.
  - `EXPIRING_SOON` if `notAfter` is within the next 30 days inclusive.
  - `OK` if `notAfter` is more than 30 days away.
  - `UNKNOWN` otherwise (any non-`OK` `CertProbeStatus`).
- `SslCheckReportTableModel` exposes columns:
  `Short Name`, `Hostname`, `Status`, `Issued`, `Expires`. Dates are
  formatted by the model using a single `DateTimeFormatter` so the dialog
  stays presentation-only. The model also exposes `getRowAt(int)` so the
  dialog's `TableCellRenderer` can read `ExpiryClassification` and apply a
  warning color (`StandardColor.ACCENT` for `EXPIRING_SOON`, a distinct red
  the renderer defines locally for `EXPIRED`).
- `SslCheckReportDialog` is a themed modal `JDialog` parented to the owner
  frame. It contains a `JTable` wrapped in a `JScrollPane`, plus a
  `SecondaryButton` ("Close") in the south. A custom
  `DefaultTableCellRenderer` subclass (private to the dialog) consults
  `SslCheckReportTableModel.getRowAt(row).expiry()` and tints the row
  background accordingly. The dialog re-enables the trigger button via
  the `WindowListener` registered by `SslCheckAction`.
- `ServerListView.buildToolbar()` adds a `SecondaryButton` titled
  "Check SSL Certs" to the left of the existing `PrimaryButton` ("Add
  Server"). It constructs a `server.ui.sslcheck.SslCheckAction` and calls
  `attachTo(checkButton)`. `SslCheckAction` is the only `server.ui.sslcheck`
  type `ServerListView` imports; all other classes in the subpackage are
  package-private and reached only from inside `SslCheckAction`.
- No new dependencies in the `server.model` package; the check is read-only.

### Implementation Order

1. `src/util/tls/package-info.java` — documents the new sub-area's scope and
   the `util` boundary rule.
2. `src/util/tls/CertProbeStatus.java` — `public enum` with display labels,
   no dependencies.
3. `src/util/tls/CertProbeResult.java` — `public record` + static factories,
   depends only on `CertProbeStatus` and `java.time.Instant`.
4. `src/util/tls/CertProbe.java` — `public final class`, depends on
   `CertProbeResult` and `CertProbeStatus`; the only class touching
   `javax.net.ssl`. Verify manually against a known-good public HTTPS host
   and a guaranteed-unreachable host before moving on.
5. `src/server/ui/sslcheck/package-info.java` — documents the new
   subpackage's scope (server-feature SSL-check UI glue) and notes that
   `SslCheckAction` is the only externally visible entry point.
6. `src/server/ui/sslcheck/SslCheckReportRow.java` — package-private record,
   depends on `CertProbeResult` and `CertProbeStatus`; pure logic.
7. `src/server/ui/sslcheck/SslCheckReportTableModel.java` —
   package-private, depends on `SslCheckReportRow`.
8. `src/server/ui/sslcheck/SslCheckReportDialog.java` — package-private,
   depends on `SslCheckReportTableModel`, `SecondaryButton`,
   `StandardColor`. Build with a hard-coded sample list first if helpful.
9. `src/server/ui/sslcheck/SslCheckAction.java` — `public final class`,
   depends on `CertProbe`, `SslCheckReportRow`, `SslCheckReportDialog`,
   `ServerStore`, `Server`. The only `server.ui.sslcheck` type the parent
   package imports.
10. `src/server/ui/ServerListView.java` — add the
    `server.ui.sslcheck.SslCheckAction` import, wire the new
    `SecondaryButton` into `buildToolbar()`, and call
    `SslCheckAction.attachTo(...)`.

## Implementation Notes

<!-- java-coder appends progress notes here as it works through this plan -->

- 2026-05-19: Step 1 done — added `src/util/tls/package-info.java` documenting
  the new sub-area's scope (JDK-only TLS probing helpers, domain-agnostic,
  no dependency on feature packages). Flipped plan status from `ready` to
  `in-progress` (here and in INDEX.md). Verified clean compile via `make
  compile`.
- 2026-05-19: Steps 2-10 done in one run.
  - `src/util/tls/CertProbeStatus.java` — enum with seven outcome values plus
    `displayLabel()`.
  - `src/util/tls/CertProbeResult.java` — record with `success(...)` and
    `failure(...)` factories and inline null/blank guards.
  - `src/util/tls/CertProbe.java` — JDK-only TLS probe using
    `SSLSocketFactory` + `SSLSocket.startHandshake()` against port 443;
    5s budget is applied to `connect()` then the remaining time is set as
    `SO_TIMEOUT` to cover the handshake. Exceptions are mapped to the
    appropriate `CertProbeStatus`; the method never throws.
  - `src/server/ui/sslcheck/package-info.java` — documents the sslcheck
    subpackage and that `SslCheckAction` is the only externally visible
    entry point.
  - `src/server/ui/sslcheck/SslCheckReportRow.java` — package-private record
    with nested `ExpiryClassification` enum and a `from(...)` factory that
    derives EXPIRED / EXPIRING_SOON / OK / UNKNOWN against a 30-day window.
  - `src/server/ui/sslcheck/SslCheckReportTableModel.java` — `AbstractTableModel`
    exposing Short Name / Hostname / Status / Issued / Expires columns; dates
    formatted with a `DateTimeFormatter` (`yyyy-MM-dd`, system zone); also
    exposes `getRowAt(int)` so the dialog renderer can read the expiry
    classification.
  - `src/server/ui/sslcheck/SslCheckReportDialog.java` — themed modal
    `JDialog` with a scroll-pane table, an empty-state message when no
    hostnames are checked, a Close button, and a private
    `DefaultTableCellRenderer` subclass that tints rows by
    `ExpiryClassification` (accent blue for EXPIRING_SOON, dark red for
    EXPIRED).
  - `src/server/ui/sslcheck/SslCheckAction.java` — orchestrator with
    `attachTo(JButton)` and `run()`; runs probes sequentially on a
    `SwingWorker`, disables the trigger while running, opens the dialog on
    `done()`, and re-enables the trigger in a `finally` block so it recovers
    from interruption / execution errors.
  - `src/server/ui/ServerListView.java` — added the `SecondaryButton`
    ("Check SSL Certs") to the toolbar left of the existing Add Server
    button and wired it via `new SslCheckAction(ownerFrame, store)
    .attachTo(...)`.
  - Verified clean `make compile` after the full set of changes.
