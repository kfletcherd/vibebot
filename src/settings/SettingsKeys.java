package settings;

/**
 * Typed enumeration of every key name used in the vibebot settings file.
 *
 * <p>Each constant represents exactly one entry in the {@code vibebot.settings}
 * file. Code that reads or writes settings should pass {@code SettingsKeys}
 * values directly wherever the receiving API accepts the enum type.
 * The {@link #key()} accessor is reserved for boundary crossings where a raw
 * {@code String} is unavoidable — for example, building a {@code Map<String, String>}
 * that is handed to the file reader or writer.
 *
 * <p>Adding a new setting requires two coordinated steps:
 * <ol>
 *   <li>Add one constant to this enum, supplying the raw file key string.</li>
 *   <li>Add the corresponding default value in {@link app.AppSettings}.</li>
 * </ol>
 *
 * <p>Because the set of settings keys is closed and known at compile time, this
 * enum enables exhaustiveness checking in {@code switch} expressions without a
 * {@code default} arm, and prevents passing arbitrary strings where a typed
 * key is expected.
 */
public enum SettingsKeys {

	/** Key for the application data directory path. */
	DATA_DIR("dataDir"),

	/** Key for the path to the SSH private key. */
	SSH_KEY_PATH("sshKeyPath");

	private final String key;

	SettingsKeys(String key) {
		this.key = key;
	}

	/**
	 * Returns the raw string written to and read from the settings file.
	 *
	 * <p>Use this accessor only when crossing a boundary that requires a plain
	 * {@code String} — such as a {@code Map<String, String>} produced by or
	 * consumed by the file parser/writer. Prefer the enum type directly in all
	 * internal APIs.
	 *
	 * @return the settings-file key string; never {@code null}
	 */
	public String key() {
		return key;
	}
}
