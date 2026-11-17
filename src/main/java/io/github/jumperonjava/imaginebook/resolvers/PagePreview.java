package io.github.jumperonjava.imaginebook.resolvers;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A link to a web page instead of a picture (a clovi.ru post, an Imgur page): the page names its picture
 * in its preview tags ({@code og:image}, else {@code twitter:image}), and the downloader follows that
 * address once. Only http and https addresses are followed.
 */
public final class PagePreview {
    private static final Pattern META = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE = Pattern.compile("([a-zA-Z:_-]+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)')");
    private static final String[] KEYS = {"og:image", "og:image:secure_url", "og:image:url", "twitter:image"};

    private PagePreview() {}

    public static Optional<URI> find(String html, URI page) {
        for (String key : KEYS) {
            Matcher meta = META.matcher(html);
            while (meta.find()) {
                String name = null;
                String content = null;
                Matcher attribute = ATTRIBUTE.matcher(meta.group());
                while (attribute.find()) {
                    String value = attribute.group(3) != null ? attribute.group(3) : attribute.group(4);
                    switch (attribute.group(1).toLowerCase()) {
                        case "property", "name" -> name = value;
                        case "content" -> content = value;
                        default -> { }
                    }
                }
                if (!key.equalsIgnoreCase(name) || content == null || content.isBlank()) continue;
                Optional<URI> address = webAddress(page, decodeEntities(content.trim()));
                if (address.isPresent()) return address;
            }
        }
        return Optional.empty();
    }

    private static Optional<URI> webAddress(URI page, String content) {
        try {
            URI address = page.resolve(content);
            String scheme = address.getScheme();
            return "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme) ? Optional.of(address) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String decodeEntities(String text) {
        return text.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">");
    }

    /** PNG, JPEG, GIF, WebP or BMP by their first bytes; anything else is treated as a page. */
    public static boolean looksLikeImage(byte[] bytes) {
        if (bytes.length >= 4 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') return true;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) return true;
        if (bytes.length >= 4 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') return true;
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return true;
        return bytes.length >= 2 && bytes[0] == 'B' && bytes[1] == 'M';
    }
}
