package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.api.auth.Role;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SuperPrivilegesTests {

    @Test
    public void testSuperConstructorFlag() {
        assertTrue(new User(User.SUPER_USER_NAME, null, null, true, null, null).isSuperUser());
        assertTrue(new User(User.SUPER_USER_NAME, null, null, true, null, null).hasSuperPrivileges());
    }

    @Test
    public void testSuperUser() throws IOException {
        GroupManager groupManager = new GroupManager(new DummyGroupPersistence());
        UserManager  userManager  = new UserManager(new DummyUserPersistence(), groupManager);

        var root = userManager.getUser(User.SUPER_USER_NAME);
        assertTrue(root.isPresent());
        assertTrue(root.get().isSuperUser());
        assertTrue(root.get().hasSuperPrivileges());
    }

    @Test
    public void testRandomUserInSuperGroupHavingSuperPrivileges() throws InvalidNameException, NameAlreadyInUseException, NameNotFoundException, LinkWithNameAlreadyExistsException, IOException {
        GroupManager groupManager = new GroupManager(new DummyGroupPersistence());
        UserManager  userManager  = new UserManager(new DummyUserPersistence(), groupManager);

        var newUser   = userManager.createUserWithoutGroup("random");
        var rootGroup = groupManager.getGroup(Group.SUPER_GROUP_NAME).orElseThrow();

        assertFalse(newUser.isSuperUser());
        assertFalse(newUser.hasSuperPrivileges());

        groupManager.addMemberToGroup(rootGroup.name(), newUser.name(), Role.MEMBER);

        assertFalse(newUser.isSuperUser());
        assertTrue(newUser.hasSuperPrivileges());
    }
}
