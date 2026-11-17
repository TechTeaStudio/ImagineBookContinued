package io.github.jumperonjava.imaginebook.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Image tags inside page text, `[url,x,y,width%,height%,rotation]`. The tag is the wire format other
 * players and older versions read, so it stays in the page; these helpers only change how it is shown:
 * the reader hides it, the editor fades it.
 */
public final class ImageTags {
    /** A tag: a link without commas, then two to five numbers. Shared with ImageSerializer. */
    public static final Pattern PATTERN = Pattern.compile("\\[[^,]*?(,[+-]?\\d+(\\.\\d+)?){2,5}\\]");
    /** Alpha of tag text in the editor: about 15 %, faint over a picture but still findable with the cursor. */
    private static final int FADED_ALPHA = 0x26;
    private static final char SECTION = '§';

    public record Span(int start, int end) {}

    public record Piece(String text, boolean tag) {}

    private ImageTags() {}

    public static List<Span> find(String text) {
        List<Span> spans = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            spans.add(new Span(matcher.start(), matcher.end()));
        }
        return spans;
    }

    public static String strip(String text) {
        return PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Splits {@code line}, which starts at {@code offset} in the page, at the borders of {@code tags}.
     * A text piece after a tag starts with the formatting codes active before it, because each piece is
     * drawn on its own and Stendhal's codes would otherwise stop at the tag.
     */
    public static List<Piece> pieces(String line, int offset, List<Span> tags) {
        List<Piece> pieces = new ArrayList<>();
        int position = 0;
        for (Span tag : tags) {
            int start = Math.max(tag.start() - offset, 0);
            int end = Math.min(tag.end() - offset, line.length());
            if (end <= 0 || start >= line.length() || start >= end) continue;
            if (start > position) pieces.add(text(line, position, start));
            pieces.add(new Piece(line.substring(start, end), true));
            position = end;
        }
        if (position < line.length() || pieces.isEmpty()) pieces.add(text(line, position, line.length()));
        return pieces;
    }

    private static Piece text(String line, int start, int end) {
        String prefix = start == 0 ? "" : activeFormatting(line.substring(0, start));
        return new Piece(prefix + line.substring(start, end), false);
    }

    /** The formatting codes in effect at the end of {@code text}: a colour or reset clears the formats before it. */
    public static String activeFormatting(String text) {
        StringBuilder active = new StringBuilder();
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != SECTION) continue;
            char code = Character.toLowerCase(text.charAt(i + 1));
            if (code == 'r') {
                active.setLength(0);
            } else if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                active.setLength(0);
                active.append(SECTION).append(code);
            } else if (code >= 'k' && code <= 'o') {
                active.append(SECTION).append(code);
            }
            i++;
        }
        return active.toString();
    }

    public static int faded(int argb) {
        return (FADED_ALPHA << 24) | (argb & 0xFFFFFF);
    }

    /** {@code page} without its tags, keeping every style; the same object when it has none. */
    public static Component strip(Component page) {
        List<Span> tags = find(page.getString());
        if (tags.isEmpty()) return page;
        MutableComponent out = Component.empty();
        int[] position = {0};
        page.visit((style, text) -> {
            int start = position[0];
            int cut = start;
            for (Span tag : tags) {
                int from = Math.max(tag.start(), start);
                int to = Math.min(tag.end(), start + text.length());
                if (from >= to) continue;
                if (from > cut) out.append(Component.literal(text.substring(cut - start, from - start)).setStyle(style));
                cut = to;
            }
            if (cut < start + text.length()) out.append(Component.literal(text.substring(cut - start)).setStyle(style));
            position[0] += text.length();
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
