package server.model;

/**
 * Canonical definition of every column in the server data model.
 *
 * <p>Each constant represents one column, carrying:
 * <ul>
 *   <li>Its zero-based index in the CSV storage layout ({@link #csvIndex()}).</li>
 *   <li>The exact field name written to the CSV header row ({@link #csvHeader()}).</li>
 *   <li>The human-readable label shown in the table UI ({@link #displayName()}).</li>
 * </ul>
 *
 * <p>This enum is the single source of truth for column identity. Both
 * {@link ServerCsvMapper} and {@link server.ui.ServerTableModel} derive their
 * column information from here. Any change to the CSV schema (column names,
 * order, or count) must be made by editing this enum only.
 *
 * <p>The CSV column order is defined by the ordinal of each constant. The table
 * presentation order is defined separately in
 * {@link server.ui.ServerTableModel} so the two can differ.
 */
public enum ServerColumn {

	/** Public-facing IP address of the server. */
	PUBLIC_IP(0, "public_ip", "Public IP"),

	/** PVLAN (private VLAN) IP address. */
	PVLAN_IP(1, "pvlan_ip", "PVLAN IP"),

	/** Brief human-readable identifier (e.g., {@code "web01"}). */
	SHORT_NAME(2, "short_name", "Short Name"),

	/** Fully-qualified domain name; may be empty. */
	HOSTNAME(3, "hostname", "Hostname"),

	/** Free-form notes about the server; may be empty. */
	NOTES(4, "notes", "Notes"),

	/** Deployment environment (e.g., {@code "production"}, {@code "staging"}). */
	ENVIRONMENT(5, "environment", "Environment"),

	/** Functional type of the server; stored as a semicolon-delimited list when multiple. */
	TYPE(6, "type", "Type"),

	/** Hosting provider for the server; may be absent. */
	HOSTING_PROVIDER(7, "hosting_provider", "Hosting Provider");

	/** Total number of columns; matches the number of enum constants. */
	public static final int COUNT = values().length;

	/** Zero-based index of this column in the CSV storage layout. */
	private final int csvIndex;

	/** Exact field name written in the CSV header row. */
	private final String csvHeader;

	/** Human-readable label displayed in the UI table header. */
	private final String displayName;

	/**
	 * Constructs a {@code ServerColumn} with the given CSV index, CSV header, and display name.
	 *
	 * @param csvIndex the zero-based column position in the CSV file
	 * @param csvHeader the exact header string used in the CSV file
	 * @param displayName the human-readable label shown in the table UI
	 */
	ServerColumn(int csvIndex, String csvHeader, String displayName) {
		this.csvIndex = csvIndex;
		this.csvHeader = csvHeader;
		this.displayName = displayName;
	}

	/**
	 * Returns the zero-based index of this column in the CSV storage layout.
	 *
	 * @return the CSV column index; zero or positive
	 */
	public int csvIndex() {
		return csvIndex;
	}

	/**
	 * Returns the exact field name used in the CSV header row.
	 *
	 * @return the CSV header string; never {@code null}
	 */
	public String csvHeader() {
		return csvHeader;
	}

	/**
	 * Returns the human-readable label shown in the UI table header.
	 *
	 * @return the display name; never {@code null}
	 */
	public String displayName() {
		return displayName;
	}
}
