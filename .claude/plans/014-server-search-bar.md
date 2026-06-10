---
id: "014"
title: Fuzzy Search Bar on the Servers Page
status: needs-architecture
created: 2026-05-12
updated: 2026-05-12
---

## Summary

Adds a search input above the servers table that filters the visible rows as
the user types. The match is intentionally simple: a case-insensitive
substring check against the displayed text of each row, treating the whole row
as one searchable string spanning every visible column. Typing narrows the
list; clearing the box shows everything again.

## Goals

- Let the user find a server quickly by typing any fragment of any of its
  visible values, without thinking about which column the value lives in.
- Keep the behavior dead simple and predictable: if the typed text appears
  anywhere in any of a row's visible columns, the row shows; otherwise it
  hides.
- Match against what the user actually sees in the table, so that searching
  for words like "Load Balancer" or "Preprocess" works exactly as the user
  expects.

## Acceptance Criteria

- A single-line text input sits above the servers table on the servers page.
  The input is clearly identifiable as a search field (e.g. a placeholder or
  label).
- Typing into the input filters the visible table rows in real time as
  characters are added or removed. There is no separate "Search" button to
  press.
- The match is case-insensitive substring matching. Typing `prep` matches a
  row whose displayed text contains "preprocess", "Preprocess", or "PREPROCESS"
  anywhere in any column.
- The match treats each row as one searchable string formed by concatenating
  the displayed text of all of its visible columns (public IP, pvlan IP,
  short name, hostname, notes, environment, type, hosting provider). Typing
  `Load Bal` shows every server whose Type column renders as "Load Balancer".
- Matching uses the user-visible display strings, not internal names. For
  example, the Environment column matches on "Production", not on its
  internal token form.
- An empty search input shows every row in the table, exactly as if no
  filtering were applied.
- The search filters the rows already loaded in memory. It never re-reads the
  stored servers file.
- Adding, editing, or deleting a server while a search is active leaves the
  filter applied to the resulting list — the user does not need to retype the
  query to see consistent results after a change.

## Forward-Compatibility Note

- This plan must compose cleanly with plan 015 (sortable column headers).
  When both ship, sorting applies to whatever rows the search is currently
  showing — the user sees the filtered rows in sorted order. pm-bot does not
  prescribe the composition; the architect should keep this in mind so the
  two features do not collide.

## Open Questions

- None.

## Architecture

<!-- architect-bot fills this section in -->

## Implementation Notes

<!-- java-coder appends progress notes here as it works through this plan -->
