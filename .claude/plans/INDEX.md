# Plans Index

This file is maintained by the `pm-bot` agent. The `java-coder` agent reads it to find the next ready plan.

| ID | Title | Status | Created | Description |
|---|---|---|---|---|
| 001 | Main Application Window with Dark Mode Theme | done | 2026-04-13 | Creates the application entry point, a Swing JFrame main window at 800x600, and a dark mode theme applied via UIManager |
| 002 | Plain Makefile Build for vibebot | done | 2026-04-13 | Adds a Makefile at the project root with compile, jar, run, and clean targets using only the standard JDK toolchain |
| 003 | Split-Pane Layout with Nav and Content Areas | done | 2026-04-16 | Adds a left nav pane (JList of NavItems) and a right CardLayout content pane to MainWindow, wired by a NavSelectionListener functional interface |
| 004 | Server List Reference Pane | done | 2026-04-17 | Adds a server table ContentView backed by a user-local CSV file, a generic CSV parser, a ServerStore, and an Add Server dialog |
| 005 | Global Settings System | done | 2026-04-17 | Introduces a persistent key=value settings file, an AppSettings singleton loaded at startup, a ServerStore dataDir migration, and a real SettingsView for editing dataDir |
| 006 | Settings Enum Refactor — SettingsKeys enum and Immutable AppSettings | done | 2026-04-17 | Converts SettingsKeys to an enum with a key() accessor and makes AppSettings fully immutable with public final fields and a static load() factory |
| 007 | Source Tree Consolidation | done | 2026-05-05 | Moves all application code from the old deep source folder into the newer flat src/ tree, leaving one unified source root |
| 008 | Edit and Delete Servers | done | 2026-05-05 | Adds an Edit button per table row that opens the Add Server dialog pre-filled for editing, plus a Delete button inside that dialog with a confirmation step |
| 009 | Server Form Field Constraints | done | 2026-05-05 | Adds IP address validation to Public IP and PVLAN IP, replaces Type with a multi-select, and replaces Environment and Hosting Provider with dropdowns |
| 010 | Move CSV Utilities Under Util and Document Util's Purpose | done | 2026-05-12 | Relocates the CSV helpers from the top-level io area into the shared util area and adds a written description explaining what util is for |
| 011 | Move Nav Helpers Under Util.UI | done | 2026-05-12 | Relocates the navigation helpers from the top-level ui.nav area into the shared util.ui area alongside the existing pane, table, and theme helpers |
| 012 | Empty the Top-Level UI Area by Moving Main Window and Settings View Into Their Features | done | 2026-05-12 | Moves the settings screen into the settings feature's UI sub-area and the main window into the application feature's UI sub-area, removing the top-level ui area entirely; depends on plan 011 |
| 013 | Server UI Touch-Ups — Column Order, Button and Dropdown Theming, Multi-Line Notes, Cell Tooltips | done | 2026-05-12 | Promotes Short Name and Hostname to the first two table columns, adds app-wide primary and secondary button styling, a softer app-wide dropdown border, and app-wide per-cell hover tooltips on every table (servers table first), and turns the Notes input on the add/edit dialog into a multi-line area |
| 014 | Fuzzy Search Bar on the Servers Page | needs-architecture | 2026-05-12 | Adds a search input above the servers table that filters visible rows in real time via case-insensitive substring match against each row's concatenated displayed text |
| 015 | Sortable Column Headers on the Servers Table | needs-architecture | 2026-05-12 | Lets the user click a column header to cycle that column through unsorted, ascending, and descending case-insensitive alphabetical sort with a visible arrow indicator, one column at a time |
| 016 | SSL Certificate Check for Servers with Hostnames | done | 2026-05-19 | Adds a button next to Add Server that checks the SSL certificate of every server with a hostname and opens a report dialog listing each host's certificate issue date, expiration date, and a flag for certificates expiring within 30 days |
