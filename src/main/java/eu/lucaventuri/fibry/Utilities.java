package eu.lucaventuri.fibry;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class Utilities {
    private Utilities() { /** Static methods only*/}

    public enum FileOperation {
        CREATE,
        UPDATE,
        DELETE;

        public static FileOperation from(String type) {
            if ("ENTRY_CREATE".equals(type))
                return CREATE;
            if ("ENTRY_MODIFY".equals(type))
                return UPDATE;
            if ("ENTRY_DELETE".equals(type))
                return DELETE;

            throw new IllegalArgumentException("Undefined type " + type);
        }
    }

    public static void watchCurrentDirectory(boolean watchCreate, boolean watchUpdate, boolean watchDelete, BiConsumer<FileOperation, File> watchConsumer) throws IOException {
        watchDirectory(".", watchCreate, watchUpdate, watchDelete, watchConsumer);
    }

    public static void watchDirectory(String path, boolean watchCreate, boolean watchUpdate, boolean watchDelete, BiConsumer<FileOperation, File> watchConsumer) throws IOException {
        watchDirectory(new File(path), watchCreate, watchUpdate, watchDelete, watchConsumer);
    }

    public static void watchDirectory(Path path, boolean watchCreate, boolean watchUpdate, boolean watchDelete, BiConsumer<FileOperation, File> watchConsumer) throws IOException {
        watchDirectory(path.toFile(), watchCreate, watchUpdate, watchDelete, watchConsumer);
    }

    public static void watchDirectory(File path, boolean watchCreate, boolean watchUpdate, boolean watchDelete, BiConsumer<FileOperation, File> watchConsumer) throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        List<WatchEvent.Kind<Path>> events = new ArrayList<>();

        if (watchCreate)
            events.add(StandardWatchEventKinds.ENTRY_CREATE);
        if (watchUpdate)
            events.add(StandardWatchEventKinds.ENTRY_MODIFY);
        if (watchDelete)
            events.add(StandardWatchEventKinds.ENTRY_DELETE);

        WatchEvent.Kind<Path> ar[] = events.toArray(new WatchEvent.Kind[0]);

        path.toPath().register(watchService, ar);

        Stereotypes.def().runOnceSilent(() -> {
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents())
                    watchConsumer.accept(FileOperation.from(event.kind().toString()), new File(path, event.context().toString()));

                key.reset();
            }
        });
    }
}
