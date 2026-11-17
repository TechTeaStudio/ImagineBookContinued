# Changelog

ImagineBook Continued is a fork of [ImagineBook](https://github.com/JumperOnJava/ImagineBook) by JavaJumper and Riflusso, built on the Minecraft 26.x port by [Bratik17078](https://github.com/Bratik17078/ImagineBook). Newest version first; every heading is the content of `VERSION` at release.

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
