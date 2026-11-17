package io.github.jumperonjava.imaginebook;

import io.github.jumperonjava.imaginebook.resolvers.AsyncImageDownloader;
import io.github.jumperonjava.imaginebook.resolvers.CodedLinkResolver;
import io.github.jumperonjava.imaginebook.util.LinkCodes;
import io.github.jumperonjava.imaginebook.resolvers.Resolver;
import io.github.jumperonjava.imaginebook.resolvers.TextureResolver;
import io.github.jumperonjava.imaginebook.resolvers.UrlResolver;

import io.github.jumperonjava.imaginebook.platform.PlatformPaths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class Imaginebook {
    public static final String MOD_ID = "imaginebook";
    public static Logger LOGGER = LoggerFactory.getLogger("ImagineBook");
    public static final String TEST_BALLER = "https://i.kym-cdn.com/photos/images/original/002/461/188/20d.png";

    public static void init() {
        createImagineBookFolder();
        io.github.jumperonjava.imaginebook.util.ImageRenderPipeline.init();

    }

    public static int LENGTH = 1023;
    public static boolean cancelledFinalize = false;

    /** A single-picture imgur.com page as its direct link; albums and galleries stay pages (see LinkCodes). */
    public static String fixImgurLink(String link) {
        return LinkCodes.directImgurLink(link);
    }

    public static Path getCachePath(){

        return PlatformPaths.gameDirectory().resolve("imaginebook");

    }
    public static Path getConfigFile(){

        return PlatformPaths.configDirectory().resolve("imaginebook.json");

    }

    private static Map<String,Resolver> resolvers = new HashMap<>(Map.of(
            "http",UrlResolver.INSTANCE,
            "https",UrlResolver.INSTANCE_S,
            "res", TextureResolver.INSTANCE,
            // Link codes that keep web addresses out of the book text; see util.LinkCodes.
            "imgur", CodedLinkResolver.IMGUR,
            "clovi", CodedLinkResolver.CLOVI,
            "u", CodedLinkResolver.CODED
    ));
    public static void addResolver(String id, Resolver resolver) {
        resolvers.put(id, resolver);
    }
    public static Resolver getResolver(String type) {
        if(resolvers.containsKey(type))
            return resolvers.get(type);
        return path -> AsyncImageDownloader.ERROR_IMAGE;
    }

    public static boolean resolverExists(String resolver) {
        return resolvers.containsKey(resolver);
    }

    public void cleanFiles(){

    }

    public static void createImagineBookFolder() {
        Path imagineBookPath = getCachePath();

        try {
            if (!Files.exists(imagineBookPath)) {
                Files.createDirectories(imagineBookPath);
                LOGGER.info("imaginebook folder created at: " + imagineBookPath.toString());
            } else {
                //LOGGER.info("imaginebook folder already exists at: " + imagineBookPath.toString());
            }
        } catch (IOException e) {
            LOGGER.info("Failed to create imaginebook folder: " + e.getMessage());
        }
    }

}
