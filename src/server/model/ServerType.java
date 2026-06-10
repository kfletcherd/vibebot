package server.model;

import java.util.Optional;

/**
 * Controlled vocabulary for the Type field on a {@link Server}.
 *
 * <p>A server may have multiple types simultaneously; the full set is stored as
 * an {@link java.util.EnumSet}. Each constant carries a user-facing
 * {@link #displayName()} and can be round-tripped through CSV via
 * {@link #name()} / {@link #fromCsv(String)}.
 */
public enum ServerType {

	/** Layer-4 or layer-7 load balancer. */
	LOAD_BALANCER("Load Balancer"),

	/** HTTP API service. */
	API("API"),

	/** Background job or queue worker. */
	WORKER("Worker"),

	/** Relational or document database. */
	DATABASE("Database"),

	/** Redis in-memory data store. */
	REDIS("Redis");

	/** The user-facing label for this type. */
	private final String displayName;

	/**
	 * Constructs a {@code ServerType} with the given display name.
	 *
	 * @param displayName the user-facing label; must not be {@code null}
	 */
	ServerType(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Returns the user-facing display string for this type.
	 *
	 * @return the display name; never {@code null}
	 */
	public String displayName() {
		return displayName;
	}

	/**
	 * Returns the {@code ServerType} whose {@link #name()} matches {@code value}
	 * (case-sensitive), or empty if the value is blank or unrecognised.
	 *
	 * @param value a single raw token (not a semicolon-delimited list);
	 *  may be blank; must not be {@code null}
	 * @return the matching type; empty for blank or unrecognised values
	 */
	public static Optional<ServerType> fromCsv(String value) {
		if (value.isBlank()) {
			return Optional.empty();
		}
		for (ServerType type : values()) {
			if (type.name().equals(value)) {
				return Optional.of(type);
			}
		}
		return Optional.empty();
	}
}
