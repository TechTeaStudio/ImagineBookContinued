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
    // i.imgur.com/<id>.<ext> or an imgur.com/<id> page; albums and galleries (a/, gallery/) are not single pictures.
    private static final Pattern IMGUR_LINK = Pattern.compile("^https?://(?:i\\.)?imgur\\.com/([A-Za-z0-9]{5,10})(?:\\.[A-Za-z0-9]{2,5})?$");
    private static final Pattern IMGUR_ID = Pattern.compile("^[A-Za-z0-9]{5,10}$");

    private LinkCodes() {}

    /** The text the book stores for a link the player entered. Codes and non-web sources pass through. */
    public static String encode(String link) {
        String trimmed = link.trim();
        Matcher imgur = IMGUR_LINK.matcher(trimmed);
        if (imgur.matches()) return IMGUR + imgur.group(1);
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
        if (stored.startsWith(CODED)) {
            try {
                return new String(Base64.getUrlDecoder().decode(stored.substring(CODED.length())), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return stored;
            }
        }
        return stored;
    }

    public static boolean isWebLink(String link) {
        return link.startsWith("https://") || link.startsWith("http://");
    }
}
