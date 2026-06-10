package server.ui.sslcheck;

import server.ui.sslcheck.SslCheckReportRow.ExpiryClassification;
import util.ui.button.SecondaryButton;
import util.ui.theme.StandardColor;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Modal dialog rendering the SSL check report as a themed table with a Close
 * button.
 */
final class SslCheckReportDialog extends JDialog {

	private static final String TITLE = "SSL Certificate Check";

	private static final int PADDING = 12;

	private static final int PREFERRED_WIDTH = 720;

	private static final int PREFERRED_HEIGHT = 420;

	private static final int ROW_HEIGHT = 24;

	/** Background tint applied to rows whose certificate is expiring within 30 days. */
	private static final Color EXPIRING_SOON_BG = StandardColor.ACCENT.color;

	/** Background tint applied to rows whose certificate has already expired. */
	private static final Color EXPIRED_BG = new Color(0xB04A4A);

	SslCheckReportDialog(JFrame owner, List<SslCheckReportRow> rows) {
		super(owner, TITLE, true);
		if (rows == null) throw new IllegalArgumentException("rows must not be null");

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		SslCheckReportTableModel model = new SslCheckReportTableModel(rows);

		JPanel content = new JPanel(new BorderLayout(PADDING, PADDING));
		content.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		content.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

		if (rows.isEmpty()) {
			content.add(buildEmptyMessage(), BorderLayout.CENTER);
		} else {
			content.add(buildTableScrollPane(model), BorderLayout.CENTER);
		}
		content.add(buildButtonRow(), BorderLayout.SOUTH);

		setContentPane(content);
		setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
		pack();
		setLocationRelativeTo(owner);
	}

	/**
	 * Convenience entry point used by {@link SslCheckAction} to build and show
	 * the dialog in one call.
	 */
	static void show(JFrame owner, List<SslCheckReportRow> rows) {
		new SslCheckReportDialog(owner, rows).setVisible(true);
	}

	private static JComponent buildEmptyMessage() {
		JLabel label = new JLabel(
			"No servers with a hostname were found to check.",
			SwingConstants.CENTER
		);
		label.setForeground(StandardColor.TEXT_PRIMARY.color);
		label.setOpaque(true);
		label.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		return label;
	}

	private static JScrollPane buildTableScrollPane(SslCheckReportTableModel model) {
		JTable table = new JTable(model);
		table.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		table.setForeground(StandardColor.TEXT_PRIMARY.color);
		table.setGridColor(StandardColor.BORDER.color);
		table.setSelectionBackground(StandardColor.BACKGROUND_SELECTED.color);
		table.setSelectionForeground(StandardColor.FOREGROUND_SELECTED.color);
		table.setRowHeight(ROW_HEIGHT);
		table.setShowGrid(true);
		table.setDefaultRenderer(Object.class, new ExpiryAwareRenderer(model));

		table.getTableHeader().setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		table.getTableHeader().setForeground(StandardColor.TEXT_PRIMARY.color);
		table.getTableHeader().setBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, StandardColor.BORDER.color)
		);

		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		scrollPane.getViewport().setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		scrollPane.setBorder(BorderFactory.createLineBorder(StandardColor.BORDER.color));
		return scrollPane;
	}

	private JPanel buildButtonRow() {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		row.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		SecondaryButton closeButton = new SecondaryButton("Close");
		closeButton.addActionListener(e -> dispose());
		row.add(closeButton);
		return row;
	}

	/**
	 * Cell renderer that tints each row according to its
	 * {@link ExpiryClassification}.
	 */
	private static final class ExpiryAwareRenderer extends DefaultTableCellRenderer {

		private final SslCheckReportTableModel model;

		ExpiryAwareRenderer(SslCheckReportTableModel model) {
			this.model = model;
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
			Component c = super.getTableCellRendererComponent(
				table, value, isSelected, hasFocus, row, column
			);
			if (!isSelected) {
				ExpiryClassification expiry = model.getRowAt(row).expiry();
				c.setBackground(backgroundFor(expiry));
				c.setForeground(foregroundFor(expiry));
			}
			return c;
		}

		private static Color backgroundFor(ExpiryClassification expiry) {
			return switch (expiry) {
				case EXPIRING_SOON -> EXPIRING_SOON_BG;
				case EXPIRED -> EXPIRED_BG;
				case OK, UNKNOWN -> StandardColor.BACKGROUND_ELEVATED.color;
			};
		}

		private static Color foregroundFor(ExpiryClassification expiry) {
			return switch (expiry) {
				case EXPIRING_SOON, EXPIRED -> StandardColor.FOREGROUND_SELECTED.color;
				case OK, UNKNOWN -> StandardColor.TEXT_PRIMARY.color;
			};
		}
	}
}
