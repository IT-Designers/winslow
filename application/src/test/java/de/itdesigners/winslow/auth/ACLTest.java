package de.itdesigners.winslow.auth;

import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ACLTest {

    private GroupManager groups;
    private UserManager  users;

    @Before
    public void reset() throws IOException {
        this.groups = new GroupManager(new DummyGroupPersistence());
        this.users  = new UserManager(new DummyUserPersistence(), this.groups);
    }

    @Test
    public void testLinkedUserCanAccess() throws InvalidNameException, IOException, NameAlreadyInUseException {
        var roles = Role.values();

        for (int offset = 0; offset < roles.length; ++offset) {
            reset();

            var user  = users.createUserAndGroup("user");
            var offsetRole = roles[offset];
            groups.createGroup("group", "user", offsetRole);

            for (int n = offset; n < roles.length; ++n) {
                final var role = roles[n];
                assertTrue(
                        "User " + user.name() + " should have access via role " + role,
                        ACL.canUserAccess(user, List.of(new Link("user::" + user.name(), role)))
                );
            }

            assertTrue(
                    "User " + user.name() + " should have access via role " + offsetRole + " and group",
                    ACL.canUserAccess(user, List.of(new Link("group", offsetRole)))
            );
        }
    }

    @Test
    public void testNotLinkedUserCannotAccess() throws InvalidNameException, IOException, NameAlreadyInUseException {
        var roles = Role.values();

        for (int offset = roles.length -1; offset >= 0; --offset) {
            reset();

            var user = users.createUserAndGroup("user");
            groups.createGroup("group");

            for (int n = 0; n < offset; ++n) {
                final var role = roles[n];
                assertFalse(
                        "User " + user.name() + " should not have access via role " + role,
                        ACL.canUserAccess(user, List.of(new Link("group", role)))
                );
            }
        }
    }

}
