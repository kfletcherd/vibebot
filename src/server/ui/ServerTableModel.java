package server.ui;

import server.model.Server;
import server.model.ServerColumn;
import server.model.ServerType;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Swing {@link AbstractTableModel} backed by a live list of {@link Server} objects.
 *
 * Displays eight data columns derived from {@link ServerColumn} in
 * presentation order — Short Name, Hostname, Public IP, PVLAN IP, Environment,
 * Hosting Provider, Type, Notes — followed by a trailing action column that
 * hosts an Edit button installed by {@link util.ui.table.ButtonColumn}.
 *
 * Note that the presentation order differs from the CSV storage order defined
 * in {@link ServerColumn}; the enum's ordinal-based CSV order is unaffected by
 * this presentation array.
 *
 * <p>The trailing button column at index {@code PRESENTATION_ORDER.length} returns
 * an empty string from {@link #getValueAt}, an empty string from
 * {@link #getColumnName}, and {@code true} from {@link #isCellEditable} so that
 * {@link util.ui.table.ButtonColumn} can take over its rendering and editing.
 * All other cells remain read-only.
 *
 * <p>New rows are added via {@link #addServer(Server)}, existing rows are updated
 * via {@link #updateServer(Server, Server)}, and rows are removed via
 * {@link #deleteServer(Server)}.
 */
final class ServerTableModel extends AbstractTableModel {

	/**
	 * Presentation order: the sequence of {@link ServerColumn} values that
	 * determines which column appears at each visual position in the table.
	 * Edit this array to reorder, add, or remove displayed columns.
	 */
	private static final ServerColumn[] PRESENTATION_ORDER = {
		ServerColumn.SHORT_NAME,
		ServerColumn.HOSTNAME,
		ServerColumn.PUBLIC_IP,
		ServerColumn.PVLAN_IP,
		ServerColumn.ENVIRONMENT,
		ServerColumn.HOSTING_PROVIDER,
		ServerColumn.TYPE,
		ServerColumn.NOTES
	};

	/** The mutable backing list of servers displayed in the table. */
	private final List<Server> servers;

	/**
	 * Constructs a {@code ServerTableModel} pre-populated with the given servers.
	 *
	 * <p>A defensive copy of the supplied list is made so that subsequent external
	 * modifications to the original list do not affect the table.
	 *
	 * @param servers the initial list of servers to display; must not be
	 *  {@code null}; may be empty
	 */
	ServerTableModel(List<Server> servers) {
		this.servers = new ArrayList<>(servers);
	}

	/**
	 * Returns the number of rows currently in the model.
	 *
	 * @return the row count; zero or positive
	 */
	@Override
	public int getRowCount() {
		return servers.size();
	}

	/** The index of the trailing action column that hosts the Edit button. */
	private static final int BUTTON_COLUMN_INDEX = PRESENTATION_ORDER.length;

	/**
	 * Returns the total number of columns: all data columns plus the trailing
	 * Edit button column.
	 *
	 * @return {@code PRESENTATION_ORDER.length + 1}
	 */
	@Override
	public int getColumnCount() {
		return PRESENTATION_ORDER.length + 1;
	}

	/**
	 * Returns the display name for the given column.
	 *
	 * <p>The trailing button column returns an empty string; all other columns
	 * return their {@link ServerColumn#displayName()}.
	 *
	 * @param column the zero-based column index
	 * @return the column header string; never {@code null}
	 */
	@Override
	public String getColumnName(int column) {
		if (column == BUTTON_COLUMN_INDEX) {
			return "";
		}
		return PRESENTATION_ORDER[column].displayName();
	}

	/**
	 * Returns the value at the specified cell.
	 *
	 * <p>All data values are returned as {@link String}s. The trailing button
	 * column always returns an empty string.
	 *
	 * @param rowIndex the zero-based row index
	 * @param columnIndex the zero-based column index
	 * @return the cell value; never {@code null}
	 */
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == BUTTON_COLUMN_INDEX) {
			return "";
		}
		Server server = servers.get(rowIndex);
		return switch (PRESENTATION_ORDER[columnIndex]) {
			case PUBLIC_IP -> server.publicIp();
			case PVLAN_IP -> server.pvlanIp();
			case SHORT_NAME -> server.shortName();
			case ENVIRONMENT -> server.environment()
				.map(e -> e.displayName())
				.orElse("");
			case HOSTING_PROVIDER -> server.hostingProvider()
				.map(hp -> hp.displayName())
				.orElse("");
			case TYPE -> server.type().stream()
				.map(ServerType::displayName)
				.collect(Collectors.joining(", "));
			case HOSTNAME -> server.hostname();
			case NOTES -> server.notes();
		};
	}

	/**
	 * Returns {@code true} only for cells in the trailing button column so that
	 * {@link util.ui.table.ButtonColumn} can activate its editor on click.
	 * All data cells remain read-only.
	 *
	 * @param rowIndex the row index
	 * @param columnIndex the column index
	 * @return {@code true} if {@code columnIndex} is the button column; otherwise
	 *  {@code false}
	 */
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == BUTTON_COLUMN_INDEX;
	}

	/**
	 * Appends a new server to the backing list and notifies listeners that a row
	 * was inserted.
	 *
	 * <p>Must be called on the Event Dispatch Thread when the table is displayed.
	 *
	 * @param server the server to add; must not be {@code null}
	 */
	void addServer(Server server) {
		int insertedRow = servers.size();
		servers.add(server);
		fireTableRowsInserted(insertedRow, insertedRow);
	}

	/**
	 * Replaces the row occupied by {@code old} with {@code updated} in the
	 * backing list and fires the appropriate table event.
	 *
	 * <p>Must be called on the Event Dispatch Thread.
	 *
	 * @param old the server to replace; must not be {@code null}
	 * @param updated the replacement server; must not be {@code null}
	 */
	void updateServer(Server old, Server updated) {
		int index = servers.indexOf(old);
		if (index < 0) {
			return;
		}
		servers.set(index, updated);
		fireTableRowsUpdated(index, index);
	}

	/**
	 * Removes {@code server} from the backing list and fires the appropriate
	 * table event.
	 *
	 * <p>Must be called on the Event Dispatch Thread.
	 *
	 * @param server the server to remove; must not be {@code null}
	 */
	void deleteServer(Server server) {
		int index = servers.indexOf(server);
		if (index < 0) {
			return;
		}
		servers.remove(index);
		fireTableRowsDeleted(index, index);
	}

	/**
	 * Returns the {@link Server} at the given row index.
	 *
	 * @param row the zero-based row index
	 * @return the server at that row; never {@code null}
	 * @throws IndexOutOfBoundsException if {@code row} is out of range
	 */
	Server getServerAt(int row) {
		return servers.get(row);
	}
}
