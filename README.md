# ImagineBook Continued

Images in Minecraft books, for Minecraft 26.3 on Fabric, compatible with [Stendhal](https://modrinth.com/mod/stendhal).

ImagineBook Continued is Tech Tea Studio's maintained fork of [ImagineBook](https://github.com/JumperOnJava/ImagineBook) by JavaJumper and Riflusso, built on the Minecraft 26.x port by [Bratik17078](https://github.com/Bratik17078/ImagineBook). The mod is client-side: you can add images to books on any server, and every player with the mod sees them.

## What it does

- Open a book and quill, press **Add image**, paste a link to a PNG, JPEG or WebP picture, then move, resize and rotate it with the controls under the book.
- The image lives in the page text as a tag, so a signed book keeps it: `[url,x,y,width%,height%,rotation]`. Players without the mod see the tag as plain text.
- `res:<namespace>:<path>` shows a texture from the game or a resource pack instead of a web picture.
- Pasting long text into the book fills the next pages within the vanilla page and book limits.

## What changed in this fork

See [CHANGELOG.md](CHANGELOG.md). In short: no crash with Stendhal, image downloads time out and back off instead of hanging or retrying every frame, one Fabric 26.3 build.

## Build

Requires Java 25.

```bash
./gradlew build
```

The jar and its sources jar land in `build/libs/`. Without Gradle (no 500 MB first download), from an installed MultiMC copy of Minecraft 26.3:

```bash
python tools/build_local.py --test
```

```bash
python tools/mixin_smoke.py
```

The second command checks, without opening a game window, that every Mixin applies with Stendhal installed.

## Releases

The release branch is `product`. A push or pull request to it builds the mod and runs the checks; a `v*` tag also creates a GitHub Release. Every published jar ships with its sources jar.

## Licence and credits

LGPL-3.0-only, see [LICENSE](LICENSE). Same licence as the original mod.

- JavaJumper: original ImagineBook
- Riflusso: NeoForge port and updates
- Bratik17078: Minecraft 26.2 and 26.3 port, book paste
- [webp-imageio](https://github.com/sejda-pdf/webp-imageio) by Sejda: WebP support
- Tech Tea Studio: this fork
