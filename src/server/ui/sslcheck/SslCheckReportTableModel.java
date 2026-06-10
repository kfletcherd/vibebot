package server.ui.sslcheck;

import util.tls.CertProbeResult;
import util.tls.CertProbeStatus;

import javax.swing.table.AbstractTableModel;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Table model presenting one row per probed host with short name, hostname,
 * status, and the certificate's issue/expiry dates.
 */
final class SslCheckReportTableModel extends AbstractTableModel {

	private static final String[] COLUMN_NAMES = {
		"Short Name",
		"Hostname",
		"Status",
		"Issued",
		"Expires"
	};

	private static final int COL_SHORT_NAME = 0;
	private static final int COL_HOSTNAME = 1;
	private static final int COL_STATUS = 2;
	private static final int COL_ISSUED = 3;
	private static final int COL_EXPIRES = 4;

	/** Placeholder shown in date columns when the probe did not return a date. */
	private static final String NO_DATE_PLACEHOLDER = "—";

	private static final DateTimeFormatter DATE_FORMAT =
		DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

	private final List<SslCheckReportRow> rows;

	SslCheckReportTableModel(List<SslCheckReportRow> rows) {
		if (rows == null) throw new IllegalArgumentException("rows must not be null");
		this.rows = new ArrayList<>(rows);
	}

	/**
	 * Returns the underlying row at {@code rowIndex} so renderers can read the
	 * row's expiry classification for coloring.
	 */
	SslCheckReportRow getRowAt(int rowIndex) {
		return rows.get(rowIndex);
	}

	@Override
	public int getRowCount() {
		return rows.size();
	}

	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return COLUMN_NAMES[columnIndex];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		SslCheckReportRow row = rows.get(rowIndex);
		CertProbeResult result = row.result();
		return switch (columnIndex) {
			case COL_SHORT_NAME -> row.shortName();
			case COL_HOSTNAME -> row.hostname();
			case COL_STATUS -> statusLabel(row);
			case COL_ISSUED -> formatDate(result.notBefore());
			case COL_EXPIRES -> formatDate(result.notAfter());
			default -> "";
		};
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	private static String statusLabel(SslCheckReportRow row) {
		CertProbeResult result = row.result();
		if (result.status() == CertProbeStatus.OK) {
			return switch (row.expiry()) {
				case EXPIRED -> "Expired";
				case EXPIRING_SOON -> "Expiring soon";
				case OK -> CertProbeStatus.OK.displayLabel();
				case UNKNOWN -> CertProbeStatus.OK.displayLabel();
			};
		}
		return result.status().displayLabel();
	}

	private static String formatDate(Optional<java.time.Instant> instant) {
		return instant.map(DATE_FORMAT::format).orElse(NO_DATE_PLACEHOLDER);
	}
}
