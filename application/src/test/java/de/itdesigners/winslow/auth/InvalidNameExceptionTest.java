package de.itdesigners.winslow.auth;

import org.junit.Test;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class InvalidNameExceptionTest {

    @Test
    public void givenSimpleValidNameNoThrows() {
        ensureNoThrowsForName(
                User.SUPER_USER_NAME,
                Group.SUPER_GROUP_NAME,
                "my-super.truper_name"
        );

        for (var prefix : Prefix.values()) {
            ensureNoThrowsForName(
                    prefix.wrap(User.SUPER_USER_NAME),
                    prefix.wrap(Group.SUPER_GROUP_NAME)
            );
        }
    }

    @Test
    public void throwsOnMalformedPrefix() {
        ensureThrowsForName("system:root", "system:::root", "system::r::oot", "/", "-name", ".name");
    }

    private void ensureThrowsForName(@Nonnull String... names) {
        for (var name : names) {
            try {
                InvalidNameException.ensureValid(name);
                fail("Did not throw for name '" + name + "'!");
            } catch (InvalidNameException e) {
                // fine
            }
        }
    }

    private void ensureNoThrowsForName(@Nonnull String... names) {
        for (var name : names) {
            try {
                assertEquals(
                        name,
                        InvalidNameException.ensureValid(name)
                );
            } catch (InvalidNameException e) {
                fail("Did throw for name '" + name + "'!");
            }
        }
    }
}
