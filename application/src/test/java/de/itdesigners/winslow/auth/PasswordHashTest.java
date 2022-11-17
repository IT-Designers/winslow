package de.itdesigners.winslow.auth;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PasswordHashTest {

    @Test
    public void testPasswordHash() {
        assertTrue(PasswordHash.calculate("password").isPasswordCorrect("password"));
    }

    @Test
    public void testWrongPasswordHash() {
        assertFalse(PasswordHash.calculate("password").isPasswordCorrect("passwort"));
    }
}
