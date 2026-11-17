package io.github.jumperonjava.imaginebook.resolvers;

import io.github.jumperonjava.imaginebook.image.BookDrawable;
import io.github.jumperonjava.imaginebook.util.LinkCodes;

/** Resolves {@code imgur:} and {@code u:} link codes (see {@link LinkCodes}) by downloading the real address. */
public class CodedLinkResolver implements Resolver {
    public static final CodedLinkResolver IMGUR = new CodedLinkResolver(LinkCodes.IMGUR);
    public static final CodedLinkResolver CODED = new CodedLinkResolver(LinkCodes.CODED);

    private final String prefix;

    private CodedLinkResolver(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public BookDrawable resolve(String path) {
        String link = LinkCodes.decode(prefix + path);
        // Only a web link may come out of a code: no res: textures and no codes inside codes.
        if (link.startsWith("https://")) return UrlResolver.INSTANCE_S.resolve(link.substring("https:".length()));
        if (link.startsWith("http://")) return UrlResolver.INSTANCE.resolve(link.substring("http:".length()));
        return AsyncImageDownloader.ERROR_IMAGE;
    }
}
