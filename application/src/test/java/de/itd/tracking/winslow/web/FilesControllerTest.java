package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.auth.Group;
import de.itd.tracking.winslow.auth.GroupAssignmentResolver;
import de.itd.tracking.winslow.auth.User;
import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.project.Project;
import de.itd.tracking.winslow.resource.PathConfiguration;
import de.itd.tracking.winslow.resource.ResourceManager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.Nonnull;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.*;
import java.util.stream.Stream;

import static junit.framework.TestCase.*;

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
                new PipelineDefinition(
                        "pipeline-definition",
                        null,
                        null,
                        Collections.emptyList(),
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
    public void testResourcesDownload() throws IOException {
        var response = controller.downloadResourceFile(
                constructRequest("sub/directory/def.txt"),
                getRoot()
        );
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(
                (DEF_TXT.getBytes(StandardCharsets.UTF_8).length),
                response.getBody().contentLength()
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
        assertEquals(
                (SOME_FILE.getBytes(StandardCharsets.UTF_8).length),
                response.getBody().contentLength()
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
        assertEquals(
                SOME_FILE.getBytes(StandardCharsets.UTF_8).length,
                response.getBody().contentLength()
        );
    }

    @Test
    public void testBasicWorkspaceListing() {
        var listing = controller.listWorkspaceDirectory(
                constructRequest("my-project-id/stage1"),
                getRoot()
        );
        assertNotNull(listing);
        var lookup = new HashMap<String, FilesController.FileInfo>();
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
                null
        );
        assertNotNull(listing);
        assertFalse(listing.iterator().hasNext());
    }

    @Test
    public void testBasicWorkspaceListingNotAllowed() {
        assertFalse(controller.listWorkspaceDirectory(
                constructRequest("my-project-id/stage1"),
                getUser("unknown-user", false)
        ).iterator().hasNext());
    }

    @Test
    public void testBasicWorkspaceListingProjectOwner() {
        assertTrue(controller.listWorkspaceDirectory(
                constructRequest("my-project-id/stage1"),
                getProjectOwner()
        ).iterator().hasNext());
    }

    @Test
    public void testBasicResourceListingUnauthorized() {
        var listing = controller.listResourceDirectory(constructRequest(""), null);
        assertNotNull(listing);
        assertFalse(listing.iterator().hasNext());
    }

    @Test
    public void testBasicResourceListing() {
        var listing = controller.listResourceDirectory(
                constructRequest(""),
                getRoot()
        );
        assertNotNull(listing);
        var lookup = new HashMap<String, FilesController.FileInfo>();
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
