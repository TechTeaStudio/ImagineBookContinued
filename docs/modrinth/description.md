# ImagineBook Continued

**Pictures in your books.** Paste a picture link into a book and quill, place the picture on the page and sign the book. Everyone with the mod sees it. Works on any server, with nothing to install there.

## How to use

1. Open a book and quill and press **Add image**.
2. Paste a link to a picture: Imgur, clovi.ru or any direct image link. The picture appears on the page, sized to fit.
3. Drag it with the mouse, or set position, size and rotation with the controls beside the book.
4. Sign the book to share it.

## What this version does better

- **Works with Stendhal.** Older 26.x builds crashed as soon as you opened a book with Stendhal installed.
- **No web addresses in the book.** It stores a short code instead of a link.
- **Clean pages.** The picture's code is hidden in signed books and faded while you write.
- **More sources.** Imgur and clovi.ru links (posts too), any direct link to a PNG, JPEG or WebP picture, web pages that have a preview picture, and textures from the game, such as `res:minecraft:textures/item/diamond.png`.
- **No hold-ups.** A broken or slow link never holds up the other pictures.
- **Hidden links (optional).** A switch under the book writes Imgur and clovi pictures as a caption with an invisible code, so the page text holds no link at all. Use it only where the server allows pictures in books.
- **Long text paste** fills the next pages.

## Good to know

- Minecraft 26.3, Fabric, needs Fabric API.
- Install it on your game only, not on the server.
- Players without the mod see a short code where the picture is. Older ImagineBook versions show an error icon instead of the picture.
- Your game downloads each picture straight from its site, which sees your IP address like any web page does. Pictures are kept in the `imaginebook` folder of your game directory; delete it to clear them.

## Licence and credits

LGPL-3.0-only, like the original. Every version has its source code attached as a `-sources.jar` file.

Based on [ImagineBook](https://modrinth.com/mod/imaginebook) by JavaJumper and Riflusso and on the Minecraft 26.x port by Bratik17078. WebP support by webp-imageio (Sejda). Maintained by Tech Tea Studio.
