package util.ui.pane;

import java.awt.CardLayout;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Hosts multiple {@link PaneView}s and shows one at a time via {@link CardLayout}.
 */
public final class CardPane {

	private final CardLayout layout;
	private final JPanel container;

	/**
	 * @param views the views to register; must not be null or empty
	 */
	public CardPane(List<? extends PaneView> views) {
		layout = new CardLayout();
		container = new JPanel(layout);
		for (PaneView view : views) {
			container.add(view.component(), view.id());
		}
	}

	/**
	 * Flips to the view with the given id. No-ops silently if the id isn't registered.
	 *
	 * @param id the id of the view to show
	 */
	public void show(String id) {
		layout.show(container, id);
	}

	/**
	 * @return the root component to embed in your layout
	 */
	public JComponent component() {
		return container;
	}
}
