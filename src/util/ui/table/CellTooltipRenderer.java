package util.ui.table;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

/**
 * Cell renderer that installs the displayed text as the cell's tooltip on every
 * render, or clears the tooltip when the value is null or blank. Installing
 * this as a {@link JTable}'s default renderer makes per-cell hover tooltips
 * available on every column with no per-column setup.
 */
public final class CellTooltipRenderer extends DefaultTableCellRenderer {

	public CellTooltipRenderer() {
		super();
	}

	@Override
	public Component getTableCellRendererComponent(
		JTable table,
		Object value,
		boolean isSelected,
		boolean hasFocus,
		int row,
		int column
	) {
		Component component = super.getTableCellRendererComponent(
			table, value, isSelected, hasFocus, row, column
		);
		String text = value == null ? "" : value.toString();
		setToolTipText(text.isBlank() ? null : text);
		return component;
	}
}
