package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.api.auth.Role;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class GroupAssignmentTests {

    @Test
    public void userCanResolveAssignedGroups() throws InvalidNameException, NameAlreadyInUseException, IOException {
        GroupManager groupManager = new GroupManager(new DummyGroupPersistence());
        UserManager  userManager  = new UserManager(groupManager);

        var bernd = userManager.createUserAndGroup("bernd");

        assertEquals(1, bernd.getGroups().size());
        assertEquals(Prefix.User.wrap(bernd.name()), bernd.getGroups().stream().findFirst().orElseThrow().name());
        assertEquals(
                bernd.name(),
                Prefix.unwrap(bernd.getGroups().stream().findFirst().orElseThrow().name()).orElseThrow()
        );

        var group = groupManager.createGroup("bernd-group", bernd.name(), Role.OWNER);

        assertEquals(2, bernd.getGroups().size());
        assertEquals(group.name(), bernd.getGroups().stream().skip(1).findFirst().orElseThrow().name());
    }
}
