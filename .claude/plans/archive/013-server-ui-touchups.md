---
id: "013"
title: Server UI Touch-Ups — Column Order, Button and Dropdown Theming, Multi-Line Notes, Cell Tooltips
status: done
created: 2026-05-12
updated: 2026-05-12
---

## Summary

Bundles five small visual and usability fixes that polish the existing servers
screen and the look of the app as a whole. The servers table reorders two
columns for readability. The app-wide look of buttons and dropdowns is softened
so it no longer hurts the user's eyes. The Notes input on the add/edit server
dialog becomes a multi-line area so longer notes are easier to read and type.
Tables across the app gain per-cell hover tooltips so truncated content can be
read at a glance, with the servers table being the first screen to benefit.

## Goals

- Make the servers table easier to scan by promoting Short Name and Hostname to
  the first two columns.
- Replace the current jarring button look with a softer, consistent primary and
  secondary button style that any screen in the app can pick up automatically.
- Replace the bright white border around dropdowns with a softer border that
  fits the dark theme, everywhere in the app.
- Give the Notes field on the add/edit server dialog enough room for several
  lines of text instead of one cramped line.
- Let the user read truncated cell content in any table in the app just by
  hovering, without having to widen columns or open an edit dialog. Every
  existing and future table picks this up automatically.

## Acceptance Criteria

### Column reorder

- In the servers table, Short Name is the leftmost column and Hostname is the
  second column from the left.
- The remaining columns keep their existing relative order to each other (only
  their absolute positions shift to make room for the two promoted columns).
- The stored servers file is unchanged on disk by this work — opening, editing,
  or adding a server produces the same stored representation it did before.
  Reading a stored file produced by an earlier version of the app still works
  exactly as before.

### Button look

- The button look applies app-wide. Any new button anywhere in the app
  automatically picks up either the primary or the secondary look without
  per-screen styling code.
- The primary look has a dark blue background with white text.
- The secondary look has a dark grey background with white text.
- The following buttons use the primary look after this work: the "Add Server"
  button on the servers page, the "OK" button on the add/edit server dialog,
  and the "Save" button on the settings page.
- The following buttons use the secondary look after this work: the per-row
  "Edit" button on the servers page, and the "Cancel" and "Delete" buttons on
  the add/edit server dialog.
- No button in the app uses the old default platform colors after this work.

### Dropdown look

- The dropdown look applies app-wide. Any dropdown anywhere in the app picks
  up the softened border automatically.
- The bright white border that currently surrounds dropdown fields on the
  add/edit server dialog is replaced by a softer border consistent with the
  rest of the dark theme. The change is visible on every dropdown in the app,
  not only the ones on that dialog.

### Notes field

- On the add/edit server dialog, the Notes input is a multi-line area, not a
  single-line input.
- The area shows roughly four to five lines of text without scrolling. If the
  user types more than that, the area scrolls rather than growing the dialog.
- The Notes value is stored and loaded exactly as it is today — the only
  change is the input control on the form.
- Existing servers that already have notes saved continue to round-trip
  correctly: opening them in the dialog shows the same text, and saving them
  unchanged produces the same stored value.

### Cell tooltips

- Cell tooltips are a generic table capability that applies app-wide. Any
  table in the app — including future tables — automatically gains per-cell
  hover tooltips without per-screen styling code, the same way the button and
  dropdown looks apply app-wide.
- The servers table is the first consumer of the behavior and exercises it on
  every column. After this work, hovering over any cell in the servers table
  shows a tooltip containing that cell's full text, with no per-column
  exceptions.
- The tooltip text matches exactly what the cell displays on screen. For
  columns that show a friendly label for an underlying coded value, the
  tooltip shows the friendly label, not the underlying code or storage form.
- When a cell is empty or blank, no tooltip appears for that cell.
- The tooltip uses the platform's standard hover tooltip behavior — it appears
  after the usual short hover delay and disappears when the pointer leaves the
  cell, the same way tooltips behave elsewhere in the app.

## Open Questions

- None.

## Architecture

### Package Structure

```
src/
├── util/ui/
│   ├── button/                 # NEW: app-wide reusable button styles
│   │   ├── PrimaryButton.java
│   │   └── SecondaryButton.java
│   ├── table/                  # generic table; gains per-cell tooltip behavior
│   │   ├── Table.java          # (existing — no signature change required)
│   │   └── ButtonColumn.java   # internal JButton becomes a SecondaryButton
│   └── theme/
│       ├── StandardColor.java  # add two semantic colors for button surfaces
│       └── StandardTheme.java  # extend UIManager defaults: ComboBox border, tooltip defaults
├── settings/ui/
│   └── SettingsView.java       # Save button swaps to PrimaryButton
└── server/ui/
    ├── ServerTableModel.java   # PRESENTATION_ORDER promotes SHORT_NAME and HOSTNAME
    ├── ServerListView.java     # swap raw JButton to PrimaryButton / SecondaryButton; install CellTooltipRenderer
    └── AddServerDialog.java    # Notes field becomes a JTextArea inside a JScrollPane; OK/Cancel/Delete adopt new button styles
```

### Components

| Component | Type | Package | Purpose |
|---|---|---|---|
| `PrimaryButton` | `class` (extends `JButton`) | `util.ui.button` | App-wide primary button: dark blue background, white foreground. Drop-in replacement for `JButton` at construction sites that previously needed accent styling. |
| `SecondaryButton` | `class` (extends `JButton`) | `util.ui.button` | App-wide secondary button: dark grey background, white foreground. Drop-in replacement for `JButton` at construction sites that previously used the default look. |
| `StandardColor` | `enum` (existing — extended) | `util.ui.theme` | Add `BUTTON_PRIMARY` (dark blue) and `BUTTON_SECONDARY` (dark grey) constants. Existing constants untouched. |
| `StandardTheme` | `class` (existing — extended) | `util.ui.theme` | Extend `apply()` to install softer `ComboBox.border` (using `StandardColor.BORDER` instead of the platform default white). Continues to be the single place that touches `UIManager`. No new public methods. |
| `CellTooltipRenderer` | `class` (extends `DefaultTableCellRenderer`) | `util.ui.table` | Decorating cell renderer that sets `setToolTipText` to the cell's displayed string on every render; sets `null` when the displayed value is `null` or blank. Wraps a delegate renderer if present, otherwise behaves as a plain text renderer. |
| `Table` | `class` (existing) | `util.ui.table` | Constructor wires `CellTooltipRenderer` as the default renderer for every column of its `JTable`. No new public methods. |
| `ButtonColumn` | `class` (existing — modified) | `util.ui.table` | Change the type of the internal shared button field from `JButton` to `SecondaryButton`. Public API (constructor signature, both `getTableCell*Component` methods, `getCellEditorValue`) is unchanged, so every existing call site is untouched. Per-row buttons in every table — present and future — gain the secondary look automatically. |
| `SettingsView` | `class` (existing — modified) | `settings.ui` | Replace the hand-themed Save `JButton` with a `PrimaryButton`. Drop the now-redundant `setBackground`, `setForeground`, and `setFocusPainted(false)` calls — `PrimaryButton` owns those internally. The button's `ActionListener` and `setAlignmentX` calls remain. |
| `ServerTableModel` | `class` (existing — modified) | `server.ui` | Rewrite the `PRESENTATION_ORDER` array so `SHORT_NAME` is index 0 and `HOSTNAME` is index 1; the remaining six `ServerColumn` values keep their existing relative order. |
| `AddServerDialog` | `class` (existing — modified) | `server.ui` | Replace the `notesField` text field with a `JTextArea` wrapped in a `JScrollPane` of roughly four to five visible rows. Replace the hand-themed OK / Cancel / Delete `JButton` instances with `PrimaryButton` / `SecondaryButton`. Drop the per-component setForeground/setBackground calls that those custom buttons now own internally. |
| `ServerListView` | `class` (existing — modified) | `server.ui` | Replace the hand-themed "Add Server" `JButton` with a `PrimaryButton`. Install `CellTooltipRenderer` on the embedded `JTable` (this view currently builds its `JTable` inline rather than via the generic `Table`; the simplest change is one call against `JTable#setDefaultRenderer` for `Object.class`). |

### Signatures

```java
// util.ui.button.PrimaryButton
public final class PrimaryButton extends JButton {
    public PrimaryButton(String text);
}

// util.ui.button.SecondaryButton
public final class SecondaryButton extends JButton {
    public SecondaryButton(String text);
}

// util.ui.theme.StandardColor (additions only — existing constants unchanged)
public enum StandardColor {
    // ... existing constants ...
    BUTTON_PRIMARY(/* dark blue hex */),
    BUTTON_SECONDARY(/* dark grey hex */);
    public final int hex;
    public final Color color;
}

// util.ui.theme.StandardTheme (no new public surface; apply() body is extended)
public final class StandardTheme {
    public static void apply();
}

// util.ui.table.CellTooltipRenderer
public final class CellTooltipRenderer extends DefaultTableCellRenderer {
    public CellTooltipRenderer();
    @Override
    public Component getTableCellRendererComponent(
        JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column
    );
}

// util.ui.table.Table (existing — constructor body changes only; public API unchanged)
public final class Table {
    public Table(List<String> headers);
    public JScrollPane component();
    public void addRow(List<String> row);
    public int rowCount();
}

// server.ui.ServerTableModel (no signature changes — only the
// PRESENTATION_ORDER array literal changes)
final class ServerTableModel extends AbstractTableModel {
    ServerTableModel(List<Server> servers);
    @Override public int getRowCount();
    @Override public int getColumnCount();
    @Override public String getColumnName(int column);
    @Override public Object getValueAt(int rowIndex, int columnIndex);
    @Override public boolean isCellEditable(int rowIndex, int columnIndex);
    void addServer(Server server);
    void updateServer(Server old, Server updated);
    void deleteServer(Server server);
    Server getServerAt(int row);
}
```

### Relationships

- `PrimaryButton` and `SecondaryButton` live in a new `util.ui.button` package and read their colors from `StandardColor.BUTTON_PRIMARY`, `StandardColor.BUTTON_SECONDARY`, and `StandardColor.FOREGROUND_SELECTED` (for white text). They expose no public API beyond a single-string constructor, so any future screen can use them without knowing the dark-mode palette.
- `StandardTheme.apply()` remains the single place that writes to `UIManager`. The dropdown softening is added there as a `ComboBox.border` entry (and any related `ComboBox.*` keys that currently inherit a white border from the platform default) so every `JComboBox` in the app picks it up at construction time without per-screen styling.
- `CellTooltipRenderer` is the generic per-cell tooltip behavior. Installing it as a `JTable` default renderer means it applies to every column without per-column setup. The `util.ui.table.Table` constructor installs it on its internal `JTable` so any future table built through `Table` gains tooltips for free. `ServerListView` (which constructs its own `JTable` directly, not via `Table`) installs the same renderer manually on its table — this is the first consumer and proves the design.
- `ButtonColumn` constructs a `SecondaryButton` internally instead of a raw `JButton`. Because per-row buttons are inherently secondary actions (the row itself is the focus, the button is an affordance on it), centralising this in `ButtonColumn` means every per-row button across every existing and future table picks up the secondary look automatically — no per-screen styling, no per-table override.
- `SettingsView`'s Save button adopts `PrimaryButton`. Save is a primary action (it commits the user's intent), so this brings the settings screen in line with the app-wide primary look the same way Add Server and OK do.
- `ServerTableModel` swaps the order of its `PRESENTATION_ORDER` constants so the visual columns become: Short Name, Hostname, Public IP, PVLAN IP, Environment, Hosting Provider, Type, Notes. The CSV order defined by `ServerColumn` ordinals is untouched, and `ServerCsvMapper` is therefore not modified — see the 2026-04-17 feedback entry "ServerColumn enum — table presentation order is separate from CSV storage order".
- `AddServerDialog`'s notes input becomes a `JTextArea` (rows ≈ 4–5, columns = `FIELD_COLUMNS`, line wrap on, wrap-style-word on) hosted in a `JScrollPane`. The dialog continues to read `notesField`'s text on save and write it on populate, so the round-trip behavior is unchanged. The dialog's button row drops its hand-rolled `JButton` colour overrides in favour of `PrimaryButton` (OK) and `SecondaryButton` (Cancel, Delete).
- No CSV mapping, store, or model code is touched by any of these changes.

### Implementation Order

1. `StandardColor` — add `BUTTON_PRIMARY` and `BUTTON_SECONDARY` constants (foundation; no other code depends on it yet).
2. `PrimaryButton` — depends only on `StandardColor` and `JButton`.
3. `SecondaryButton` — depends only on `StandardColor` and `JButton`.
4. `ButtonColumn` — change the internal shared button field's type from `JButton` to `SecondaryButton`. Public API unchanged; every existing per-row button in the app picks up the secondary look automatically.
5. `StandardTheme` — extend `apply()` to install the softer `ComboBox.border` (and any sibling keys required to fully remove the white outline). No new types.
6. `CellTooltipRenderer` — generic cell renderer, depends only on Swing.
7. `Table` — wire `CellTooltipRenderer` into the existing `Table` constructor so any future caller benefits automatically. Public API unchanged.
8. `ServerTableModel` — swap the `PRESENTATION_ORDER` array so `SHORT_NAME` is index 0 and `HOSTNAME` is index 1. No other change.
9. `SettingsView` — swap the Save `JButton` to `PrimaryButton`; remove the now-redundant `setBackground`, `setForeground`, and `setFocusPainted(false)` calls.
10. `ServerListView` — swap the "Add Server" `JButton` to `PrimaryButton`; install `CellTooltipRenderer` as the default renderer on the inline `JTable` (one call). Remove the now-redundant button colour overrides.
11. `AddServerDialog` — convert `notesField` to a `JTextArea` inside a `JScrollPane`; swap OK to `PrimaryButton`, Cancel and Delete to `SecondaryButton`; remove the now-redundant button colour overrides. Verify populate/save round-trip with the new control.

## Implementation Notes

<!-- java-coder appends progress notes here as it works through this plan -->

- 2026-05-12 — Step 1: Added `BUTTON_PRIMARY` (0x1F3A5F dark blue) and
  `BUTTON_SECONDARY` (0x3A3A3A dark grey) to `StandardColor`.
- 2026-05-12 — Step 2: Created `util.ui.button.PrimaryButton` extending
  `JButton` and applying the primary surface, white foreground, no focus
  paint, and 6px/14px label padding.
- 2026-05-12 — Step 3: Created `util.ui.button.SecondaryButton` with the
  same shape as `PrimaryButton` but the secondary surface.
- 2026-05-12 — Step 4: Reworked `ButtonColumn` to hold a `SecondaryButton`
  instead of `JButton`. Public API untouched; per-row buttons in every
  table now adopt the secondary look automatically.
- 2026-05-12 — Step 5: Extended `StandardTheme.apply()` with
  `ComboBox.background/foreground/selection*/button*/border` keys (using
  `StandardColor.BORDER` for the outline) plus a matching `ToolTip.border`,
  so dropdowns and tooltips both pick up the dark theme outline app-wide.
- 2026-05-12 — Step 6: Added `util.ui.table.CellTooltipRenderer` extending
  `DefaultTableCellRenderer`; sets the tooltip to the cell's text on every
  render or clears it when the value is null or blank.
- 2026-05-12 — Step 7: Installed `CellTooltipRenderer` as the
  `Object.class` default renderer inside `Table`'s constructor so every
  future table built through `Table` gets tooltips for free.
- 2026-05-12 — Step 8: Reordered `ServerTableModel.PRESENTATION_ORDER` so
  Short Name is index 0 and Hostname index 1; CSV order in `ServerColumn`
  is unaffected per the 2026-04-17 feedback entry.
- 2026-05-12 — Step 9: Swapped `SettingsView`'s Save button to
  `PrimaryButton`; removed the obsolete `setBackground`,
  `setForeground`, and `setFocusPainted(false)` calls.
- 2026-05-12 — Step 10: Swapped `ServerListView`'s "Add Server" button to
  `PrimaryButton` and installed `CellTooltipRenderer` on the inline
  `JTable` for per-cell hover tooltips on the servers table. Dropped the
  now-unused `JButton` import.
- 2026-05-12 — Step 11: Converted `AddServerDialog.notesField` to a
  `JTextArea` (4 rows, wrap on, wrap-style-word on) hosted in a
  `JScrollPane`. OK is now a `PrimaryButton`; Cancel and Delete are
  `SecondaryButton`s. `setText`/`getText` round-trips work unchanged
  against `JTextArea`.
- 2026-05-12 — Verified with `make clean && make compile` (clean build)
  and `grep -rn "new JButton" src` (no matches). All hand-rolled button
  styling has been removed; only `PrimaryButton` and `SecondaryButton`
  set button colors now.
- 2026-05-12 — Follow-up bug fix: the `ComboBox.*` namespace alone was
  not enough — `JComboBox` still rendered a bright white inset because
  Metal's `MetalComboBoxEditor.EditorBorder` (and several other Metal
  bevels) paint from the L&F-level system color keys, not from any
  `ComboBox.*` key. Extended `StandardTheme.apply()` to override
  `control` (→ `BACKGROUND_ELEVATED`), `controlHighlight`,
  `controlLtHighlight`, `controlShadow`, and `controlDkShadow`
  (all → `BORDER`), and added `ComboBox.editorBackground` and
  `ComboBox.editorBorder` for L&Fs that honor them. Empirical diagnostic
  (one-shot `JComboBox` border dump, since deleted) confirmed all four
  bevel keys were defaulting to white/grey before the fix and now
  resolve to the dark border colour after. No per-component styling was
  added to `AddServerDialog`; the fix remains app-wide.
