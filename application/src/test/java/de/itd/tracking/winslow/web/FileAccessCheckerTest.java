package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.auth.*;
import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.project.Project;
import de.itd.tracking.winslow.resource.PathConfiguration;
import de.itd.tracking.winslow.resource.ResourceManager;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileAccessCheckerTest {

    private Path              workDir;
    private PathConfiguration config;
    private FileAccessChecker checker;

    @Before
    public void before() {
        workDir = Path.of("/winslow");
        config  = new PathConfiguration();
        checker = new FileAccessChecker(
                new ResourceManager(workDir, config),
                id -> Optional.of(new Project(
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
                ))
        );
    }

    @Test
    public void anyValidUserCanAccessGlobalResources() {
        var user = new User("just-me", false, DUMMY_GROUP_RESOLVER);
        assertTrue(checker.isAllowedToAccessPath(user, config.getRelativePathOfResources().resolve("some-file")));
        assertTrue(checker.isAllowedToAccessPath(user, config.getRelativePathOfResources()));
    }

    @Test
    public void invalidUsersCantDoAThing() {
        assertFalse(checker.isAllowedToAccessPath(null, config.getRelativePathOfResources().resolve("some-file")));
        assertFalse(checker.isAllowedToAccessPath(null, config.getRelativePathOfResources()));
        assertFalse(checker.isAllowedToAccessPath(null, Path.of("/")));
    }

    @Test
    public void noneCanAccessWorkDir() {
        var root = new User("root", true, DUMMY_GROUP_RESOLVER);
        var user = new User("user", false, DUMMY_GROUP_RESOLVER);

        assertFalse(checker.isAllowedToAccessPath(root, workDir.relativize(workDir)));
        assertFalse(checker.isAllowedToAccessPath(user, workDir.relativize(workDir)));
    }

    @Test
    public void onlyRootCanAccessWorkspacesMainDirectory() {
        var root = new User("root", true, DUMMY_GROUP_RESOLVER);
        var user = new User("user", false, DUMMY_GROUP_RESOLVER);

        assertTrue(checker.isAllowedToAccessPath(root, config.getRelativePathOfWorkspaces()));
        assertFalse(checker.isAllowedToAccessPath(user, config.getRelativePathOfWorkspaces()));
    }

    @Test
    public void onlyPrivilegedUsersCanAccessWorkspacesOfProject() {
        var groupRepository = new GroupRepository();
        var userRepository  = new UserRepository(groupRepository);

        var root   = userRepository.getUser("root").orElseThrow();
        var owner  = userRepository.createUser("project-owner", false);
        var member = userRepository.createUser("project-member", false);
        var other  = userRepository.createUser("random-guy", false);

        groupRepository.createGroup("project-group", false).withUser(member.getName());

        var workspace = config.getRelativePathOfWorkspaces().resolve("workspace-id");

        assertTrue(checker.isAllowedToAccessPath(root,  workspace));
        assertTrue(checker.isAllowedToAccessPath(owner,  workspace));
        assertTrue(checker.isAllowedToAccessPath(member,  workspace));
        assertFalse(checker.isAllowedToAccessPath(other,  workspace));
    }

    @Test
    public void canNotAccessOutsideFilesNotEventAsSuperUser() {
        var superUser = new User("root", true, DUMMY_GROUP_RESOLVER);
        assertFalse(checker.isAllowedToAccessPath(superUser, Path.of("test")));
        assertFalse(checker.isAllowedToAccessPath(superUser, Path.of("../tmp")));
        assertFalse(checker.isAllowedToAccessPath(superUser, config.getRelativePathOfResources().resolve("../tmp")));
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

}
