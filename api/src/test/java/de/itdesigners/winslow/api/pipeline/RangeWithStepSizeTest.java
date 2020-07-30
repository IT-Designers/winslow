package de.itdesigners.winslow.api.pipeline;

import org.junit.Test;

import static org.junit.Assert.*;

public class RangeWithStepSizeTest {

    @Test
    public void testMinMaxReOrderInConstructor0and1() {
        var range = new RangeWithStepSize(0, 1, 1);
        assertEquals(0, range.getMin(), 0.0f);
        assertEquals(1, range.getMax(), 0.0f);
        assertEquals(1, range.getStepSize(), 0.0f);
    }

    @Test
    public void testMinMaxReOrderInConstructor1and0() {
        var range = new RangeWithStepSize(1, 0, 1);
        assertEquals(0, range.getMin(), 0.0f);
        assertEquals(1, range.getMax(), 0.0f);
        assertEquals(1, range.getStepSize(), 0.0f);
    }

    @Test
    public void testMinMaxReOrderInConstructor11andNeg2() {
        var range = new RangeWithStepSize(11, -2, -1);
        assertEquals(-2, range.getMin(), 0.0f);
        assertEquals(11, range.getMax(), 0.0f);
        assertEquals(1, range.getStepSize(), 0.0f);
    }

    @Test
    public void testMinMaxReOrderInConstructorNeg1and0() {
        var range = new RangeWithStepSize(-1, 0, -1);
        assertEquals(-1, range.getMin(), 0.0f);
        assertEquals(0, range.getMax(), 0.0f);
        assertEquals(1, range.getStepSize(), 0.0f);
    }

    @Test
    public void testStepCountOnAlignedMinMaxStepSize() {
        var range = new RangeWithStepSize(1, 9, 1);
        assertEquals(9, range.getStepCount());
        assertEquals("1", range.getValue(0));
        assertEquals("2", range.getValue(1));
        assertEquals("3", range.getValue(2));
        assertEquals("4", range.getValue(3));
        assertEquals("5", range.getValue(4));
        assertEquals("6", range.getValue(5));
        assertEquals("7", range.getValue(6));
        assertEquals("8", range.getValue(7));
        assertEquals("9", range.getValue(8));
    }

    @Test
    public void testStepCountOnNotAlignedMinMaxStepSize() {
        var range = new RangeWithStepSize(1.5f, 8.f, 1.75f);
        assertEquals(5, range.getStepCount());
        assertEquals(String.valueOf(1.50f), range.getValue(0));
        assertEquals(String.valueOf(3.25f), range.getValue(1));
        assertEquals(String.valueOf(5.00f), range.getValue(2));
        assertEquals(String.valueOf(6.75f), range.getValue(3));
        assertEquals(String.valueOf(8.00f), range.getValue(4));
    }

    @Test
    public void testGetValueWithOutOfBoundsIndex() {
        var range = new RangeWithStepSize(1, 3, 1);
        assertEquals(String.valueOf(1), range.getValue(-4));
        assertEquals(String.valueOf(1), range.getValue(-1));
        assertEquals(String.valueOf(1), range.getValue(0));
        assertEquals(String.valueOf(2), range.getValue(1));
        assertEquals(String.valueOf(3), range.getValue(2));
        assertEquals(String.valueOf(3), range.getValue(5));
        assertEquals(String.valueOf(3), range.getValue(8999));
    }

    @Test
    public void testGetValueReturnsIntegerOnIntegerRange() {
        var range = new RangeWithStepSize(1, 9, 1);
        for (int i = 0; i < range.getStepCount() * 2; ++i) {
            var n = i - (range.getStepCount() / 2);
            try {
                Integer.parseInt(range.getValue(n));
            } catch (NumberFormatException e) {
                fail(e.getMessage());
            }
        }
        assertEquals(9, range.getStepCount());
    }

    @Test
    public void testGetValueNotReturnsIntegerOnNonIntegerMin() {
        var range = new RangeWithStepSize(1.1f, 9, 1);
        assertEquals(9, range.getStepCount());
        for (int i = 0; i < range.getStepCount() * 2; ++i) {
            var n = i - (range.getStepCount() / 2);
            assertThrows(
                    NumberFormatException.class,
                    () -> Integer.parseInt(range.getValue(n))
            );
        }
    }

    @Test
    public void testGetValueNotReturnsIntegerOnNonIntegerMax() {
        var range = new RangeWithStepSize(1, 8.8f, 1);
        assertEquals(9, range.getStepCount());
        for (int i = 0; i < range.getStepCount() * 2; ++i) {
            var n = i - (range.getStepCount() / 2);
            assertThrows(
                    NumberFormatException.class,
                    () -> Integer.parseInt(range.getValue(n))
            );
        }
    }

    @Test
    public void testGetValueNotReturnsIntegerOnNonIntegerStep() {
        var range = new RangeWithStepSize(1, 9f, 1.1f);
        assertEquals(9, range.getStepCount());
        for (int i = 0; i < range.getStepCount() * 2; ++i) {
            var n = i - (range.getStepCount() / 2);
            assertThrows(
                    NumberFormatException.class,
                    () -> Integer.parseInt(range.getValue(n))
            );
        }
    }
}
