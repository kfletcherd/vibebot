package util.tls;

import java.time.Instant;
import java.util.Optional;

/**
 * Immutable outcome of a single {@link CertProbe} attempt against one host.
 *
 * @param host the host that was probed
 * @param status the categorized outcome
 * @param notBefore the certificate's {@code notBefore} instant when {@code status} is
 *  {@link CertProbeStatus#OK}, otherwise empty
 * @param notAfter the certificate's {@code notAfter} instant when {@code status} is
 *  {@link CertProbeStatus#OK}, otherwise empty
 * @param errorDetail a short description of the underlying failure when
 *  {@code status} is not {@link CertProbeStatus#OK}, otherwise empty
 */
public record CertProbeResult(
	String host,
	CertProbeStatus status,
	Optional<Instant> notBefore,
	Optional<Instant> notAfter,
	Optional<String> errorDetail
) {

	public CertProbeResult {
		if (host == null || host.isBlank()) {
			throw new IllegalArgumentException("host must be non-blank");
		}
		if (status == null) throw new IllegalArgumentException("status must not be null");
		if (notBefore == null) throw new IllegalArgumentException("notBefore must not be null");
		if (notAfter == null) throw new IllegalArgumentException("notAfter must not be null");
		if (errorDetail == null) throw new IllegalArgumentException("errorDetail must not be null");
	}

	/**
	 * Builds a successful result carrying the certificate's validity window.
	 */
	public static CertProbeResult success(String host, Instant notBefore, Instant notAfter) {
		return new CertProbeResult(
			host,
			CertProbeStatus.OK,
			Optional.of(notBefore),
			Optional.of(notAfter),
			Optional.empty()
		);
	}

	/**
	 * Builds a failure result for the given non-{@code OK} status.
	 *
	 * @throws IllegalArgumentException if {@code status} is {@link CertProbeStatus#OK}
	 */
	public static CertProbeResult failure(String host, CertProbeStatus status, String detail) {
		if (status == CertProbeStatus.OK) {
			throw new IllegalArgumentException("failure() requires a non-OK status");
		}
		return new CertProbeResult(
			host,
			status,
			Optional.empty(),
			Optional.empty(),
			Optional.ofNullable(detail).filter(s -> !s.isBlank())
		);
	}
}
