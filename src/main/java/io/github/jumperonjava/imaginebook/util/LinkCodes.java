package io.github.jumperonjava.imaginebook.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How a picture's link is written into the book. Servers with link filters refuse books whose text
 * contains web addresses, and the visible tags since ImagineBook 1.2 put the address in plain sight.
 * So the book stores a code that has no scheme, no domain and no comma:
 * {@code imgur:<id>} for Imgur pictures, {@code u:<base64url of the link>} for any other web link.
 * The game still downloads from the real address; the server never sees it. ImagineBook before
 * Continued 1.3.1.0 cannot read these codes.
 */
public final class LinkCodes {
    public static final String IMGUR = "imgur:";
    public static final String CODED = "u:";
    /** clovi.ru: {@code clovi:<file id>} for a file, {@code clovi:post/<number>} for a post (a page; see PagePreview). */
    public static final String CLOVI = "clovi:";
    private static final String CLOVI_POST = "post/";
    private static final Pattern CLOVI_FILE_LINK = Pattern.compile("^https?://(?:www\\.)?clovi\\.ru/f/([A-Za-z0-9]{4,16})/?$");
    private static final Pattern CLOVI_POST_LINK = Pattern.compile("^https?://(?:www\\.)?clovi\\.ru/post/(\\d{1,12})/?$");
    private static final Pattern CLOVI_FILE_ID = Pattern.compile("^[A-Za-z0-9]{4,16}$");
    private static final Pattern CLOVI_POST_ID = Pattern.compile("^\\d{1,12}$");
    // i.imgur.com/<id>.<ext> or an imgur.com/<id> page; albums and galleries (a/, gallery/) are not single pictures.
    private static final Pattern IMGUR_LINK = Pattern.compile("^https?://(?:i\\.)?imgur\\.com/([A-Za-z0-9]{5,10})(?:\\.[A-Za-z0-9]{2,5})?$");
    private static final Pattern IMGUR_ID = Pattern.compile("^[A-Za-z0-9]{5,10}$");
    private static final Pattern IMGUR_PAGE = Pattern.compile("^https?://imgur\\.com/([A-Za-z0-9]{5,10})/?$");

    private LinkCodes() {}

    /** The text the book stores for a link the player entered. Codes and non-web sources pass through. */
    public static String encode(String link) {
        String trimmed = link.trim();
        Matcher imgur = IMGUR_LINK.matcher(trimmed);
        if (imgur.matches()) return IMGUR + imgur.group(1);
        Matcher cloviFile = CLOVI_FILE_LINK.matcher(trimmed);
        if (cloviFile.matches()) return CLOVI + cloviFile.group(1);
        Matcher cloviPost = CLOVI_POST_LINK.matcher(trimmed);
        if (cloviPost.matches()) return CLOVI + CLOVI_POST + cloviPost.group(1);
        if (!isWebLink(trimmed)) return trimmed;
        return CODED + Base64.getUrlEncoder().withoutPadding().encodeToString(trimmed.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The real address behind a stored link, for the link field and the resolvers. An Imgur code opens
     * as {@code .png}: Imgur serves any picture under any extension, and downloads are decoded by content.
     * A code that does not decode comes back unchanged and resolves to the error picture.
     */
    public static String decode(String stored) {
        if (stored.startsWith(IMGUR)) {
            String id = stored.substring(IMGUR.length());
            return IMGUR_ID.matcher(id).matches() ? "https://i.imgur.com/" + id + ".png" : stored;
        }
        if (stored.startsWith(CLOVI)) {
            String id = stored.substring(CLOVI.length());
            if (id.startsWith(CLOVI_POST) && CLOVI_POST_ID.matcher(id.substring(CLOVI_POST.length())).matches()) {
                return "https://clovi.ru/post/" + id.substring(CLOVI_POST.length());
            }
            return CLOVI_FILE_ID.matcher(id).matches() ? "https://clovi.ru/f/" + id : stored;
        }
        if (stored.startsWith(CODED)) {
            try {
                return new String(Base64.getUrlDecoder().decode(stored.substring(CODED.length())), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return stored;
            }
        }
        return stored;
    }

    /**
     * An imgur.com page of a single picture as its direct link; everything else unchanged. Albums and
     * galleries (imgur.com/a/..., imgur.com/gallery/...) stay pages: the downloader shows their preview
     * picture. Until 1.3.2.0 every imgur.com/&lt;rest&gt; became i.imgur.com/&lt;rest&gt;.png, a dead address for those.
     */
    public static String directImgurLink(String link) {
        Matcher page = IMGUR_PAGE.matcher(link.trim());
        return page.matches() ? "https://i.imgur.com/" + page.group(1) + ".png" : link;
    }

    public static boolean isWebLink(String link) {
        return link.startsWith("https://") || link.startsWith("http://");
    }
}
