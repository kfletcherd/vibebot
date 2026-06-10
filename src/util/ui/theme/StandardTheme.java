package util.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;

/**
 * Global theme applicator. Just call this sucker before any swing components
 * are being used
 */
public final class StandardTheme {

	/**
	 * Need to use the static {@link #apply} method
	 */
	private StandardTheme() {
		throw new AssertionError("StandardTheme is not instantiable");
	}

	/**
	 * Set all standard theme colors globally
	 */
	public static void apply() {
		ColorUIResource bg = res(StandardColor.BACKGROUND_PRIMARY);
		ColorUIResource surface = res(StandardColor.BACKGROUND_ELEVATED);
		ColorUIResource fg = res(StandardColor.TEXT_PRIMARY);
		ColorUIResource muted = res(StandardColor.TEXT_SECONDARY);
		ColorUIResource accent = res(StandardColor.ACCENT);
		ColorUIResource border = res(StandardColor.BORDER);
		ColorUIResource selBg = res(StandardColor.BACKGROUND_SELECTED);
		ColorUIResource selFg = res(StandardColor.FOREGROUND_SELECTED);
		ColorUIResource focus = res(StandardColor.BORDER_FOCUSED);

		UIManager.put("Panel.background", bg);
		UIManager.put("Panel.foreground", fg);
		UIManager.put("RootPane.background", bg);
		UIManager.put("Viewport.background", bg);

		UIManager.put("window", bg);
		UIManager.put("windowBorder", bg);
		UIManager.put("windowText", fg);
		UIManager.put("Frame.background", bg);
		UIManager.put("OptionPane.background", bg);
		UIManager.put("OptionPane.foreground", fg);
		UIManager.put("OptionPane.messageForeground", fg);

		UIManager.put("Button.background", surface);
		UIManager.put("Button.foreground", fg);
		UIManager.put("Button.focus", accent);
		UIManager.put("Button.select", accent);
		UIManager.put("ToggleButton.background", surface);
		UIManager.put("ToggleButton.foreground", fg);
		UIManager.put("ToggleButton.select", accent);
		UIManager.put("CheckBox.background", bg);
		UIManager.put("CheckBox.foreground", fg);
		UIManager.put("CheckBox.focus", accent);
		UIManager.put("RadioButton.background", bg);
		UIManager.put("RadioButton.foreground", fg);
		UIManager.put("RadioButton.focus", accent);

		UIManager.put("Label.background", bg);
		UIManager.put("Label.foreground", fg);
		UIManager.put("Label.disabledForeground", muted);

		for (String p : new String[]{"TextField", "TextArea", "TextPane", "EditorPane", "PasswordField", "FormattedTextField"}) {
			UIManager.put(p + ".background", surface);
			UIManager.put(p + ".foreground", fg);
			UIManager.put(p + ".caretForeground", fg);
			UIManager.put(p + ".selectionBackground", selBg);
			UIManager.put(p + ".selectionForeground", selFg);
			UIManager.put(p + ".inactiveForeground", muted);
			UIManager.put(p + ".focusedBackground", surface);
		}
		UIManager.put("TextField.shadow", focus);
		UIManager.put("TextField.darkShadow", focus);

		UIManager.put("ScrollPane.background", bg);
		UIManager.put("ScrollPane.foreground", fg);
		UIManager.put("ScrollBar.background", bg);
		UIManager.put("ScrollBar.thumb", surface);
		UIManager.put("ScrollBar.thumbHighlight", border);
		UIManager.put("ScrollBar.thumbDarkShadow", border);
		UIManager.put("ScrollBar.thumbShadow", border);
		UIManager.put("ScrollBar.track", bg);
		UIManager.put("ScrollBar.trackHighlight", bg);

		UIManager.put("List.background", bg);
		UIManager.put("List.foreground", fg);
		UIManager.put("List.selectionBackground", selBg);
		UIManager.put("List.selectionForeground", selFg);
		UIManager.put("List.dropCellBackground", selBg);
		UIManager.put(
			"List.focusCellHighlightBorder",
			new BorderUIResource(BorderFactory.createLineBorder(StandardColor.ACCENT.color))
		);

		UIManager.put("Table.background", bg);
		UIManager.put("Table.foreground", fg);
		UIManager.put("Table.selectionBackground", selBg);
		UIManager.put("Table.selectionForeground", selFg);
		UIManager.put("Table.gridColor", border);
		UIManager.put("TableHeader.background", surface);
		UIManager.put("TableHeader.foreground", fg);
		UIManager.put("TableHeader.focusCellBackground", surface);
		UIManager.put("TableHeader.cellBorder", BorderFactory.createMatteBorder(0, 0, 1, 0, StandardColor.BORDER.color));

		for (String p : new String[]{"Menu", "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem"}) {
			UIManager.put(p + ".background", bg);
			UIManager.put(p + ".foreground", fg);
			UIManager.put(p + ".selectionBackground", selBg);
			UIManager.put(p + ".selectionForeground", selFg);
			UIManager.put(p + ".acceleratorForeground", muted);
			UIManager.put(p + ".acceleratorSelectionForeground", selFg);
			UIManager.put(p + ".disabledForeground", muted);
		}
		UIManager.put("MenuBar.background", surface);
		UIManager.put("MenuBar.foreground", fg);
		UIManager.put("PopupMenu.background", bg);
		UIManager.put("PopupMenu.foreground", fg);
		UIManager.put("Separator.background", border);
		UIManager.put("Separator.foreground", border);

		UIManager.put("ToolTip.background", surface);
		UIManager.put("ToolTip.foreground", fg);
		UIManager.put("ToolTip.border", new BorderUIResource(
			BorderFactory.createLineBorder(StandardColor.BORDER.color)
		));

		BorderUIResource lineBorder = new BorderUIResource(
			BorderFactory.createLineBorder(StandardColor.BORDER.color)
		);
		UIManager.put("TextField.border", lineBorder);
		UIManager.put("PasswordField.border", lineBorder);
		UIManager.put("TextArea.border", lineBorder);
		UIManager.put("ScrollPane.border", lineBorder);

		UIManager.put("ComboBox.background", surface);
		UIManager.put("ComboBox.foreground", fg);
		UIManager.put("ComboBox.selectionBackground", selBg);
		UIManager.put("ComboBox.selectionForeground", selFg);
		UIManager.put("ComboBox.buttonBackground", surface);
		UIManager.put("ComboBox.buttonDarkShadow", border);
		UIManager.put("ComboBox.buttonHighlight", border);
		UIManager.put("ComboBox.buttonShadow", border);
		UIManager.put("ComboBox.border", lineBorder);
		UIManager.put("ComboBox.editorBackground", surface);
		UIManager.put("ComboBox.editorBorder", lineBorder);

		// Metal's MetalComboBoxEditor.EditorBorder and several other Metal UI
		// delegates paint bevels using these L&F-level system color keys rather
		// than any ComboBox.* namespace. Without overriding them the combo's
		// internal editor draws a bright white inset around itself even when
		// ComboBox.border is dark.
		UIManager.put("control", surface);
		UIManager.put("controlHighlight", border);
		UIManager.put("controlLtHighlight", border);
		UIManager.put("controlShadow", border);
		UIManager.put("controlDkShadow", border);
	}

	/**
	 * Small helper for reducing some extra typing
	 *
	 * @param sc The standard color to wrap in a color ui resource
	 * @return The color ui resource, duh
	 */
	private static ColorUIResource res(StandardColor sc) {
		return new ColorUIResource(sc.color);
	}

}
