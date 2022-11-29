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
    public void testPasswordHash01234567() {
        assertTrue(PasswordHash.calculate("01234567").isPasswordCorrect("01234567"));
    }

    @Test
    public void testPasswordHash0123456789() {
        assertTrue(PasswordHash.calculate("0123456789").isPasswordCorrect("0123456789"));
    }


    @Test
    public void testPasswordHash0123456789Wrong() {
        assertFalse(PasswordHash.calculate("0123456788").isPasswordCorrect("0123456789"));
    }

    @Test
    public void testPasswordHash01234567890123456789() {
        assertTrue(PasswordHash.calculate("01234567890123456789").isPasswordCorrect("01234567890123456789"));
    }

    @Test
    public void testWrongPasswordHash() {
        assertFalse(PasswordHash.calculate("password").isPasswordCorrect("passwort"));
    }
}
