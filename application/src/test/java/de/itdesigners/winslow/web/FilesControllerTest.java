package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;
import de.itdesigners.winslow.api.file.FileInfo;
import de.itdesigners.winslow.auth.Group;
import de.itdesigners.winslow.auth.GroupAssignmentResolver;
import de.itdesigners.winslow.auth.Prefix;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.resource.PathConfiguration;
import de.itdesigners.winslow.resource.ResourceManager;
import de.itdesigners.winslow.web.api.FilesController;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.Nonnull;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertArrayEquals;

public class FilesControllerTest {

    public static final String ABC_TXT   = "Some Fox jumps over some stuff and the font looks awesome";
    public static final String SOME_FILE = "VERY IMPORTANT";
    public static final String DEF_TXT   = "se alphabet";

    private FilesController   controller;
    private FileAccessChecker fileAccessChecker;
    private ResourceManager   resourceManager;
    private PathConfiguration pathConfiguration;
    private Path              workDirectory;

    @Before
    public void before() throws IOException {
        workDirectory     = Files.createTempDirectory(FilesControllerTest.class.getSimpleName());
        pathConfiguration = new PathConfiguration();
        resourceManager   = new ResourceManager(workDirectory, pathConfiguration);
        fileAccessChecker = new FileAccessChecker(resourceManager, id -> Optional.of(new Project(
                id,
                "accounting-group",
                List.of(new Link("user::project-owner", Role.OWNER)),
                null,
                "project-name",
                null,
                new PipelineDefinition("pipeline-definition"),
                null
        )));
        controller        = new FilesController(resourceManager, fileAccessChecker);

        var workspaces  = workDirectory.resolve(pathConfiguration.getRelativePathOfWorkspaces());
        var resourceDir = workDirectory.resolve(pathConfiguration.getRelativePathOfResources());

        Files.createDirectories(resourceDir.resolve("sub/directory"));
        Files.writeString(resourceDir.resolve("sub/directory").resolve("def.txt"), DEF_TXT);
        Files.writeString(resourceDir.resolve("abc.txt"), ABC_TXT);

        Files.createDirectories(workspaces.resolve("my-project-id/stage1"));
        Files.writeString(workspaces.resolve("my-project-id/stage1/some.file"), SOME_FILE);
    }

    @Test
    public void testResourceDeletion() {
        String file      = "abc.txt";
        String directory = "sub";
        assertEquals(HttpStatus.OK, controller.deleteInResource(constructRequest(file), getRoot()).getStatusCode());
        assertFalse(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve(file)
        ));
        assertTrue(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve(directory)
        ));
        assertEquals(
                HttpStatus.OK,
                controller.deleteInResource(constructRequest(directory), getRoot()).getStatusCode()
        );
        assertFalse(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve(directory)
        ));
    }

    @Test
    public void testResourceDeletionInvalid() {
        assertEquals(
                HttpStatus.NOT_FOUND,
                controller.deleteInResource(constructRequest("some-invalid-path"), getRoot()).getStatusCode()
        );
    }

    @Test
    public void testResourceDeletionUnauthorized() {
        String file      = "abc.txt";
        String directory = "sub";
        assertEquals(HttpStatus.NOT_FOUND, controller.deleteInResource(constructRequest(file), null).getStatusCode());
        assertEquals(
                HttpStatus.NOT_FOUND,
                controller.deleteInResource(constructRequest(directory), null).getStatusCode()
        );
        assertTrue(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve(file)
        ));
        assertTrue(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve(directory)
        ));
    }

    @Test
    public void testWorkspaceDeletion() {
        String file      = "my-project-id/stage1/some.file";
        String directory = "my-project-id/stage1";
        assertEquals(HttpStatus.OK, controller.deleteInWorkspace(constructRequest(file), getRoot()).getStatusCode());
        assertFalse(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(file)
        ));
        assertTrue(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(directory)
        ));

        assertEquals(
                HttpStatus.OK,
                controller.deleteInWorkspace(constructRequest(directory), getRoot()).getStatusCode()
        );
        assertFalse(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(directory)
        ));
    }

    @Test
    public void testWorkspaceDeletionUnauthorized() {
        String file      = "my-project-id/stage1/some.file";
        String directory = "my-project-id/stage1";
        assertEquals(HttpStatus.NOT_FOUND, controller.deleteInWorkspace(constructRequest(file), null).getStatusCode());
        assertEquals(
                HttpStatus.NOT_FOUND,
                controller.deleteInWorkspace(constructRequest(directory), null).getStatusCode()
        );
        assertTrue(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(file)
        ));
        assertTrue(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(directory)
        ));
    }

    @Test
    public void testWorkspaceDeletionNotAllowed() {
        String file      = "my-project-id/stage1/some.file";
        String directory = "my-project-id/stage1";
        assertEquals(
                HttpStatus.NOT_FOUND,
                controller.deleteInWorkspace(constructRequest(file), getUser("random-guy")).getStatusCode()
        );
        assertEquals(
                HttpStatus.NOT_FOUND,
                controller
                        .deleteInWorkspace(constructRequest(directory), getUser("random-guy"))
                        .getStatusCode()
        );
        assertTrue(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(file)
        ));
        assertTrue(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(directory)
        ));
    }

    @Test
    public void testWorkspaceDeletionProjectOwner() {
        String file      = "my-project-id/stage1/some.file";
        String directory = "my-project-id/stage1";
        assertEquals(
                HttpStatus.OK,
                controller.deleteInWorkspace(constructRequest(file), getProjectOwner()).getStatusCode()
        );
        assertEquals(
                HttpStatus.OK,
                controller.deleteInWorkspace(constructRequest(directory), getProjectOwner()).getStatusCode()
        );
        assertFalse(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(file)
        ));
        assertFalse(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(directory)
        ));
    }

    @Test
    public void testResourceCreateDirectory() {
        String directory = "the-new-shit";
        assertEquals(
                Optional.of("/resources/" + directory),
                controller.createResourceDirectory(constructRequest(directory), getRoot())
        );
        assertEquals(
                Optional.of("/resources/" + directory),
                controller.createResourceDirectory(constructRequest(directory), getRoot())
        );
        assertTrue(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve(directory)
        ));
    }

    @Test
    public void testResourceCreateDirectoryUnauthorized() {
        String directory = "the-new-shit";
        assertEquals(
                Optional.empty(),
                controller.createResourceDirectory(constructRequest(directory), null)
        );
        assertFalse(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve(directory)
        ));
    }

    @Test
    public void testWorkspaceCreateDirectory() {
        var directory = "my-project-id/next-lvl-stage";
        assertEquals(
                Optional.of("/workspaces/" + directory),
                controller.createWorkspaceDirectory(constructRequest(directory), getRoot())
        );
        assertEquals(
                Optional.of("/workspaces/" + directory),
                controller.createWorkspaceDirectory(constructRequest(directory), getRoot())
        );
        assertTrue(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(directory)
        ));
    }

    @Test
    public void testWorkspaceCreateDirectoryUnauthorized() {
        String directory = "my-project-id/next-lvl-stage";
        assertEquals(
                Optional.empty(),
                controller.createWorkspaceDirectory(constructRequest(directory), null)
        );
        assertFalse(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(directory)
        ));
    }

    @Test
    public void testWorkspaceCreateDirectoryNotAllowed() {
        String directory = "my-project-id/next-lvl-stage";
        assertEquals(
                Optional.empty(),
                controller.createWorkspaceDirectory(
                        constructRequest(directory),
                        getUser("random-guy")
                )
        );
        assertFalse(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(directory)
        ));
    }

    @Test
    public void testWorkspaceCreateDirectoryProjectOwner() {
        String directory = "my-project-id/next-lvl-stage";
        assertEquals(
                Optional.of("/workspaces/" + directory),
                controller.createWorkspaceDirectory(
                        constructRequest(directory),
                        getProjectOwner()
                )
        );
        assertTrue(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve(directory)
        ));
    }

    @Test
    public void testResourceUpload() throws IOException {
        controller.uploadResourceFile(
                constructRequest("bitcoin.privatekey"),
                getRoot(),
                constructUploadFile(ABC_TXT),
                false,
                null
        );
        String content = Files.readString(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve("bitcoin.privatekey")
        );
        assertEquals(ABC_TXT, content);
    }

    @Test
    public void testResourceUploadLastModified() throws IOException {
        controller.uploadResourceFile(
                constructRequest("bitcoin.privatekey"),
                getRoot(),
                constructUploadFile(ABC_TXT),
                false,
                1337L
        );

        assertEquals(
                1337L,
                Files.getLastModifiedTime(
                        workDirectory
                                .resolve(pathConfiguration.getRelativePathOfResources())
                                .resolve("bitcoin.privatekey")
                ).toMillis()
        );
    }

    @Test
    public void testResourceUploadOverwrite() throws IOException {
        controller.uploadResourceFile(
                constructRequest("abc.txt"),
                getRoot(),
                constructUploadFile(DEF_TXT),
                false,
                null
        );
        String content = Files.readString(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve("abc.txt")
        );
        assertEquals(DEF_TXT, content);
    }

    @Test
    public void testResourceUploadUnauthorized() {
        Assert.assertThrows(
                "404 NOT_FOUND",
                ResponseStatusException.class,
                () -> controller.uploadResourceFile(
                        constructRequest("bitcoin.privatekey"),
                        null,
                        constructUploadFile(ABC_TXT),
                        false,
                        null
                )
        );
        assertFalse(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve("bitcoin.privatekey")
        ));
    }

    @Test
    public void testWorkspaceUpload() throws IOException {
        controller.uploadWorkspaceFile(
                constructRequest("my-project-id/bitcoin.privatekey"),
                getRoot(),
                constructUploadFile(ABC_TXT),
                false,
                null
        );
        String content = Files.readString(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve("my-project-id/bitcoin.privatekey")
        );
        assertEquals(ABC_TXT, content);
    }

    @Test
    public void testWorkspaceUploadLastModified() throws IOException {
        controller.uploadWorkspaceFile(
                constructRequest("my-project-id/bitcoin.privatekey"),
                getRoot(),
                constructUploadFile(ABC_TXT),
                false,
                1337L
        );

        assertEquals(
                1337L,
                Files.getLastModifiedTime(
                        workDirectory
                                .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                                .resolve("my-project-id/bitcoin.privatekey")
                ).toMillis()
        );
    }

    @Test
    public void testWorkspaceUploadUnauthorized() {
        Assert.assertThrows(
                "404 NOT_FOUND",
                ResponseStatusException.class,
                () -> controller.uploadWorkspaceFile(
                        constructRequest("my-project-id/bitcoin.privatekey"),
                        null,
                        constructUploadFile(ABC_TXT),
                        false,
                        null
                )
        );
        assertFalse(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve("my-project-id/bitcoin.privatekey")
        ));
    }

    @Test
    public void testWorkspaceUploadNotAllowed() {
        Assert.assertThrows(
                "404 NOT_FOUND",
                ResponseStatusException.class,
                () -> controller.uploadWorkspaceFile(
                        constructRequest("my-project-id/bitcoin.privatekey"),
                        getUser("random-guy"),
                        constructUploadFile(ABC_TXT),
                        false,
                        null
                )
        );
        assertFalse(Files.exists(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve("my-project-id/bitcoin.privatekey")
        ));
    }

    @Test
    public void testWorkspaceUploadProjectOwner() throws IOException {
        controller.uploadWorkspaceFile(
                constructRequest("my-project-id/bitcoin.privatekey"),
                getProjectOwner(),
                constructUploadFile(ABC_TXT),
                false,
                null
        );
        String content = Files.readString(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve("my-project-id/bitcoin.privatekey")
        );
        assertEquals(ABC_TXT, content);
    }

    @Test
    public void testZipArchiveUpload() throws IOException {
        testZipArchiveUpload(false);
        testZipArchiveUpload(true);
    }

    public void testZipArchiveUpload(boolean decompress) throws IOException {
        var abcContent = "Se Compressed File Content";
        var archive    = new ByteArrayOutputStream();
        var lastMod    = FileTime.from(1337L, TimeUnit.SECONDS); // zip only supports full seconds

        try (ZipOutputStream zos = new ZipOutputStream(archive)) {
            zos.putNextEntry(new ZipEntry("bernd/abc.txt").setLastModifiedTime(lastMod));
            var baos = new ByteArrayOutputStream();
            try (PrintStream ps = new PrintStream(baos)) {
                ps.print(abcContent);
            }
            new ByteArrayInputStream(baos.toByteArray()).transferTo(zos);
            zos.flush();
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("bernd/blubb/bli/abc.txt").setLastModifiedTime(lastMod));
            baos = new ByteArrayOutputStream();
            try (PrintStream ps = new PrintStream(baos)) {
                ps.print(abcContent);
            }
            new ByteArrayInputStream(baos.toByteArray()).transferTo(zos);
            zos.flush();
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("bernd/blubb/bli/").setLastModifiedTime(lastMod));
            zos.flush();
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("bernd/blubb/bli/xyz.txt").setLastModifiedTime(lastMod));
            baos = new ByteArrayOutputStream();
            try (PrintStream ps = new PrintStream(baos)) {
                ps.print(abcContent);
            }
            new ByteArrayInputStream(baos.toByteArray()).transferTo(zos);
            zos.flush();
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("bli/blubb/def.txt").setLastModifiedTime(lastMod));
            baos = new ByteArrayOutputStream();
            try (PrintStream ps = new PrintStream(baos)) {
                ps.print(abcContent);
            }
            new ByteArrayInputStream(baos.toByteArray()).transferTo(zos);
            zos.flush();
            zos.closeEntry();
        }

        var content = new ByteArrayInputStream(archive.toByteArray());

        controller.uploadResourceFile(
                constructRequest("my-project-id/se.zip"),
                getProjectOwner(),
                content,
                decompress,
                null
        );

        if (decompress) {
            assertFileContentTime(
                    workDirectory
                            .resolve(pathConfiguration.getRelativePathOfResources())
                            .resolve("my-project-id/bernd/abc.txt"),
                    abcContent,
                    lastMod.toMillis()
            );
            assertFileContentTime(
                    workDirectory
                            .resolve(pathConfiguration.getRelativePathOfResources())
                            .resolve("my-project-id/bernd/blubb/bli/abc.txt"),
                    abcContent,
                    lastMod.toMillis()
            );
            assertFileContentTime(
                    workDirectory
                            .resolve(pathConfiguration.getRelativePathOfResources())
                            .resolve("my-project-id/bernd/blubb/bli/xyz.txt"),
                    abcContent,
                    lastMod.toMillis()
            );
            assertFileContentTime(
                    workDirectory
                            .resolve(pathConfiguration.getRelativePathOfResources())
                            .resolve("my-project-id/bli/blubb/def.txt"),
                    abcContent,
                    lastMod.toMillis()
            );
        } else {
            assertArrayEquals(
                    archive.toByteArray(),
                    Files.readAllBytes(
                            workDirectory
                                    .resolve(pathConfiguration.getRelativePathOfResources())
                                    .resolve("my-project-id/se.zip"))
            );
        }
    }

    private void assertFileContentTime(@Nonnull Path path, @Nonnull String content, long time) throws IOException {
        assertEquals(content, Files.readString(path));
        assertEquals(time, Files.getLastModifiedTime(path).toMillis());
    }

    @Test
    public void testTarGzArchiveUpload() throws IOException {
        testTarGzArchiveUpload(false);
        testTarGzArchiveUpload(true);
    }

    public void testTarGzArchiveUpload(boolean decompress) throws IOException {
        var abcContent = "Se Compressed File Content";
        var archive    = new ByteArrayOutputStream();
        var time       = 4242_000L; // tar-gz only supports whole seconds

        try (GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(archive)) {
            try (TarArchiveOutputStream taos = new TarArchiveOutputStream(gcos)) {
                var baos = new ByteArrayOutputStream();
                try (PrintStream ps = new PrintStream(baos)) {
                    ps.print(abcContent);
                }
                var array        = baos.toByteArray();
                var archiveEntry = new TarArchiveEntry("bernd/abc.txt");
                archiveEntry.setSize(array.length);
                archiveEntry.setModTime(time);
                taos.putArchiveEntry(archiveEntry);
                new ByteArrayInputStream(array).transferTo(taos);
                taos.flush();
                taos.closeArchiveEntry();

                baos = new ByteArrayOutputStream();
                try (PrintStream ps = new PrintStream(baos)) {
                    ps.print(abcContent);
                }
                array        = baos.toByteArray();
                archiveEntry = new TarArchiveEntry("bernd/bli/blubb/abc.txt");
                archiveEntry.setSize(array.length);
                archiveEntry.setModTime(time);
                taos.putArchiveEntry(archiveEntry);
                new ByteArrayInputStream(array).transferTo(taos);
                taos.flush();
                taos.closeArchiveEntry();

                baos = new ByteArrayOutputStream();
                try (PrintStream ps = new PrintStream(baos)) {
                    ps.print(abcContent);
                }
                array        = baos.toByteArray();
                archiveEntry = new TarArchiveEntry("bernd/bli/blubb/xyz.txt");
                archiveEntry.setSize(array.length);
                archiveEntry.setModTime(time);
                taos.putArchiveEntry(archiveEntry);
                new ByteArrayInputStream(array).transferTo(taos);
                taos.flush();
                taos.closeArchiveEntry();

                baos = new ByteArrayOutputStream();
                try (PrintStream ps = new PrintStream(baos)) {
                    ps.print(abcContent);
                }
                array        = baos.toByteArray();
                archiveEntry = new TarArchiveEntry("bernd/bli/def.txt");
                archiveEntry.setSize(array.length);
                archiveEntry.setModTime(time);
                taos.putArchiveEntry(archiveEntry);
                new ByteArrayInputStream(array).transferTo(taos);
                taos.flush();
                taos.closeArchiveEntry();
            }
        }

        var content = new ByteArrayInputStream(archive.toByteArray());

        controller.uploadResourceFile(
                constructRequest("my-project-id/se.tar.gz"),
                getProjectOwner(),
                content,
                decompress,
                null
        );

        if (decompress) {
            assertFileContentTime(
                    workDirectory
                            .resolve(pathConfiguration.getRelativePathOfResources())
                            .resolve("my-project-id/bernd/abc.txt"),
                    abcContent,
                    time
            );
            assertFileContentTime(
                    workDirectory
                            .resolve(pathConfiguration.getRelativePathOfResources())
                            .resolve("my-project-id/bernd/bli/blubb/abc.txt"),
                    abcContent,
                    time
            );
            assertFileContentTime(
                    workDirectory
                            .resolve(pathConfiguration.getRelativePathOfResources())
                            .resolve("my-project-id/bernd/bli/blubb/xyz.txt"),
                    abcContent,
                    time
            );
            assertFileContentTime(
                    workDirectory
                            .resolve(pathConfiguration.getRelativePathOfResources())
                            .resolve("my-project-id/bernd/bli/def.txt"),
                    abcContent,
                    time
            );
        } else {
            assertArrayEquals(
                    archive.toByteArray(),
                    Files.readAllBytes(
                            workDirectory
                                    .resolve(pathConfiguration.getRelativePathOfResources())
                                    .resolve("my-project-id/se.tar.gz"))
            );
        }
    }

    @Test
    public void testResourcesDownload() throws IOException {
        var response = controller.downloadResourceFile(
                constructRequest("sub/directory/def.txt"),
                getRoot(),
                false
        );
        assertNotNull(response);
        assertNotNull(response.getBody());

        var baos = new ByteArrayOutputStream();
        response.getBody().writeTo(baos);
        var content = baos.toByteArray();

        assertEquals((DEF_TXT.getBytes(StandardCharsets.UTF_8).length), content.length);
        assertEquals(
                DEF_TXT.getBytes(StandardCharsets.UTF_8).length,
                response.getHeaders().getContentLength()
        );

    }

    @Test
    public void testResourcesDownloadCompressed() throws IOException {
        var response = controller.downloadResourceFile(
                constructRequest("sub/directory/def.txt"),
                getRoot(),
                true
        );
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response
                           .getHeaders()
                           .get(HttpHeaders.CONTENT_DISPOSITION)
                           .get(0)
                           .contains("filename=\"def.txt.tar.gz\""));

        var baos = new ByteArrayOutputStream();
        response.getBody().writeTo(baos);
        var content = baos.toByteArray();

        try (GzipCompressorInputStream gcis = new GzipCompressorInputStream(new ByteArrayInputStream(content))) {
            try (TarArchiveInputStream taos = new TarArchiveInputStream(gcis)) {

                var entry = taos.getNextTarEntry();
                assertEquals(entry.getName(), "def.txt");


                baos = new ByteArrayOutputStream();
                taos.transferTo(baos);
                content = baos.toByteArray();

                assertEquals((DEF_TXT.getBytes(StandardCharsets.UTF_8).length), content.length);
                assertEquals(
                        DEF_TXT.getBytes(StandardCharsets.UTF_8).length,
                        response.getHeaders().getContentLength()
                );
            }
        }

    }

    @Test
    public void testResourcesDownloadInvalidNoExplicitCompress() {
        testResourcesDownloadInvalid(false);
    }

    @Test
    public void testResourcesDownloadInvalidExplicitCompress() {
        testResourcesDownloadInvalid(true);
    }

    private void testResourcesDownloadInvalid(boolean explicitCompress) {
        assertNull(controller.downloadResourceFile(
                constructRequest("sub/directory/def.txt_invalid"),
                getRoot(),
                explicitCompress
        ));
    }

    @Test
    public void testResourcesDownloadUnauthorizedNoExplicitCompress() {
        testResourcesDownloadUnauthorized(false);
    }

    @Test
    public void testResourcesDownloadUnauthorizedExplicitCompress() {
        testResourcesDownloadUnauthorized(true);
    }

    private void testResourcesDownloadUnauthorized(boolean explicitCompress) {
        assertNull(controller.downloadResourceFile(
                constructRequest("sub/directory/def.txt"),
                null,
                explicitCompress
        ));
    }

    @Test
    public void testWorkspaceDownload() throws IOException {
        var response = controller.downloadWorkspaceFile(
                constructRequest("my-project-id/stage1/some.file"),
                getRoot(),
                false
        );
        assertNotNull(response);
        assertNotNull(response.getBody());

        var baos = new ByteArrayOutputStream();
        response.getBody().writeTo(baos);
        var content = baos.toByteArray();

        assertEquals((SOME_FILE.getBytes(StandardCharsets.UTF_8).length), content.length);
        assertEquals(
                SOME_FILE.getBytes(StandardCharsets.UTF_8).length,
                response.getHeaders().getContentLength()
        );
    }


    @Test
    public void testWorkspaceDownloadCompressed() throws IOException {
        var response = controller.downloadWorkspaceFile(
                constructRequest("my-project-id/stage1/some.file"),
                getRoot(),
                true
        );
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response
                           .getHeaders()
                           .get(HttpHeaders.CONTENT_DISPOSITION)
                           .get(0)
                           .contains("filename=\"some.file.tar.gz\""));

        var baos = new ByteArrayOutputStream();
        response.getBody().writeTo(baos);
        var content = baos.toByteArray();


        try (GzipCompressorInputStream gcis = new GzipCompressorInputStream(new ByteArrayInputStream(content))) {
            try (TarArchiveInputStream taos = new TarArchiveInputStream(gcis)) {

                var entry = taos.getNextTarEntry();
                assertEquals(entry.getName(), "some.file");


                baos = new ByteArrayOutputStream();
                taos.transferTo(baos);
                content = baos.toByteArray();

                assertEquals((SOME_FILE.getBytes(StandardCharsets.UTF_8).length), content.length);
                assertEquals(
                        SOME_FILE.getBytes(StandardCharsets.UTF_8).length,
                        response.getHeaders().getContentLength()
                );
            }
        }
    }


    @Test
    public void testWorkspaceDownloadInvalidNoExplicitCompress() {
        testWorkspaceDownloadInvalid(false);
    }

    @Test
    public void testWorkspaceDownloadInvalidExplicitCompress() {
        testWorkspaceDownloadInvalid(true);
    }

    private void testWorkspaceDownloadInvalid(boolean explicitCompress) {
        assertNull(controller.downloadWorkspaceFile(
                constructRequest("my-project-id/stage1/some.file_invalid"),
                getRoot(),
                explicitCompress
        ));
    }

    @Test
    public void testWorkspaceDownloadUnauthorizedNoExplicitCompress() {
        testWorkspaceDownloadUnauthorized(false);
    }

    @Test
    public void testWorkspaceDownloadUnauthorizedExplicitCompress() {
        testWorkspaceDownloadUnauthorized(true);
    }

    private void testWorkspaceDownloadUnauthorized(boolean explicitCompress) {
        assertNull(controller.downloadWorkspaceFile(
                constructRequest("my-project-id/stage1/some.file"),
                null,
                explicitCompress
        ));
    }

    @Test
    public void testWorkspaceDownloadNotAllowedNoExplicitCompress() {
        testWorkspaceDownloadNotAllowed(false);
    }

    @Test
    public void testWorkspaceDownloadNotAllowedExplicitCompress() {
        testWorkspaceDownloadNotAllowed(true);
    }

    private void testWorkspaceDownloadNotAllowed(boolean explicitCompress) {
        assertNull(controller.downloadWorkspaceFile(
                constructRequest("my-project-id/stage1/some.file"),
                getUser("random-user"),
                explicitCompress
        ));
    }

    @Test
    public void testWorkspaceDownloadProjectOwner() throws IOException {
        var response = controller.downloadWorkspaceFile(
                constructRequest("my-project-id/stage1/some.file"),
                getProjectOwner(),
                false
        );

        assertNotNull(response);
        assertNotNull(response.getBody());

        var baos = new ByteArrayOutputStream();
        response.getBody().writeTo(baos);
        var content = baos.toByteArray();

        assertEquals(SOME_FILE.getBytes(StandardCharsets.UTF_8).length, content.length);
        assertEquals(
                SOME_FILE.getBytes(StandardCharsets.UTF_8).length,
                response.getHeaders().getContentLength()
        );
    }

    @Test
    public void testWorkspaceDownloadProjectOwnerCompressed() throws IOException {
        var response = controller.downloadWorkspaceFile(
                constructRequest("my-project-id/stage1/some.file"),
                getProjectOwner(),
                true
        );
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response
                           .getHeaders()
                           .get(HttpHeaders.CONTENT_DISPOSITION)
                           .get(0)
                           .contains("filename=\"some.file.tar.gz\""));

        var baos = new ByteArrayOutputStream();
        response.getBody().writeTo(baos);
        var content = baos.toByteArray();


        try (GzipCompressorInputStream gcis = new GzipCompressorInputStream(new ByteArrayInputStream(content))) {
            try (TarArchiveInputStream taos = new TarArchiveInputStream(gcis)) {

                var entry = taos.getNextTarEntry();
                assertEquals(entry.getName(), "some.file");


                baos = new ByteArrayOutputStream();
                taos.transferTo(baos);
                content = baos.toByteArray();

                assertEquals(SOME_FILE.getBytes(StandardCharsets.UTF_8).length, content.length);
                assertEquals(
                        SOME_FILE.getBytes(StandardCharsets.UTF_8).length,
                        response.getHeaders().getContentLength()
                );
            }
        }
    }

    @Test
    public void testResourceDownloadDirectoryNoExplicitCompress() throws IOException {
        testResourceDownloadDirectoryResponse(false);
    }

    @Test
    public void testResourceDownloadDirectoryExplicitCompress() throws IOException {
        testResourceDownloadDirectoryResponse(true);
    }

    private void testResourceDownloadDirectoryResponse(boolean explicitCompress) throws IOException {
        var response = controller.downloadResourceFile(
                constructRequest("sub/"),
                getProjectOwner(),
                explicitCompress
        );
        assertNotNull(response);
        assertNotNull(response.getBody());

        var baos = new ByteArrayOutputStream();
        response.getBody().writeTo(baos);

        var bais = new ByteArrayInputStream(baos.toByteArray());
        var gcis = new GzipCompressorInputStream(bais);
        var tais = new TarArchiveInputStream(gcis);
        var next = (TarArchiveEntry) null;

        boolean subDirectoryDefTxt = false;

        while ((next = tais.getNextTarEntry()) != null) {
            if (next.getName().equals("directory/def.txt")) {
                subDirectoryDefTxt = true;
                assertFalse(next.isDirectory());
                var entryBaos = new ByteArrayOutputStream();
                tais.transferTo(entryBaos);
                assertEquals(DEF_TXT.getBytes(StandardCharsets.UTF_8).length, next.getSize());
                assertEquals(DEF_TXT.getBytes(StandardCharsets.UTF_8).length, entryBaos.size());
                assertArrayEquals(DEF_TXT.getBytes(StandardCharsets.UTF_8), entryBaos.toByteArray());
            } else {
                fail("Unexpected ZipEntry: " + next.getName());
            }
        }

        assertTrue(subDirectoryDefTxt);
    }

    @Test
    public void testBasicWorkspaceListing() {
        var listing = controller.listWorkspaceDirectory(
                constructRequest("my-project-id/stage1"),
                getRoot(),
                false
        );
        assertNotNull(listing);
        var lookup = new HashMap<String, FileInfo>();
        listing.forEach(fi -> lookup.put(fi.path(), fi));

        {
            var info = lookup.remove("/workspaces/my-project-id/stage1/some.file");
            assertNotNull(info);
            assertEquals("some.file", info.name());
            assertFalse(info.directory());
            assertEquals(
                    (Long) (long) (SOME_FILE.getBytes(StandardCharsets.UTF_8).length),
                    info.fileSize()
            );
        }

        assertTrue(lookup.isEmpty());
    }

    @Test
    public void testBasicWorkspaceListingUnauthorized() {
        var listing = controller.listWorkspaceDirectory(
                constructRequest("my-project-id/stage1"),
                null,
                false
        );
        assertNotNull(listing);
        assertFalse(listing.iterator().hasNext());
    }

    @Test
    public void testBasicWorkspaceListingNotAllowed() {
        assertFalse(controller.listWorkspaceDirectory(
                constructRequest("my-project-id/stage1"),
                getUser("unknown-user"),
                false
        ).iterator().hasNext());
    }

    @Test
    public void testBasicWorkspaceListingProjectOwner() {
        assertTrue(controller.listWorkspaceDirectory(
                constructRequest("my-project-id/stage1"),
                getProjectOwner(),
                false
        ).iterator().hasNext());
    }

    @Test
    public void testBasicResourceListingUnauthorized() {
        var listing = controller.listResourceDirectory(constructRequest(""), null, false);
        assertNotNull(listing);
        assertFalse(listing.iterator().hasNext());
    }

    @Test
    public void testBasicResourceListing() {
        var listing = controller.listResourceDirectory(
                constructRequest(""),
                getRoot(),
                false
        );
        assertNotNull(listing);
        var lookup = new HashMap<String, FileInfo>();
        listing.forEach(fi -> lookup.put(fi.path(), fi));

        {
            var info = lookup.remove("/resources/sub");
            assertNotNull(info);
            assertEquals("sub", info.name());
            assertTrue(info.directory());
            assertNull(info.fileSize());
        }

        {
            var info = lookup.remove("/resources/abc.txt");
            assertNotNull(info);
            assertEquals("abc.txt", info.name());
            assertFalse(info.directory());
            assertEquals(
                    (Long) (long) (ABC_TXT.getBytes(StandardCharsets.UTF_8).length),
                    info.fileSize()
            );
        }

        assertTrue(lookup.isEmpty());
    }

    @Test
    public void testHttpServletPathExtraction() {
        assertEquals(
                Optional.of(Path.of("some/very/clever/path")),
                FilesController.normalizedPath(constructRequest("/some/api/**", "/some/api/some/very/clever/path"))
        );
    }

    @Nonnull
    private static InputStream constructUploadFile(@Nonnull String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static HttpServletRequest constructRequest(@Nonnull String path) {
        var prefix = "/some/api/";
        return constructRequest(prefix + "**", prefix + path);
    }

    private static HttpServletRequest constructRequest(@Nonnull String pattern, @Nonnull String path) {
        return new HttpServletRequest() {
            Map<String, Object> map = Map.of(
                    HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, pattern,
                    HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, path
            );

            @Override
            public Object getAttribute(String s) {
                return map.get(s);
            }

            @Override
            public Enumeration<String> getAttributeNames() {
                return null;
            }

            @Override
            public String getCharacterEncoding() {
                return null;
            }

            @Override
            public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

            }

            @Override
            public int getContentLength() {
                return 0;
            }

            @Override
            public long getContentLengthLong() {
                return 0;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public ServletInputStream getInputStream() throws IOException {
                return null;
            }

            @Override
            public String getParameter(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getParameterNames() {
                return null;
            }

            @Override
            public String[] getParameterValues(String s) {
                return new String[0];
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return null;
            }

            @Override
            public String getProtocol() {
                return null;
            }

            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public String getServerName() {
                return null;
            }

            @Override
            public int getServerPort() {
                return 0;
            }

            @Override
            public BufferedReader getReader() throws IOException {
                return null;
            }

            @Override
            public String getRemoteAddr() {
                return null;
            }

            @Override
            public String getRemoteHost() {
                return null;
            }

            @Override
            public void setAttribute(String s, Object o) {

            }

            @Override
            public void removeAttribute(String s) {

            }

            @Override
            public Locale getLocale() {
                return null;
            }

            @Override
            public Enumeration<Locale> getLocales() {
                return null;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public RequestDispatcher getRequestDispatcher(String s) {
                return null;
            }

            @Override
            @Deprecated
            public String getRealPath(String s) {
                return null;
            }

            @Override
            public int getRemotePort() {
                return 0;
            }

            @Override
            public String getLocalName() {
                return null;
            }

            @Override
            public String getLocalAddr() {
                return null;
            }

            @Override
            public int getLocalPort() {
                return 0;
            }

            @Override
            public ServletContext getServletContext() {
                return null;
            }

            @Override
            public AsyncContext startAsync() throws IllegalStateException {
                return null;
            }

            @Override
            public AsyncContext startAsync(
                    ServletRequest servletRequest,
                    ServletResponse servletResponse) throws IllegalStateException {
                return null;
            }

            @Override
            public boolean isAsyncStarted() {
                return false;
            }

            @Override
            public boolean isAsyncSupported() {
                return false;
            }

            @Override
            public AsyncContext getAsyncContext() {
                return null;
            }

            @Override
            public DispatcherType getDispatcherType() {
                return null;
            }

            @Override
            public String getAuthType() {
                return null;
            }

            @Override
            public Cookie[] getCookies() {
                return new Cookie[0];
            }

            @Override
            public long getDateHeader(String s) {
                return 0;
            }

            @Override
            public String getHeader(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getHeaders(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                return null;
            }

            @Override
            public int getIntHeader(String s) {
                return 0;
            }

            @Override
            public String getMethod() {
                return null;
            }

            @Override
            public String getPathInfo() {
                return null;
            }

            @Override
            public String getPathTranslated() {
                return null;
            }

            @Override
            public String getContextPath() {
                return null;
            }

            @Override
            public String getQueryString() {
                return null;
            }

            @Override
            public String getRemoteUser() {
                return null;
            }

            @Override
            public boolean isUserInRole(String s) {
                return false;
            }

            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public String getRequestedSessionId() {
                return null;
            }

            @Override
            public String getRequestURI() {
                return null;
            }

            @Override
            public StringBuffer getRequestURL() {
                return null;
            }

            @Override
            public String getServletPath() {
                return null;
            }

            @Override
            public HttpSession getSession(boolean b) {
                return null;
            }

            @Override
            public HttpSession getSession() {
                return null;
            }

            @Override
            public String changeSessionId() {
                return null;
            }

            @Override
            public boolean isRequestedSessionIdValid() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromCookie() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromURL() {
                return false;
            }

            @Override
            @Deprecated
            public boolean isRequestedSessionIdFromUrl() {
                return false;
            }

            @Override
            public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
                return false;
            }

            @Override
            public void login(String s, String s1) throws ServletException {

            }

            @Override
            public void logout() throws ServletException {

            }

            @Override
            public Collection<Part> getParts() throws IOException, ServletException {
                return null;
            }

            @Override
            public Part getPart(String s) throws IOException, ServletException {
                return null;
            }

            @Override
            public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
                return null;
            }
        };
    }

    private static final GroupAssignmentResolver DUMMY_GROUP_RESOLVER = new GroupAssignmentResolver() {
        @Override
        public boolean isPartOfGroup(@Nonnull String user, @Nonnull String group) {
            // check for  the user group
            return Objects.equals(Prefix.User.wrap(user), group);
        }

        @Nonnull
        @Override
        public List<Group> getAssignedGroups(@Nonnull String user) {
            // create a group with the user-refix on-the-fly for the given user and set it as OWNER
            return List.of(new Group(Prefix.User.wrap(user), List.of(new Link(user, Role.OWNER))));
        }

        @Nonnull
        @Override
        public Optional<Group> getGroup(@Nonnull String name) {
            // create a group on-the-fly and if it is a user group (prefixed) add the
            // name without prefix as group owner (user::abc -> [abc, OWNER])
            return Optional.of(
                    new Group(
                            name,
                            Prefix.unwrap(name).stream()
                                  .map(n -> new Link(n, Role.OWNER))
                                  .toList()
                    )
            );
        }
    };

    private User getRoot() {
        return getUser(User.SUPER_USER_NAME);
    }

    private User getProjectOwner() {
        return getUser("project-owner");
    }

    private User getUser(String name) {
        return new User(name, null, null, true, null, DUMMY_GROUP_RESOLVER);
    }
}
