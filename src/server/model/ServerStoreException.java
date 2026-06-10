package server.model;

import java.nio.file.Path;

/**
 * Checked exception thrown by {@link ServerStore} when the server CSV file
 * cannot be loaded due to a parse error, unexpected column count, malformed
 * data, or an I/O failure.
 *
 * <p>The exception carries the resolved {@link Path} of the offending file so
 * that callers and UI layers can surface a meaningful, actionable error message
 * that includes the exact location of the problem file.
 */
public final class ServerStoreException extends Exception {

	/** The resolved path to the CSV file that could not be loaded. */
	private final Path filePath;

	/**
	 * Constructs a {@code ServerStoreException} with a detail message and the path
	 * to the file that caused the failure.
	 *
	 * @param message a human-readable description of the failure; must not be
	 *  {@code null}
	 * @param filePath the resolved path of the CSV file that could not be loaded;
	 *  must not be {@code null}
	 */
	public ServerStoreException(String message, Path filePath) {
		super(message);
		this.filePath = filePath;
	}

	/**
	 * Constructs a {@code ServerStoreException} with a detail message, a cause,
	 * and the path to the file that caused the failure.
	 *
	 * @param message a human-readable description of the failure; must not be
	 *  {@code null}
	 * @param filePath the resolved path of the CSV file that could not be loaded;
	 *  must not be {@code null}
	 * @param cause the underlying exception that triggered this failure; may be
	 *  {@code null}
	 */
	public ServerStoreException(String message, Path filePath, Throwable cause) {
		super(message, cause);
		this.filePath = filePath;
	}

	/**
	 * Returns the resolved path of the CSV file that could not be loaded.
	 *
	 * @return the file path; never {@code null}
	 */
	public Path filePath() {
		return filePath;
	}
}
