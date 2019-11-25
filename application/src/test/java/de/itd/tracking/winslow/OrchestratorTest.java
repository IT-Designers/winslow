package de.itd.tracking.winslow;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class OrchestratorTest {

    @Test
    public void ensureForcePurgeDoesNotAcceptNotNormalizedPaths() throws IOException {
        var temp = Files.createTempDirectory("whatever");
        temp.toFile().deleteOnExit();

        var path = temp.toAbsolutePath();

        var subDir = path.resolve("someRootDir/someProjectDir");
        Files.createDirectories(subDir);

        assertTrue(Files.exists(subDir));
        Orchestrator.forcePurge(path, subDir);
        assertFalse(Files.exists(subDir));
        assertTrue(Files.exists(path));

        var subDir2 = path.resolve("someDir2");
        Files.createDirectories(subDir2);
        Files.createDirectories(subDir2.resolve("abc"));
        assertTrue(Files.exists(subDir2));
        assertTrue(Files.exists(subDir2.resolve("abc")));

        assertThrows(IOException.class, () -> Orchestrator.forcePurge(path, subDir2.resolve("abc/../..")));
        assertThrows(IOException.class, () -> Orchestrator.forcePurge(path, subDir2.resolve("..")));
        assertThrows(IOException.class, () -> Orchestrator.forcePurge(path, path));
        assertThrows(IOException.class, () -> Orchestrator.ensurePathToPurgeIsValid(path, Path.of("/../../tmp/123/path-does-not-exist")));
        assertThrows(IOException.class, () -> Orchestrator.ensurePathToPurgeIsValid(path, Path.of("../../tmp/123/path-does-not-exist")));

        // nothing should have been deleted!
        assertTrue(Files.exists(subDir2));
        assertTrue(Files.exists(subDir2.resolve("abc")));
        assertTrue(Files.exists(path));

    }
}
