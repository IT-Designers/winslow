package de.itdesigners.winslow.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GroupAssignmentTests {

    @Test
    public void userCanResolveAssignedGroups() throws InvalidNameException, NameAlreadyInUseException {
        GroupRepository groupRepository = new GroupRepository();
        UserRepository  userRepository  = new UserRepository(groupRepository);

        var bernd = userRepository.createUserAndGroup("bernd");

        assertEquals(1, bernd.getGroups().count());
        assertEquals(Prefix.User.wrap(bernd.getName()), bernd.getGroups().findFirst().orElseThrow().getName());
        assertEquals(bernd.getName(), Prefix.unwrap(bernd.getGroups().findFirst().orElseThrow().getName()).orElseThrow());

        var group = groupRepository.createGroup("bernd-group", bernd.getName(), Role.OWNER);

        assertEquals(2, bernd.getGroups().count());
        assertEquals(group.getName(), bernd.getGroups().skip(1).findFirst().orElseThrow().getName());
    }
}
