package server.model;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Immutable value object representing a single server entry in the server list.
 *
 * <p>The {@code environment} and {@code hostingProvider} fields are optional;
 * an absent value means the attribute was not set or was not recognised when
 * reading from CSV. The {@code type} set must be non-empty; the compact
 * constructor enforces this.
 *
 * <p>IP address format and hostname format are not validated here; those checks
 * are the responsibility of {@code AddServerDialog}.
 *
 * @param publicIp the public-facing IP address; never {@code null}
 * @param pvlanIp the PVLAN (private VLAN) IP address; never {@code null}
 * @param shortName a brief human-readable identifier (e.g., {@code "web01"});
 *  never {@code null}
 * @param hostname the fully-qualified domain name; may be empty; never {@code null}
 * @param notes free-form notes about this server; may be empty; never {@code null}
 * @param environment the deployment environment; empty when absent; never {@code null}
 * @param type the set of functional types; must be non-empty; never {@code null}
 * @param hostingProvider the hosting provider; empty when absent; never {@code null}
 */
public record Server(
	String publicIp,
	String pvlanIp,
	String shortName,
	String hostname,
	String notes,
	Optional<ServerEnvironment> environment,
	EnumSet<ServerType> type,
	Optional<HostingProvider> hostingProvider
) {

	/**
	 * Validates that no field is {@code null} and that {@code type} is non-empty.
	 *
	 * @throws IllegalArgumentException if any field is {@code null} or
	 *  {@code type} is empty
	 */
	public Server {
		if (publicIp == null) throw new IllegalArgumentException("publicIp must not be null");
		if (pvlanIp == null) throw new IllegalArgumentException("pvlanIp must not be null");
		if (shortName == null) throw new IllegalArgumentException("shortName must not be null");
		if (hostname == null) throw new IllegalArgumentException("hostname must not be null");
		if (notes == null) throw new IllegalArgumentException("notes must not be null");
		if (environment == null) throw new IllegalArgumentException("environment must not be null");
		if (type == null) throw new IllegalArgumentException("type must not be null");
		if (type.isEmpty()) throw new IllegalArgumentException("type must not be empty");
		if (hostingProvider == null) throw new IllegalArgumentException("hostingProvider must not be null");
		type = EnumSet.copyOf(type);
	}
}
