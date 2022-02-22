package de.itdesigners.winslow;

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
        Orchestrator.forcePurge(path, path, subDir, 0);
        assertFalse(Files.exists(subDir));
        assertTrue(Files.exists(path));

        var subDir2 = path.resolve("someDir2");
        Files.createDirectories(subDir2);
        Files.createDirectories(subDir2.resolve("abc"));
        assertTrue(Files.exists(subDir2));
        assertTrue(Files.exists(subDir2.resolve("abc")));

        assertThrows(IOException.class, () -> Orchestrator.forcePurge(path, path, subDir2.resolve("abc/../.."), 0));
        assertThrows(IOException.class, () -> Orchestrator.forcePurge(path, path, subDir2.resolve(".."), 0));
        assertThrows(IOException.class, () -> Orchestrator.forcePurge(path, path, path, 0));
        assertThrows(IOException.class, () -> Orchestrator.ensurePathToPurgeIsValid(path, path, Path.of("/../../tmp/123/path-does-not-exist"), 0));
        assertThrows(IOException.class, () -> Orchestrator.ensurePathToPurgeIsValid(path, path, Path.of("../../tmp/123/path-does-not-exist"), 0));

        // nothing should have been deleted!
        assertTrue(Files.exists(subDir2));
        assertTrue(Files.exists(subDir2.resolve("abc")));
        assertTrue(Files.exists(path));
    }

    @Test
    public void testEnsurePathToPurgeIsValidConsidersScopeProperly() throws IOException {
        var dir = Path.of("/workspace");

        assertThrows(IOException.class, () -> Orchestrator.ensurePathToPurgeIsValid(dir, dir.resolve(".."), dir.resolve("doesn-not-matter"), 0));
        assertThrows(IOException.class, () -> Orchestrator.ensurePathToPurgeIsValid(dir, dir.resolve("scoped"), dir.resolve("outside-scope"), 0));
        assertThrows(IOException.class, () -> Orchestrator.ensurePathToPurgeIsValid(dir, dir.resolve("scoped"), dir.resolve("scoped"), 0));
        assertThrows(IOException.class, () -> Orchestrator.ensurePathToPurgeIsValid(dir, dir.resolve("scoped"), dir.resolve("scoped"), 1));
        Orchestrator.ensurePathToPurgeIsValid(dir, dir.resolve("scoped"), dir.resolve("scoped").resolve("nested"), 1);
        Orchestrator.ensurePathToPurgeIsValid(dir, dir.resolve("scoped"), dir.resolve("scoped").resolve("inner"), 0);
    }
}
