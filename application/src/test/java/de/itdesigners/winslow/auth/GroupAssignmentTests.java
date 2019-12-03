package de.itdesigners.winslow.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GroupAssignmentTests {

    @Test
    public void userCanResolveAssignedGroups() {
        GroupRepository groupRepository = new GroupRepository();
        UserRepository  userRepository  = new UserRepository(groupRepository);

        var bernd = userRepository.createUser("bernd", false);

        assertEquals(1, bernd.getGroups().count());
        assertEquals(bernd.getName(), bernd.getGroups().findFirst().get());

        var group = groupRepository.createGroup("bernd-group", false);
        group.withUser(bernd.getName());

        assertEquals(2, bernd.getGroups().count());
        assertEquals(group.getName(), bernd.getGroups().skip(1).findFirst().get());
    }
}
