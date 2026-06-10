package util.ui.theme;

import java.awt.Color;

/**
 * Standard colors for this app
 */
public enum StandardColor {

	/** Primary background color */
	BACKGROUND_PRIMARY(0x1E1E1E),

	/** Slightly elevated surface color (e.g., panels, cards) */
	BACKGROUND_ELEVATED(0x2D2D2D),

	/** Primary text color */
	TEXT_PRIMARY(0xE0E0E0),

	/** Secondary text color (muted a little) */
	TEXT_SECONDARY(0x9E9E9E),

	/** Primary accent color used for highlights and focus rings. */
	ACCENT(0x82AAFF),

	/** Component borders and dividers. */
	BORDER(0x3C3C3C),

	/** The selection background in lists, tables, text fields, etc. */
	BACKGROUND_SELECTED(0x264F78),

	/** The selection foreground for lists, tables, text fields, etc. */
	FOREGROUND_SELECTED(0xFFFFFF),

	/** Focused component outline (e.g., text field focus border). */
	BORDER_FOCUSED(0x5C9BD4),

	/** Surface color for app-wide primary buttons (dark blue). */
	BUTTON_PRIMARY(0x1F3A5F),

	/** Surface color for app-wide secondary buttons (dark grey). */
	BUTTON_SECONDARY(0x3A3A3A);

	/**
	 * The color's hex code
	 */
	public final int hex;

	/**
	 * The Color object of this color
	 */
	public final Color color;

	StandardColor(int hex){
		this.hex = hex;
		this.color = new Color(hex);
	}

}
