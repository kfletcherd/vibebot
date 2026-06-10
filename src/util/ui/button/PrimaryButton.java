package util.ui.button;

import util.ui.theme.StandardColor;

import javax.swing.BorderFactory;
import javax.swing.JButton;

/**
 * App-wide primary button: dark blue background with white text.
 * Drop-in replacement for {@link JButton} on primary actions.
 */
public final class PrimaryButton extends JButton {

	/** Inner padding around the label in pixels. */
	private static final int PAD_VERTICAL = 6;

	/** Inner horizontal padding around the label in pixels. */
	private static final int PAD_HORIZONTAL = 14;

	public PrimaryButton(String text) {
		super(text);
		setBackground(StandardColor.BUTTON_PRIMARY.color);
		setForeground(StandardColor.FOREGROUND_SELECTED.color);
		setFocusPainted(false);
		setOpaque(true);
		setBorderPainted(false);
		setBorder(BorderFactory.createEmptyBorder(
			PAD_VERTICAL, PAD_HORIZONTAL, PAD_VERTICAL, PAD_HORIZONTAL
		));
	}
}
