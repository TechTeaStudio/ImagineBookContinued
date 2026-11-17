---
type: runbook
status: active
priority: high
tags: [ops, runbook]
aliases: []
summary: ""
related: []
created: <% tp.date.now("YYYY-MM-DD") %>
updated: <% tp.date.now("YYYY-MM-DD") %>
---

# <% tp.file.title %>

## When to use

The trigger: a failed build or workflow step, an error line in the game log, a crash report.

## Impact

What is broken and who notices.

## Diagnosis

Checks that confirm the cause before acting, as commands that can be pasted.

```bash
python tools/build_local.py --test
```

## Resolution

1. Command, expected outcome, what to do if it fails.
2. ...

## Verification

```bash
python tools/mixin_smoke.py
```

## Links

- [[Build and Test]]
