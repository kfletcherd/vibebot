package util.csv;

import java.util.List;

/**
 * Represents one parsed row from a CSV file as an ordered, immutable list of
 * string field values.
 *
 * <p>{@code CsvRecord} carries no knowledge of column semantics — it simply
 * preserves the raw string values in the order they appeared in the source row.
 * Domain-specific interpretation (e.g., mapping column indices to named fields)
 * is the responsibility of a dedicated mapper class such as
 * {@code ServerCsvMapper}.
 *
 * <p>The record is intentionally thin: construction is via the canonical
 * constructor and field access is through the {@link #fields()} accessor.
 *
 * <p>Example usage:
 * <pre>{@code
 * CsvRecord row = new CsvRecord(List.of("10.0.0.1", "192.168.1.1", "web01"));
 * String ip = row.fields().get(0); // "10.0.0.1"
 * }</pre>
 *
 * @param fields an ordered, non-null list of raw string field values for this
 *  row; individual entries may be empty strings but not {@code null}
 */
public record CsvRecord(List<String> fields) {

	/**
	 * Validates and defensively copies the fields list on construction.
	 *
	 * @param fields the raw field values; must not be {@code null}
	 * @throws IllegalArgumentException if {@code fields} is {@code null}
	 */
	public CsvRecord {
		if (fields == null) {
			throw new IllegalArgumentException("fields must not be null");
		}
		fields = List.copyOf(fields);
	}

	/**
	 * Returns the number of fields in this record.
	 *
	 * @return the field count; zero or positive
	 */
	public int size() {
		return fields.size();
	}

	/**
	 * Returns the field value at the given zero-based column index.
	 *
	 * @param index the zero-based column index
	 * @return the raw string value at {@code index}; never {@code null}
	 * @throws IndexOutOfBoundsException if {@code index} is out of range
	 */
	public String get(int index) {
		return fields.get(index);
	}
}
