# ImagineBook Continued

**Put pictures in your books.** Paste a link into a book and quill, place the picture on the page, move, resize and rotate it, then sign the book. Everyone who has the mod sees the picture. The mod is client-side only, so it works on any server, vanilla ones included.

ImagineBook Continued is a maintained fork of [ImagineBook](https://modrinth.com/mod/imaginebook) by JavaJumper and Riflusso, built on the Minecraft 26.x port by Bratik17078. It brings the mod to **Minecraft 26.3 on Fabric** and makes it work together with **[Stendhal](https://modrinth.com/mod/stendhal)**.

## Features

- **Pictures from the web:** PNG, JPEG and WebP by direct link. Imgur page links are turned into direct image links for you.
- **Pictures from the game:** `res:minecraft:textures/item/diamond.png` shows any texture from the game or from your resource packs.
- **Full placement control:** drag a picture with the mouse, or set X, Y, width, height and rotation in the fields beside the book. Scroll over a field to nudge the value; hold Shift, Ctrl or Alt to move in bigger steps.
- **Stored in the book itself:** each picture is a short tag in the page text, `[link,x,y,width%,height%,rotation]`. Signed books keep their pictures and hide the tags; while you write, tags are faded so they never cover the picture. You can copy a tag between books.
- **Long text paste:** pasting a long text fills the following pages, within the vanilla page and book limits.
- **Works with Stendhal:** previous 26.x builds crashed as soon as a book and quill was opened with Stendhal installed. This fork fixes that, and both mods work side by side.
- **Never stalls on a bad link:** downloads time out, and a broken link is retried twice with a pause, not on every frame.

## How to use

1. Take a book and quill and open it.
2. Press **Add image** and paste a direct link to a picture, for example `https://i.imgur.com/<id>.png`.
3. Click the picture on the page to select it. Drag it, or use the fields and buttons beside the book. **Remove image** deletes it.
4. Sign the book as usual if you want to share it.

Players without the mod see the tag as plain text in its place.

## Compatibility

- Minecraft 26.3, Fabric Loader 0.20.1 or newer, Fabric API, Java 25.
- Client only: install it on your game, not on the server.
- Compatible with Stendhal 1.4.9.

## Privacy

When a page with a picture is shown for the first time, your game downloads that picture straight from the host in the link, so the host sees your IP address, as with any web page. Downloaded pictures are kept in the `imaginebook` folder of your game directory and loaded from there next time; delete the folder to clear them. Nothing else is sent anywhere.

## Licence and source

LGPL-3.0-only, the same licence as the original ImagineBook. The source code of every version is attached to that version on this page as the `-sources.jar` file.

## Credits

- **JavaJumper:** the original ImagineBook
- **Riflusso:** NeoForge port and updates of the original
- **Bratik17078:** the Minecraft 26.2 and 26.3 port and the long text paste
- **webp-imageio** by Sejda: WebP support
- **Tech Tea Studio:** this fork, the Stendhal fix and the download fixes
