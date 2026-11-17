---
type: note
status: active
priority: high
tags: [architecture, mixin, compatibility, stendhal]
aliases: [Stendhal crash, removeButton crash]
summary: "Why the editor widgets are added from Screen.init instead of BookEditScreen.init: Stendhal cancels BookEditScreen.init() at its head, which crashed ImagineBook. Fix, null tolerance and the checks that pin it."
related:
  - "[[Hook Screen.init instead of BookEditScreen.init]]"
  - "[[Module Layout]]"
  - "[[Build and Test]]"
  - "[[v1.3.0.0 First release of the fork]]"
created: 2026-11-17
updated: 2026-11-17
---

# Stendhal Compatibility

## Summary

Stendhal, a book editor mod, replaces the vanilla book and quill editor by cancelling `BookEditScreen.init()` at its head. ImagineBook used to add its editor widgets at the tail of that same method, so with Stendhal installed the widgets were never created and the game crashed on the first frame of a book and quill. Since 1.3.0.0 the widgets are added from `Screen`, right after it has called `init()`, whoever built the screen, and every use of them tolerates their absence.

## Context

### The crash

Opening a book and quill with Stendhal installed threw a `NullPointerException` in `extractRenderState`:

```text
Cannot assign field "visible" because "this.removeButton" is null
```

### The cause

- Stendhal's `BookEditScreenMixin` injects at `HEAD` of `BookEditScreen.init()` and cancels it to build its own editor.
- ImagineBook's editor Mixin had an `@Inject` at `TAIL` of the same `init()`. A cancelled method never reaches its tail, so that injection never ran.
- `extractRenderState` then set `removeButton.visible` on a field that was still null.

## Details

### The fix

`mixin/ScreenInitMixin.java` targets `net.minecraft.client.gui.screens.Screen`:

- It injects into both callers of `init()`: `init(II)V` on first open and `rebuildWidgets()V` on resize (which clears the widgets and calls `init()` again).
- The injection point is the `INVOKE` of `Screen.init()V` with `shift = At.Shift.AFTER`, so it runs after `init()` has returned, whether vanilla or Stendhal built the screen.
- If the screen implements `compat/BookEditScreenHooks`, it calls `imaginebook$afterInit()`. `NewBookEditScreenMixin` implements that interface, so at runtime only `BookEditScreen` answers.
- `imaginebook$afterInit()` in `NewBookEditScreenMixin` adds the URL field, the Add and Remove buttons and the position, size and rotation controls, as the old tail injection did.

### Null tolerance

Another mod may build the screen differently, so nothing assumes the widgets exist:

- `extractRenderState` shows or hides `removeButton`, the six fields and the five control buttons only when `removeButton` is not null; the images themselves are drawn either way.
- `mouseClicked` skips a null `forwardButton` or `backButton`: Stendhal may build the screen without the vanilla page buttons (CHANGELOG 1.3.0.0: a missing page button no longer crashes a click).
- The control hover checks run only when `xPosButton` is not null; `updateFields()` returns early when `urlField` is null.

### What pins it

| Check | Where | What it proves |
|---|---|---|
| `StendhalCompatSourceTest` | `src/test/java/.../StendhalCompatSourceTest.java`, runs in every build | The editor Mixin has no `@Inject` into `init` and implements `BookEditScreenHooks`; `ScreenInitMixin` targets `init(II)V` and `rebuildWidgets()V` with `shift = At.Shift.AFTER`; it is registered in `imaginebook.mixins.json`. |
| Local Mixin smoke | `python tools/mixin_smoke.py` | With Fabric API and Stendhal in the mods folder, every Mixin applies (`defaultRequire: 1` turns a missed injection into an error). `--no-stendhal` runs it without Stendhal. |
| CI Mixin smoke | `./gradlew runMixinSmoke` in `.github/workflows/build.yml` | Every Mixin applies against Minecraft 26.3 with Fabric API. Stendhal is not on the Gradle classpath, so CI does not load it. |
| In-game test | `python tools/ingame_test/run.py` (optional, opens a window) | A real client with Stendhal opens a book and quill, selects an image, resizes the screen and exits with PASSED. |

See [[Build and Test]] for how to run each one.

### Rules for future changes

- Never add editor widgets from an injection into `BookEditScreen.init()`; add them in `imaginebook$afterInit()`.
- Any new widget field is nullable: check it before every use.
- After touching `BookEditScreen` Mixins, run `tools/mixin_smoke.py` with Stendhal before release.

## Links

- [[Hook Screen.init instead of BookEditScreen.init]]
- [[Module Layout]]
- [[Build and Test]]
- [[v1.3.0.0 First release of the fork]]
