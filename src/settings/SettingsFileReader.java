package settings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a {@code key=value} settings file from disk and returns its contents as
 * a {@link Map}.
 *
 * <p>The expected file format is:
 * <ul>
 *   <li>UTF-8 encoded, one entry per line.</li>
 *   <li>Lines beginning with {@code #} are treated as comments and ignored.</li>
 *   <li>Blank lines are ignored.</li>
 *   <li>Each data line must contain exactly one {@code =} character; the key is
 *       everything to the left (trimmed), the value is everything to the right
 *       (trimmed).</li>
 * </ul>
 *
 * <p>If the file does not exist, it is created with the supplied default entries
 * and a comment header, then those defaults are returned as the result. This
 * means callers always receive a fully-populated map regardless of whether the
 * file existed before the call.
 *
 * <p>This is a stateless utility class — all methods are static and the class
 * cannot be instantiated.
 */
public final class SettingsFileReader {

	/** The separator character between key and value in each data line. */
	private static final char SEPARATOR = '=';

	/** Prevent instantiation. */
	private SettingsFileReader() {
		throw new UnsupportedOperationException("SettingsFileReader is a utility class");
	}

	/**
	 * Reads the settings file at {@code path} and returns its contents as a
	 * {@link Map}.
	 *
	 * <p>If the file does not exist, it is created with {@code defaults} as its
	 * initial content (written via {@link SettingsFileWriter#write}) and those
	 * defaults are returned immediately. If the file exists but contains no
	 * recognized keys, an empty map is returned.
	 *
	 * <p>Keys from the file take precedence over nothing; the returned map
	 * contains only what is present in the file (or in {@code defaults} when the
	 * file is newly created). Callers are responsible for merging defaults for any
	 * keys absent from the returned map.
	 *
	 * @param path the path to the settings file; must not be {@code null}
	 * @param defaults the key=value pairs written when the file does not yet
	 *  exist; must not be {@code null}; an empty map is acceptable and will
	 *  produce an empty file (with only the comment header)
	 * @return a mutable {@link LinkedHashMap} of all key=value pairs parsed from
	 *  the file; never {@code null}; order matches file line order
	 * @throws IOException if the file exists but cannot be read, or if the file
	 *  does not exist and cannot be created
	 */
	public static Map<String, String> load(Path path, Map<String, String> defaults)
		throws IOException {
		if (!Files.exists(path)) {
			createWithDefaults(path, defaults);
			return new LinkedHashMap<>(defaults);
		}
		return parseFile(path);
	}

	/**
	 * Creates the parent directories (if needed), writes the comment header, and
	 * writes each entry from {@code defaults} to a new file at {@code path}.
	 *
	 * @param path the file to create; must not be {@code null}
	 * @param defaults the initial key=value pairs; must not be {@code null}
	 * @throws IOException if the directories or file cannot be created
	 */
	private static void createWithDefaults(Path path, Map<String, String> defaults)
		throws IOException {
		Files.createDirectories(path.getParent());
		SettingsFileWriter.write(path, defaults);
	}

	/**
	 * Reads all lines from the file at {@code path} and returns the parsed
	 * key=value pairs, skipping comment and blank lines.
	 *
	 * @param path the file to read; must exist; must not be {@code null}
	 * @return a mutable {@link LinkedHashMap} of parsed entries; never {@code null}
	 * @throws IOException if the file cannot be read
	 */
	private static Map<String, String> parseFile(Path path) throws IOException {
		List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		Map<String, String> result = new LinkedHashMap<>();
		for (String line : lines) {
			parseLine(line, result);
		}
		return result;
	}

	/**
	 * Parses a single line from the settings file and adds the entry to
	 * {@code result} if the line is a valid {@code key=value} pair.
	 *
	 * <p>Comment lines (starting with {@code #}) and blank lines are ignored
	 * silently. Lines that contain no {@code =} character are also ignored.
	 *
	 * @param line the raw line text; must not be {@code null}
	 * @param result the map to add the parsed entry to; must not be {@code null}
	 */
	private static void parseLine(String line, Map<String, String> result) {
		String trimmed = line.trim();
		if (trimmed.isEmpty() || trimmed.startsWith("#")) {
			return;
		}
		int separatorIndex = trimmed.indexOf(SEPARATOR);
		if (separatorIndex < 0) {
			return;
		}
		String key = trimmed.substring(0, separatorIndex).trim();
		String value = trimmed.substring(separatorIndex + 1).trim();
		if (!key.isEmpty()) {
			result.put(key, value);
		}
	}
}
