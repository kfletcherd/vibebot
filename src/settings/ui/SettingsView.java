package settings.ui;

import app.AppSettings;
import server.model.ServerStore;
import server.model.ServerStoreException;
import settings.SettingsFileWriter;
import settings.SettingsKeys;
import util.ui.button.PrimaryButton;
import util.ui.pane.PaneView;
import util.ui.theme.StandardColor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link PaneView} implementation for the {@code "settings"} navigation item.
 *
 * <p>Renders a simple form with two labeled text fields:
 * <ol>
 *   <li><b>Data directory</b> — the path where {@code servers.csv} and
 *       {@code vibebot.settings} are stored (mapped to
 *       {@link SettingsKeys#DATA_DIR}).</li>
 *   <li><b>SSH key path</b> — the path to the SSH private key (mapped to
 *       {@link SettingsKeys#SSH_KEY_PATH}).</li>
 * </ol>
 *
 * <p>A single <em>Save</em> button covers both fields. On save:
 * <ol>
 *   <li>The {@code dataDir} field is validated as a syntactically valid path.
 *       If it is invalid, a status label displays the error and no changes are
 *       persisted.</li>
 *   <li>Both values are written to disk via {@link SettingsFileWriter#write}.</li>
 *   <li>A new {@link AppSettings} instance is constructed via
 *       {@link AppSettings#loadTyped(java.util.Map)} and installed via
 *       {@link AppSettings#setInstance(AppSettings)}.</li>
 *   <li>If {@code dataDir} changed, {@link ServerStore#reload(Path)} is called on
 *       a background thread. The status label is updated on the EDT when the
 *       reload completes.</li>
 * </ol>
 *
 * <p>The root component returned by {@link #component()} is always the same
 * {@link JPanel} instance created at construction time.
 */
public final class SettingsView implements PaneView {

	/** The view id that must match the corresponding {@link util.ui.nav.NavItem}. */
	private static final String VIEW_ID = "settings";

	/** Preferred width of each text field in pixels. */
	private static final int FIELD_WIDTH = 400;

	/** Preferred height of each text field in pixels. */
	private static final int FIELD_HEIGHT = 28;

	/** Outer padding around the form content area in pixels. */
	private static final int OUTER_PADDING = 24;

	/** Vertical spacing between form rows in pixels. */
	private static final int ROW_SPACING = 12;

	/** Vertical spacing between the label and its text field in pixels. */
	private static final int LABEL_FIELD_GAP = 4;

	/** Vertical spacing between the last field and the Save button in pixels. */
	private static final int BUTTON_GAP = 20;

	/** The application settings snapshot used only to pre-populate the text fields at construction time. */
	private final AppSettings appSettings;

	/** The server store reloaded when the data directory changes. */
	private final ServerStore serverStore;

	/** Root panel returned by {@link #component()}; same instance on every call. */
	private final JPanel rootPanel;

	/** Text field for the data directory setting. */
	private final JTextField dataDirField;

	/** Text field for the SSH key path setting. */
	private final JTextField sshKeyPathField;

	/** Status label displayed below the Save button. */
	private final JLabel statusLabel;

	/**
	 * Constructs a {@code SettingsView}.
	 *
	 * <p>Pre-populates both text fields with the current values from
	 * {@code appSettings}. No I/O is performed at construction time.
	 *
	 * @param appSettings the global application settings; must not be {@code null}
	 * @param serverStore the server store to reload if the data directory changes;
	 *  may be {@code null} if the store failed to load on startup (in which case
	 *  the reload step is skipped)
	 */
	public SettingsView(AppSettings appSettings, ServerStore serverStore) {
		this.appSettings = appSettings;
		this.serverStore = serverStore;

		dataDirField = buildTextField(appSettings.dataDir().toString());
		sshKeyPathField = buildTextField(appSettings.sshKeyPath());
		statusLabel = buildStatusLabel();

		rootPanel = buildRootPanel();
	}

	/**
	 * Returns {@code "settings"}, matching the nav item that triggers this view.
	 *
	 * @return the view identifier; never {@code null}
	 */
	@Override
	public String id() {
		return VIEW_ID;
	}

	/**
	 * Returns the root panel for this view.
	 *
	 * <p>The same instance is returned on every call.
	 *
	 * @return the non-null root {@link JPanel}; same instance on every call
	 */
	@Override
	public JComponent component() {
		return rootPanel;
	}

	/**
	 * Assembles the full form panel.
	 *
	 * @return the configured root panel; never {@code null}
	 */
	private JPanel buildRootPanel() {
		JPanel panel = new JPanel();
		panel.setBackground(StandardColor.BACKGROUND_PRIMARY.color);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(
			OUTER_PADDING, OUTER_PADDING, OUTER_PADDING, OUTER_PADDING
		));

		panel.add(buildFormTitle());
		panel.add(Box.createVerticalStrut(ROW_SPACING * 2));
		panel.add(buildLabeledRow("Data directory", dataDirField));
		panel.add(Box.createVerticalStrut(ROW_SPACING));
		panel.add(buildLabeledRow("SSH key path", sshKeyPathField));
		panel.add(Box.createVerticalStrut(BUTTON_GAP));
		panel.add(buildSaveButton());
		panel.add(Box.createVerticalStrut(ROW_SPACING));
		panel.add(statusLabel);
		panel.add(Box.createVerticalGlue());

		return panel;
	}

	/**
	 * Builds the section title label for the settings form.
	 *
	 * @return a styled {@link JLabel}; never {@code null}
	 */
	private JLabel buildFormTitle() {
		JLabel title = new JLabel("Settings");
		title.setForeground(StandardColor.TEXT_PRIMARY.color);
		title.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		return title;
	}

	/**
	 * Builds a vertically stacked label-and-field row for a single setting.
	 *
	 * @param labelText the human-readable label for the field; must not be {@code null}
	 * @param field the text field to place below the label; must not be {@code null}
	 * @return a {@link JPanel} containing the label and field; never {@code null}
	 */
	private JPanel buildLabeledRow(String labelText, JTextField field) {
		JPanel row = new JPanel();
		row.setBackground(StandardColor.BACKGROUND_PRIMARY.color);
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setAlignmentX(JPanel.LEFT_ALIGNMENT);

		JLabel label = new JLabel(labelText);
		label.setForeground(StandardColor.TEXT_SECONDARY.color);
		label.setAlignmentX(JLabel.LEFT_ALIGNMENT);

		row.add(label);
		row.add(Box.createVerticalStrut(LABEL_FIELD_GAP));
		row.add(field);

		return row;
	}

	/**
	 * Creates a themed text field pre-populated with {@code initialValue}.
	 *
	 * @param initialValue the text shown on first render; must not be {@code null}
	 * @return a configured {@link JTextField}; never {@code null}
	 */
	private JTextField buildTextField(String initialValue) {
		JTextField field = new JTextField(initialValue);
		field.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		field.setForeground(StandardColor.TEXT_PRIMARY.color);
		field.setCaretColor(StandardColor.TEXT_PRIMARY.color);
		field.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(StandardColor.BORDER.color),
			BorderFactory.createEmptyBorder(4, 8, 4, 8)
		));
		field.setMaximumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
		field.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
		field.setAlignmentX(JTextField.LEFT_ALIGNMENT);
		return field;
	}

	/**
	 * Builds the status label shown below the Save button.
	 *
	 * <p>The label starts empty and is updated after each save attempt to indicate
	 * success or the nature of the failure.
	 *
	 * @return a configured {@link JLabel}; never {@code null}
	 */
	private JLabel buildStatusLabel() {
		JLabel label = new JLabel(" ");
		label.setForeground(StandardColor.TEXT_SECONDARY.color);
		label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		return label;
	}

	/**
	 * Builds the Save button that persists both fields when clicked.
	 *
	 * @return a configured {@link JButton}; never {@code null}
	 */
	private JButton buildSaveButton() {
		PrimaryButton button = new PrimaryButton("Save");
		button.setAlignmentX(JButton.LEFT_ALIGNMENT);
		button.addActionListener(e -> onSave());
		return button;
	}

	/**
	 * Handles the Save button click.
	 *
	 * <p>Called on the EDT. Validates the data directory field, then — if valid —
	 * persists both settings to disk, constructs a fresh {@link AppSettings} via
	 * {@link AppSettings#loadTyped(java.util.Map)}, installs it via
	 * {@link AppSettings#setInstance(AppSettings)}, and reloads the
	 * {@link ServerStore} on a background thread if the data directory changed.
	 */
	private void onSave() {
		AppSettings current = AppSettings.instance();

		String rawDataDir = dataDirField.getText().trim();
		String rawSshKey = sshKeyPathField.getText().trim();

		Path newDataDir;
		try {
			newDataDir = Path.of(rawDataDir);
		} catch (InvalidPathException e) {
			setStatus("Invalid data directory: " + e.getReason(), true);
			return;
		}

		Path previousDataDir = current.dataDir();
		boolean dataDirChanged = !newDataDir.equals(previousDataDir);

		Map<SettingsKeys, String> newTyped = buildSettingsMap(rawDataDir, rawSshKey);

		Map<String, String> newRaw = toStringKeyMap(newTyped);
		try {
			SettingsFileWriter.write(
				newDataDir.resolve("vibebot.settings"),
				newRaw
			);
		} catch (IOException e) {
			setStatus("Could not save settings: " + e.getMessage(), true);
			return;
		}

		AppSettings newSettings = AppSettings.loadTyped(newTyped);
		AppSettings.setInstance(newSettings);

		if (dataDirChanged && serverStore != null) {
			setStatus("Saving and reloading server list...", false);
			Thread.ofVirtual().start(() -> reloadStoreInBackground(newDataDir));
		} else {
			setStatus("Settings saved.", false);
		}
	}

	/**
	 * Reloads the {@link ServerStore} on a background thread after the data
	 * directory changes, then posts a status update back to the EDT.
	 *
	 * @param newDataDir the new data directory to reload from; must not be
	 *  {@code null}
	 */
	private void reloadStoreInBackground(Path newDataDir) {
		try {
			serverStore.reload(newDataDir);
			SwingUtilities.invokeLater(() -> setStatus("Settings saved.", false));
		} catch (ServerStoreException e) {
			SwingUtilities.invokeLater(
				() -> setStatus("Settings saved, but server list reload failed: " + e.getMessage(), true)
			);
		}
	}

	/**
	 * Builds a typed settings map from the two field values.
	 *
	 * <p>Using {@link SettingsKeys} as the map key type ensures that callers which
	 * receive this map (such as {@link AppSettings#loadTyped(Map)}) are type-safe and
	 * cannot inadvertently pass an arbitrary string where a known key is expected.
	 *
	 * @param dataDirValue the raw data directory string; must not be {@code null}
	 * @param sshKeyValue the raw SSH key path string; must not be {@code null}
	 * @return a typed map keyed by {@link SettingsKeys}; never {@code null}
	 */
	private Map<SettingsKeys, String> buildSettingsMap(String dataDirValue, String sshKeyValue) {
		Map<SettingsKeys, String> map = new LinkedHashMap<>();
		map.put(SettingsKeys.DATA_DIR, dataDirValue);
		map.put(SettingsKeys.SSH_KEY_PATH, sshKeyValue);
		return map;
	}

	/**
	 * Converts a typed settings map to a {@code Map<String, String>} for the file
	 * I/O boundary.
	 *
	 * <p>This is the only location in this class where {@link SettingsKeys#key()}
	 * is called. The result is passed directly to
	 * {@link SettingsFileWriter#write} and is not used for any other purpose.
	 *
	 * @param typed the typed settings map; must not be {@code null}
	 * @return a string-keyed copy of {@code typed}; never {@code null}
	 */
	private Map<String, String> toStringKeyMap(Map<SettingsKeys, String> typed) {
		Map<String, String> raw = new LinkedHashMap<>();
		for (Map.Entry<SettingsKeys, String> entry : typed.entrySet()) {
			raw.put(entry.getKey().key(), entry.getValue());
		}
		return raw;
	}

	/**
	 * Updates the status label text and color on the EDT.
	 *
	 * <p>Must be called from the EDT. If an error condition is indicated, the label
	 * is shown in the accent color to draw attention; otherwise it uses the muted
	 * text color.
	 *
	 * @param message the status message to display; must not be {@code null}
	 * @param isError {@code true} to show the message as an error (accent color),
	 *  {@code false} to show it as informational (muted color)
	 */
	private void setStatus(String message, boolean isError) {
		statusLabel.setText(message);
		statusLabel.setForeground(isError ? StandardColor.ACCENT.color : StandardColor.TEXT_SECONDARY.color);
	}
}
