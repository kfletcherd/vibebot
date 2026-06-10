package util.ui.pane;

import javax.swing.JComponent;

/**
 * A view that can be registered and displayed in a {@link CardPane}.
 */
public interface PaneView {

	/**
	 * Unique id used to identify this view in a {@link CardPane}.
	 *
	 * @return non-null, non-blank string id
	 */
	String id();

	/**
	 * The Swing component that renders this view's content.
	 *
	 * @return the same instance on every call; never {@code null}
	 */
	JComponent component();
}
