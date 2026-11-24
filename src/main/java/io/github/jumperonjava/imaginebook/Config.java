package io.github.jumperonjava.imaginebook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jumperonjava.imaginebook.util.FileReadWrite;

import java.nio.file.Path;
import java.util.function.Function;

public class Config {
    public static Config INSTANCE = new Config(Imaginebook.getConfigFile());
    private static Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;

    public Config(Path configFile) {
        this.file = configFile;
    }

    private Data get() {
        var data = FileReadWrite.read(file);
        if(data.isEmpty())
            return new Data();
        else
            return JSON.fromJson(data, Data.class);
    }
    public void mutateData(Function<Data, Data> function) {
        var data = function.apply(get());
        FileReadWrite.write(file, JSON.toJson(data));
        cached = data;
    }

    private Data cached;

    /** The settings, read from the file once; the editor asks on every drag step. */
    public Data data() {
        if (cached == null) cached = get();
        return cached;
    }

    public static class Data {
        public boolean imgurAccept = false;
        /** Write Imgur and clovi pictures as a caption with an invisible code (util.HiddenImages). Off by default. */
        public boolean hideLinks = false;
    }

}
