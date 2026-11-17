---
type: note
status: active
priority: high
tags: [architecture, mixin, compatibility, stendhal]
aliases: [Stendhal crash, removeButton crash, Stendhal toolbar overlap]
summary: "ImagineBook with Stendhal: the editor widgets are added from Screen.init because Stendhal cancels BookEditScreen.init() (1.3.0.0), and the image controls sit in a panel beside the book because Stendhal's toolbar fills the bottom band (1.3.0.3). Fixes, null tolerance and the checks that pin them."
related:
  - "[[Hook Screen.init instead of BookEditScreen.init]]"
  - "[[Fade tags in the editor, hide them in the reader]]"
  - "[[Module Layout]]"
  - "[[Build and Test]]"
  - "[[v1.3.0.0 First release of the fork]]"
  - "[[v1.3.0.3 Tags faded, controls beside the book]]"
created: 2026-11-17
updated: 2026-11-17
---

# Stendhal Compatibility

## Summary

Stendhal, a book editor mod, replaces the vanilla book and quill editor by cancelling `BookEditScreen.init()` at its head. ImagineBook used to add its editor widgets at the tail of that same method, so with Stendhal installed the widgets were never created and the game crashed on the first frame of a book and quill. Since 1.3.0.0 the widgets are added from `Screen`, right after it has called `init()`, whoever built the screen, and every use of them tolerates their absence. The owner confirmed this in game with Stendhal 1.4.9 on 2026-11-17. Since 1.3.0.3 the image controls also keep out of the bottom band, where Stendhal puts its formatting toolbar (see [[#Layout and the bottom band]]).

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

## Layout and the bottom band

### Where Stendhal puts its controls

Read from Stendhal 1.4.9's bytecode with `javap`:

- Formatting toolbar and symbol buttons: centred, from `y = height - 50` down to the bottom edge.
- Book actions: a column at the top left.
- Page actions, Done and Sign: a column at the top right.

### What went wrong up to 1.3.0.2

ImagineBook's image controls (URL field, Add, Remove, position, size and rotation) filled two rows along the bottom edge, from `height - 46`. With Stendhal they covered its formatting toolbar. The owner found this in game on 2026-11-17 with Stendhal 1.4.9.

### The fix: a panel beside the book

`util/ControlPanelLayout`, called from `imaginebook$afterInit()` in `NewBookEditScreenMixin`:

- Collects the bounds of every `AbstractWidget` already on the screen. `imaginebook$afterInit()` runs after whoever built the screen, so Stendhal's buttons are among them.
- The panel has 5 rows of 20 px with 4 px gaps (116 px) and is up to 200 px wide: Add and Remove, the URL, x and y, width and height, rotation.
- `place()` tries left of the 192 px book first, then right of it, scanning down from the top, and takes the first spot where the panel, padded by 2 px, touches no widget and ends above the bottom band (`BOTTOM_BAND = 54`: Stendhal's toolbar from `height - 50` plus a margin).
- A side narrower than 160 px is not used, so the panel needs a GUI width of at least 528. When nothing fits, `bottomRows()` gives the old two rows as a fallback for narrow windows.
- On a vanilla screen the panel takes the top left corner. With Stendhal it goes left of the book, under Stendhal's book actions column and above the toolbar.

### What pins it

- `ControlPanelLayoutTest` (`src/test/java/.../util/ControlPanelLayoutTest.java`) models Stendhal 1.4.9's screen at 929 x 590: the panel goes left, below Stendhal's column and above the band, and no slot overlaps a widget. It also checks the vanilla top left corner, that slots stay inside the panel and do not overlap, the bottom rows fallback at 400 x 240, and that a side under the minimum width is not used.
- `tools/mixin_smoke.py` with Stendhal passed for 1.3.0.3 and again for 1.3.1.0. No in-game check of the layout is recorded yet.

### Related: faded tags

The editor fade in `MultiLineEditBoxMixin` draws a line in pieces; a text piece after a tag starts with the formatting codes active before it, so Stendhal's formatting does not stop at a tag. See [[Fade tags in the editor, hide them in the reader]].

### Layout rules

- Never place editor controls at fixed coordinates near the bottom edge; give them a slot in `ControlPanelLayout` and keep `ROWS` in step.
- If Stendhal moves its controls, read them again with `javap` and update `BOTTOM_BAND` and the model in `ControlPanelLayoutTest`.

## Links

- [[Hook Screen.init instead of BookEditScreen.init]]
- [[Fade tags in the editor, hide them in the reader]]
- [[Module Layout]]
- [[Build and Test]]
- [[v1.3.0.0 First release of the fork]]
- [[v1.3.0.3 Tags faded, controls beside the book]]
