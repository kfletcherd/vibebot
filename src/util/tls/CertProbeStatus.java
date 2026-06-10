package util.tls;

/**
 * Outcome category for a single {@link CertProbe} attempt against one host.
 */
public enum CertProbeStatus {

	/** The handshake succeeded and a leaf certificate was read. */
	OK("OK"),

	/** The connect or handshake exceeded the probe's time budget. */
	TIMED_OUT("Timed out"),

	/** The remote host actively refused the TCP connection. */
	CONNECTION_REFUSED("Connection refused"),

	/** DNS could not resolve the host name. */
	UNKNOWN_HOST("Unknown host"),

	/** The TLS handshake failed (bad cert, protocol mismatch, etc.). */
	HANDSHAKE_FAILED("Handshake failed"),

	/** The handshake succeeded but the peer returned no usable certificate. */
	NO_CERTIFICATE("No certificate"),

	/** Any other failure not covered by the more specific values. */
	OTHER_ERROR("Error");

	private final String displayLabel;

	CertProbeStatus(String displayLabel) {
		this.displayLabel = displayLabel;
	}

	/**
	 * Returns a human-friendly label suitable for display in a report row.
	 */
	public String displayLabel() {
		return displayLabel;
	}
}
