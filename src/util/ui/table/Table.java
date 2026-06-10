package util.ui.table;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.util.List;

/**
 * Simple implementation of a generic table for general use
 */
public final class Table {

	/**
	 * Row height for all rows in pixels
	 */
	private static final int ROW_HEIGHT = 24;

	/**
	 * The scroll pane returned by {@link #component()}; allocated once at construction
	 */
	private final JScrollPane scrollPane;

	/**
	 * The internal data model; all mutations go through this
	 */
	private final TableModel tableModel;

	/**
	 * Constructs a {@code TableView} with the given headers and initial rows.
	 *
	 * <p>Defensive copies of both lists are made so that subsequent external
	 * mutations do not affect the table.
	 *
	 * @param headers the column header labels in display order
	 */
	public Table(List<String> headers) {
		tableModel = new TableModel(headers);
		JTable t = new JTable((tableModel));
		t.setRowHeight(ROW_HEIGHT);
		t.setShowGrid(true);
		t.setDefaultRenderer(Object.class, new CellTooltipRenderer());
		scrollPane = new JScrollPane(t);
	}

	/**
	 * Returns the scroll pane component containing the table.
	 *
	 * @return Returns the JScrollPane for injection into other UI elements
	 */
	public JScrollPane component() {
		return scrollPane;
	}

	/**
	 * Appends a single row to the bottom of the table.
	 *
	 * <p>A defensive copy of the supplied list is made.
	 *
	 * @param row the cell values for the new row, in column order; must not be
	 *  {@code null}
	 */
	public void addRow(List<String> row) {
		tableModel.addRow(row);
	}

	/**
	 * Returns the current number of data rows in the table.
	 *
	 * @return the row count; zero or positive
	 */
	public int rowCount() {
		return tableModel.getRowCount();
	}

}
