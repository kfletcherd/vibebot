package util.csv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Stateless utility that reads and writes CSV files conforming to RFC 4180.
 *
 * <p>This class is domain-agnostic: it operates only on {@link Path},
 * {@link CsvRecord}, and {@link String}. It knows nothing about specific column
 * semantics. Domain-specific interpretation is the responsibility of mapper
 * classes (e.g., {@code ServerCsvMapper}).
 *
 * <p>Supported CSV features:
 * <ul>
 *   <li>Fields separated by commas.</li>
 *   <li>Fields may be enclosed in double-quotes, allowing embedded commas and
 *       newlines inside quoted fields.</li>
 *   <li>A double-quote inside a quoted field is escaped as two consecutive
 *       double-quote characters ({@code ""}).</li>
 *   <li>Unquoted fields are trimmed of leading/trailing whitespace.</li>
 *   <li>Empty fields (two adjacent commas, or a quoted empty string) are stored
 *       as empty strings, not {@code null}.</li>
 * </ul>
 *
 * <p>Files are read and written using UTF-8 encoding.
 *
 * <p>This class is non-instantiable; use the static factory methods directly.
 */
public final class CsvParser {

	/** The field delimiter character as defined by RFC 4180. */
	private static final char DELIMITER = ',';

	/** The quote character used to enclose fields per RFC 4180. */
	private static final char QUOTE = '"';

	/** Line separator used when writing CSV files. */
	private static final String LINE_SEPARATOR = "\n";

	/** Prevents instantiation. */
	private CsvParser() {
		throw new AssertionError("CsvParser is not instantiable");
	}

	/**
	 * Reads all rows from the CSV file at {@code path} and returns them as a list
	 * of {@link CsvRecord}s.
	 *
	 * <p>The header row, if present, is returned as the first element — this method
	 * does not skip or validate it. Callers that need to distinguish the header from
	 * data rows should process {@code result.get(0)} separately.
	 *
	 * <p>Blank lines are skipped.
	 *
	 * @param path the path to the CSV file; must not be {@code null}
	 * @return a list of {@link CsvRecord}s, one per non-blank line; may be empty if
	 *  the file is empty; never {@code null}
	 * @throws IOException if the file cannot be opened or read
	 */
	public static List<CsvRecord> read(Path path) throws IOException {
		List<CsvRecord> records = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					records.add(parseLine(line));
				}
			}
		}
		return records;
	}

	/**
	 * Writes the given list of {@link CsvRecord}s to the file at {@code path},
	 * replacing any existing contents.
	 *
	 * <p>Each record is serialized to a single line. Fields that contain a comma, a
	 * double-quote, or a newline are wrapped in double-quotes; double-quote
	 * characters inside such fields are escaped as {@code ""}. Fields that need no
	 * quoting are written as-is.
	 *
	 * @param path the destination file path; must not be {@code null}; parent
	 *  directories must already exist
	 * @param records the records to write; must not be {@code null}; may be empty
	 * @throws IOException if the file cannot be created or written
	 */
	public static void write(Path path, List<CsvRecord> records) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(
			path,
			StandardCharsets.UTF_8,
			StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING
		)) {
			for (CsvRecord record : records) {
				writer.write(serializeRecord(record));
				writer.write(LINE_SEPARATOR);
			}
		}
	}

	/**
	 * Parses a single CSV line into a {@link CsvRecord}.
	 *
	 * <p>Handles quoted fields, escaped double-quotes ({@code ""}), and empty fields.
	 * This method does not handle multi-line quoted fields — each call processes
	 * exactly one physical line.
	 *
	 * @param line the raw CSV line to parse; must not be {@code null}
	 * @return the parsed record; never {@code null}
	 */
	private static CsvRecord parseLine(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		int i = 0;
		while (i < line.length()) {
			char c = line.charAt(i);
			if (inQuotes) {
				if (c == QUOTE) {
					if (i + 1 < line.length() && line.charAt(i + 1) == QUOTE) {
						// Escaped double-quote inside a quoted field
						current.append(QUOTE);
						i += 2;
					} else {
						// Closing quote
						inQuotes = false;
						i++;
					}
				} else {
					current.append(c);
					i++;
				}
			} else {
				if (c == QUOTE) {
					inQuotes = true;
					i++;
				} else if (c == DELIMITER) {
					fields.add(current.toString().trim());
					current.setLength(0);
					i++;
				} else {
					current.append(c);
					i++;
				}
			}
		}
		// Append final field (no trailing delimiter required)
		fields.add(current.toString().trim());
		return new CsvRecord(fields);
	}

	/**
	 * Serializes a {@link CsvRecord} to a single CSV line string.
	 *
	 * <p>Fields containing a comma, double-quote, or newline are quoted. Internal
	 * double-quotes are escaped as {@code ""}.
	 *
	 * @param record the record to serialize; must not be {@code null}
	 * @return the CSV line string without a trailing newline; never {@code null}
	 */
	private static String serializeRecord(CsvRecord record) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < record.size(); i++) {
			if (i > 0) {
				sb.append(DELIMITER);
			}
			sb.append(escapeField(record.get(i)));
		}
		return sb.toString();
	}

	/**
	 * Escapes a single field value for CSV output.
	 *
	 * <p>The field is wrapped in double-quotes if it contains a comma, a
	 * double-quote, or a newline character. Double-quote characters within the field
	 * are doubled.
	 *
	 * @param value the raw field value; must not be {@code null}
	 * @return the escaped field ready for CSV output; never {@code null}
	 */
	private static String escapeField(String value) {
		boolean needsQuoting = value.contains(String.valueOf(DELIMITER))
			|| value.contains(String.valueOf(QUOTE))
			|| value.contains("\n")
			|| value.contains("\r");
		if (!needsQuoting) {
			return value;
		}
		String escaped = value.replace(String.valueOf(QUOTE), String.valueOf(QUOTE) + QUOTE);
		return QUOTE + escaped + QUOTE;
	}
}
