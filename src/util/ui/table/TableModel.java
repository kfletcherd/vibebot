package util.ui.table;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

class TableModel extends AbstractTableModel {

	/**
	 * Column header labels in display order.
	 */
	private final List<String> headers;

	/**
	 * Mutable backing list of rows; each row is a list of cell strings.
	 */
	private final List<List<String>> rows;

	TableModel(List<String> headers) {
		this.headers = headers;
		this.rows = new ArrayList<>();
	}

	@Override
	public int getRowCount() {
		return rows.size();
	}

	@Override
	public int getColumnCount() {
		return headers.size();
	}

	@Override
	public String getColumnName(int column) {
		return headers.get(column);
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return rows.get(rowIndex).get(columnIndex);
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	/**
	 * Add a new row to the table
	 *
	 * @param row The row to add to the table
	 */
	void addRow(List<String> row) {
		rows.add(new ArrayList<>(row));
		fireTableRowsInserted(row.size(), row.size());
	}

}
