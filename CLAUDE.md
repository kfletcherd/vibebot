# vibebot

## Agents

| Agent | Role | Invoke when | Sample Call |
|---|---|---|---|
| `pm-bot` | Turns ideas into user-readable feature plans | You have a new feature idea or vague requirement | @pm-bot "I want a thing that does X" |
| `architect-bot` | Translates feature plans into technical blueprints | A plan has status `needs-architecture` | @architect-bot "pick up plan-007" |
| `java-coder` | Implements Java code one component at a time | A plan has status `ready` | @java-coder "pick up plan-007" |

## Three-Bot Workflow

The standard flow for building anything new:

1. **Ideate with pm-bot.** Describe your idea (rough is fine). pm-bot will ask one clarifying question if needed, then produce a plan file in `.claude/plans/` with status `needs-architecture`.
2. **Architect with architect-bot.** Hand the plan ID to architect-bot. It will add package structure, component definitions, method signatures, and an implementation order to the plan, then flip the status to `ready`.
3. **Implement with java-coder.** Tell java-coder to pick up the plan by ID. It will read the Architecture section, start at the top of the Implementation Order, and work through it one component per response.
4. **Iterate.** java-coder updates the plan's Implementation Notes as it goes. If scope changes mid-implementation, go back to pm-bot to revise, then re-architect before continuing.

## Shared Files

| File | Owner | Readers |
|---|---|---|
| `.claude/plans/INDEX.md` | pm-bot (creates rows), architect-bot and java-coder (update status) | all agents |
| `.claude/plans/<id>-<slug>.md` | pm-bot (creates), architect-bot (fills Architecture section), java-coder (updates Implementation Notes) | all agents |
| `.claude/plans/archive/<id>-<slug>.md` | java-coder (moves the plan file here via `git mv` when it flips status to `done`) | all agents (read-only for context) |
| `.claude/java-coder-feedback.md` | human (writes) | architect-bot and java-coder (read to respect constraints) |

Active and archived plans both still appear in `.claude/plans/INDEX.md` — archiving is a directory move only, not an index change. The active `.claude/plans/` directory stays focused on plans currently in flight (`needs-architecture`, `ready`, `in-progress`, `blocked`); `.claude/plans/archive/` collects everything that has reached `done`.

## Project Conventions

- All source code is Java 25, standard library only.
- Follow the style rules enforced by the `java-coder` agent.
- Plans live in `.claude/plans/` and are the source of truth for what is being built and why.
- The feedback log at `.claude/java-coder-feedback.md` is the source of truth for accumulated coding lessons.

## Package Layout

Code is organized by feature, not by technical kind. There are no top-level "kind" packages (e.g. no top-level `ui/`, `io/`, `model/`); every class lives under either a feature package or `util/`.

- **`<feature>/`** — one package per feature (e.g. `app`, `server`, `settings`). A feature owns everything it needs:
  - **`<feature>/`** (top level of the feature) — the feature's bootstrap or shared types.
  - **`<feature>/model/`** — domain types, persistence, and business logic for the feature. Optional; only present when the feature has enough model surface to justify the split (e.g. `server/model/`).
  - **`<feature>/ui/`** — the feature's Swing screens, dialogs, table models, etc. Optional; present whenever the feature has UI (e.g. `server/ui/`, `settings/ui/`, `app/ui/`).
- **`util/`** — domain-agnostic, broadly reusable helpers. Code in `util` and its subpackages **must not** depend on any feature package (`app`, `server`, `settings`). The purpose is documented in `src/util/package-info.java`.
  - **`util/<area>/`** — a sub-area of `util` grouping related helpers (e.g. `util/csv`, `util/ui/{pane,table,theme,nav}`).
  - **`util/ui/`** — generic Swing widgets and theming that any feature's `<feature>/ui/` can consume.

When deciding where new code goes, ask:
1. Does it depend on any feature package? If yes, it belongs in that feature.
2. Is it a Swing widget specific to one feature? Goes in `<feature>/ui/`.
3. Is it reusable across features with no domain coupling? Goes in `util/` (or `util/ui/` if it's a widget).
