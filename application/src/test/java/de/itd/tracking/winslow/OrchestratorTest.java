package de.itd.tracking.winslow;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OrchestratorTest {

    @Test
    public void ensureForcePurgeDoesNotAcceptNotNormalizedPaths() throws IOException {
        var temp = Files.createTempDirectory("whatever");
        temp.toFile().deleteOnExit();

        var path = temp.toAbsolutePath();

        var subDir = path.resolve("someDir");
        Files.createDirectories(subDir);

        assertTrue(Files.exists(subDir));
        Orchestrator.forcePurge(subDir);
        assertFalse(Files.exists(subDir));
        assertTrue(Files.exists(path));

        var subDir2 = path.resolve("someDir2");
        Files.createDirectories(subDir2);
        Files.createDirectories(subDir2.resolve("abc"));
        assertTrue(Files.exists(subDir2));
        assertTrue(Files.exists(subDir2.resolve("abc")));


        Orchestrator.forcePurge(subDir2.resolve("abc/../.."));
        Orchestrator.forcePurge(subDir2.resolve(".."));

        // nothing should have been deleted!
        assertTrue(Files.exists(subDir2));
        assertTrue(Files.exists(subDir2.resolve("abc")));
        assertTrue(Files.exists(path));

    }
}
