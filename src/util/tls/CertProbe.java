package util.tls;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;

/**
 * Synchronously fetches the leaf X.509 certificate from a single host on a
 * fixed port using JDK-built-in TLS.
 *
 * <p>One {@code CertProbe} instance is safe to reuse across many hosts on the
 * same thread. {@link #probe(String)} blocks for at most the configured
 * timeout (covering both TCP connect and the TLS handshake) and never throws —
 * every failure is mapped to a {@link CertProbeResult} carrying the
 * appropriate {@link CertProbeStatus}.
 *
 * <p>This class must not be called from the Swing Event Dispatch Thread.
 */
public final class CertProbe {

	/** The standard HTTPS port used when no port is supplied. */
	public static final int DEFAULT_PORT = 443;

	/** The default per-host time budget covering connect + handshake. */
	public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

	private final Duration timeout;
	private final int port;
	private final SSLSocketFactory socketFactory;

	public CertProbe() {
		this(DEFAULT_TIMEOUT, DEFAULT_PORT);
	}

	/**
	 * @param timeout the per-host time budget; must be positive
	 * @param port the TCP port to connect to; must be in {@code 1..65535}
	 */
	public CertProbe(Duration timeout, int port) {
		if (timeout == null || timeout.isZero() || timeout.isNegative()) {
			throw new IllegalArgumentException("timeout must be positive");
		}
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException("port must be in 1..65535");
		}
		this.timeout = timeout;
		this.port = port;
		this.socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
	}

	/**
	 * Probes {@code host} on the configured port and returns the outcome.
	 *
	 * @param host the host name to probe; must be non-blank
	 */
	public CertProbeResult probe(String host) {
		if (host == null || host.isBlank()) {
			throw new IllegalArgumentException("host must be non-blank");
		}

		int timeoutMillis = (int) Math.min(Integer.MAX_VALUE, timeout.toMillis());
		long startNanos = System.nanoTime();

		try (SSLSocket socket = (SSLSocket) socketFactory.createSocket()) {
			socket.connect(new InetSocketAddress(host, port), timeoutMillis);

			int elapsedMillis = (int) Math.min(
				Integer.MAX_VALUE,
				Duration.ofNanos(System.nanoTime() - startNanos).toMillis()
			);
			int remainingMillis = Math.max(1, timeoutMillis - elapsedMillis);
			socket.setSoTimeout(remainingMillis);

			socket.startHandshake();
			Certificate[] chain = socket.getSession().getPeerCertificates();
			if (chain == null || chain.length == 0 || !(chain[0] instanceof X509Certificate leaf)) {
				return CertProbeResult.failure(
					host,
					CertProbeStatus.NO_CERTIFICATE,
					"Peer returned no X.509 certificate"
				);
			}

			Instant notBefore = leaf.getNotBefore().toInstant();
			Instant notAfter = leaf.getNotAfter().toInstant();
			return CertProbeResult.success(host, notBefore, notAfter);
		} catch (SocketTimeoutException e) {
			return CertProbeResult.failure(host, CertProbeStatus.TIMED_OUT, e.getMessage());
		} catch (UnknownHostException e) {
			return CertProbeResult.failure(host, CertProbeStatus.UNKNOWN_HOST, e.getMessage());
		} catch (ConnectException e) {
			return CertProbeResult.failure(host, CertProbeStatus.CONNECTION_REFUSED, e.getMessage());
		} catch (SSLException e) {
			return CertProbeResult.failure(host, CertProbeStatus.HANDSHAKE_FAILED, e.getMessage());
		} catch (IOException e) {
			return CertProbeResult.failure(host, CertProbeStatus.OTHER_ERROR, e.getMessage());
		} catch (RuntimeException e) {
			return CertProbeResult.failure(host, CertProbeStatus.OTHER_ERROR, e.getMessage());
		}
	}
}
