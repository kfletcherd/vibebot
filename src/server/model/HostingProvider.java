package server.model;

import java.util.Optional;

/**
 * Controlled vocabulary for the Hosting Provider field on a {@link Server}.
 *
 * <p>Each constant carries a user-facing {@link #displayName()} and can be
 * round-tripped through CSV via {@link #name()} / {@link #fromCsv(String)}.
 */
public enum HostingProvider {

	/** Google Cloud Platform. */
	GCP("GCP"),

	/** Hypervisor (on-premises). */
	HV("HV"),

	/** Amazon Web Services. */
	AWS("AWS"),

	/** No hosting provider; the server is not hosted by any tracked provider. */
	NOT_APPLICABLE("Not Applicable");

	/** The user-facing label for this provider. */
	private final String displayName;

	/**
	 * Constructs a {@code HostingProvider} with the given display name.
	 *
	 * @param displayName the user-facing label; must not be {@code null}
	 */
	HostingProvider(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Returns the user-facing display string for this provider.
	 *
	 * @return the display name; never {@code null}
	 */
	public String displayName() {
		return displayName;
	}

	/**
	 * Returns the {@code HostingProvider} whose {@link #name()} matches {@code value}
	 * (case-sensitive), or empty if the value is blank or unrecognised.
	 *
	 * @param value the raw CSV field value; may be blank; must not be {@code null}
	 * @return the matching provider; empty for blank or unrecognised values
	 */
	public static Optional<HostingProvider> fromCsv(String value) {
		if (value.isBlank()) {
			return Optional.empty();
		}
		for (HostingProvider provider : values()) {
			if (provider.name().equals(value)) {
				return Optional.of(provider);
			}
		}
		return Optional.empty();
	}
}
