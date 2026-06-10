---
id: "002"
title: Plain Makefile Build for vibebot
status: done
created: 2026-04-13
updated: 2026-04-13
---

## Summary

This plan introduces a `Makefile` at the project root that can compile all Java source files, package them into a runnable JAR, and launch the application — all using only the standard JDK toolchain (`javac`, `jar`, `java`). Because the project intentionally uses no third-party dependencies, a dedicated build tool (Maven, Gradle) would add complexity without benefit; a plain `Makefile` is sufficient and keeps the project self-contained.

## Package Structure

No new Java packages are introduced by this plan. The `Makefile` operates on the existing layout:

```
vibebot/
├── Makefile                  # new — build entry point
├── src/
│   └── main/
│       └── java/
│           └── com/example/vibebot/
│               ├── app/      # contains Main.java (entry point)
│               └── ui/       # Swing window and theme classes
└── out/                      # generated at build time, git-ignored
    ├── classes/              # javac output
    └── vibebot.jar           # jar output
```

## Components

| Component | Type | Location | Purpose |
|---|---|---|---|
| `Makefile` | build file | project root | Defines `compile`, `jar`, `run`, and `clean` targets |
| `.gitignore` update | config | project root | Ensures `out/` is excluded from version control |

## Relationships

The `Makefile` has four targets that depend on each other in sequence:

- `compile` — invokes `javac` on all `.java` files under `src/main/java/`, writing `.class` files to `out/classes/`
- `jar` — depends on `compile`; invokes `jar` to produce `out/vibebot.jar` with `Main-Class: com.example.vibebot.app.Main` in the manifest
- `run` — depends on `jar`; invokes `java -jar out/vibebot.jar`
- `clean` — removes the `out/` directory entirely

The default target (when `make` is run with no arguments) is `run`, so a developer can compile, package, and launch in one command.

## Implementation Order

1. `Makefile` — the single deliverable; implement all four targets (`compile`, `jar`, `run`, `clean`) plus the default target alias
2. `.gitignore` update — append `out/` to the project root `.gitignore` (create the file if it does not exist)

## Open Questions

None.

## Implementation Notes

**2026-04-13 — Makefile + .gitignore (plan complete)**
- `Makefile` written at project root with `compile`, `jar`, `run`, `clean` targets and `.DEFAULT_GOAL := run`
- Variables declared at the top (`SRC_DIR`, `OUT_DIR`, `CLASSES`, `JAR_FILE`, `MAIN_CLASS`) — no magic strings inline
- `SOURCES` uses `$(shell find ...)` so new `.java` files are picked up automatically without editing the Makefile
- `compile` target lists `$(SOURCES)` as a prerequisite so Make skips recompilation when sources are unchanged
- All targets declared in `.PHONY` since none produce files matching their target names
- `.gitignore` created at project root with `out/` excluded
