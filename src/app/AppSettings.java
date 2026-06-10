package app;

import settings.SettingsKeys;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable, global in-memory view of all application settings.
 *
 * <p>{@code AppSettings} is an immutable value object managed as a singleton.
 * A single instance is created by {@link AppLauncher} via
 * {@link #load(Map)} from the raw map returned by
 * {@link settings.SettingsFileReader}, then stored via
 * {@link #setInstance(AppSettings)} before any Swing code runs. All subsequent
 * code retrieves the instance via {@link #instance()}.
 *
 * <p>Because instances are immutable, replacing settings requires constructing a
 * new instance with {@link #load(Map)} and registering it with
 * {@link #setInstance(AppSettings)}.
 *
 * <p>Singleton contract:
 * <ul>
 *   <li>{@link #setInstance(AppSettings)} must be called before the first call
 *       to {@link #instance()}.</li>
 *   <li>{@link #instance()} throws {@link IllegalStateException} if called before
 *       {@link #setInstance}. This makes an uninitialized access a loud, immediate
 *       failure rather than a silent {@code null} dereference.</li>
 *   <li>Tests may call {@link #setInstance} to inject a custom instance before
 *       constructing the class under test.</li>
 * </ul>
 */
public final class AppSettings {

	/** Default value for {@link SettingsKeys#DATA_DIR}: {@code <user.home>/.vibebot}. */
	public static final String DEFAULT_DATA_DIR =
		Path.of(System.getProperty("user.home"), ".vibebot").toString();

	/** Default value for {@link SettingsKeys#SSH_KEY_PATH}. */
	public static final String DEFAULT_SSH_KEY_PATH = "~/.ssh/id_rsa";

	/** Name of the settings file stored inside the data directory. */
	private static final String SETTINGS_FILE_NAME = "vibebot.settings";

	/** The singleton instance; {@code null} until {@link #setInstance} is called. */
	private static volatile AppSettings instance;

	/** Current application data directory. */
	public final Path dataDir;

	/** Current SSH private-key path string. */
	public final String sshKeyPath;

	/** Path to the settings file ({@code vibebot.settings}) inside {@link #dataDir}. */
	public final Path settingsFilePath;

	/**
	 * Private constructor; callers must use {@link #load(Map)}.
	 *
	 * @param dataDir the resolved data directory path; must not be {@code null}
	 * @param sshKeyPath the SSH key path string; must not be {@code null}
	 * @param settingsFilePath the settings file path; must not be {@code null}
	 */
	private AppSettings(Path dataDir, String sshKeyPath, Path settingsFilePath) {
		this.dataDir = dataDir;
		this.sshKeyPath = sshKeyPath;
		this.settingsFilePath = settingsFilePath;
	}

	/**
	 * Constructs a new {@code AppSettings} from a raw string key=value map.
	 *
	 * <p>This overload is intended for the file I/O boundary: the map produced by
	 * {@link settings.SettingsFileReader} uses {@link String} keys. Keys absent
	 * from {@code raw} receive their default values. Unknown keys are silently
	 * ignored. The returned instance is fully immutable.
	 *
	 * @param raw the key=value pairs loaded from disk; must not be {@code null};
	 *  may be empty (all settings fall back to defaults)
	 * @return a new {@code AppSettings} populated from {@code raw}; never
	 *  {@code null}
	 */
	public static AppSettings load(Map<String, String> raw) {
		Path dataDir = Path.of(raw.getOrDefault(SettingsKeys.DATA_DIR.key(), DEFAULT_DATA_DIR));
		String sshKeyPath = raw.getOrDefault(SettingsKeys.SSH_KEY_PATH.key(), DEFAULT_SSH_KEY_PATH);
		Path settingsFilePath = dataDir.resolve(SETTINGS_FILE_NAME);
		return new AppSettings(dataDir, sshKeyPath, settingsFilePath);
	}

	/**
	 * Constructs a new {@code AppSettings} from a typed {@link SettingsKeys} map.
	 *
	 * <p>This factory is intended for internal callers that build settings maps
	 * using enum keys directly, such as {@link settings.ui.SettingsView}. Enum keys are
	 * looked up directly — no {@code .key()} conversion is performed. Keys absent
	 * from {@code typed} receive their default values. The returned instance is
	 * fully immutable.
	 *
	 * <p>A distinct name is required because Java erases both
	 * {@code Map<String,String>} and {@code Map<SettingsKeys,String>} to
	 * {@code Map} at the bytecode level, making them indistinguishable as
	 * overloads.
	 *
	 * @param typed the key=value pairs keyed by {@link SettingsKeys}; must not be
	 *  {@code null}; may be empty (all settings fall back to defaults)
	 * @return a new {@code AppSettings} populated from {@code typed}; never
	 *  {@code null}
	 */
	public static AppSettings loadTyped(Map<SettingsKeys, String> typed) {
		Path dataDir = Path.of(typed.getOrDefault(SettingsKeys.DATA_DIR, DEFAULT_DATA_DIR));
		String sshKeyPath = typed.getOrDefault(SettingsKeys.SSH_KEY_PATH, DEFAULT_SSH_KEY_PATH);
		Path settingsFilePath = dataDir.resolve(SETTINGS_FILE_NAME);
		return new AppSettings(dataDir, sshKeyPath, settingsFilePath);
	}

	/**
	 * Returns the global {@code AppSettings} instance.
	 *
	 * @return the singleton instance; never {@code null}
	 * @throws IllegalStateException if {@link #setInstance(AppSettings)} has not
	 *  yet been called
	 */
	public static AppSettings instance() {
		AppSettings current = instance;
		if (current == null) {
			throw new IllegalStateException(
				"AppSettings.setInstance() must be called before AppSettings.instance()"
			);
		}
		return current;
	}

	/**
	 * Registers the singleton instance.
	 *
	 * <p>Must be called before any call to {@link #instance()}. Calling this
	 * method again replaces the previously registered instance; this is intended
	 * for settings updates (via {@link #load(Map)}) and for test scenarios.
	 *
	 * @param settings the instance to register; must not be {@code null}
	 */
	public static void setInstance(AppSettings settings) {
		if (settings == null) {
			throw new IllegalArgumentException("settings must not be null");
		}
		instance = settings;
	}

	/**
	 * Returns the current application data directory.
	 *
	 * <p>Delegates to the public field {@link #dataDir}.
	 *
	 * @return the data directory path; never {@code null}
	 */
	public Path dataDir() {
		return dataDir;
	}

	/**
	 * Returns the current SSH private-key path as a plain string.
	 *
	 * <p>Delegates to the public field {@link #sshKeyPath}. The value is not
	 * resolved to a {@link Path} here because no current feature consumes it as a
	 * path. Callers that need a {@link Path} should perform their own resolution at
	 * the point of use.
	 *
	 * @return the SSH key path string; never {@code null}
	 */
	public String sshKeyPath() {
		return sshKeyPath;
	}

	/**
	 * Returns the path to the settings file.
	 *
	 * <p>Delegates to the public field {@link #settingsFilePath}. The settings file
	 * is always {@code vibebot.settings} inside the data directory captured at
	 * construction time.
	 *
	 * @return the settings file path; never {@code null}
	 */
	public Path settingsFilePath() {
		return settingsFilePath;
	}

	/**
	 * Returns a map containing the current values for all known settings keys,
	 * suitable for passing directly to {@link settings.SettingsFileWriter#write}.
	 *
	 * @return a {@link LinkedHashMap} of all current settings; never {@code null}
	 */
	public Map<String, String> toMap() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put(SettingsKeys.DATA_DIR.key(), dataDir.toString());
		map.put(SettingsKeys.SSH_KEY_PATH.key(), sshKeyPath);
		return map;
	}
}
