---
type: index
status: active
priority: high
tags: [system, index]
aliases: []
summary: "Vault meta: the map of the vault and the Templater templates."
related:
  - "[[Vault Map]]"
created: 2026-11-17
updated: 2026-11-18
---

# 00 System

Meta notes about the vault itself: where things live and the templates new notes start from.

%% Begin Waypoint %%
%% End Waypoint %%

## Key documents

- [[Vault Map]]: the project at a glance, folders, conventions and where to start
- `Templates/`: Note, Decision, Meeting and Runbook templates for Templater

## Status

- Vault created on 2026-11-17, at version 1.3.0.0 of the mod; notes cover it up to 1.3.2.1.
- Folders in use: `01 Architecture`, `04 Operations`, `05 Summaries`, `06 Decisions`.

## All notes

```dataview
TABLE WITHOUT ID file.link AS "Note", summary AS "Summary", updated AS "Updated"
FROM "00 System" AND -"00 System/Templates"
WHERE file.name != this.file.name
SORT file.name ASC
```
