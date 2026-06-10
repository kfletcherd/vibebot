package util.ui.nav;

import util.ui.pane.CardPane;
import util.ui.pane.PaneView;

/**
 * A single entry in a {@link NavPane}.
 *
 * <p>The {@code id} should match the {@link PaneView#id()} of the view it corresponds
 * to in a {@link CardPane}, so a nav selection can be passed directly to {@link CardPane#show(String)}.
 *
 * @param id unique identifier for this item
 * @param label display text shown in the nav list
 */
public record NavItem(String id, String label) {

	/**
	 * @throws IllegalArgumentException if {@code id} or {@code label} is null or blank
	 */
	public NavItem {
		if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
		if (label == null || label.isBlank()) throw new IllegalArgumentException("label must not be blank");
	}

	/**
	 * Returns {@link #label} so the nav list renders human-readable text
	 */
	@Override
	public String toString() {
		return label;
	}

}
