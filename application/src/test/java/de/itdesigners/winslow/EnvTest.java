package de.itdesigners.winslow;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

class EnvTest {

    @Nested
    class LockDurationMsTests {

        @Test
        void whenLockDurationMsGivenDefaultOf5minThenReturnItInMilliseconds() {
            assertEquals(300_000, Env.lockDurationMs());
        }

        @Test
        @SetEnvironmentVariable(key = "WINSLOW_LOCK_DURATION_MS", value = "10")
        void whenLockDurationMsGiven10SecondsThenReturnIt() {
            assertEquals(10_000, Env.lockDurationMs());
        }

        @Test
        @SetEnvironmentVariable(key = "WINSLOW_LOCK_DURATION_MS", value = "9")
        void whenLockDurationMsGivenValueSmallerThan10SecondsThenReturn10Seconds() {
            assertEquals(10_000, Env.lockDurationMs());
        }

        @Test
        @SetEnvironmentVariable(key = "WINSLOW_LOCK_DURATION_MS", value = "3600000")
        void whenLockDurationMsGivenValueOf1hThenReturn10Seconds() {
            assertEquals(3_600_000, Env.lockDurationMs());
        }
    }


    @Test
    void whenGetApiNoAuthPathGivenNoApiPathThenReturnDefaultNoAuthPath() {
        assertEquals("/api/v1/noauth/", Env.getApiNoAuthPath());
    }

    @Test
    void whenGetRootUsersGivenNoRootUsersThenReturnEmptyList() {
        assertEquals(0, Env.getRootUsers().length);
    }

    @Test
    @SetEnvironmentVariable(key = "WINSLOW_ROOT_USERS", value = "kristin")
    void whenGetRootUsersGivenOneUserThenReturnOneUser() {
        assertEquals(1, Env.getRootUsers().length);
        assertEquals("kristin", Env.getRootUsers()[0]);
    }

    @Test
    @SetEnvironmentVariable(key = "WINSLOW_ROOT_USERS", value = "kristin,paul")
    void whenGetRootUsersGivenTwoUsersThenReturnTwoUsers() {
        assertEquals(2, Env.getRootUsers().length);
        assertEquals("kristin", Env.getRootUsers()[0]);
        assertEquals("paul", Env.getRootUsers()[1]);

    }

    @Test
    @SetEnvironmentVariable(key = "WINSLOW_ROOT_USERS", value = "kristin-paul")
    void whenGetRootUsersGivenTwoUsersButNotKommaSeperatedThenReturnItAsOneUser() {
        assertEquals(1, Env.getRootUsers().length);
        assertEquals("kristin-paul", Env.getRootUsers()[0]);
    }

    @Nested
    class IsLocalAuthEnabledTests {
        // These are white box tests, not refactoring stable!!!

        //explicit
        @Test
        @SetEnvironmentVariable(key = "WINSLOW_AUTH_METHOD", value = "local")
        void whenIsLocalAuthEnabledGivenWINSLOW_AUTH_METHODIsLocalThenReturnTrue() {
            assertTrue(Env.isLocalAuthEnabled());
        }

        @Test
        @SetEnvironmentVariable(key = "WINSLOW_AUTH_METHOD", value = "!local")
        void whenIsLocalAuthEnabledGivenWINSLOW_AUTH_METHODIsNotLocalThenReturnFalse() {
            assertFalse(Env.isLocalAuthEnabled());
        }

        //implicit
        @Test
        void whenIsLocalAuthEnabledGivenWINSLOW_AUTH_METHOD_IsNullAnd_WINSLOW_DEV_ENV_IsNullThenReturnTrue() {
            assertTrue(Env.isLocalAuthEnabled());
        }

        @Test
        @SetEnvironmentVariable(key = "WINSLOW_DEV_ENV", value = "1")
        void whenIsLocalAuthEnabledGivenWINSLOW_AUTH_METHOD_IsNullAnd_WINSLOW_DEV_ENV_IsEnabledThenReturnFalse() {
            assertFalse(Env.isLocalAuthEnabled());
        }

        @Test
        @SetEnvironmentVariable(key = "WINSLOW_DEV_ENV", value = "0")
        @SetEnvironmentVariable(key = "WINSLOW_AUTH_METHOD", value = "!local")
        void whenIsLocalAuthEnabledGivenWINSLOW_AUTH_METHOD_IsNotLocalAnd_WINSLOW_DEV_ENV_IsDisabledThenReturnFalse() {
            assertFalse(Env.isLocalAuthEnabled());
        }
    }

    @Test
    void whenGetRootUsersGivenRootUsersNotSetThenReturnEmptyList() {
        assertEquals(0, Env.getRootUsers().length);
    }

}
