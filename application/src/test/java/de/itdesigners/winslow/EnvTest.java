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

/*
    @Test
    @SetEnvironmentVariable(key = "WINSLOW_API_PATH", value = "/api/v1/noauth/")
    void whenGetApiNoAuthPathGivenApiPathIsTheSameAsHardCodedNoAuthPathThenThrowException() {
        assertThrows(IllegalArgumentException.class, Env::getApiNoAuthPath);
    }

    @Test
    @SetEnvironmentVariable(key = "WINSLOW_API_PATH", value = "/api/v2/auth")
    void whenGetApiNoAuthPathGivenApiPathIsDifferentThenReturnHardCodedNoAuthApiPath() {
        assertEquals("/api/v1/noauth/", Env.getApiNoAuthPath());
    }
*/

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

    @Test
    void whenGetRootUsersGivenRootUsersNotSetThenReturnEmptyList() {
        assertEquals(0, Env.getRootUsers().length);
    }

    @Test
    void whenIsLocalAuthEnabledGivenWINSLOW_AUTH_METHODIsLocalThenReturnTrue() {
        assertTrue(Env.isLocalAuthEnabled());
    }

    @Test
    void whenIsLocalAuthEnabledGivenWinslowAuthMethodIsUnsetAndWinslowDevEnvIsDisabledThenReturnTrue() {
        assertTrue(Env.isLocalAuthEnabled());
    }

    @Test
    @SetEnvironmentVariable(key = "WINSLOW_ROOT_USERS", value = "kristin-paul")
    void whenIsLocalAuthEnabledGivenWinslowAuthMethodIsSetAndWinslowDevEnvIsDisabledThenReturnFalse() {
        assertFalse(Env.isLocalAuthEnabled());
    }

}
