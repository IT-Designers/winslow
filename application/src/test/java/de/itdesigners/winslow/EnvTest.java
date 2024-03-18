package de.itdesigners.winslow;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
