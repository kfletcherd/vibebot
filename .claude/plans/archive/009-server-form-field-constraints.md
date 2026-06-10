---
id: "009"
title: Server Form Field Constraints
status: done
created: 2026-05-05
updated: 2026-05-05
---

## Summary

The Add/Edit Server form currently accepts free text for every field. This plan tightens four of those fields: two IP address fields get format validation, and three descriptive fields (Hosting Provider, Type, Environment) are replaced with controlled selection controls. The goal is to prevent bad data from ever reaching the saved server record.

## Goals

- IP address fields (Public IP and PVLAN IP) should only accept values that look like valid IP addresses. The form should reject or clearly flag any value that does not match before allowing the user to save.
- A new "Hosting Provider" field should appear on the form as a dropdown. The only valid choices are: GCP, HV, AWS, Not Applicable.
- The "Type" field should become a multi-select control. The user can pick one or more of: Load Balancer, API, Worker, Database, Redis.
- The "Environment" field should become a dropdown. The only valid choices are: Production, Staging, Development.
- The Hostname field should only accept a bare hostname (e.g. `preprocess.feedonomics.com`). Values that include a protocol prefix (e.g. `https://`) or a path segment (e.g. `/path/to/endpoint`) are not valid and should be rejected before saving. Empty values remain allowed.

## Acceptance Criteria

- Entering a non-IP value (e.g. "hello") in Public IP or PVLAN IP and attempting to save shows the user an error and does not save.
- Both IPv4 and IPv6 address formats are accepted as valid.
- Empty IP fields remain allowed (the field is not required by this plan).
- The Hosting Provider dropdown shows exactly the four options listed above and nothing else.
- The Type control allows the user to select multiple values simultaneously. At least one selection is required to save.
- The Environment dropdown shows exactly the three options listed above and nothing else.
- A server record saved with any of these fields round-trips correctly: loading the record back into the form re-populates all controlled fields to the same values that were saved.
- Existing saved server records that predate this change can still be opened in the form. Fields that held values no longer valid under the new rules should be treated as unset/blank rather than crashing.
- Entering a hostname that includes a protocol prefix or a path (e.g. `https://example.com/path`) and attempting to save shows the user an error and does not save.
- A bare hostname with no protocol and no path (e.g. `preprocess.feedonomics.com`) is accepted without error.
- An empty Hostname field is accepted without error (the field is not required by this plan).

## Open Questions

- Should "Hosting Provider" be required before saving, or is it optional? The same question applies to "Environment." (Type is listed as requiring at least one selection above, but double-check this is correct.)
- Multi-select for "Type" will change how that value is stored in the CSV. Currently Type is a single text column. Should it become a semicolon-delimited list within the same column, or should the CSV format change more significantly? This decision affects how old records are read back.
- Where should the new "Hosting Provider" field appear relative to the existing fields in the form layout?

## Architecture

### Package Structure

No new packages. All changes are within existing packages:

- `server.model` — three new enum types; `Server`, `ServerColumn`, `ServerCsvMapper` modified
- `server.ui` — `AddServerDialog`, `ServerTableModel` modified

### Components

| Component | Type | Package | Purpose |
|---|---|---|---|
| `HostingProvider` | new enum | `server.model` | Controlled vocabulary for the Hosting Provider field; `GCP`, `HV`, `AWS`, `NOT_APPLICABLE` |
| `ServerEnvironment` | new enum | `server.model` | Controlled vocabulary for the Environment field; `PRODUCTION`, `STAGING`, `DEVELOPMENT` |
| `ServerType` | new enum | `server.model` | Controlled vocabulary for the Type field; `LOAD_BALANCER`, `API`, `WORKER`, `DATABASE`, `REDIS` |
| `Server` | modified record | `server.model` | `environment` becomes `Optional<ServerEnvironment>`; `type` becomes `EnumSet<ServerType>`; new `hostingProvider` field of type `Optional<HostingProvider>` |
| `ServerColumn` | modified enum | `server.model` | Adds `HOSTING_PROVIDER` constant at CSV index 7; `COUNT` auto-updates |
| `ServerCsvMapper` | modified class | `server.model` | `toServer` and `toRecord` updated to serialise/deserialise the new typed fields |
| `AddServerDialog` | modified class | `server.ui` | Environment `JTextField` replaced with `JComboBox<ServerEnvironment>`; Type `JTextField` replaced with `JList<ServerType>` in `JScrollPane`; new `JComboBox<HostingProvider>` added; IP and hostname validation added to save path |
| `ServerTableModel` | modified class | `server.ui` | `getValueAt` cases for `ENVIRONMENT`, `TYPE`, and new `HOSTING_PROVIDER` updated to render display names; `PRESENTATION_ORDER` updated to include `HOSTING_PROVIDER` |

### Signatures

#### `HostingProvider`

```java
package server.model;

import java.util.Optional;

public enum HostingProvider {
    GCP,
    HV,
    AWS,
    NOT_APPLICABLE;

    /**
     * Returns the user-facing display string for this provider.
     *
     * @return the display name; never {@code null}
     */
    public String displayName();

    /**
     * Returns the {@code HostingProvider} whose CSV token matches {@code value},
     * or empty if the value is blank or unrecognised.
     *
     * @param value the raw CSV field value; may be blank; must not be {@code null}
     * @return the matching provider; empty for blank or unrecognised values
     */
    public static Optional<HostingProvider> fromCsv(String value);
}
```

#### `ServerEnvironment`

```java
package server.model;

import java.util.Optional;

public enum ServerEnvironment {
    PRODUCTION,
    STAGING,
    DEVELOPMENT;

    /**
     * Returns the user-facing display string for this environment.
     *
     * @return the display name; never {@code null}
     */
    public String displayName();

    /**
     * Returns the {@code ServerEnvironment} whose CSV token matches {@code value},
     * or empty if the value is blank or unrecognised.
     *
     * @param value the raw CSV field value; may be blank; must not be {@code null}
     * @return the matching environment; empty for blank or unrecognised values
     */
    public static Optional<ServerEnvironment> fromCsv(String value);
}
```

#### `ServerType`

```java
package server.model;

import java.util.Optional;

public enum ServerType {
    LOAD_BALANCER,
    API,
    WORKER,
    DATABASE,
    REDIS;

    /**
     * Returns the user-facing display string for this type.
     *
     * @return the display name; never {@code null}
     */
    public String displayName();

    /**
     * Returns the {@code ServerType} whose CSV token matches {@code value},
     * or empty if the value is blank or unrecognised.
     *
     * @param value a single raw token (not a semicolon-delimited list);
     *  may be blank; must not be {@code null}
     * @return the matching type; empty for blank or unrecognised values
     */
    public static Optional<ServerType> fromCsv(String value);
}
```

#### `Server` (modified record)

```java
package server.model;

import java.util.EnumSet;
import java.util.Optional;

public record Server(
    String publicIp,
    String pvlanIp,
    String shortName,
    String hostname,
    String notes,
    Optional<ServerEnvironment> environment,
    EnumSet<ServerType> type,
    Optional<HostingProvider> hostingProvider
) {
    /**
     * Validates that no field is {@code null} and that {@code type} is non-empty.
     *
     * @throws IllegalArgumentException if any field is {@code null} or
     *  {@code type} is empty
     */
    public Server { ... }
}
```

#### `ServerColumn` (modified enum — constant additions only)

```java
/** Functional type of the server; stored as a semicolon-delimited list when multiple. */
TYPE(6, "type", "Type"),

/** Hosting provider for the server; may be absent. */
HOSTING_PROVIDER(7, "hosting_provider", "Hosting Provider");
```

#### `ServerCsvMapper` (modified methods)

```java
/**
 * Converts a data {@link CsvRecord} to a {@link Server}.
 *
 * <p>The {@code environment} and {@code hostingProvider} fields are parsed via
 * {@link ServerEnvironment#fromCsv} and {@link HostingProvider#fromCsv}; blank or
 * unrecognised values yield an empty {@link Optional}. The {@code type} field is
 * split on {@code ";"} and each token passed to {@link ServerType#fromCsv};
 * unrecognised tokens are silently dropped. If no valid tokens remain, the type set
 * is empty — the mapper does not enforce the "at least one" rule (that is a form
 * concern).
 *
 * @param record the data record to convert; must have exactly
 *  {@link ServerColumn#COUNT} fields; must not be {@code null}
 * @return the corresponding {@link Server}; never {@code null}
 * @throws IllegalArgumentException if field count does not match
 */
public static Server toServer(CsvRecord record);

/**
 * Converts a {@link Server} to a {@link CsvRecord} for writing.
 *
 * <p>{@code environment} and {@code hostingProvider} serialise as their
 * {@link Enum#name()} when present, or as an empty string when absent.
 * {@code type} serialises as the {@link Enum#name()} values of each element
 * joined by {@code ";"}.
 *
 * @param server the server to convert; must not be {@code null}
 * @return the corresponding {@link CsvRecord}; never {@code null}
 */
public static CsvRecord toRecord(Server server);
```

#### `AddServerDialog` (modified fields and methods)

```java
// Replaces environmentField JTextField
private final JComboBox<ServerEnvironment> environmentCombo;

// Replaces typeField JTextField
private final JList<ServerType> typeList;

// New field
private final JComboBox<HostingProvider> hostingProviderCombo;

/**
 * Validates IP address and hostname fields before constructing a {@link Server}.
 * Shows a {@link JOptionPane} error dialog and returns without saving if any
 * field fails validation. IP fields are checked against a standard IPv4/IPv6
 * pattern; blank values pass. Hostname is checked to have no {@code "://"} and
 * no {@code "/"} character; blank values pass.
 */
private void handleOk();

/**
 * Populates all form controls from the given server's current values.
 *
 * @param server the server whose values should be loaded into the form;
 *  must not be {@code null}
 */
private void populateFields(Server server);

/**
 * Adds a label-component row to the given form panel at the specified grid row.
 *
 * <p>Accepts any {@link java.awt.Component} so that combo boxes, lists, and
 * text fields can all be placed uniformly.
 *
 * @param form the form panel; must not be {@code null}
 * @param labelText the text for the label; must not be {@code null}
 * @param component the form control for the row; must not be {@code null}
 * @param row the zero-based grid row index
 */
private static void addFormRow(JPanel form, String labelText, java.awt.Component component, int row);

/**
 * Returns {@code true} when the required form controls have valid selections:
 * {@code shortName} is non-blank, at least one {@link ServerType} is selected
 * in {@code typeList}, and any text fields that are non-blank pass their format
 * checks.
 *
 * @return {@code true} if the form may be saved; {@code false} otherwise
 */
private boolean isFormValid();

/**
 * Enables or disables the OK button based on {@link #isFormValid()}.
 */
private void updateOkState();
```

### Relationships

- The three new enums (`HostingProvider`, `ServerEnvironment`, `ServerType`) are pure value types with no dependencies on other project classes. They must be implemented first.
- `Server` depends on all three enums and must follow them. The compact constructor validates non-null on every field and enforces non-empty on `type`; it does not validate IP format or hostname format (that remains `AddServerDialog`'s responsibility).
- `ServerColumn` adds `HOSTING_PROVIDER` at CSV index 7, shifting `COUNT` from 7 to 8. `ServerCsvMapper` must be updated at the same time because `isValidHeader` and `toServer`/`toRecord` both key off `ServerColumn.COUNT` and specific constants.
- `ServerCsvMapper.toServer` splits the `type` column on `";"` and calls `ServerType.fromCsv` on each token. Single-value old records (no semicolon) are handled naturally because splitting a string with no delimiter yields a one-element array.
- `AddServerDialog` replaces two `JTextField` members with typed selection controls and adds a third. The existing `addFormRow` overload that accepts `JTextField` must be generalised to accept `java.awt.Component` so combo boxes and scroll panes can be added in the same layout. The `wireValidation` / `updateOkState` approach is replaced by `isFormValid()`, which checks: `shortName` non-blank, `typeList` has at least one selected value. The `handleOk` method runs IP regex and hostname checks before constructing the `Server`; a failed check shows `JOptionPane.showMessageDialog` and returns early without saving.
- `ServerTableModel.getValueAt` must handle the new field types for `ENVIRONMENT`, `TYPE`, and `HOSTING_PROVIDER`: `environment` renders as `displayName()` or empty string, `type` renders as the display names of each selected value joined by `", "`, `hostingProvider` renders as `displayName()` or empty string. `PRESENTATION_ORDER` should be updated to include `HOSTING_PROVIDER` after `TYPE`.
- **Open question resolutions recorded here:**
  - Hosting Provider and Environment are optional (no selection required). Type requires at least one selection.
  - Multi-type values are stored as a semicolon-delimited string in the existing Type column. Old single-value records are read back transparently.
  - Hosting Provider appears after Environment and before Type in the form layout (row order: Public IP, PVLAN IP, Short Name, Environment, Hosting Provider, Type, Hostname, Notes).

### Implementation Order

1. `HostingProvider` enum
2. `ServerEnvironment` enum
3. `ServerType` enum
4. `Server` record (update field types, add `hostingProvider`, update compact constructor)
5. `ServerColumn` enum (add `HOSTING_PROVIDER` constant)
6. `ServerCsvMapper` (update `toServer` and `toRecord` for new types)
7. `ServerTableModel` (update `PRESENTATION_ORDER` and `getValueAt` for new/changed fields)
8. `AddServerDialog` (replace controls, generalise `addFormRow`, update validation and save logic)

## Implementation Notes

### 2026-05-05 — All components implemented; plan complete

**HostingProvider** (`server/model/HostingProvider.java`) — new enum with `GCP`, `HV`, `AWS`, `NOT_APPLICABLE`. Each constant carries a `displayName()` and a static `fromCsv(String)` that matches by `name()` and returns `Optional.empty()` for blank or unrecognised values.

**ServerEnvironment** (`server/model/ServerEnvironment.java`) — new enum with `PRODUCTION`, `STAGING`, `DEVELOPMENT`. Same `displayName()` / `fromCsv(String)` pattern as `HostingProvider`.

**ServerType** (`server/model/ServerType.java`) — new enum with `LOAD_BALANCER`, `API`, `WORKER`, `DATABASE`, `REDIS`. Same `displayName()` / `fromCsv(String)` pattern; designed for use as individual tokens within a semicolon-delimited CSV field.

**Server** (`server/model/Server.java`) — record updated: `environment` is now `Optional<ServerEnvironment>`, `type` is now `EnumSet<ServerType>`, new `hostingProvider` field of type `Optional<HostingProvider>`. Compact constructor validates all fields non-null and `type` non-empty; it defensively copies `type` to prevent external mutation.

**ServerColumn** (`server/model/ServerColumn.java`) — `HOSTING_PROVIDER(7, "hosting_provider", "Hosting Provider")` added after `TYPE`. `COUNT` auto-updates to 8.

**ServerCsvMapper** (`server/model/ServerCsvMapper.java`) — `toServer` uses `ServerEnvironment.fromCsv`, splits the type field on `";"` via a private `parseTypeField` helper (unrecognised tokens silently dropped, empty set returned when nothing valid), and uses `HostingProvider.fromCsv`. `toRecord` serialises `environment` and `hostingProvider` as `Enum#name()` or `""`, and `type` as names joined by `";"`.

**ServerTableModel** (`server/ui/ServerTableModel.java`) — `PRESENTATION_ORDER` updated to include `HOSTING_PROVIDER` between `ENVIRONMENT` and `TYPE`. `getValueAt` switch updated: `ENVIRONMENT` maps to `displayName()` or `""`, `HOSTING_PROVIDER` maps to `displayName()` or `""`, `TYPE` joins display names with `", "`.

**AddServerDialog** (`server/ui/AddServerDialog.java`) — `environmentField` replaced with `JComboBox<ServerEnvironment>`; `typeField` replaced with `JList<ServerType>` inside a `JScrollPane`; new `JComboBox<HostingProvider>` added. `addFormRow` signature generalised from `JTextField` to `java.awt.Component`. Validation logic consolidated into `isFormValid()` (shortName non-blank + at least one type selected); `wireValidation()` attaches a document listener to `shortNameField` and a list selection listener to `typeList`. `handleOk` runs IP regex and hostname checks before constructing `Server`, showing `JOptionPane.showMessageDialog` on failure. Combo renderers use reflection to call `displayName()` on enum items; the type list uses a typed `instanceof` pattern-match renderer. `populateFields` restores combo selections via `setSelectedItem` and type selections via index scanning.

**Verified:** `make compile` produced no errors or warnings.
