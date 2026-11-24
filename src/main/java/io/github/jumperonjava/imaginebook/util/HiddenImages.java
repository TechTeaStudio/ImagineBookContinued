package io.github.jumperonjava.imaginebook.util;

import io.github.jumperonjava.imaginebook.image.ImageData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Hidden pictures: a visible caption followed by an invisible code, so the page text holds no link,
 * no domain and no tag at all. Servers with link filters refused books even with link codes (1.3.x).
 *
 * The code is binary written with two characters a vanilla server keeps (StringUtil.isAllowedChatCharacter)
 * and a player without the mod does not see: U+200C, zero width in the vanilla font
 * (assets/minecraft/font/include/space.json), and the plain space. Layout after the marker, MSB first:
 * version 3 bits, caption length 7, source 3 (1 imgur, 2 clovi file, 3 clovi post), id length 5,
 * id 6 bits per char (base62), x 10 and y 10 (two's complement, clamped to -512..511), width % 10 and
 * height % 10 (clamped to 0..1023), rotation 9 (0..359), CRC-8 (poly 0x07) over the bits before it.
 * An Imgur picture takes about 120 characters, two blank-looking book lines. Only sources with a short
 * id fit; other links stay visible tags. This is a wire format: never change a field without a version.
 */
public final class HiddenImages {
    public static final char ZERO = '‌';
    public static final char ONE = ' ';
    public static final String MARKER = "‌‌‌‌ ";
    private static final int VERSION = 1;
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int MAX_CAPTION = 127;
    private static final int MAX_ID = 31;
    private static final String[] PREFIX = {null, LinkCodes.IMGUR, LinkCodes.CLOVI, LinkCodes.CLOVI + "post/"};

    /** A hidden picture found in page text: its span covers the caption and the code. */
    public record Decoded(int start, int end, ImageData image) {}

    private record Source(int type, String id) {}

    private HiddenImages() {}

    public static boolean canHide(String url) {
        return source(url) != null;
    }

    private static Source source(String url) {
        if (url == null) return null;
        String post = PREFIX[3];
        if (url.startsWith(post)) return valid(3, url.substring(post.length()), true);
        if (url.startsWith(LinkCodes.CLOVI)) return valid(2, url.substring(LinkCodes.CLOVI.length()), false);
        if (url.startsWith(LinkCodes.IMGUR)) return valid(1, url.substring(LinkCodes.IMGUR.length()), false);
        return null;
    }

    private static Source valid(int type, String id, boolean digitsOnly) {
        if (id.isEmpty() || id.length() > MAX_ID) return null;
        for (char c : id.toCharArray()) {
            if (ALPHABET.indexOf(c) < 0 || (digitsOnly && !Character.isDigit(c))) return null;
        }
        return new Source(type, id);
    }

    /**
     * What the editor writes for a picture: the hidden form when {@code hide} is on and the source fits,
     * the visible tag otherwise. A picture that already has a caption keeps it.
     */
    public static String write(ImageData image, boolean hide, String defaultCaption) {
        String tag = image.bookString();
        if (tag.isEmpty() || !hide) return tag;
        String caption = image.caption != null ? image.caption : defaultCaption;
        return encode(image, caption).orElse(tag);
    }

    public static Optional<String> encode(ImageData image, String caption) {
        Source src = source(image.getUrl());
        if (src == null) return Optional.empty();
        String cap = caption == null ? "" : caption.replace(String.valueOf(ZERO), "");
        if (cap.length() > MAX_CAPTION) cap = cap.substring(0, MAX_CAPTION);
        Bits bits = new Bits();
        bits.put(VERSION, 3);
        bits.put(cap.length(), 7);
        bits.put(src.type(), 3);
        bits.put(src.id().length(), 5);
        for (char c : src.id().toCharArray()) bits.put(ALPHABET.indexOf(c), 6);
        bits.put(clamp(Math.round(image.x), -512, 511) & 0x3FF, 10);
        bits.put(clamp(Math.round(image.y), -512, 511) & 0x3FF, 10);
        bits.put(clamp(Math.round(image.widthFraction * 100), 0, 1023), 10);
        bits.put(clamp(Math.round(image.heightFraction * 100), 0, 1023), 10);
        bits.put(Math.floorMod(Math.round(image.rotation), 360), 9);
        bits.put(crc8(bits), 8);
        StringBuilder out = new StringBuilder(cap).append(MARKER);
        for (int i = 0; i < bits.size(); i++) out.append(bits.get(i) ? ONE : ZERO);
        return Optional.of(out.toString());
    }

    /** Every hidden picture in {@code text}, in order. A damaged code (CRC mismatch) is skipped. */
    public static List<Decoded> findAll(String text) {
        List<Decoded> found = new ArrayList<>();
        int from = 0;
        while (true) {
            int at = text.indexOf(MARKER, from);
            if (at < 0) break;
            Decoded d = decodeAt(text, at, from);
            if (d != null) {
                found.add(d);
                from = d.end();
            } else {
                from = at + 1;
            }
        }
        return found;
    }

    private static Decoded decodeAt(String text, int marker, int lowerBound) {
        Reader r = new Reader(text, marker + MARKER.length());
        try {
            if (r.take(3) != VERSION) return null;
            int captionLength = r.take(7);
            int type = r.take(3);
            if (type < 1 || type > 3) return null;
            int idLength = r.take(5);
            if (idLength < 1) return null;
            StringBuilder id = new StringBuilder();
            for (int i = 0; i < idLength; i++) {
                int k = r.take(6);
                if (k >= ALPHABET.length()) return null;
                id.append(ALPHABET.charAt(k));
            }
            int x = signed10(r.take(10));
            int y = signed10(r.take(10));
            int w = r.take(10);
            int h = r.take(10);
            int rotation = r.take(9);
            if (rotation >= 360) return null;
            int crc = crc8(r.bits);
            if (r.take(8) != crc) return null;
            int captionStart = Math.max(lowerBound, marker - captionLength);
            ImageData image = new ImageData(PREFIX[type] + id, (short) 0, (short) 0, w / 100f, h / 100f);
            image.x = x;
            image.y = y;
            image.rotation = rotation;
            image.caption = text.substring(captionStart, marker);
            return new Decoded(captionStart, r.pos, image);
        } catch (IllegalStateException damaged) {
            return null;
        }
    }

    private static int signed10(int v) {
        return v >= 512 ? v - 1024 : v;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** CRC-8, polynomial 0x07, over the bits packed MSB first into bytes (last byte zero-padded). */
    private static int crc8(Bits bits) {
        int crc = 0;
        for (int i = 0; i < bits.size(); i += 8) {
            int b = 0;
            for (int k = 0; k < 8; k++) b = (b << 1) | (i + k < bits.size() && bits.get(i + k) ? 1 : 0);
            crc ^= b;
            for (int k = 0; k < 8; k++) crc = (crc & 0x80) != 0 ? ((crc << 1) ^ 0x07) & 0xFF : (crc << 1) & 0xFF;
        }
        return crc;
    }

    private static final class Bits {
        private final List<Boolean> list = new ArrayList<>();

        void put(int value, int width) {
            for (int i = width - 1; i >= 0; i--) list.add(((value >> i) & 1) == 1);
        }

        boolean get(int i) {
            return list.get(i);
        }

        int size() {
            return list.size();
        }
    }

    /** Reads bits from the text; any character other than ZERO or ONE means the code is damaged. */
    private static final class Reader {
        private final String text;
        private int pos;
        private final Bits bits = new Bits();

        Reader(String text, int pos) {
            this.text = text;
            this.pos = pos;
        }

        int take(int width) {
            int v = 0;
            for (int i = 0; i < width; i++) {
                if (pos >= text.length()) throw new IllegalStateException("truncated");
                char c = text.charAt(pos++);
                if (c != ZERO && c != ONE) throw new IllegalStateException("foreign character");
                bits.list.add(c == ONE);
                v = (v << 1) | (c == ONE ? 1 : 0);
            }
            return v;
        }
    }
}
