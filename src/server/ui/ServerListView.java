package server.ui;

import server.model.Server;
import server.model.ServerStore;
import server.model.ServerStoreException;
import server.ui.sslcheck.SslCheckAction;
import util.ui.button.PrimaryButton;
import util.ui.button.SecondaryButton;
import util.ui.pane.PaneView;
import util.ui.table.ButtonColumn;
import util.ui.table.CellTooltipRenderer;
import util.ui.theme.StandardColor;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.Optional;

/**
 * {@link PaneView} that displays the server list table and hosts the
 * "Add Server" toolbar action.
 *
 * <p>If the {@link ServerStore} was loaded successfully, this view renders:
 * <ul>
 *   <li>NORTH — a thin toolbar with an "Add Server" button (accent-colored) aligned
 *       to the right.</li>
 *   <li>CENTER — a {@link JScrollPane} wrapping a {@link JTable} backed by
 *       {@link ServerTableModel}, styled with {@link StandardColor} values. Each row
 *       has an Edit button in the trailing column; clicking it opens
 *       {@link AddServerDialog} in edit mode.</li>
 * </ul>
 *
 * <p>If the store could not be loaded (the {@code loadError} parameter is
 * non-empty), the entire content area is replaced by a plain error panel
 * that displays the failure message and the path to the offending file.
 * No table, toolbar, or button is shown in this state.
 *
 * <p>The same {@link JComponent} is returned by {@link #component()} on every
 * call; it is allocated once during construction.
 */
public final class ServerListView implements PaneView {

	/** The view identifier used to register this view in the {@code CardLayout}. */
	private static final String VIEW_ID = "servers";

	/** Padding around the error message text in pixels. */
	private static final int ERROR_PADDING = 20;

	/** The outer panel that backs this view and is returned by {@link #component()}. */
	private final JPanel rootPanel;

	/** The owner frame used to parent any dialogs opened from this view. */
	private final JFrame ownerFrame;

	/** The store, present only when the load succeeded. */
	private final ServerStore store;

	/** The table model, present only when the load succeeded. */
	private final ServerTableModel tableModel;

	/**
	 * Constructs a {@code ServerListView}.
	 *
	 * <p>If {@code loadError} is non-empty, the view renders an error panel and
	 * ignores {@code store}. If {@code loadError} is empty, the view renders the
	 * full table and toolbar using {@code store}.
	 *
	 * @param ownerFrame the parent frame for dialogs opened from this view; must
	 *  not be {@code null}
	 * @param store the loaded server store; may be {@code null} when
	 *  {@code loadError} is present
	 * @param loadError an {@link Optional} wrapping a {@link ServerStoreException}
	 *  if loading failed, or empty if loading succeeded; must not be {@code null}
	 */
	public ServerListView(
		JFrame ownerFrame,
		ServerStore store,
		Optional<ServerStoreException> loadError
	) {
		this.ownerFrame = ownerFrame;
		this.store = store;
		rootPanel = new JPanel(new BorderLayout());
		rootPanel.setBackground(StandardColor.BACKGROUND_ELEVATED.color);

		if (loadError.isPresent()) {
			tableModel = null;
			rootPanel.add(buildErrorPanel(loadError.get()), BorderLayout.CENTER);
		} else {
			tableModel = new ServerTableModel(store.servers());
			rootPanel.add(buildToolbar(), BorderLayout.NORTH);
			rootPanel.add(buildTableScrollPane(), BorderLayout.CENTER);
		}
	}

	/**
	 * Returns the unique identifier for this view.
	 *
	 * @return {@code "servers"}; never {@code null}
	 */
	@Override
	public String id() {
		return VIEW_ID;
	}

	/**
	 * Returns the root panel component for this view.
	 *
	 * <p>The same instance is returned on every call.
	 *
	 * @return the root {@link JPanel}; never {@code null}; same instance on every call
	 */
	@Override
	public JComponent component() {
		return rootPanel;
	}

	/**
	 * Builds the toolbar panel containing the "Check SSL Certs" and "Add Server"
	 * buttons.
	 *
	 * @return a {@link JPanel} with the buttons right-aligned; never {@code null}
	 */
	private JPanel buildToolbar() {
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
		toolbar.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, StandardColor.BORDER.color));

		SecondaryButton checkSslButton = new SecondaryButton("Check SSL Certs");
		new SslCheckAction(ownerFrame, store).attachTo(checkSslButton);

		PrimaryButton addButton = new PrimaryButton("Add Server");
		addButton.addActionListener(e -> openAddServerDialog());

		toolbar.add(checkSslButton);
		toolbar.add(addButton);
		return toolbar;
	}

	/**
	 * Builds the scroll pane wrapping the server table, applying theme colors
	 * to the table header, cells, and viewport, and installing a
	 * {@link ButtonColumn} on the trailing Edit column.
	 *
	 * @return a fully themed {@link JScrollPane}; never {@code null}
	 */
	private JScrollPane buildTableScrollPane() {
		JTable table = new JTable(tableModel);
		table.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		table.setForeground(StandardColor.TEXT_PRIMARY.color);
		table.setGridColor(StandardColor.BORDER.color);
		table.setSelectionBackground(StandardColor.BACKGROUND_SELECTED.color);
		table.setSelectionForeground(StandardColor.FOREGROUND_SELECTED.color);
		table.setRowHeight(24);
		table.setShowGrid(true);
		table.setDefaultRenderer(Object.class, new CellTooltipRenderer());

		table.getTableHeader().setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		table.getTableHeader().setForeground(StandardColor.TEXT_PRIMARY.color);
		table.getTableHeader().setBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, StandardColor.BORDER.color)
		);

		int editColumnIndex = tableModel.getColumnCount() - 1;
		new ButtonColumn(table, editColumnIndex, "Edit", this::openEditServerDialog);

		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		scrollPane.getViewport().setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		return scrollPane;
	}

	/**
	 * Builds the error panel shown when the CSV store could not be loaded.
	 *
	 * <p>Displays a multi-line message telling the user the file is unreadable and
	 * showing the full file path. No table or toolbar is included.
	 *
	 * @param error the exception describing the load failure; must not be
	 *  {@code null}
	 * @return the error panel; never {@code null}
	 */
	private static JPanel buildErrorPanel(ServerStoreException error) {
		String message = "The server list could not be loaded because the CSV file is corrupt or unreadable.\n"
			+ "Please fix or delete the file and restart the application.\n"
			+ "File: " + error.filePath().toAbsolutePath();

		JTextArea textArea = new JTextArea(message);
		textArea.setEditable(false);
		textArea.setOpaque(false);
		textArea.setForeground(StandardColor.TEXT_PRIMARY.color);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setBorder(null);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
		panel.setBorder(BorderFactory.createEmptyBorder(
			ERROR_PADDING, ERROR_PADDING, ERROR_PADDING, ERROR_PADDING
		));
		panel.add(textArea, BorderLayout.NORTH);
		return panel;
	}

	/**
	 * Opens {@link AddServerDialog} in add mode, wiring the save callback to
	 * persist the new server and refresh the table.
	 */
	private void openAddServerDialog() {
		new AddServerDialog(ownerFrame, this::handleServerAdded);
	}

	/**
	 * Opens {@link AddServerDialog} in edit mode for the server at the given
	 * row index. The save callback updates the existing entry in the store and
	 * the table model; the delete callback removes it from both.
	 *
	 * @param row the zero-based table row index of the server to edit
	 */
	private void openEditServerDialog(int row) {
		Server old = tableModel.getServerAt(row);
		new AddServerDialog(
			ownerFrame,
			Optional.of(old),
			updated -> handleServerUpdated(old, updated),
			this::handleServerDeleted
		);
	}

	/**
	 * Called when the user confirms adding a server from {@link AddServerDialog}.
	 *
	 * <p>Persists the server via the store and updates the table model. Any I/O
	 * error during persistence is printed to stderr; the server is still added to
	 * the table model so the session remains usable.
	 *
	 * @param server the newly created server; must not be {@code null}
	 */
	private void handleServerAdded(Server server) {
		try {
			store.add(server);
		} catch (IOException e) {
			System.err.println("Failed to persist server to CSV: " + e.getMessage());
		}
		SwingUtilities.invokeLater(() -> tableModel.addServer(server));
	}

	/**
	 * Called when the user saves edits from the edit-mode {@link AddServerDialog}.
	 *
	 * <p>Replaces {@code old} with {@code updated} in the store and the table
	 * model. Any I/O error is printed to stderr; the table model is still updated
	 * so the session remains usable.
	 *
	 * @param old the server before editing; must not be {@code null}
	 * @param updated the server after editing; must not be {@code null}
	 */
	private void handleServerUpdated(Server old, Server updated) {
		try {
			store.update(old, updated);
		} catch (IOException e) {
			System.err.println("Failed to persist server update to CSV: " + e.getMessage());
		}
		SwingUtilities.invokeLater(() -> tableModel.updateServer(old, updated));
	}

	/**
	 * Called when the user confirms deletion from the edit-mode
	 * {@link AddServerDialog}.
	 *
	 * <p>Removes the server from the store and the table model. Any I/O error is
	 * printed to stderr; the table model is still updated so the session remains
	 * usable.
	 *
	 * @param server the server to remove; must not be {@code null}
	 */
	private void handleServerDeleted(Server server) {
		try {
			store.delete(server);
		} catch (IOException e) {
			System.err.println("Failed to persist server deletion to CSV: " + e.getMessage());
		}
		SwingUtilities.invokeLater(() -> tableModel.deleteServer(server));
	}
}
