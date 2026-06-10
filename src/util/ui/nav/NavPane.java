package util.ui.nav;

import util.ui.pane.CardPane;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;

/**
 * A nav list that fires a callback when the user selects an item.
 *
 * <p>Pair with a {@link CardPane} by passing the selected {@link NavItem#id()}
 * to {@link CardPane#show(String)} in your {@code onSelect} callback.
 */
public final class NavPane {

	/** Default preferred width of the nav pane in pixels. */
	public static final int PREFERRED_WIDTH = 220;

	private static final int CELL_V_PAD = 6;
	private static final int CELL_H_PAD = 12;

	private final JScrollPane scrollPane;

	/**
	 * @param items the nav items to display; must not be null or empty
	 * @param onSelect callback fired on the EDT when the user selects an item
	 */
	public NavPane(List<NavItem> items, Consumer<NavItem> onSelect) {
		DefaultListModel<NavItem> model = new DefaultListModel<>();
		items.forEach(model::addElement);

		JList<NavItem> list = new JList<>(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new CellRenderer());

		list.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				NavItem selected = list.getSelectedValue();
				if (selected != null) onSelect.accept(selected);
			}
		});

		scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(PREFERRED_WIDTH, 0));
	}

	/**
	 * @return the root component to embed in your layout
	 */
	public JComponent component() {
		return scrollPane;
	}

	private final class CellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(
			JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
		) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			setBorder(new EmptyBorder(CELL_V_PAD, CELL_H_PAD, CELL_V_PAD, CELL_H_PAD));
			return this;
		}
	}
}
