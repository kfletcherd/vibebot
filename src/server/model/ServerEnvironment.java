package server.model;

import java.util.Optional;

/**
 * Controlled vocabulary for the Environment field on a {@link Server}.
 *
 * <p>Each constant carries a user-facing {@link #displayName()} and can be
 * round-tripped through CSV via {@link #name()} / {@link #fromCsv(String)}.
 */
public enum ServerEnvironment {

	/** Live production environment. */
	PRODUCTION("Production"),

	/** Pre-production staging environment. */
	STAGING("Staging"),

	/** Local or shared development environment. */
	DEVELOPMENT("Development");

	/** The user-facing label for this environment. */
	private final String displayName;

	/**
	 * Constructs a {@code ServerEnvironment} with the given display name.
	 *
	 * @param displayName the user-facing label; must not be {@code null}
	 */
	ServerEnvironment(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Returns the user-facing display string for this environment.
	 *
	 * @return the display name; never {@code null}
	 */
	public String displayName() {
		return displayName;
	}

	/**
	 * Returns the {@code ServerEnvironment} whose {@link #name()} matches {@code value}
	 * (case-sensitive), or empty if the value is blank or unrecognised.
	 *
	 * @param value the raw CSV field value; may be blank; must not be {@code null}
	 * @return the matching environment; empty for blank or unrecognised values
	 */
	public static Optional<ServerEnvironment> fromCsv(String value) {
		if (value.isBlank()) {
			return Optional.empty();
		}
		for (ServerEnvironment env : values()) {
			if (env.name().equals(value)) {
				return Optional.of(env);
			}
		}
		return Optional.empty();
	}
}
