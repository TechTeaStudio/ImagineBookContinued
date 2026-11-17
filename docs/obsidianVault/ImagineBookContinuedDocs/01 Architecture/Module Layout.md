---
type: note
status: active
priority: high
tags: [architecture, mixin, fabric]
aliases: [Package layout, Source layout]
summary: "What each package and class under io.github.jumperonjava.imaginebook does: entry point, tag parsing, image resolvers and downloader, the viewer and editor Mixins, utilities."
related:
  - "[[Stendhal Compatibility]]"
  - "[[Build and Test]]"
  - "[[Single Fabric 26.3 target]]"
created: 2026-11-17
updated: 2026-11-17
---

# Module Layout

## Summary

All code lives in one Fabric source set under `src/main/java/io/github/jumperonjava/imaginebook`, the package name kept from upstream. The mod reads image tags from book page text, resolves each tag to a texture (downloaded, cached or from a resource pack) and draws it through Mixins into the vanilla book viewer and book editor. Seven Mixins are registered in `src/main/resources/imaginebook.mixins.json`, all client-side, with `defaultRequire: 1` so an injection that does not apply is an error.

## Context

The image format is plain page text, so a book stays valid for players without the mod. A tag is `[url,x,y,width%,height%,rotation]`; `x` and `y` are required, one size number sets both width and height, and the URL part chooses a resolver by its scheme (`http:`, `https:` or `res:` followed by a resource id, `res:<namespace>:<path>`). `ImageData.getImage()` splits the URL at the first colon and hands the rest to the resolver.

## Details

### Root package

| Class | Role |
|---|---|
| `Imaginebook` | Shared state. `init()` creates the cache folder `<game dir>/imaginebook` and registers the render pipeline. Holds the resolver registry by scheme (`http`, `https`, `res`; an unknown scheme gives the error image) and `fixImgurLink`, which turns `https://imgur.com/<id>` into `https://i.imgur.com/<id>.png`. |
| `ImageSerializer` | `parseSafeModeImages(page)` finds tags with a regex and builds `ImageData`. The binary `serialize`/`deserializeImageMetadata` pair is the legacy format: Base64 on the last line of a page exactly 1023 characters long (`Imaginebook.LENGTH`), still read by the viewer for old books. |
| `Config` | Gson file `<config dir>/imaginebook.json`, one field `imgurAccept` (false by default). |

### `image/`

- `ImageData`: URL, `x`, `y`, width and height fractions, rotation. `bookString()` writes the tag back; `renderImage()` translates, rotates around the image centre and scales.
- `Image`: a texture-backed drawable. `BookDrawable`: the drawable interface both images and placeholders implement.

### `resolvers/`

- `Resolver`: `resolve(path)` returns a `BookDrawable`.
- `UrlResolver`: one instance per scheme, all sharing one `AsyncImageDownloader`.
- `TextureResolver`: parses the `res:` path as an `Identifier`; an invalid id gives the error image.
- `AsyncImageDownloader`: one daemon thread ("ImagineBook image downloader"). Caches files under `<game dir>/imaginebook/<host>/<path hash>.cache`, decodes with `ImageIO` (WebP through the bundled `webp-imageio`) and re-encodes to PNG. Connect timeout 10 s, read timeout 15 s, a User-Agent header. Shows placeholder textures while downloading or on error (`downloading.png`, `not_loaded.png`, `size_error.png`, `invalid_url.png`).
- `RetryPolicy`: a failed URL waits 30 s times the number of failures and is given up after 3 failures until the game restarts. Pinned by `RetryPolicyTest`.

### `mixin/`

| Mixin | Target | Role |
|---|---|---|
| `BookScreenMixin` | `BookViewScreen` | The viewer. Parses every page on construction and on `setBookAccess`, draws the images at the head of `extractRenderState` so vanilla text and controls draw on top. |
| `NewBookEditScreenMixin` | `BookEditScreen` | The editor. Parses pages after the constructor, implements `BookEditScreenHooks.imaginebook$afterInit()` which adds the URL field, Add and Remove buttons and the x, y, width, height and rotation controls, handles paste in `keyPressed` through `BookPaste`, and draws in `extractRenderState`. Every use of its widgets tolerates null. |
| `ScreenInitMixin` | `Screen` | Calls `imaginebook$afterInit()` right after `Screen.init(II)V` and `Screen.rebuildWidgets()V` have called `init()`. See [[Stendhal Compatibility]]. |
| `MinecraftClientMixin` | `net.minecraft.client.gui.Gui` | Despite the name, hooks `setScreen`: keeps the editor open while `Imaginebook.cancelledFinalize` is set. `updateFields()` in the editor never sets the flag at present (its error list is always empty), so this path is dormant. |
| `MultiLineEditBoxAccessor` | `MultiLineEditBox` | Accessor for `textField`. |
| `MultilineTextFieldAccessor` | `MultilineTextField` | Accessors for `selectCursor` and `width`, used by paste. |
| `RenderPipelineAccessor` | `RenderPipelines` | Reads `GUI_TEXTURED_SNIPPET` and invokes `register`. |

### `util/`

- `BookPaste`: plans a paste before the book changes: replaces the selection, splits the result over pages within the character limit and the page width, prefers a word or line boundary, keeps surrogate pairs whole, and refuses (empty result) when the pages run out. Pinned by `BookPasteTest`.
- `ImageRenderPipeline`: registers `imaginebook:pipeline/gui_textured_nocull`, the GUI textured pipeline without back-face culling, so mirrored images still draw. The Mixin smoke mod asserts this.
- `VersionFunctions`: thin wrappers over the GUI pose stack and texture drawing.
- `DeletedImageData` (writes an empty tag, used by Remove) and `FileReadWrite` (text file read and write, used by `Config`).

### `compat/` and `platform/`

- `compat/BookEditScreenHooks`: the one-method interface between `ScreenInitMixin` and the editor Mixin.
- `platform/PlatformEntrypoint`: the Fabric `client` entrypoint, calls `Imaginebook.init()`. `platform/PlatformPaths`: game and config folders from `FabricLoader`.

### Outside `src/main`

- `src/smoke/`: a development-only mod for the windowless Mixin check, never shipped (see [[Build and Test]]).
- `src/test/`: JUnit 5 tests: `BookPasteTest`, `RetryPolicyTest`, `StendhalCompatSourceTest`, `CiWorkflowTest`, `VersionDriftTest`.
- `src/main/resources/assets/imaginebook/`: icon, placeholder textures, `en_us`, `ru_ru` and `uk_ua` language files.

## Links

- [[Stendhal Compatibility]]
- [[Build and Test]]
- [[06 Decisions]]
