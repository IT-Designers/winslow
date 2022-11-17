package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.auth.Role;
import de.itdesigners.winslow.auth.*;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.resource.PathConfiguration;
import de.itdesigners.winslow.resource.ResourceManager;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileAccessCheckerTest {

    private Path              workDir;
    private PathConfiguration config;
    private FileAccessChecker checker;

    private boolean publicProject = false;

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
                        publicProject,
                        new PipelineDefinition(
                                "pipeline-definition",
                                null,
                                null,
                                Collections.emptyList(),
                                null,
                                null,
                                null
                        ),
                        null
                ))
        );
    }

    @Test
    public void anyValidUserCanAccessGlobalResources() {
        var user = new User("just-me", null, null, null, DUMMY_GROUP_RESOLVER);
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
        var root = new User(User.SUPER_USER_NAME, null, null, null, DUMMY_GROUP_RESOLVER);
        var user = new User("not_" + User.SUPER_USER_NAME, null, null, null, DUMMY_GROUP_RESOLVER);

        assertFalse(checker.isAllowedToAccessPath(root, workDir.relativize(workDir)));
        assertFalse(checker.isAllowedToAccessPath(user, workDir.relativize(workDir)));
    }

    @Test
    public void everyoneCanAccessWorkspacesMainDirectory() {
        var root = new User(User.SUPER_USER_NAME, null, null, null, DUMMY_GROUP_RESOLVER);
        var user = new User("not_" + User.SUPER_USER_NAME, null, null, null, DUMMY_GROUP_RESOLVER);

        assertTrue(checker.isAllowedToAccessPath(root, config.getRelativePathOfWorkspaces()));
        assertTrue(checker.isAllowedToAccessPath(user, config.getRelativePathOfWorkspaces()));
    }

    @Test
    public void onlyPrivilegedUsersCanAccessWorkspacesOfProject() throws InvalidNameException, NameAlreadyInUseException, IOException {
        var groupRepository = new GroupManager(new DummyGroupPersistence());
        var userRepository  = new UserManager(groupRepository);

        var root   = userRepository.getUser("root").orElseThrow();
        var owner  = userRepository.createUserWithoutGroup("project-owner");
        var member = userRepository.createUserWithoutGroup("project-member");
        var other  = userRepository.createUserWithoutGroup("random-guy");

        groupRepository.createGroup("project-group", member.name(), Role.MEMBER);

        var workspace = config.getRelativePathOfWorkspaces().resolve("workspace-id");

        assertTrue(checker.isAllowedToAccessPath(root, workspace));
        assertTrue(checker.isAllowedToAccessPath(owner, workspace));
        assertTrue(checker.isAllowedToAccessPath(member, workspace));
        assertFalse(checker.isAllowedToAccessPath(other, workspace));
    }

    @Test
    public void canNotAccessOutsideFilesNotEventAsSuperUser() {
        var superUser = new User(User.SUPER_USER_NAME, null, null, null, DUMMY_GROUP_RESOLVER);
        assertFalse(checker.isAllowedToAccessPath(superUser, Path.of("test")));
        assertFalse(checker.isAllowedToAccessPath(superUser, Path.of("../tmp")));
        assertFalse(checker.isAllowedToAccessPath(superUser, config.getRelativePathOfResources().resolve("../tmp")));
    }

    @Test
    public void canAccessPublicProjectAsNonSuperuserAndNonMember() {
        var waldo = new User("waldo", null, null, null, DUMMY_GROUP_RESOLVER);

        this.publicProject = true;
        var workspace = config.getRelativePathOfWorkspaces().resolve("workspace-id");

        assertTrue(checker.isAllowedToAccessPath(waldo, workspace));
    }


    private static final GroupAssignmentResolver DUMMY_GROUP_RESOLVER = new GroupAssignmentResolver() {
        @Override
        public boolean isPartOfGroup(@Nonnull String user, @Nonnull String group) {
            return false;
        }

        @Nonnull
        @Override
        public List<Group> getAssignedGroups(@Nonnull String user) {
            return Collections.emptyList();
        }

        @Nonnull
        @Override
        public Optional<Group> getGroup(@Nonnull String name) {
            return Optional.empty();
        }
    };

}
