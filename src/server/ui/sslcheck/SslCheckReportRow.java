package server.ui.sslcheck;

import util.tls.CertProbeResult;
import util.tls.CertProbeStatus;

import java.time.Duration;
import java.time.Instant;

/**
 * Per-row presentation data for the SSL check report.
 *
 * @param shortName the server's short name as shown in the report
 * @param hostname the hostname that was probed
 * @param result the underlying probe outcome
 * @param expiry a derived classification driving row coloring
 */
record SslCheckReportRow(
	String shortName,
	String hostname,
	CertProbeResult result,
	ExpiryClassification expiry
) {

	/** Window inside which a still-valid certificate is treated as expiring soon. */
	static final Duration EXPIRY_WARNING_WINDOW = Duration.ofDays(30);

	SslCheckReportRow {
		if (shortName == null) throw new IllegalArgumentException("shortName must not be null");
		if (hostname == null || hostname.isBlank()) {
			throw new IllegalArgumentException("hostname must be non-blank");
		}
		if (result == null) throw new IllegalArgumentException("result must not be null");
		if (expiry == null) throw new IllegalArgumentException("expiry must not be null");
	}

	/**
	 * Classifies a row by how its certificate stands relative to a reference instant.
	 */
	enum ExpiryClassification {
		OK,
		EXPIRING_SOON,
		EXPIRED,
		UNKNOWN
	}

	/**
	 * Builds a row from a probe result, deriving the expiry classification against
	 * {@code now} using the 30-day warning window.
	 */
	static SslCheckReportRow from(String shortName, CertProbeResult result, Instant now) {
		if (result == null) throw new IllegalArgumentException("result must not be null");
		if (now == null) throw new IllegalArgumentException("now must not be null");
		return new SslCheckReportRow(
			shortName,
			result.host(),
			result,
			classify(result, now)
		);
	}

	private static ExpiryClassification classify(CertProbeResult result, Instant now) {
		if (result.status() != CertProbeStatus.OK || result.notAfter().isEmpty()) {
			return ExpiryClassification.UNKNOWN;
		}
		Instant expires = result.notAfter().get();
		if (!expires.isAfter(now)) {
			return ExpiryClassification.EXPIRED;
		}
		Instant warningCutoff = now.plus(EXPIRY_WARNING_WINDOW);
		if (!expires.isAfter(warningCutoff)) {
			return ExpiryClassification.EXPIRING_SOON;
		}
		return ExpiryClassification.OK;
	}
}
