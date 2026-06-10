# vibebot

A local desktop application built in Java 25 using only the standard library.

## Requirements

- Java 25 JDK on your `PATH`
- `make` (ships with macOS via Xcode Command Line Tools)

## Build & Run

### `make`

Compiles, packages, and launches the application in one step. This is the default target.

```sh
make
```

### `make compile`

Compiles all Java source files under `src/main/java/` and writes `.class` files to `out/classes/`. Skips recompilation if no sources have changed since the last run.

```sh
make compile
```

### `make jar`

Runs `compile` if needed, then packages everything into a runnable JAR at `out/vibebot.jar`.

```sh
make jar
```

### `make run`

Runs `compile` and `jar` if needed, then launches the application with `java -jar`.

```sh
make run
```

### `make clean`

Removes the generated `out/` directory. Run this to force a full recompile from scratch.

```sh
make clean
```

## Development

This project uses two local Claude Code agents to manage planning and implementation:

- **`pm-bot`** — turns ideas into structured plans stored in `.claude/plans/`
- **`java-coder`** — implements Java code one component at a time, guided by those plans
