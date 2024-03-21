package de.itdesigners.winslow;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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



}
