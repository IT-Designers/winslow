package de.itdesigners.winslow;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class BackoffTest {

    private JavaThreadSleepWrapper javaThreadSleepWrapper = mock(JavaThreadSleepWrapper.class);

    private Backoff underTest;


    @Test
    public void whenSleepThenCallJavaThreadSleepWrapper() throws InterruptedException {
        underTest = new Backoff(1, 100, 2, javaThreadSleepWrapper);
        underTest.sleep();

        verify(javaThreadSleepWrapper).sleep(anyLong());
    }

    @Test
    public void whenCreatingBackoffThenSleepInMsIsDefaultZero() {
        underTest = new Backoff(1, 100, 2, javaThreadSleepWrapper);

        assertEquals(0, underTest.getSleepInMs());
    }

    @Test
    public void whenResetGivenMinThenSetSleepInMsToMin() {
        underTest = new Backoff(1, 100, 2, javaThreadSleepWrapper);

        underTest.reset();

        assertEquals(1, underTest.getSleepInMs());
    }

    @Test
    public void whenGrowGivenSleepInMs_X_MultiplierIsGreaterThanMaxThenSetSleepInMsToMax() {
        underTest = new Backoff(1, 100, 101, javaThreadSleepWrapper);

        underTest.grow();
        underTest.grow();

        assertEquals(100, underTest.getSleepInMs());
    }

    @Test
    public void whenGrowGivenSleepInMs_X_MultiplierIsLessThanMinThenSetSleepInMsToMin() {
        underTest = new Backoff(1, 100, 2, javaThreadSleepWrapper);

        underTest.grow();

        assertEquals(1, underTest.getSleepInMs());
    }

    @Test
    public void whenGrowGivenSleepInMs_X_MultiplierIsBetweenMinAndMaxThenSetSleepInMsToX() {
        underTest = new Backoff(1, 100, 4, javaThreadSleepWrapper);

        underTest.grow();
        underTest.grow();

        assertEquals(4, underTest.getSleepInMs());
    }

    // k = #calls of grow()
    // f(k=0) = min
    // f(k=1) = f(k=0) * multiplier
    // f(k=2) = f(k=1) * multiplier
    @Test
    public void checkThatGrowFollowsTheFormula() {
        underTest = new Backoff(1, 100, 2, javaThreadSleepWrapper);

        underTest.grow();
        assertEquals(1, underTest.getSleepInMs());

        underTest.grow();
        assertEquals(2, underTest.getSleepInMs());

        underTest.grow();
        assertEquals(4, underTest.getSleepInMs());

        underTest.grow();
        assertEquals(8, underTest.getSleepInMs());
    }

    @Test
    public void whenSleepThenGrowIsCalledAfterSleep() {
        underTest = new Backoff(1, 100, 2, javaThreadSleepWrapper);

        assertEquals(0, underTest.getSleepInMs());

        underTest.sleep();

        assertEquals(1, underTest.getSleepInMs());
    }

}
