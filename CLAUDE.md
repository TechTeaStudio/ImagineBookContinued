# CLAUDE.md

Guidance for Claude Code in this repository.

## Session start

Read the global instructions first: `C:\Users\Yaroslav\.claude\CLAUDE.md` (commit format, odometer versions, never push, never open pull requests, no `---` rules in Markdown, `gh` only after per-action confirmation).

## Guardrails (read before changing anything)

- **Stendhal must keep working.** Stendhal (`stendhal.mixins.json`, `BookEditScreenMixin`) injects at the head of `BookEditScreen.init()` and cancels it. Anything ImagineBook injects into `BookEditScreen.init` never runs then, and the 1.2.9 fork crashed on opening a book and quill (`this.removeButton` is null). Editor widgets are created from `ScreenInitMixin`, after `Screen.init(II)V` and `Screen.rebuildWidgets()V` have called `init()`, through `compat/BookEditScreenHooks`. Never add `@Inject(method = "init"...)` to `NewBookEditScreenMixin`; `StendhalCompatSourceTest` fails on it. Every use of an editor widget must tolerate `null`, and so must the page buttons, which Stendhal may assign itself.
- **`defaultRequire` is 1.** A Mixin that misses its target crashes the game at class load. After touching a Mixin, run `python tools/mixin_smoke.py` (windowless, loads Stendhal too); CI runs `gradlew runMixinSmoke`.
- **LGPL-3.0-only.** The code comes from ImagineBook (JavaJumper, Riflusso) and Bratik17078's 26.x port. Keep `LICENSE`, the credits in `README.md`, `fabric.mod.json` (`authors`, `contributors`) and the Modrinth page. The repository is private, so every published jar goes out with its `-sources.jar` (both jars carry `LICENSE`).
- **Downloads are bounded.** `AsyncImageDownloader` uses 10 s connect and 15 s read timeouts, a User-Agent, and `RetryPolicy` (3 attempts, 30 s then 60 s). Without them one hanging host stalled every image and a failed image was fetched again every frame.
- **The page tag format is a wire format.** `[url,x,y,width%,height%,rotation]` is stored in book text that other players and older versions read (`ImageSerializer`, regex in `util/ImageTags.PATTERN`). Do not change it. Since 1.3.1.0 the link part is a code from `util/LinkCodes` (`imgur:<id>`, `u:<base64url>`), so book text never contains a web address that server link filters refuse; `CodedLinkResolver` downloads the real address, and plain `http(s)` links still resolve. Codes are part of the wire format too: never rename `imgur:` or `u:`. Tags stay in the text; only their display changes: the reader strips them (`BookScreenMixin`, `BookViewScreen.visitText`), the editor fades them (`MultiLineEditBoxMixin`: three `GuiGraphicsExtractor.text` calls by ordinal, line bounds read from `StringView.beginIndex/endIndex` because `StringView` is package-private). Do not hide them fully in the editor: invisible characters get half-deleted with Backspace.
- **Controls stay clear of other mods.** `util/ControlPanelLayout` puts the image controls in the first free spot beside the book, avoiding every widget already on the screen and the bottom band from `height - 54`, where Stendhal puts its formatting toolbar and symbol buttons. The 1.3.0.2 bottom rows covered that toolbar; they remain only as the fallback for windows too narrow for a side panel.
- **One target:** Minecraft 26.3, Fabric Loader 0.20.1, Java 25. Minecraft 26.x runs with Mojang names, so there are no mappings. The Stonecutter, NeoForge and 26.2 builds were dropped and stay in git history.

## Layout

```
VERSION                     single version source (odometer, one digit per part)
build.gradle, smoke.gradle  Fabric Loom build; smoke.gradle adds the windowless Mixin check
src/main/java/.../imaginebook/
  mixin/                    BookScreenMixin (viewer), NewBookEditScreenMixin (editor), ScreenInitMixin, accessors
  compat/                   BookEditScreenHooks: the editor hook Stendhal cannot cancel
  resolvers/                AsyncImageDownloader, RetryPolicy, Url/Texture resolvers
  image/, util/             image model and rendering, BookPaste, ImageRenderPipeline
src/main/resources/         fabric.mod.json, imaginebook.mixins.json, assets (icon, lang en/ru/uk, textures)
src/smoke/                  development-only mod that loads the patched classes and exits
src/test/java/              JUnit 5 tests
tools/build_local.py        Gradle-free build and tests
tools/mixin_smoke.py        Gradle-free Mixin check with Stendhal
tools/ingame_test/          optional: drives a real client window through the book editor
docs/obsidianVault/         project vault (studio conventions)
```

## Build and test

CI and anyone with a fast connection:

```bash
./gradlew build
./gradlew runMixinSmoke -x downloadAssets
```

The owner's internet is metered: the first Gradle + Loom build downloads about 500 MB. The local route compiles with ECJ against the MultiMC copy of Minecraft 26.3 (`MULTIMC_ROOT`, default `D:/UslessTrash/MultiMC`) and downloads about 6 MB once into `tools/.cache/`:

```bash
python tools/build_local.py --test
python tools/mixin_smoke.py
```

`tools/ingame_test/run.py` opens a game window; on the owner's Radeon 780M the driver sometimes crashes natively while loading, so the owner tests in game himself. Do not run it unless asked.

## Versioning and commits

- `VERSION` feeds `build.gradle`, `fabric.mod.json` (`${version}`) and `tools/build_local.py`; the jar version is `<VERSION>+26.3`. `VersionDriftTest` checks that the newest `## ` heading in `CHANGELOG.md` equals `VERSION`.
- Bump `VERSION` and open the CHANGELOG entry before committing. Commit message: `v<VERSION> <Short description>`, one line. Never push unless asked.

## Release flow

`product` is the release branch; day-to-day work happens on `main`. `.github/workflows/build.yml` runs on push and pull request to `product` and on `v*` tags. `CiWorkflowTest` holds the workflow and the build scripts to each other.

A release is a `v<VERSION>` tag on `product` (`git tag v<VERSION> product`, then push the tag, only when the owner asks). CI then runs `tools/publish_modrinth.py`, which uploads the jar and its sources jar to Modrinth (project slug `imaginebook-continued`, Fabric, 26.3, client only, Fabric API required, Stendhal optional) with the `CHANGELOG.md` section as the changelog, and creates a GitHub Release.

- Publishing is tag-only on purpose: every Modrinth version notifies followers, so a CI or docs bump must not publish.
- The script refuses a tag that does not match `VERSION`, an empty changelog section or a missing sources jar, and skips a version already on Modrinth, so re-running the job is safe.
- Dependency IDs are hard-coded in the script (`P7dR8mSH` Fabric API, `BWloQ9jK` Stendhal) because the version API takes IDs, not slugs.
- The token is the repository secret `MODRINTH_TOKEN` (scopes Create versions, Read projects, Read versions). Never print it, never ask the owner to paste it into the chat.
- `python tools/publish_modrinth.py --dry-run` prints what would be sent, without network or token.
