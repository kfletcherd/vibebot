package server.ui;

import server.model.HostingProvider;
import server.model.Server;
import server.model.ServerEnvironment;
import server.model.ServerType;
import util.ui.button.PrimaryButton;
import util.ui.button.SecondaryButton;
import util.ui.theme.StandardColor;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Modal dialog for adding a new server or editing an existing one.
 *
 * <p>Supports two modes selected by the constructor used:
 * <ul>
 *   <li><b>Add mode</b> — opened via {@link #AddServerDialog(JFrame, Consumer)}.
 *       Fields are blank, the title is "Add Server", and no Delete button is shown.</li>
 *   <li><b>Edit mode</b> — opened via
 *       {@link #AddServerDialog(JFrame, Optional, Consumer, Consumer)} with a
 *       non-empty {@code existing} value. Fields are pre-populated from the existing
 *       server, the title is "Edit Server", and a Delete button is shown in the
 *       bottom-left of the button row.</li>
 * </ul>
 *
 * <p>The OK button is enabled when the required fields are valid: {@code shortName}
 * is non-blank and at least one {@link ServerType} is selected in the type list.
 * IP address fields and the hostname field are validated on save; a failure shows
 * a {@link JOptionPane} error and prevents saving.
 *
 * <p>Clicking OK invokes {@code onSave} with the constructed {@link Server} and
 * disposes the dialog. Clicking Cancel or closing the window disposes the dialog
 * without invoking any callback. In edit mode, clicking Delete shows a
 * {@link JOptionPane} confirmation prompt; on confirmation, {@code onDelete} is
 * called and the dialog disposes itself.
 *
 * <p>The dialog is themed using {@link StandardColor} constants.
 */
public final class AddServerDialog {

	/** Dialog title in add mode. */
	private static final String TITLE_ADD = "Add Server";

	/** Dialog title in edit mode. */
	private static final String TITLE_EDIT = "Edit Server";

	/** Width of each text field in columns (characters). */
	private static final int FIELD_COLUMNS = 30;

	/** Padding between form rows in pixels. */
	private static final int ROW_PADDING = 6;

	/** Outer padding around the form panel in pixels. */
	private static final int OUTER_PADDING = 16;

	/** Number of visible rows in the type multi-select list. */
	private static final int TYPE_LIST_VISIBLE_ROWS = 5;

	/** Number of visible rows in the notes text area. */
	private static final int NOTES_VISIBLE_ROWS = 4;

	/** Confirmation message shown before deleting a server. */
	private static final String DELETE_CONFIRM_MESSAGE =
		"Are you sure you want to delete this server? This cannot be undone.";

	/** Title of the delete confirmation dialog. */
	private static final String DELETE_CONFIRM_TITLE = "Confirm Deletion";

	/** Error message shown when an IP address field contains an invalid value. */
	private static final String IP_VALIDATION_MESSAGE =
		"IP address fields must contain a valid IPv4 or IPv6 address, or be left blank.";

	/** Error message shown when the hostname field contains a protocol prefix or path. */
	private static final String HOSTNAME_VALIDATION_MESSAGE =
		"Hostname must not include a protocol prefix (e.g. https://) or a path segment (e.g. /path).";

	/** Title of the validation error dialog. */
	private static final String VALIDATION_ERROR_TITLE = "Validation Error";

	/**
	 * Pattern that accepts IPv4 addresses and IPv6 addresses (including
	 * compressed forms). Blank values are handled before this pattern is applied.
	 */
	private static final Pattern IP_PATTERN = Pattern.compile(
		"^(?:"
		+ "(?:\\d{1,3}\\.){3}\\d{1,3}"
		+ "|"
		+ "(?:[0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}"
		+ ")$"
	);

	/** The underlying Swing dialog. */
	private final JDialog dialog;

	/** Text field for the public IP address. */
	private final JTextField publicIpField;

	/** Text field for the PVLAN IP address. */
	private final JTextField pvlanIpField;

	/** Text field for the short name (required). */
	private final JTextField shortNameField;

	/** Dropdown for the deployment environment (optional). */
	private final JComboBox<ServerEnvironment> environmentCombo;

	/** Dropdown for the hosting provider (optional). */
	private final JComboBox<HostingProvider> hostingProviderCombo;

	/** Multi-select list for server types (at least one required). */
	private final JList<ServerType> typeList;

	/** Text field for the hostname (optional). */
	private final JTextField hostnameField;

	/** Multi-line text area for the notes (optional). */
	private final JTextArea notesField;

	/** The OK button; enabled only when the form is valid. */
	private final JButton okButton;

	/** Invoked with the saved {@link Server} when the user confirms. */
	private final Consumer<Server> onSave;

	/** Invoked with the server to delete when the user confirms deletion. */
	private final Consumer<Server> onDelete;

	/** The server being edited in edit mode; empty in add mode. */
	private final Optional<Server> existing;

	/**
	 * Opens the dialog in add mode (empty fields, "Add Server" title, no Delete
	 * button).
	 *
	 * <p>Delegates to the four-argument constructor with {@link Optional#empty()}
	 * and a no-op delete callback.
	 *
	 * @param owner the parent frame for modality; must not be {@code null}
	 * @param onSave called with the new {@link Server} when the user clicks OK;
	 *  must not be {@code null}
	 */
	public AddServerDialog(JFrame owner, Consumer<Server> onSave) {
		this(owner, Optional.empty(), onSave, ignored -> {});
	}

	/**
	 * Opens the dialog in either add or edit mode depending on {@code existing}.
	 *
	 * <p>When {@code existing} is empty, behaviour is identical to
	 * {@link #AddServerDialog(JFrame, Consumer)}. When {@code existing} is
	 * present, the dialog title becomes "Edit Server", all fields are
	 * pre-populated from the existing server, and a Delete button is shown.
	 * Clicking Delete presents a {@link JOptionPane} confirmation; on
	 * confirmation, {@code onDelete} is called and the dialog disposes itself.
	 *
	 * @param owner the parent frame for modality; must not be {@code null}
	 * @param existing empty for add mode; the server to edit for edit mode; must
	 *  not be {@code null}
	 * @param onSave called with the saved {@link Server} when the user clicks OK;
	 *  must not be {@code null}
	 * @param onDelete called with the server to delete when the user confirms
	 *  deletion; ignored in add mode; must not be {@code null}
	 */
	public AddServerDialog(
		JFrame owner,
		Optional<Server> existing,
		Consumer<Server> onSave,
		Consumer<Server> onDelete
	) {
		this.existing = existing;
		this.onSave = onSave;
		this.onDelete = onDelete;

		publicIpField = createField();
		pvlanIpField = createField();
		shortNameField = createField();
		environmentCombo = createEnvironmentCombo();
		hostingProviderCombo = createHostingProviderCombo();
		typeList = createTypeList();
		hostnameField = createField();
		notesField = createNotesArea();
		okButton = createOkButton();

		existing.ifPresent(this::populateFields);

		String title = existing.isPresent() ? TITLE_EDIT : TITLE_ADD;
		dialog = new JDialog(owner, title, true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		dialog.setContentPane(buildContentPanel());
		dialog.pack();
		dialog.setLocationRelativeTo(owner);

		wireValidation();
		updateOkState();

		dialog.setVisible(true);
	}

	/**
	 * Populates all form controls from the given server's current values.
	 *
	 * @param server the server whose values should be loaded into the form;
	 *  must not be {@code null}
	 */
	private void populateFields(Server server) {
		publicIpField.setText(server.publicIp());
		pvlanIpField.setText(server.pvlanIp());
		shortNameField.setText(server.shortName());
		server.environment().ifPresentOrElse(
			environmentCombo::setSelectedItem,
			() -> environmentCombo.setSelectedIndex(0)
		);
		server.hostingProvider().ifPresentOrElse(
			hostingProviderCombo::setSelectedItem,
			() -> hostingProviderCombo.setSelectedIndex(0)
		);
		selectTypes(server.type());
		hostnameField.setText(server.hostname());
		notesField.setText(server.notes());
	}

	/**
	 * Selects the given set of server types in the type list, clearing any
	 * prior selection first.
	 *
	 * @param types the set of types to select; must not be {@code null}
	 */
	private void selectTypes(EnumSet<ServerType> types) {
		typeList.clearSelection();
		ServerType[] allTypes = ServerType.values();
		for (int i = 0; i < allTypes.length; i++) {
			if (types.contains(allTypes[i])) {
				typeList.addSelectionInterval(i, i);
			}
		}
	}

	/**
	 * Creates a themed text field with the standard column width.
	 *
	 * @return a new {@link JTextField} styled for the dark theme; never {@code null}
	 */
	private static JTextField createField() {
		JTextField field = new JTextField(FIELD_COLUMNS);
		field.setBackground(StandardColor.BACKGROUND_PRIMARY.color);
		field.setForeground(StandardColor.TEXT_PRIMARY.color);
		field.setCaretColor(StandardColor.TEXT_PRIMARY.color);
		field.setSelectionColor(StandardColor.BACKGROUND_SELECTED.color);
		field.setSelectedTextColor(StandardColor.FOREGROUND_SELECTED.color);
		field.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(StandardColor.BORDER.color),
			BorderFactory.createEmptyBorder(2, 4, 2, 4)
		));
		return field;
	}

	/**
	 * Creates a themed multi-line text area for the Notes field, sized for
	 * roughly four to five visible rows and wrapped on word boundaries.
	 */
	private static JTextArea createNotesArea() {
		JTextArea area = new JTextArea(NOTES_VISIBLE_ROWS, FIELD_COLUMNS);
		area.setBackground(StandardColor.BACKGROUND_PRIMARY.color);
		area.setForeground(StandardColor.TEXT_PRIMARY.color);
		area.setCaretColor(StandardColor.TEXT_PRIMARY.color);
		area.setSelectionColor(StandardColor.BACKGROUND_SELECTED.color);
		area.setSelectedTextColor(StandardColor.FOREGROUND_SELECTED.color);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		return area;
	}

	/**
	 * Creates a themed combo box containing a blank sentinel followed by all
	 * {@link ServerEnvironment} values.
	 *
	 * @return the configured combo box; never {@code null}
	 */
	private static JComboBox<ServerEnvironment> createEnvironmentCombo() {
		JComboBox<ServerEnvironment> combo = new JComboBox<>();
		combo.addItem(null);
		for (ServerEnvironment env : ServerEnvironment.values()) {
			combo.addItem(env);
		}
		combo.setBackground(StandardColor.BACKGROUND_PRIMARY.color);
		combo.setForeground(StandardColor.TEXT_PRIMARY.color);
		applyEnumRenderer(combo);
		return combo;
	}

	/**
	 * Creates a themed combo box containing a blank sentinel followed by all
	 * {@link HostingProvider} values.
	 *
	 * @return the configured combo box; never {@code null}
	 */
	private static JComboBox<HostingProvider> createHostingProviderCombo() {
		JComboBox<HostingProvider> combo = new JComboBox<>();
		combo.addItem(null);
		for (HostingProvider provider : HostingProvider.values()) {
			combo.addItem(provider);
		}
		combo.setBackground(StandardColor.BACKGROUND_PRIMARY.color);
		combo.setForeground(StandardColor.TEXT_PRIMARY.color);
		applyEnumRenderer(combo);
		return combo;
	}

	/**
	 * Installs a cell renderer on a combo box that shows each enum value using
	 * its {@code displayName()} method via reflection, and shows a blank string
	 * for the {@code null} sentinel.
	 *
	 * <p>Uses an unchecked cast internally; this is safe because the renderer
	 * only formats the value and never modifies it.
	 *
	 * @param combo the combo box to configure; must not be {@code null}
	 */
	@SuppressWarnings("unchecked")
	private static void applyEnumRenderer(JComboBox<?> combo) {
		combo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(
				JList<?> list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus
			) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value == null) {
					setText("");
				} else {
					try {
						setText((String) value.getClass().getMethod("displayName").invoke(value));
					} catch (ReflectiveOperationException ex) {
						setText(value.toString());
					}
				}
				setBackground(isSelected
					? StandardColor.BACKGROUND_SELECTED.color
					: StandardColor.BACKGROUND_PRIMARY.color
				);
				setForeground(isSelected
					? StandardColor.FOREGROUND_SELECTED.color
					: StandardColor.TEXT_PRIMARY.color
				);
				return this;
			}
		});
	}

	/**
	 * Creates a themed multi-select {@link JList} populated with all
	 * {@link ServerType} values.
	 *
	 * @return the configured list; never {@code null}
	 */
	private static JList<ServerType> createTypeList() {
		JList<ServerType> list = new JList<>(ServerType.values());
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setVisibleRowCount(TYPE_LIST_VISIBLE_ROWS);
		list.setBackground(StandardColor.BACKGROUND_PRIMARY.color);
		list.setForeground(StandardColor.TEXT_PRIMARY.color);
		list.setSelectionBackground(StandardColor.BACKGROUND_SELECTED.color);
		list.setSelectionForeground(StandardColor.FOREGROUND_SELECTED.color);
		list.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(
				JList<?> l,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus
			) {
				super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
				if (value instanceof ServerType st) {
					setText(st.displayName());
				}
				setBackground(isSelected
					? StandardColor.BACKGROUND_SELECTED.color
					: StandardColor.BACKGROUND_PRIMARY.color
				);
				setForeground(isSelected
					? StandardColor.FOREGROUND_SELECTED.color
					: StandardColor.TEXT_PRIMARY.color
				);
				return this;
			}
		});
		return list;
	}

	/**
	 * Creates the OK button, initially disabled, styled with the accent color.
	 *
	 * @return the configured OK button; never {@code null}
	 */
	private JButton createOkButton() {
		PrimaryButton btn = new PrimaryButton("OK");
		btn.setEnabled(false);
		btn.addActionListener(e -> handleOk());
		return btn;
	}

	/**
	 * Builds the main content panel containing the form grid and the button row.
	 *
	 * @return the assembled content panel; never {@code null}
	 */
	private JPanel buildContentPanel() {
		JPanel content = new JPanel(new BorderLayout(0, OUTER_PADDING));
		content.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		content.setBorder(BorderFactory.createEmptyBorder(
			OUTER_PADDING, OUTER_PADDING, OUTER_PADDING, OUTER_PADDING
		));
		content.add(buildFormPanel(), BorderLayout.CENTER);
		content.add(buildButtonRow(), BorderLayout.SOUTH);
		return content;
	}

	/**
	 * Builds the form grid with label-control pairs for all server fields.
	 *
	 * <p>Row order: Public IP, PVLAN IP, Short Name, Environment, Hosting Provider,
	 * Type, Hostname, Notes.
	 *
	 * @return the form panel; never {@code null}
	 */
	private JPanel buildFormPanel() {
		JPanel form = new JPanel(new GridBagLayout());
		form.setBackground(StandardColor.BACKGROUND_ELEVATED.color);

		JScrollPane typeScroll = new JScrollPane(typeList);
		typeScroll.setBackground(StandardColor.BACKGROUND_PRIMARY.color);
		typeScroll.getViewport().setBackground(StandardColor.BACKGROUND_PRIMARY.color);
		typeScroll.setBorder(BorderFactory.createLineBorder(StandardColor.BORDER.color));

		JScrollPane notesScroll = new JScrollPane(notesField);
		notesScroll.setBackground(StandardColor.BACKGROUND_PRIMARY.color);
		notesScroll.getViewport().setBackground(StandardColor.BACKGROUND_PRIMARY.color);
		notesScroll.setBorder(BorderFactory.createLineBorder(StandardColor.BORDER.color));

		addFormRow(form, "Public IP", publicIpField, 0);
		addFormRow(form, "PVLAN IP", pvlanIpField, 1);
		addFormRow(form, "Short Name *", shortNameField, 2);
		addFormRow(form, "Environment", environmentCombo, 3);
		addFormRow(form, "Hosting Provider", hostingProviderCombo, 4);
		addFormRow(form, "Type *", typeScroll, 5);
		addFormRow(form, "Hostname", hostnameField, 6);
		addFormRow(form, "Notes", notesScroll, 7);

		return form;
	}

	/**
	 * Adds a label-component row to the given form panel at the specified grid row.
	 *
	 * <p>Accepts any {@link Component} so that combo boxes, scroll panes, and text
	 * fields can all be placed uniformly.
	 *
	 * @param form the form panel; must not be {@code null}
	 * @param labelText the text for the label; must not be {@code null}
	 * @param component the form control for the row; must not be {@code null}
	 * @param row the zero-based grid row index
	 */
	private static void addFormRow(JPanel form, String labelText, Component component, int row) {
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = row;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.insets = new Insets(ROW_PADDING, 0, ROW_PADDING, 8);

		JLabel label = new JLabel(labelText);
		label.setForeground(StandardColor.TEXT_PRIMARY.color);
		form.add(label, labelConstraints);

		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridx = 1;
		fieldConstraints.gridy = row;
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.weightx = 1.0;
		fieldConstraints.insets = new Insets(ROW_PADDING, 0, ROW_PADDING, 0);
		form.add(component, fieldConstraints);
	}

	/**
	 * Builds the button row.
	 *
	 * <p>In add mode: Cancel on the left (right-aligned panel), OK on the right.
	 * In edit mode: Delete on the far left, then Cancel and OK on the right, so
	 * the destructive action is visually separated from the safe ones.
	 *
	 * @return the button panel; never {@code null}
	 */
	private JPanel buildButtonRow() {
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(StandardColor.BACKGROUND_ELEVATED.color);

		if (existing.isPresent()) {
			SecondaryButton deleteButton = new SecondaryButton("Delete");
			deleteButton.addActionListener(e -> handleDelete());

			JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			leftPanel.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
			leftPanel.add(deleteButton);
			row.add(leftPanel, BorderLayout.WEST);
		}

		JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		rightPanel.setBackground(StandardColor.BACKGROUND_ELEVATED.color);

		SecondaryButton cancelButton = new SecondaryButton("Cancel");
		cancelButton.addActionListener(e -> dialog.dispose());

		rightPanel.add(cancelButton);
		rightPanel.add(okButton);
		row.add(rightPanel, BorderLayout.EAST);

		return row;
	}

	/**
	 * Attaches listeners to all controls that affect form validity so the OK
	 * button state is recomputed on every relevant change.
	 */
	private void wireValidation() {
		shortNameField.getDocument().addDocumentListener(
			new javax.swing.event.DocumentListener() {
				@Override
				public void insertUpdate(javax.swing.event.DocumentEvent e) { updateOkState(); }
				@Override
				public void removeUpdate(javax.swing.event.DocumentEvent e) { updateOkState(); }
				@Override
				public void changedUpdate(javax.swing.event.DocumentEvent e) { updateOkState(); }
			}
		);
		typeList.addListSelectionListener(e -> updateOkState());
	}

	/**
	 * Returns {@code true} when the required form controls have valid selections:
	 * {@code shortName} is non-blank and at least one {@link ServerType} is
	 * selected in {@code typeList}.
	 *
	 * @return {@code true} if the form may be saved; {@code false} otherwise
	 */
	private boolean isFormValid() {
		if (shortNameField.getText().isBlank()) {
			return false;
		}
		if (typeList.getSelectedValuesList().isEmpty()) {
			return false;
		}
		return true;
	}

	/**
	 * Enables or disables the OK button based on {@link #isFormValid()}.
	 */
	private void updateOkState() {
		okButton.setEnabled(isFormValid());
	}

	/**
	 * Validates IP address and hostname fields before constructing a {@link Server}.
	 * Shows a {@link JOptionPane} error dialog and returns without saving if any
	 * field fails validation. IP fields are checked against a standard IPv4/IPv6
	 * pattern; blank values pass. Hostname is checked to have no {@code "://"} and
	 * no {@code "/"} character; blank values pass.
	 */
	private void handleOk() {
		String publicIp = publicIpField.getText().trim();
		String pvlanIp = pvlanIpField.getText().trim();
		String hostname = hostnameField.getText().trim();

		if (!isValidIp(publicIp) || !isValidIp(pvlanIp)) {
			JOptionPane.showMessageDialog(
				dialog,
				IP_VALIDATION_MESSAGE,
				VALIDATION_ERROR_TITLE,
				JOptionPane.ERROR_MESSAGE
			);
			return;
		}
		if (!isValidHostname(hostname)) {
			JOptionPane.showMessageDialog(
				dialog,
				HOSTNAME_VALIDATION_MESSAGE,
				VALIDATION_ERROR_TITLE,
				JOptionPane.ERROR_MESSAGE
			);
			return;
		}

		Optional<ServerEnvironment> environment = Optional.ofNullable(
			(ServerEnvironment) environmentCombo.getSelectedItem()
		);
		Optional<HostingProvider> hostingProvider = Optional.ofNullable(
			(HostingProvider) hostingProviderCombo.getSelectedItem()
		);
		List<ServerType> selectedTypes = typeList.getSelectedValuesList();
		EnumSet<ServerType> type = EnumSet.copyOf(selectedTypes);

		Server server = new Server(
			publicIp,
			pvlanIp,
			shortNameField.getText().trim(),
			hostname,
			notesField.getText().trim(),
			environment,
			type,
			hostingProvider
		);
		onSave.accept(server);
		dialog.dispose();
	}

	/**
	 * Shows a confirmation prompt. If the user confirms, invokes the
	 * {@link #onDelete} callback with the existing server and disposes the dialog.
	 * If the user cancels, returns to the edit dialog with no changes.
	 */
	private void handleDelete() {
		int choice = JOptionPane.showConfirmDialog(
			dialog,
			DELETE_CONFIRM_MESSAGE,
			DELETE_CONFIRM_TITLE,
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE
		);
		if (choice == JOptionPane.OK_OPTION) {
			existing.ifPresent(onDelete);
			dialog.dispose();
		}
	}

	/**
	 * Returns {@code true} if {@code value} is blank or matches a valid IPv4 or
	 * IPv6 address.
	 *
	 * @param value the trimmed field value; must not be {@code null}
	 * @return {@code true} for blank or valid IP; {@code false} otherwise
	 */
	private static boolean isValidIp(String value) {
		if (value.isBlank()) {
			return true;
		}
		return IP_PATTERN.matcher(value).matches();
	}

	/**
	 * Returns {@code true} if {@code value} is blank or is a bare hostname with
	 * no protocol prefix and no path segment.
	 *
	 * @param value the trimmed field value; must not be {@code null}
	 * @return {@code true} for blank or valid hostname; {@code false} otherwise
	 */
	private static boolean isValidHostname(String value) {
		if (value.isBlank()) {
			return true;
		}
		if (value.contains("://")) {
			return false;
		}
		if (value.contains("/")) {
			return false;
		}
		return true;
	}
}
