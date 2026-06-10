package app;

import settings.SettingsFileReader;
import settings.SettingsKeys;
import app.ui.MainWindow;
import util.ui.theme.StandardTheme;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Owns the application lifecycle and orchestrates the startup sequence.
 *
 * <p>Responsibilities in order:
 * <ol>
 *   <li>Determine the default settings file path
 *       ({@code <user.home>/.vibebot/vibebot.settings}).</li>
 *   <li>Load the settings file via {@link SettingsFileReader#load}, falling back
 *       to defaults for any absent keys, and construct an {@link AppSettings}
 *       instance from the resulting map.</li>
 *   <li>Register the instance as the global singleton via
 *       {@link AppSettings#setInstance(AppSettings)}.</li>
 *   <li>Apply the dark-mode theme via {@link StandardTheme#apply()} before any
 *       Swing component is constructed so that {@code UIManager} defaults are
 *       populated first.</li>
 *   <li>Schedule window creation on the Event Dispatch Thread (EDT) using
 *       {@link SwingUtilities#invokeLater}, passing the {@link AppSettings}
 *       instance into {@link MainWindow}.</li>
 * </ol>
 *
 * <p>This class is not instantiable. The sole entry point is {@link #launch()},
 * which is expected to be called once from {@code Main.main(String[])}.
 */
public final class AppLauncher {

	/** Name of the settings file stored inside the data directory. */
	private static final String SETTINGS_FILE_NAME = "vibebot.settings";

	/** Prevent instantiation. */
	private AppLauncher() {
		throw new UnsupportedOperationException("AppLauncher is a utility class");
	}

	/**
	 * Starts the application.
	 *
	 * <p>Loads application settings from disk (creating the file with defaults on
	 * first run), registers the resulting {@link AppSettings} singleton, applies
	 * the dark-mode theme, and then schedules the primary window to be created and
	 * shown on the Event Dispatch Thread. This method returns immediately after
	 * scheduling the EDT task; it does not block until the window is visible.
	 *
	 * <p>If the settings file cannot be read or created, a warning is printed to
	 * {@code System.err} and default settings are used so the application can still
	 * start.
	 */
	public static void launch() {
		AppSettings settings = loadSettings();
		AppSettings.setInstance(settings);
		StandardTheme.apply();
		SwingUtilities.invokeLater(() -> openWindow(settings));
	}

	/**
	 * Loads settings from disk and returns a fully populated {@link AppSettings}.
	 *
	 * <p>The settings file is resolved as
	 * {@code <user.home>/.vibebot/vibebot.settings}. If the file does not exist, it
	 * is created with the default values. If any I/O error occurs, a warning is
	 * printed to {@code System.err} and default settings are returned so the
	 * application degrades gracefully.
	 *
	 * @return an {@link AppSettings} populated from disk or defaults; never
	 *  {@code null}
	 */
	private static AppSettings loadSettings() {
		Path defaultSettingsPath = resolveDefaultSettingsPath();
		Map<String, String> defaults = buildDefaults(defaultSettingsPath.getParent());
		try {
			Map<String, String> raw = SettingsFileReader.load(defaultSettingsPath, defaults);
			return AppSettings.load(mergeWithDefaults(raw, defaults));
		} catch (IOException e) {
			System.err.println(
				"[vibebot] Could not load settings file — using defaults. " + e.getMessage()
			);
			return AppSettings.load(defaults);
		}
	}

	/**
	 * Returns the default path for the settings file:
	 * {@code <user.home>/.vibebot/vibebot.settings}.
	 *
	 * @return the settings file path; never {@code null}
	 */
	private static Path resolveDefaultSettingsPath() {
		return Path.of(
			System.getProperty("user.home"),
			".vibebot",
			SETTINGS_FILE_NAME
		);
	}

	/**
	 * Builds the default key=value map using {@code dataDir} as the data directory.
	 *
	 * @param dataDir the default data directory path; must not be {@code null}
	 * @return a mutable map of default settings; never {@code null}
	 */
	private static Map<String, String> buildDefaults(Path dataDir) {
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put(SettingsKeys.DATA_DIR.key(), dataDir.toString());
		defaults.put(SettingsKeys.SSH_KEY_PATH.key(), AppSettings.DEFAULT_SSH_KEY_PATH);
		return defaults;
	}

	/**
	 * Returns a new map that contains all entries from {@code raw}, with any keys
	 * absent in {@code raw} filled in from {@code defaults}.
	 *
	 * @param raw the map parsed from disk; must not be {@code null}
	 * @param defaults the fallback values; must not be {@code null}
	 * @return a merged map; never {@code null}
	 */
	private static Map<String, String> mergeWithDefaults(
		Map<String, String> raw,
		Map<String, String> defaults
	) {
		Map<String, String> merged = new LinkedHashMap<>(defaults);
		merged.putAll(raw);
		return merged;
	}

	/**
	 * Creates and shows the primary application window.
	 *
	 * <p>Must be called on the Event Dispatch Thread. Extracted as a helper so
	 * that {@link #launch()} stays readable and this body can be tested or
	 * extended independently.
	 *
	 * @param settings the loaded application settings; must not be {@code null}
	 */
	private static void openWindow(AppSettings settings) {
		MainWindow window = new MainWindow(settings);
		window.show();
	}
}
