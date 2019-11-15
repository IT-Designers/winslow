package de.itd.tracking.winslow.auth;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SuperPrivilegesTests {

    @Test
    public void testSuperConstructorFlag() {
        assertTrue(new User("user", true, null).isSuperUser());
        assertTrue(new User("user", true, null).hasSuperPrivileges());
    }

    @Test
    public void testSuperUser() {
        GroupRepository groupRepository = new GroupRepository();
        UserRepository userRepository = new UserRepository(groupRepository);

        var root = userRepository.getUser(UserRepository.SUPERUSER);
        assertTrue(root.isPresent());
        assertTrue(root.get().isSuperUser());
        assertTrue(root.get().hasSuperPrivileges());
    }

    @Test
    public void testRandomUserInSuperGroupHavingSuperPrivileges() {
        GroupRepository groupRepository = new GroupRepository();
        UserRepository userRepository = new UserRepository(groupRepository);

        var newUser = userRepository.createUser("random", false);
        var rootGroup = groupRepository.getGroup(GroupRepository.SUPERGROUP).get();

        assertFalse(newUser.isSuperUser());
        assertFalse(newUser.hasSuperPrivileges());

        rootGroup.withUser(newUser.getName());

        assertFalse(newUser.isSuperUser());
        assertTrue(newUser.hasSuperPrivileges());
    }
}
