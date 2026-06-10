---
id: "015"
title: Sortable Column Headers on the Servers Table
status: needs-architecture
created: 2026-05-12
updated: 2026-05-12
---

## Summary

Lets the user sort the servers table by clicking a column header. Clicking
cycles that column through three states — unsorted, ascending, descending — and
shows an arrow in the header so the current state is obvious. Only one column
is sorted at a time. Sorting is plain case-insensitive alphabetical order on
the text the user already sees in that column.

## Goals

- Give the user a one-click way to reorder the table by any column.
- Make the current sort state visible at a glance, so the user always knows
  which column is sorted and in which direction.
- Keep the sorting rules simple and predictable: alphabetical, case-insensitive,
  on the same display strings the user sees in the table.

## Acceptance Criteria

- Clicking a column header cycles that column through three sort states in
  this order: unsorted, ascending, descending, then back to unsorted.
- At most one column is sorted at a time. Clicking a different column resets
  the previously-sorted column to unsorted and starts the newly-clicked column
  at ascending.
- A visible sort indicator (e.g. an up arrow for ascending, a down arrow for
  descending) appears in the header of the currently-sorted column. No
  indicator appears on any other column header, and no indicator appears on
  any column when the table is unsorted.
- Sorting is alphabetical and case-insensitive on the displayed text of the
  column.
- For the Type column, which renders as a comma-joined list of values, the
  sort compares the entire displayed string as-is. No special multi-value
  handling beyond that.
- When the table is in the unsorted state, rows appear in the same order they
  would have appeared without this feature (i.e. the underlying stored order).
- Sorting happens entirely in memory over the rows currently being shown. It
  never re-reads the stored servers file.
- Adding, editing, or deleting a server while a sort is active leaves the
  sort applied — the resulting list is reshown in the current sort order
  without the user needing to click the header again.

## Forward-Compatibility Note

- This plan must compose cleanly with plan 014 (fuzzy search bar). When both
  ship, the sort applies to whatever rows the search is currently showing —
  i.e. the table sorts the filtered rows, not the full list. pm-bot does not
  prescribe the composition; the architect should keep this in mind so the
  two features do not collide.

## Open Questions

- None.

## Architecture

<!-- architect-bot fills this section in -->

## Implementation Notes

<!-- java-coder appends progress notes here as it works through this plan -->
