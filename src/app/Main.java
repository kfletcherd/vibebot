package app;

/**
 * Application entry point for vibebot.
 *
 * <p>This class is intentionally thin. It contains only the JVM entry point
 * and immediately delegates all startup work to {@link AppLauncher#launch()}.
 * Keeping logic out of {@code main} makes the launcher independently testable
 * and allows alternative entry points (e.g. test harnesses) to reuse the same
 * bootstrap path.
 */
public final class Main {

	/** Prevent instantiation — this is a pure entry-point class. */
	private Main() {}

	/**
	 * JVM entry point. Delegates immediately to {@link AppLauncher#launch()}.
	 *
	 * @param args command-line arguments (currently unused)
	 */
	public static void main(String[] args) {
		AppLauncher.launch();
	}
}
