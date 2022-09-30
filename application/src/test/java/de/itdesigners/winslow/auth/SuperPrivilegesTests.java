package de.itdesigners.winslow.auth;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SuperPrivilegesTests {

    @Test
    public void testSuperConstructorFlag() {
        assertTrue(new User(User.SUPER_USER_NAME, null).isSuperUser());
        assertTrue(new User(User.SUPER_USER_NAME, null).hasSuperPrivileges());
    }

    @Test
    public void testSuperUser() {
        GroupRepository groupRepository = new GroupRepository();
        UserRepository  userRepository  = new UserRepository(groupRepository);

        var root = userRepository.getUser(User.SUPER_USER_NAME);
        assertTrue(root.isPresent());
        assertTrue(root.get().isSuperUser());
        assertTrue(root.get().hasSuperPrivileges());
    }

    @Test
    public void testRandomUserInSuperGroupHavingSuperPrivileges() throws InvalidNameException, NameAlreadyInUseException, NameNotFoundException, LinkWithNameAlreadyExistsException {
        GroupRepository groupRepository = new GroupRepository();
        UserRepository  userRepository  = new UserRepository(groupRepository);

        var newUser   = userRepository.createUserWithoutGroup("random");
        var rootGroup = groupRepository.getGroup(Group.SUPER_GROUP_NAME).orElseThrow();

        assertFalse(newUser.isSuperUser());
        assertFalse(newUser.hasSuperPrivileges());

        groupRepository.addMemberToGroup(rootGroup.getName(), newUser.getName(), Role.MEMBER);

        assertFalse(newUser.isSuperUser());
        assertTrue(newUser.hasSuperPrivileges());
    }
}
