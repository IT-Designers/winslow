package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.file.FileInfo;
import de.itdesigners.winslow.auth.Group;
import de.itdesigners.winslow.auth.GroupAssignmentResolver;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.resource.PathConfiguration;
import de.itdesigners.winslow.resource.ResourceManager;
import de.itdesigners.winslow.web.api.FilesController;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.Nonnull;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
                "project-owner",
                List.of("project-group"),
                null,
                "project-name",
                null,
                new PipelineDefinition(
                        "pipeline-definition",
                        null,
                        null,
                        Collections.emptyList(),
                        null,
                        null,
                        null
                )
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
                controller.deleteInWorkspace(constructRequest(file), getUser("random-guy", false)).getStatusCode()
        );
        assertEquals(
                HttpStatus.NOT_FOUND,
                controller
                        .deleteInWorkspace(constructRequest(directory), getUser("random-guy", false))
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
                        getUser("random-guy", false)
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
                constructUploadFile(ABC_TXT)
        );
        String content = Files.readString(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfResources())
                        .resolve("bitcoin.privatekey")
        );
        assertEquals(ABC_TXT, content);
    }

    @Test
    public void testResourceUploadOverwrite() throws IOException {
        controller.uploadResourceFile(
                constructRequest("abc.txt"),
                getRoot(),
                constructUploadFile(DEF_TXT)
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
                        constructUploadFile(ABC_TXT)
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
                constructUploadFile(ABC_TXT)
        );
        String content = Files.readString(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve("my-project-id/bitcoin.privatekey")
        );
        assertEquals(ABC_TXT, content);
    }

    @Test
    public void testWorkspaceUploadUnauthorized() {
        Assert.assertThrows(
                "404 NOT_FOUND",
                ResponseStatusException.class,
                () -> controller.uploadWorkspaceFile(
                        constructRequest("my-project-id/bitcoin.privatekey"),
                        null,
                        constructUploadFile(ABC_TXT)
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
                        getUser("random-guy", false),
                        constructUploadFile(ABC_TXT)
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
                constructUploadFile(ABC_TXT)
        );
        String content = Files.readString(
                workDirectory
                        .resolve(pathConfiguration.getRelativePathOfWorkspaces())
                        .resolve("my-project-id/bitcoin.privatekey")
        );
        assertEquals(ABC_TXT, content);
    }

    @Test
    public void testResourcesDownload() throws IOException {
        var response = controller.downloadResourceFile(
                constructRequest("sub/directory/def.txt"),
                getRoot()
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
    public void testResourcesDownloadInvalid() {
        assertNull(controller.downloadResourceFile(
                constructRequest("sub/directory/def.txt_invalid"),
                getRoot()
        ));
    }

    @Test
    public void testResourcesDownloadUnauthorized() {
        assertNull(controller.downloadResourceFile(
                constructRequest("sub/directory/def.txt"),
                null
        ));
    }

    @Test
    public void testWorkspaceDownload() throws IOException {
        var response = controller.downloadWorkspaceFile(
                constructRequest("my-project-id/stage1/some.file"),
                getRoot()
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
    public void testWorkspaceDownloadInvalid() {
        assertNull(controller.downloadWorkspaceFile(
                constructRequest("my-project-id/stage1/some.file_invalid"),
                getRoot()
        ));
    }

    @Test
    public void testWorkspaceDownloadUnauthorized() {
        assertNull(controller.downloadWorkspaceFile(
                constructRequest("my-project-id/stage1/some.file"),
                null
        ));
    }

    @Test
    public void testWorkspaceDownloadNotAllowed() {
        assertNull(controller.downloadWorkspaceFile(
                constructRequest("my-project-id/stage1/some.file"),
                getUser("random-user", false)
        ));
    }

    @Test
    public void testWorkspaceDownloadProjectOwner() throws IOException {
        var response = controller.downloadWorkspaceFile(
                constructRequest("my-project-id/stage1/some.file"),
                getProjectOwner()
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
    public void testResourceDownloadDirectory() throws IOException {
        var response = controller.downloadResourceFile(
                constructRequest("sub/"),
                getProjectOwner()
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
        listing.forEach(fi -> lookup.put(fi.path, fi));

        {
            var info = lookup.remove("/workspaces/my-project-id/stage1/some.file");
            assertNotNull(info);
            assertEquals("some.file", info.name);
            assertFalse(info.directory);
            assertEquals(
                    (Long) (long) (SOME_FILE.getBytes(StandardCharsets.UTF_8).length),
                    info.fileSize
            );
        }

        assertTrue(lookup.isEmpty());
    }

    private User getUser(String root, boolean b) {
        return new User(root, b, DUMMY_GROUP_RESOLVER);
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
                getUser("unknown-user", false),
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
        listing.forEach(fi -> lookup.put(fi.path, fi));

        {
            var info = lookup.remove("/resources/sub");
            assertNotNull(info);
            assertEquals("sub", info.name);
            assertTrue(info.directory);
            assertNull(info.fileSize);
        }

        {
            var info = lookup.remove("/resources/abc.txt");
            assertNotNull(info);
            assertEquals("abc.txt", info.name);
            assertFalse(info.directory);
            assertEquals(
                    (Long) (long) (ABC_TXT.getBytes(StandardCharsets.UTF_8).length),
                    info.fileSize
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

    private static MultipartFile constructUploadFile(@Nonnull String content) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getOriginalFilename() {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return 0;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return new byte[0];
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public void transferTo(File file) throws IOException, IllegalStateException {
                this.transferTo(file.toPath());
            }
        };
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
        public boolean canAccessGroup(@Nonnull String user, @Nonnull String group) {
            return false;
        }

        @Nonnull
        @Override
        public Stream<String> getAssignedGroups(@Nonnull String user) {
            return Stream.empty();
        }

        @Nonnull
        @Override
        public Optional<Group> getGroup(@Nonnull String name) {
            return Optional.empty();
        }
    };

    private User getRoot() {
        return getUser("root", true);
    }

    private User getProjectOwner() {
        return getUser("project-owner", false);
    }
}
