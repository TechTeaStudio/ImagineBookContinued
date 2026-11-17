package io.github.jumperonjava.imaginebook.util;

/**
 * Where a new picture starts and how big it may get. 100 % is the picture's own pixel size, so a
 * 1000 x 1000 photo covered the whole book until it was shrunk by hand; a new picture is now fitted
 * into the page's text area once it has loaded. Pictures already in books keep their size.
 */
public final class ImageFit {
    /** BookViewScreen.PAGE_TEXT_X_OFFSET and PAGE_TEXT_Y_OFFSET in 26.3: the text area's corner on the book. */
    public static final int PAGE_X = 36;
    public static final int PAGE_Y = 30;
    /** BookEditScreen.TEXT_WIDTH and TEXT_HEIGHT in 26.3. */
    public static final int PAGE_WIDTH = 114;
    public static final int PAGE_HEIGHT = 126;

    private ImageFit() {}

    /** The largest whole percent, at most 100, at which a picture of this size fits the text area. */
    public static int percent(int width, int height) {
        if (width <= 0 || height <= 0) return 100;
        double scale = Math.min(1.0, Math.min((double) PAGE_WIDTH / width, (double) PAGE_HEIGHT / height));
        return Math.max(1, (int) Math.floor(scale * 100));
    }
}
