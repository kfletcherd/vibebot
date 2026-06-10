package settings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Writes a {@link Map} of settings to a {@code key=value} file on disk.
 *
 * <p>The output format is:
 * <ul>
 *   <li>UTF-8 encoded.</li>
 *   <li>A comment header line ({@code # vibebot settings}) as the first line.</li>
 *   <li>One {@code key=value} line per map entry, in the map's iteration order.</li>
 * </ul>
 *
 * <p>The file is always overwritten in full. Partial updates are not supported;
 * callers must supply the complete desired map each time.
 *
 * <p>This is a stateless utility class — all methods are static and the class
 * cannot be instantiated.
 */
public final class SettingsFileWriter {

	/** The comment header written at the top of every settings file. */
	private static final String FILE_HEADER = "# vibebot settings";

	/** The separator character placed between key and value on each data line. */
	private static final char SEPARATOR = '=';

	/** Prevent instantiation. */
	private SettingsFileWriter() {
		throw new UnsupportedOperationException("SettingsFileWriter is a utility class");
	}

	/**
	 * Writes {@code settings} to the file at {@code path}, overwriting any existing
	 * content.
	 *
	 * <p>The file is written atomically with respect to the JVM process: the content
	 * is assembled in memory first and then written in a single {@link Files#write}
	 * call. If the file's parent directories do not exist, they are created before
	 * writing.
	 *
	 * @param path the file to write; must not be {@code null}
	 * @param settings the key=value pairs to persist; must not be {@code null};
	 *  an empty map produces a file containing only the comment header
	 * @throws IOException if the parent directories cannot be created or the file
	 *  cannot be written
	 */
	public static void write(Path path, Map<String, String> settings) throws IOException {
		Files.createDirectories(path.getParent());
		List<String> lines = buildLines(settings);
		Files.write(path, lines, StandardCharsets.UTF_8);
	}

	/**
	 * Assembles the full list of lines to be written, starting with the comment
	 * header followed by one {@code key=value} line per entry.
	 *
	 * @param settings the entries to format; must not be {@code null}
	 * @return a non-empty list of lines (at minimum the header); never {@code null}
	 */
	private static List<String> buildLines(Map<String, String> settings) {
		List<String> lines = new ArrayList<>(settings.size() + 1);
		lines.add(FILE_HEADER);
		for (Map.Entry<String, String> entry : settings.entrySet()) {
			lines.add(entry.getKey() + SEPARATOR + entry.getValue());
		}
		return lines;
	}
}
