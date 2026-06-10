package util.ui.table;

import util.ui.button.SecondaryButton;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Component;
import java.util.function.Consumer;

/**
 * Renders and edits a {@link JTable} column as a clickable
 * {@link SecondaryButton}.
 *
 * Install an instance on any column of any table by passing the target table,
 * the zero-based column index, the button label, and a {@link Consumer} that
 * receives the row index whenever the button is clicked. Per-row buttons in
 * any table — present or future — automatically pick up the secondary look.
 */
public final class ButtonColumn
	extends AbstractCellEditor
	implements TableCellRenderer, TableCellEditor {

	/** The shared button component used for both rendering and editing. */
	private final SecondaryButton button;

	/** The row index of the cell currently being edited. */
	private int editingRow;

	/**
	 * Installs a {@code ButtonColumn} on the given column of the given table.
	 */
	public ButtonColumn(JTable table, int column, String label, Consumer<Integer> onClick) {
		this.button = new SecondaryButton(label);
		this.editingRow = -1;

		button.addActionListener(e -> {
			onClick.accept(editingRow);
			fireEditingStopped();
		});

		TableColumn tableColumn = table.getColumnModel().getColumn(column);
		tableColumn.setCellRenderer(this);
		tableColumn.setCellEditor(this);
	}

	/**
	 * Returns the button component for rendering the cell at the given position.
	 *
	 * @param table the table asking for the renderer; may be {@code null}
	 * @param value the cell value (unused — the label is fixed at construction time)
	 * @param isSelected {@code true} if the cell is selected
	 * @param hasFocus {@code true} if the cell has keyboard focus
	 * @param row the zero-based row index
	 * @param column the zero-based column index
	 * @return the shared button component; never {@code null}
	 */
	@Override
	public Component getTableCellRendererComponent(
		JTable table,
		Object value,
		boolean isSelected,
		boolean hasFocus,
		int row,
		int column
	) {
		return button;
	}

	/**
	 * Returns the button component for editing the cell at the given position,
	 * recording the row index so the action listener can pass it to the callback.
	 *
	 * @param table the table asking for the editor; may be {@code null}
	 * @param value the cell value (unused)
	 * @param isSelected {@code true} if the cell is selected
	 * @param row the zero-based row index
	 * @param column the zero-based column index
	 * @return the shared button component; never {@code null}
	 */
	@Override
	public Component getTableCellEditorComponent(
		JTable table,
		Object value,
		boolean isSelected,
		int row,
		int column
	) {
		editingRow = row;
		return button;
	}

	/**
	 * Returns the current editor value.
	 *
	 * <p>Button columns do not produce a meaningful cell value; this method
	 * returns an empty string to satisfy the {@link TableCellEditor} contract.
	 *
	 * @return an empty string; never {@code null}
	 */
	@Override
	public Object getCellEditorValue() {
		return "";
	}
}
