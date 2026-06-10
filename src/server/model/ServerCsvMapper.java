package server.model;

import util.csv.CsvRecord;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Converts between {@link CsvRecord} and {@link Server}, delegating all column
 * identity to {@link ServerColumn}.
 *
 * <p>This class is the single source of truth for CSV serialization logic. Column
 * indexes, column count, and header strings are all derived from {@link ServerColumn}
 * so that any schema change only requires updating that enum.
 *
 * <p>The expected column order matches {@link ServerColumn} ordinals:
 * <ol start="0">
 *   <li>{@code public_ip}</li>
 *   <li>{@code pvlan_ip}</li>
 *   <li>{@code short_name}</li>
 *   <li>{@code hostname}</li>
 *   <li>{@code notes}</li>
 *   <li>{@code environment}</li>
 *   <li>{@code type}</li>
 *   <li>{@code hosting_provider}</li>
 * </ol>
 *
 * <p>This class is non-instantiable; use the static methods directly.
 */
public final class ServerCsvMapper {

	/** The canonical header row fields, derived from {@link ServerColumn}. */
	private static final List<String> HEADER_FIELDS = Arrays.stream(ServerColumn.values())
		.sorted((a, b) -> Integer.compare(a.csvIndex(), b.csvIndex()))
		.map(ServerColumn::csvHeader)
		.collect(Collectors.toUnmodifiableList());

	/** The header as a {@link CsvRecord} for convenient use with {@link util.csv.CsvParser}. */
	private static final CsvRecord HEADER_RECORD = new CsvRecord(HEADER_FIELDS);

	/** Delimiter used to join multiple {@link ServerType} values in a single CSV field. */
	private static final String TYPE_DELIMITER = ";";

	/** Prevents instantiation. */
	private ServerCsvMapper() {
		throw new AssertionError("ServerCsvMapper is not instantiable");
	}

	/**
	 * Returns the expected header {@link CsvRecord} for a servers CSV file.
	 *
	 * <p>Callers that write a new or updated CSV file should write this record as
	 * the first row.
	 *
	 * @return the header record; never {@code null}
	 */
	public static CsvRecord headerRecord() {
		return HEADER_RECORD;
	}

	/**
	 * Validates that a {@link CsvRecord} matches the expected header definition.
	 *
	 * <p>Comparison is case-sensitive and position-sensitive. Returns {@code true}
	 * only when the record has exactly {@link ServerColumn#COUNT} fields and each
	 * field equals the corresponding canonical header name.
	 *
	 * @param record the record to validate; must not be {@code null}
	 * @return {@code true} if the record is a valid header; {@code false} otherwise
	 */
	public static boolean isValidHeader(CsvRecord record) {
		if (record.size() != ServerColumn.COUNT) {
			return false;
		}
		for (int i = 0; i < ServerColumn.COUNT; i++) {
			if (!HEADER_FIELDS.get(i).equals(record.get(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Converts a data {@link CsvRecord} to a {@link Server}.
	 *
	 * <p>The {@code environment} and {@code hostingProvider} fields are parsed via
	 * {@link ServerEnvironment#fromCsv} and {@link HostingProvider#fromCsv}; blank or
	 * unrecognised values yield an empty {@link Optional}. The {@code type} field is
	 * split on {@code ";"} and each token passed to {@link ServerType#fromCsv};
	 * unrecognised tokens are silently dropped. If no valid tokens remain, the type set
	 * is empty — the mapper does not enforce the "at least one" rule (that is a form
	 * concern).
	 *
	 * @param record the data record to convert; must have exactly
	 *  {@link ServerColumn#COUNT} fields; must not be {@code null}
	 * @return the corresponding {@link Server}; never {@code null}
	 * @throws IllegalArgumentException if field count does not match
	 */
	public static Server toServer(CsvRecord record) {
		if (record.size() != ServerColumn.COUNT) {
			throw new IllegalArgumentException(
				"Expected " + ServerColumn.COUNT + " fields but found " + record.size()
			);
		}
		Optional<ServerEnvironment> environment = ServerEnvironment.fromCsv(
			record.get(ServerColumn.ENVIRONMENT.csvIndex())
		);
		EnumSet<ServerType> type = parseTypeField(
			record.get(ServerColumn.TYPE.csvIndex())
		);
		Optional<HostingProvider> hostingProvider = HostingProvider.fromCsv(
			record.get(ServerColumn.HOSTING_PROVIDER.csvIndex())
		);
		return new Server(
			record.get(ServerColumn.PUBLIC_IP.csvIndex()),
			record.get(ServerColumn.PVLAN_IP.csvIndex()),
			record.get(ServerColumn.SHORT_NAME.csvIndex()),
			record.get(ServerColumn.HOSTNAME.csvIndex()),
			record.get(ServerColumn.NOTES.csvIndex()),
			environment,
			type,
			hostingProvider
		);
	}

	/**
	 * Converts a {@link Server} to a {@link CsvRecord} for writing.
	 *
	 * <p>{@code environment} and {@code hostingProvider} serialise as their
	 * {@link Enum#name()} when present, or as an empty string when absent.
	 * {@code type} serialises as the {@link Enum#name()} values of each element
	 * joined by {@code ";"}.
	 *
	 * @param server the server to convert; must not be {@code null}
	 * @return the corresponding {@link CsvRecord}; never {@code null}
	 */
	public static CsvRecord toRecord(Server server) {
		String environmentCsv = server.environment()
			.map(Enum::name)
			.orElse("");
		String typeCsv = server.type().stream()
			.map(Enum::name)
			.collect(Collectors.joining(TYPE_DELIMITER));
		String hostingProviderCsv = server.hostingProvider()
			.map(Enum::name)
			.orElse("");
		return new CsvRecord(List.of(
			server.publicIp(),
			server.pvlanIp(),
			server.shortName(),
			server.hostname(),
			server.notes(),
			environmentCsv,
			typeCsv,
			hostingProviderCsv
		));
	}

	/**
	 * Splits a semicolon-delimited type field and maps each token to a
	 * {@link ServerType}. Unrecognised tokens are silently dropped. Returns an
	 * empty set when no valid tokens are found.
	 *
	 * @param raw the raw CSV field value; must not be {@code null}
	 * @return a set of recognised types; may be empty
	 */
	private static EnumSet<ServerType> parseTypeField(String raw) {
		EnumSet<ServerType> result = EnumSet.noneOf(ServerType.class);
		if (raw.isBlank()) {
			return result;
		}
		for (String token : raw.split(TYPE_DELIMITER)) {
			ServerType.fromCsv(token.trim()).ifPresent(result::add);
		}
		return result;
	}
}
