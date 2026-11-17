# Changelog

ImagineBook Continued is a fork of [ImagineBook](https://github.com/JumperOnJava/ImagineBook) by JavaJumper and Riflusso, built on the Minecraft 26.x port by [Bratik17078](https://github.com/Bratik17078/ImagineBook). Newest version first; every heading is the content of `VERSION` at release.

## 1.3.1.1

- Fixed: text in square brackets on a page, such as `[1]`, broke pictures. A picture after it on the same line did not show, and moving or editing a picture could rewrite that text instead of the picture's tag. Only real image tags count now, and a tag never runs across lines.
- A new picture starts on the page's text area and, once it has loaded, shrinks to fit it. 100 % is the picture's own pixel size, so a photo used to cover the whole book. Pictures already in books keep their size.
- Fixed: the log called the download size limit 1 MB; it is 8 MB.

## 1.3.1.0

- Books no longer contain web addresses, so servers that filter links accept them. A pasted Imgur link is stored as `imgur:<id>`, any other link as `u:` and its address in base64url; the game still downloads from the real address. The link field keeps showing the real link.
- Books with plain `https://` links keep working.
- ImagineBook versions before 1.3.1.0 (the original mod and other 26.x ports included) cannot read the new codes and show the error picture for them.
- Fixed: a link over 255 characters showed the raw key `imaginebook.error.too_long` instead of the message.

## 1.3.0.3

- Image tags no longer cover the picture: a signed book hides them, and the book and quill fades them to a faint trace you can still find with the cursor.
- The image controls no longer cover Stendhal's formatting toolbar. They moved from the bottom edge to a panel beside the book, in the first spot free of other buttons; only a window too narrow for that keeps the old bottom rows.

## 1.3.0.2

First version published on Modrinth. The mod is the same as 1.3.0.0, which fixed, compared with the 26.x port of ImagineBook 1.2.9:

- the crash when opening a book and quill with Stendhal installed;
- image downloads that never timed out and blocked every later image;
- broken image links that were downloaded again on every frame.

Release tooling: fixed the CI Mixin check, which found the smoke mod's `fabric.mod.json` twice and stopped 1.3.0.1 before it was published.

## 1.3.0.1

- The mod itself is unchanged from 1.3.0.0.
- Releases are now published to Modrinth automatically from a version tag, each with its sources jar.

## 1.3.0.0

First release of the fork, for Minecraft 26.3 on Fabric.

- Fixed: opening a book and quill crashed the game when Stendhal was installed (`Cannot assign field "visible" because "this.removeButton" is null`). Stendhal cancels `BookEditScreen.init()` at its head, so the editor widgets added at the tail of that method were never created. They are now added after `Screen` has called `init()`, whoever built the screen, and every use of them tolerates their absence.
- Fixed: a page button missing from a screen built by another mod no longer crashes a mouse click.
- Fixed: image downloads had no timeout, so one hanging host blocked every later image. Connections now time out after 10 s (connect) and 15 s (read) and send a User-Agent.
- Fixed: a failed image was downloaded again on every frame the page stayed open. Retries now wait 30 s, then 60 s, and stop after 3 failures until the game restarts.
- Fixed the English wording of the "URL is longer than 255 characters" error.
- New mod icon.
- Removed the 1.20 to 1.21 Stonecutter build and the NeoForge and 26.2 targets from this fork; it ships the Fabric 26.3 build only. They remain in the git history.
- Removed unused textures (`_error.png`, `_error - Copy.png`, `png_error.png`, `transparent.png`).

## 1.2.9 (upstream and the 26.x port)

- Ported to Minecraft 26.2 and 26.3 (Bratik17078).
- Pasting into the book text area replaces the selection and flows overflow onto new pages within the vanilla page and book limits (Bratik17078).
- Fixed crash related to a function not available on Java 17 (upstream).
