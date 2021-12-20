package eu.lucaventuri.fibry;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestUtilities {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testFileChange() throws IOException, InterruptedException {
        File f = new File("fileTest");
        File f2 = new File("fileTest2");
        CountDownLatch latch = new CountDownLatch(2);

        f.delete();
        f2.delete();

        try {
            Utilities.watchDirectory(".", true, false, false, (operation, file) -> {
                assertEquals(Utilities.FileOperation.CREATE, operation);
                assertTrue(f.getName().startsWith("fileTest"));
                System.out.println(file.toPath().normalize().toFile().getAbsolutePath());
                latch.countDown();
            });

            Utilities.watchDirectory(".", true, false, false, (operation, file) -> System.out.println(file));

            Files.write(f.toPath(), "TEST".getBytes());
            Files.write(f2.toPath(), "TEST".getBytes());

            latch.await();
        } finally {
            f.delete();
            f2.delete();
        }
    }
}
