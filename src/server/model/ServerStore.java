package server.model;

import app.AppSettings;
import util.csv.CsvParser;
import util.csv.CsvRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Runtime repository for the in-memory list of {@link Server} objects, backed by
 * a user-local CSV file on disk.
 *
 * <p>On construction, {@code ServerStore} resolves the CSV path as
 * {@code <dataDir>/servers.csv}, creates the file and its parent directory if
 * they do not already exist (writing only the header row), and then loads all
 * data rows into memory via {@link CsvParser} and {@link ServerCsvMapper}.
 *
 * <p>If the file exists but cannot be parsed — due to an unexpected column count,
 * a malformed header, or an I/O error — a checked {@link ServerStoreException} is
 * thrown, carrying the resolved path so the caller can display an actionable error
 * message.
 *
 * <p>The {@link #add(Server)} method appends a new server to both the in-memory
 * list and the CSV file on disk atomically with respect to the application (though
 * not with respect to external concurrent writers).
 *
 * <p>Construct exactly one instance per application session and share it between
 * the UI and any other components that need server data. Construction,
 * {@link #add}, and {@link #reload} should be called from a context where I/O is
 * acceptable (i.e., not the Event Dispatch Thread).
 */
public final class ServerStore {

	/** The CSV file name inside the data directory. */
	private static final String CSV_FILE_NAME = "servers.csv";

	/** The resolved path to the CSV file. */
	private Path csvPath;

	/** The live, mutable list of servers held in memory. */
	private final List<Server> serverList;

	/**
	 * Constructs a {@code ServerStore} using the data directory from
	 * {@link AppSettings#instance()}.
	 *
	 * <p>This constructor delegates to {@link #ServerStore(Path)} with
	 * {@code AppSettings.instance().dataDir()} as the data directory. It exists
	 * so that call sites that do not need to customize the data directory continue
	 * to work without any changes.
	 *
	 * @throws ServerStoreException if the CSV file cannot be created or parsed
	 */
	public ServerStore() throws ServerStoreException {
		this(AppSettings.instance().dataDir());
	}

	/**
	 * Constructs a {@code ServerStore} by resolving the CSV path inside
	 * {@code dataDir}, creating the file if absent, and loading all existing rows
	 * into memory.
	 *
	 * <p>The CSV file is located at {@code <dataDir>/servers.csv}. If the file
	 * does not exist, it is created with only the header row so subsequent reads
	 * return an empty server list rather than an error.
	 *
	 * @param dataDir the directory in which {@code servers.csv} lives; must not
	 *  be {@code null}
	 * @throws ServerStoreException if the file exists but cannot be read or parsed;
	 *  the exception carries the resolved file path
	 */
	public ServerStore(Path dataDir) throws ServerStoreException {
		this.csvPath = dataDir.resolve(CSV_FILE_NAME);
		this.serverList = new ArrayList<>();
		initializeFile();
		loadFromDisk();
	}

	/**
	 * Returns an unmodifiable view of the current in-memory server list.
	 *
	 * @return a read-only list of {@link Server}s; never {@code null}; may be empty
	 */
	public List<Server> servers() {
		return Collections.unmodifiableList(serverList);
	}

	/**
	 * Appends a new server to the in-memory list and persists it to the CSV file.
	 *
	 * <p>The CSV file is rewritten in full on each call. This is acceptable for
	 * typical server-list sizes. This method should not be called on the EDT.
	 *
	 * @param server the server to add; must not be {@code null}
	 * @throws IOException if the CSV file cannot be written
	 */
	public void add(Server server) throws IOException {
		serverList.add(server);
		persistToDisk();
	}

	/**
	 * Replaces {@code old} with {@code updated} in the in-memory list and
	 * rewrites the CSV.
	 *
	 * @param old the existing server to replace; must not be {@code null}; must
	 *  be present in the store
	 * @param updated the replacement server; must not be {@code null}
	 * @throws IOException if the CSV file cannot be written
	 * @throws NoSuchElementException if {@code old} is not found in the store
	 */
	public void update(Server old, Server updated) throws IOException {
		int index = serverList.indexOf(old);
		if (index < 0) {
			throw new NoSuchElementException("Server not found in store: " + old);
		}
		serverList.set(index, updated);
		persistToDisk();
	}

	/**
	 * Removes {@code server} from the in-memory list and rewrites the CSV.
	 *
	 * @param server the server to remove; must not be {@code null}; must be
	 *  present in the store
	 * @throws IOException if the CSV file cannot be written
	 * @throws NoSuchElementException if {@code server} is not found in the store
	 */
	public void delete(Server server) throws IOException {
		boolean removed = serverList.remove(server);
		if (!removed) {
			throw new NoSuchElementException("Server not found in store: " + server);
		}
		persistToDisk();
	}

	/**
	 * Returns the resolved path to the CSV file managed by this store.
	 *
	 * @return the file path; never {@code null}
	 */
	public Path csvPath() {
		return csvPath;
	}

	/**
	 * Reloads the server list from a new data directory.
	 *
	 * <p>This method clears the current in-memory list, updates {@link #csvPath}
	 * to {@code <newDataDir>/servers.csv}, re-creates the file if necessary, and
	 * reloads all rows from disk. It is safe to call only from a non-EDT thread.
	 *
	 * @param newDataDir the new data directory; must not be {@code null}
	 * @throws ServerStoreException if the file at the new path cannot be
	 *  initialized or read
	 */
	public void reload(Path newDataDir) throws ServerStoreException {
		serverList.clear();
		csvPath = newDataDir.resolve(CSV_FILE_NAME);
		initializeFile();
		loadFromDisk();
	}

	/**
	 * Creates the parent directory and the CSV file if either does not yet exist.
	 *
	 * <p>A newly created file receives only the header row so it is a valid, empty
	 * CSV on the next read.
	 *
	 * @throws ServerStoreException if the directory or file cannot be created
	 */
	private void initializeFile() throws ServerStoreException {
		try {
			Files.createDirectories(csvPath.getParent());
			if (!Files.exists(csvPath)) {
				CsvParser.write(csvPath, List.of(ServerCsvMapper.headerRecord()));
			}
		} catch (IOException e) {
			throw new ServerStoreException(
				"Could not create or initialize the CSV file: " + e.getMessage(),
				csvPath,
				e
			);
		}
	}

	/**
	 * Reads all data rows from the CSV file and populates {@link #serverList}.
	 *
	 * <p>The first row is treated as a header and validated against
	 * {@link ServerCsvMapper#isValidHeader}. If it does not match, or if any data
	 * row has the wrong column count, a {@link ServerStoreException} is thrown.
	 *
	 * @throws ServerStoreException if the file cannot be read or parsed
	 */
	private void loadFromDisk() throws ServerStoreException {
		List<CsvRecord> rows;
		try {
			rows = CsvParser.read(csvPath);
		} catch (IOException e) {
			throw new ServerStoreException(
				"Could not read the CSV file: " + e.getMessage(),
				csvPath,
				e
			);
		}

		if (rows.isEmpty()) {
			// File exists but is totally blank; treat as empty list
			return;
		}

		CsvRecord header = rows.get(0);
		if (!ServerCsvMapper.isValidHeader(header)) {
			throw new ServerStoreException(
				"CSV header does not match the expected columns",
				csvPath
			);
		}

		for (int i = 1; i < rows.size(); i++) {
			CsvRecord row = rows.get(i);
			try {
				serverList.add(ServerCsvMapper.toServer(row));
			} catch (IllegalArgumentException e) {
				throw new ServerStoreException(
					"Row " + i + " has invalid data: " + e.getMessage(),
					csvPath,
					e
				);
			}
		}
	}

	/**
	 * Writes the current in-memory server list to the CSV file, prefixed by the
	 * header row.
	 *
	 * @throws IOException if the file cannot be written
	 */
	private void persistToDisk() throws IOException {
		List<CsvRecord> rows = new ArrayList<>();
		rows.add(ServerCsvMapper.headerRecord());
		for (Server server : serverList) {
			rows.add(ServerCsvMapper.toRecord(server));
		}
		CsvParser.write(csvPath, rows);
	}
}
