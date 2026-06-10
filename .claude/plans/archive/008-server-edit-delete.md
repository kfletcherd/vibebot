---
id: "008"
title: Edit and Delete Servers
status: done
created: 2026-05-05
updated: 2026-05-05
---

## Summary

Adds the ability to edit and delete existing servers from the server list. Each row in the table gets an Edit button. Clicking it opens the existing Add Server dialog pre-filled with that row's data. Saving the dialog updates the entry in place. The dialog also gets a Delete button that asks the user to confirm before removing the server permanently.

## Goals

- Users can correct or update a server's details without deleting and re-adding it.
- Users can remove a server they no longer need.
- The edit and delete interactions feel like a natural extension of the existing Add Server flow, not a separate feature bolted on.

## Acceptance Criteria

- Every row in the server table has an Edit button.
- Clicking Edit opens a dialog pre-filled with that server's current values and titled "Edit Server."
- Saving the dialog updates that row immediately on screen and persists the change to disk.
- The Edit dialog has a Delete button, visually separated from Save and Cancel.
- Clicking Delete shows a confirmation prompt: "Are you sure you want to delete this server? This cannot be undone." with Delete and Cancel options.
- Confirming deletion removes the row from the table and from disk, then closes the dialog.
- Cancelling the confirmation returns the user to the Edit dialog with no changes made.
- The existing Add Server button and flow remain unchanged.

## Open Questions

None.

## Architecture

### Package Structure

No new packages. One new class lands in `util.ui.table`; all other changes are modifications to existing classes in `server.model` and `server.ui`.

```
util.ui.table
└── ButtonColumn          (new)

server.model
└── ServerStore           (modified)

server.ui
├── AddServerDialog       (modified)
├── ServerTableModel      (modified)
└── ServerListView        (modified)
```

### Components

| Component | Type | Package | Purpose |
|---|---|---|---|
| `ButtonColumn` | `final class` | `util.ui.table` | New. Renders a `JButton` in a table column and fires a `Consumer<Integer>` with the row index when clicked. Implements both `TableCellRenderer` and `TableCellEditor` using the standard Swing button-in-table pattern. |
| `ServerStore` | `final class` | `server.model` | Modified. Adds `update` and `delete` methods that mutate the in-memory list and rewrite the CSV. |
| `AddServerDialog` | `final class` | `server.ui` | Modified. Supports two modes (add and edit) selected by an `Optional<Server>` constructor argument. In edit mode the dialog is pre-populated, retitled "Edit Server", and shows a Delete button. Fires separate `onSave` and `onDelete` callbacks. |
| `ServerTableModel` | `final class` | `server.ui` | Modified. Adds `updateServer` and `deleteServer` to reflect in-place changes without a full reload. Overrides `getColumnCount` and `getColumnClass` to account for the extra Edit button column. |
| `ServerListView` | `final class` | `server.ui` | Modified. Wires `ButtonColumn` into the table, opens `AddServerDialog` in edit mode, and routes save/delete callbacks to both the store and the table model. |

### Signatures

#### `util.ui.table.ButtonColumn`

```java
public final class ButtonColumn
    extends javax.swing.AbstractCellEditor
    implements javax.swing.table.TableCellRenderer,
               javax.swing.table.TableCellEditor {

    /**
     * Installs a ButtonColumn on the given column of the given table.
     *
     * @param table the table to install into; must not be {@code null}
     * @param column the zero-based column index to render as buttons
     * @param label the text displayed on every button; must not be {@code null}
     * @param onClick consumer called with the row index when a button is clicked;
     *  must not be {@code null}
     */
    public ButtonColumn(JTable table, int column, String label, Consumer<Integer> onClick);

    @Override
    public java.awt.Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected,
        boolean hasFocus, int row, int column);

    @Override
    public java.awt.Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected, int row, int column);

    @Override
    public Object getCellEditorValue();
}
```

#### `server.model.ServerStore` (additions only)

```java
/**
 * Replaces {@code old} with {@code updated} in the in-memory list and rewrites
 * the CSV.
 *
 * @param old the existing server to replace; must not be {@code null}; must be
 *  present in the store
 * @param updated the replacement server; must not be {@code null}
 * @throws IOException if the CSV file cannot be written
 * @throws java.util.NoSuchElementException if {@code old} is not found
 */
public void update(Server old, Server updated) throws IOException;

/**
 * Removes {@code server} from the in-memory list and rewrites the CSV.
 *
 * @param server the server to remove; must not be {@code null}; must be
 *  present in the store
 * @throws IOException if the CSV file cannot be written
 * @throws java.util.NoSuchElementException if {@code server} is not found
 */
public void delete(Server server) throws IOException;
```

#### `server.ui.AddServerDialog` (full revised public API)

```java
public final class AddServerDialog {

    /**
     * Opens the dialog in add mode (empty fields, "Add Server" title, no Delete
     * button).
     *
     * @param owner the parent frame for modality; must not be {@code null}
     * @param onSave called with the new {@link Server} when the user clicks OK;
     *  must not be {@code null}
     */
    public AddServerDialog(JFrame owner, Consumer<Server> onSave);

    /**
     * Opens the dialog in either add or edit mode depending on {@code existing}.
     *
     * <p>When {@code existing} is empty the behaviour is identical to the
     * single-callback constructor. When {@code existing} is present the dialog
     * title becomes "Edit Server", all fields are pre-populated from the existing
     * server, and a Delete button is shown. Clicking Delete shows a
     * {@code JOptionPane} confirmation prompt; on confirmation, {@code onDelete}
     * is called and the dialog disposes itself.
     *
     * @param owner the parent frame for modality; must not be {@code null}
     * @param existing empty for add mode; the server to edit for edit mode; must
     *  not be {@code null}
     * @param onSave called with the saved {@link Server} when the user clicks OK;
     *  must not be {@code null}
     * @param onDelete called with the server to delete when the user confirms
     *  deletion; ignored in add mode; must not be {@code null}
     */
    public AddServerDialog(
        JFrame owner,
        Optional<Server> existing,
        Consumer<Server> onSave,
        Consumer<Server> onDelete);
}
```

#### `server.ui.ServerTableModel` (additions only)

```java
/**
 * Replaces the row occupied by {@code old} with {@code updated} in the backing
 * list and fires the appropriate table event.
 *
 * <p>Must be called on the Event Dispatch Thread.
 *
 * @param old the server to replace; must not be {@code null}
 * @param updated the replacement server; must not be {@code null}
 */
void updateServer(Server old, Server updated);

/**
 * Removes {@code server} from the backing list and fires the appropriate table
 * event.
 *
 * <p>Must be called on the Event Dispatch Thread.
 *
 * @param server the server to remove; must not be {@code null}
 */
void deleteServer(Server server);
```

`ServerTableModel` also gains an extra column at index `PRESENTATION_ORDER.length` (after all data columns) that `ButtonColumn` will occupy. `getColumnCount` returns `PRESENTATION_ORDER.length + 1`. `getColumnName` returns `""` for that index. `isCellEditable` returns `true` for that index (required for `TableCellEditor` interaction). `getValueAt` returns `""` for that index.

### Relationships

`ButtonColumn` is a generic `util.ui.table` helper with no dependency on any `server.*` type; it communicates purely through a `Consumer<Integer>` row-index callback. `ServerListView.buildTableScrollPane` creates the `JTable` with `tableModel`, then installs a `ButtonColumn` on the last column index; its `onClick` lambda receives the row index, retrieves the `Server` via `tableModel.getServerAt(int row)` (package-private accessor added to `ServerTableModel`), and opens `AddServerDialog` in edit mode. The edit dialog's `onSave` callback calls `store.update`, then `SwingUtilities.invokeLater` to call `tableModel.updateServer`. The `onDelete` callback calls `store.delete`, then `SwingUtilities.invokeLater` to call `tableModel.deleteServer`. The single-argument `AddServerDialog` constructor delegates to the two-callback constructor by passing `Optional.empty()` and a no-op `onDelete`.

`ServerStore.update` and `ServerStore.delete` both mutate `serverList` in place and call the existing private `persistToDisk()` method, so no new I/O logic is needed.

### Implementation Order

1. `util.ui.table.ButtonColumn` — no server dependencies; can be built and tested in isolation.
2. `server.model.ServerStore` — add `update` and `delete`; pure model change, no UI dependency.
3. `server.ui.ServerTableModel` — add `updateServer`, `deleteServer`, `getServerAt`, and the extra button column (updated `getColumnCount`, `getColumnName`, `isCellEditable`, `getValueAt`).
4. `server.ui.AddServerDialog` — refactor to two-mode design with the new four-argument constructor; keep the existing single-argument constructor as a delegate.
5. `server.ui.ServerListView` — wire `ButtonColumn` into `buildTableScrollPane`, add `openEditServerDialog(int row)`, and update `handleServerAdded` if needed.

## Implementation Notes

### 2026-05-05 — All components implemented; build passes clean.

1. **`util.ui.table.ButtonColumn`** — Created. Extends `AbstractCellEditor`, implements
   `TableCellRenderer` and `TableCellEditor`. The constructor installs itself on the
   specified column via `TableColumn#setCellRenderer` / `#setCellEditor`. The action
   listener records `editingRow` on each editor activation and calls `fireEditingStopped`
   after the `onClick` consumer fires.

2. **`server.model.ServerStore`** — Added `update(Server, Server)` and `delete(Server)`.
   Both mutate `serverList` in place, throw `NoSuchElementException` when the target is
   absent, then call the existing `persistToDisk()`. Added `java.util.NoSuchElementException`
   import.

3. **`server.ui.ServerTableModel`** — Added `BUTTON_COLUMN_INDEX` constant. `getColumnCount`
   returns `PRESENTATION_ORDER.length + 1`. `getColumnName` returns `""` for the button
   column. `getValueAt` returns `""` for the button column. `isCellEditable` returns `true`
   only for the button column. Added `updateServer(Server, Server)`, `deleteServer(Server)`,
   and package-private `getServerAt(int)`.

4. **`server.ui.AddServerDialog`** — Redesigned to two-mode. The original
   single-argument constructor now delegates to the four-argument constructor with
   `Optional.empty()` and a no-op delete callback. Edit mode pre-populates all fields via
   `populateFields`, changes the title to "Edit Server", and adds a Delete button on the
   left side of the button row using `BorderLayout` split. `handleDelete` uses
   `JOptionPane.showConfirmDialog` with `OK_CANCEL_OPTION`; confirmation calls `onDelete`
   and disposes. The `onConfirm` field was renamed to `onSave` to match the new API.

5. **`server.ui.ServerListView`** — Added `ButtonColumn` import. `buildTableScrollPane`
   derives `editColumnIndex` from `tableModel.getColumnCount() - 1` and installs a
   `ButtonColumn` referencing `this::openEditServerDialog`. Added `openEditServerDialog(int)`
   which fetches the `Server` via `tableModel.getServerAt(row)` and opens the dialog in
   edit mode. Added `handleServerUpdated` and `handleServerDeleted` which call the store
   methods and then `SwingUtilities.invokeLater` to update the table model.
