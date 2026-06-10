package app.ui;

import app.AppSettings;
import server.model.ServerStore;
import server.model.ServerStoreException;
import server.ui.ServerListView;
import settings.ui.SettingsView;
import util.ui.nav.NavItem;
import util.ui.nav.NavPane;
import util.ui.pane.CardPane;
import util.ui.pane.PaneView;
import util.ui.theme.StandardColor;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.util.List;
import java.util.Optional;

/**
 * Primary application window for the vibebot desktop application.
 *
 * <p>Encapsulates a {@link JFrame} configured with a fixed default size, a
 * descriptive title, and a terminate-on-close policy. The window relies on
 * {@code UIManager} defaults being populated by
 * {@link util.ui.theme.StandardTheme} before construction; it does not apply
 * the theme itself.
 *
 * <p>The frame's content area is a {@link JSplitPane} split horizontally:
 * <ul>
 *   <li>Left — a {@link NavPane} fixed at {@link NavPane#PREFERRED_WIDTH} pixels,
 *       listing the application's top-level navigation items.</li>
 *   <li>Right — a {@link CardPane} backed by a {@code CardLayout} that swaps
 *       to the matching view whenever the user selects a nav item.</li>
 * </ul>
 * The divider is locked at construction time and is not user-draggable.
 *
 * <p>The {@link ServerListView} is constructed by loading a {@link ServerStore}
 * during window construction. This I/O occurs synchronously on the calling
 * thread. Because the application is small and the CSV file is user-local, this
 * is acceptable; a future plan may move startup I/O off the EDT.
 *
 * <p>Create an instance and call {@link #show()} to make the window visible.
 * Because Swing components must only be mutated on the Event Dispatch Thread
 * (EDT), callers are responsible for invoking {@link #show()} from the EDT
 * (e.g., via {@code SwingUtilities.invokeLater}).
 */
public final class MainWindow {

	/** Default width of the application window in pixels. */
	private static final int DEFAULT_WIDTH = 1500;

	/** Default height of the application window in pixels. */
	private static final int DEFAULT_HEIGHT = 800;

	/** Human-readable title displayed in the window's title bar. */
	private static final String WINDOW_TITLE = "vibebot";

	/**
	 * Divider position for the split pane in pixels; matches
	 * {@link NavPane#PREFERRED_WIDTH} so the nav pane is exactly at its
	 * preferred size with no further adjustment required.
	 */
	private static final int DIVIDER_LOCATION = NavPane.PREFERRED_WIDTH;

	/** The underlying Swing frame owned by this window. */
	private final JFrame frame;

	/**
	 * Constructs and configures the primary application window, including the
	 * split-pane layout with a navigation pane and a content pane.
	 *
	 * <p>The window is fully initialized but not yet visible. Call {@link #show()}
	 * to display it.
	 *
	 * @param appSettings the loaded application settings; must not be {@code null}
	 */
	public MainWindow(AppSettings appSettings) {
		frame = new JFrame(WINDOW_TITLE);
		configureFrame();
		frame.setContentPane(buildSplitPane(appSettings));
	}

	/**
	 * Makes the window visible on screen.
	 *
	 * <p>Must be called on the Event Dispatch Thread.
	 */
	public void show() {
		frame.setVisible(true);
	}

	/**
	 * Returns the preferred size of this window.
	 *
	 * @return a {@link Dimension} representing the configured default size
	 */
	public Dimension getPreferredWindowSize() {
		return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	/**
	 * Applies size, close behavior, and background color to the frame.
	 */
	private void configureFrame() {
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		frame.setMinimumSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
		frame.setLocationRelativeTo(null);
		frame.getContentPane().setBackground(StandardColor.BACKGROUND_PRIMARY.color);
		frame.setResizable(true);
	}

	/**
	 * Builds the split pane that fills the window's content area.
	 *
	 * <p>Constructs the nav items, the pane views, a {@link NavPane}, and a
	 * {@link CardPane}, then assembles them into a {@link JSplitPane}. The
	 * divider is locked at {@link #DIVIDER_LOCATION} and the divider component is
	 * disabled so the user cannot drag it.
	 *
	 * @param appSettings the loaded application settings; must not be {@code null}
	 * @return a fully configured {@link JSplitPane}; never {@code null}
	 */
	private JSplitPane buildSplitPane(AppSettings appSettings) {
		List<NavItem> navItems = buildNavItems();
		List<PaneView> paneViews = buildPaneViews(navItems, frame, appSettings);

		CardPane cardPane = new CardPane(paneViews);
		NavPane navPane = new NavPane(
			navItems,
			item -> cardPane.show(item.id())
		);

		JSplitPane splitPane = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT,
			navPane.component(),
			cardPane.component()
		);

		splitPane.setDividerLocation(DIVIDER_LOCATION);
		splitPane.setDividerSize(1);
		splitPane.setEnabled(false);
		splitPane.setBackground(StandardColor.BORDER.color);
		splitPane.setBorder(null);

		return splitPane;
	}

	/**
	 * Returns the ordered list of top-level navigation items for this release.
	 *
	 * <p>Each item's {@code id} must correspond to a registered {@link PaneView}
	 * returned by {@link #buildPaneViews}. Adding or removing items here requires
	 * a matching change in that method.
	 *
	 * @return a non-empty, ordered list of {@link NavItem}s; never {@code null}
	 */
	private static List<NavItem> buildNavItems() {
		return List.of(
			new NavItem("home", "Home"),
			new NavItem("servers", "Servers"),
			new NavItem("settings", "Settings")
		);
	}

	/**
	 * Returns one {@link PaneView} for every {@link NavItem} in {@code navItems}.
	 *
	 * <p>The {@code "servers"} nav item is backed by a {@link ServerListView}. The
	 * {@code "settings"} nav item is backed by a {@link SettingsView}. All other
	 * items (currently {@code "home"}) are backed by {@link PlaceholderPaneView}
	 * stubs.
	 *
	 * <p>The {@link ServerStore} is constructed here with the data directory from
	 * {@code appSettings}. If the CSV cannot be loaded, a {@link ServerStoreException}
	 * is caught and forwarded to {@link ServerListView} as an {@link Optional} so the
	 * view can render an error panel instead of the table.
	 *
	 * @param navItems the nav items whose IDs and labels the views must mirror;
	 *  must not be {@code null} or empty
	 * @param ownerFrame the parent frame passed to dialogs spawned from the servers
	 *  view; must not be {@code null}
	 * @param appSettings the loaded application settings; must not be {@code null}
	 * @return a non-empty list of {@link PaneView}s in the same order as
	 *  {@code navItems}; never {@code null}
	 */
	private static List<PaneView> buildPaneViews(
		List<NavItem> navItems,
		JFrame ownerFrame,
		AppSettings appSettings
	) {
		ServerStore store = null;
		Optional<ServerStoreException> loadError = Optional.empty();
		try {
			store = new ServerStore(appSettings.dataDir());
		} catch (ServerStoreException e) {
			loadError = Optional.of(e);
		}

		final ServerStore resolvedStore = store;
		final Optional<ServerStoreException> resolvedError = loadError;

		return navItems.stream()
			.map(item -> buildViewForItem(item, ownerFrame, resolvedStore, resolvedError, appSettings))
			.toList();
	}

	/**
	 * Returns the appropriate {@link PaneView} for a single {@link NavItem}.
	 *
	 * <p>The {@code "servers"} item receives a {@link ServerListView}; the
	 * {@code "settings"} item receives a {@link SettingsView}; all others receive
	 * a {@link PlaceholderPaneView}.
	 *
	 * @param item the nav item to build a view for; must not be {@code null}
	 * @param ownerFrame the parent frame for dialogs; must not be {@code null}
	 * @param store the loaded {@link ServerStore}, or {@code null} if loading failed
	 * @param loadError the load error, if any; must not be {@code null}
	 * @param appSettings the loaded application settings; must not be {@code null}
	 * @return the view for this item; never {@code null}
	 */
	private static PaneView buildViewForItem(
		NavItem item,
		JFrame ownerFrame,
		ServerStore store,
		Optional<ServerStoreException> loadError,
		AppSettings appSettings
	) {
		return switch (item.id()) {
			case "servers" -> new ServerListView(ownerFrame, store, loadError);
			case "settings" -> new SettingsView(appSettings, store);
			default -> new PlaceholderPaneView(item);
		};
	}

	// -------------------------------------------------------------------------
	// Placeholder inner view
	// -------------------------------------------------------------------------

	/**
	 * Minimal {@link PaneView} stub that displays a centered label with the
	 * nav item's label text.
	 *
	 * <p>This inner class is intentionally private so it is not accessible outside
	 * this compilation unit. It is a temporary scaffold to be replaced when each
	 * section's real view is implemented.
	 */
	private static final class PlaceholderPaneView implements PaneView {

		/** The nav item whose id and label this view mirrors. */
		private final NavItem navItem;

		/** The Swing component returned by {@link #component()}; allocated once. */
		private final JLabel label;

		/**
		 * Constructs a placeholder view for the given nav item.
		 *
		 * @param navItem the nav item this view represents; must not be {@code null}
		 */
		PlaceholderPaneView(NavItem navItem) {
			this.navItem = navItem;
			label = new JLabel(navItem.label(), SwingConstants.CENTER);
			label.setForeground(StandardColor.TEXT_SECONDARY.color);
			label.setBackground(StandardColor.BACKGROUND_ELEVATED.color);
			label.setOpaque(true);
		}

		/**
		 * Returns the unique identifier for this view, matching the owning nav item's id.
		 *
		 * @return the nav item's id string; never {@code null}
		 */
		@Override
		public String id() {
			return navItem.id();
		}

		/**
		 * Returns the label component that represents this placeholder view.
		 *
		 * <p>The same instance is returned on every call.
		 *
		 * @return the non-null {@link JLabel} for this placeholder; same instance on every call
		 */
		@Override
		public JComponent component() {
			return label;
		}
	}
}
