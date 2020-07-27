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
        assertEquals(1, range.getValue(0).floatValue(), 0);
        assertEquals(2, range.getValue(1).floatValue(), 0);
        assertEquals(3, range.getValue(2).floatValue(), 0);
        assertEquals(4, range.getValue(3).floatValue(), 0);
        assertEquals(5, range.getValue(4).floatValue(), 0);
        assertEquals(6, range.getValue(5).floatValue(), 0);
        assertEquals(7, range.getValue(6).floatValue(), 0);
        assertEquals(8, range.getValue(7).floatValue(), 0);
        assertEquals(9, range.getValue(8).floatValue(), 0);
    }

    @Test
    public void testStepCountOnNotAlignedMinMaxStepSize() {
        var range = new RangeWithStepSize(1.5f, 8.f, 1.75f);
        assertEquals(5, range.getStepCount());
        assertEquals(1.50f, range.getValue(0).floatValue(), 0);
        assertEquals(3.25f, range.getValue(1).floatValue(), 0);
        assertEquals(5.00f, range.getValue(2).floatValue(), 0);
        assertEquals(6.75f, range.getValue(3).floatValue(), 0);
        assertEquals(8.00f, range.getValue(4).floatValue(), 0);
    }

    @Test
    public void testGetValueWithOutOfBoundsIndex() {
        var range = new RangeWithStepSize(1, 3, 1);
        assertEquals(1, range.getValue(-4).floatValue(), 0f);
        assertEquals(1, range.getValue(-1).floatValue(), 0f);
        assertEquals(1, range.getValue(0).floatValue(), 0f);
        assertEquals(2, range.getValue(1).floatValue(), 0f);
        assertEquals(3, range.getValue(2).floatValue(), 0f);
        assertEquals(3, range.getValue(5).floatValue(), 0f);
        assertEquals(3, range.getValue(8999).floatValue(), 0f);
    }

    @Test
    public void testGetValueReturnsIntegerOnIntegerRange() {
        var range = new RangeWithStepSize(1, 9, 1);
        for (int i = 0; i < range.getStepCount() * 2; ++i) {
            var n = i - (range.getStepCount() / 2);
            assertTrue(range.getValue(n) instanceof Integer);
        }
        assertEquals(9, range.getStepCount());
        assertTrue(range.getValue(-2) instanceof Integer);
        assertTrue(range.getValue(-1) instanceof Integer);
        assertTrue(range.getValue(0) instanceof Integer);
        assertTrue(range.getValue(1) instanceof Integer);
        assertTrue(range.getValue(2) instanceof Integer);
        assertTrue(range.getValue(3) instanceof Integer);
        assertTrue(range.getValue(4) instanceof Integer);
        assertTrue(range.getValue(5) instanceof Integer);
        assertTrue(range.getValue(6) instanceof Integer);
        assertTrue(range.getValue(7) instanceof Integer);
        assertTrue(range.getValue(8) instanceof Integer);
        assertTrue(range.getValue(9) instanceof Integer);
        assertTrue(range.getValue(10) instanceof Integer);
        assertTrue(range.getValue(11) instanceof Integer);
    }

    @Test
    public void testGetValueNotReturnsIntegerOnNonIntegerMin() {
        var range = new RangeWithStepSize(1.1f, 9, 1);
        assertEquals(9, range.getStepCount());
        for (int i = 0; i < range.getStepCount() * 2; ++i) {
            var n = i - (range.getStepCount() / 2);
            assertFalse(range.getValue(n) instanceof Integer);
        }
    }

    @Test
    public void testGetValueNotReturnsIntegerOnNonIntegerMax() {
        var range = new RangeWithStepSize(1, 8.8f, 1);
        assertEquals(9, range.getStepCount());
        for (int i = 0; i < range.getStepCount() * 2; ++i) {
            var n = i - (range.getStepCount() / 2);
            assertFalse(range.getValue(n) instanceof Integer);
        }
    }

    @Test
    public void testGetValueNotReturnsIntegerOnNonIntegerStep() {
        var range = new RangeWithStepSize(1, 9f, 1.1f);
        assertEquals(9, range.getStepCount());
        for (int i = 0; i < range.getStepCount() * 2; ++i) {
            var n = i - (range.getStepCount() / 2);
            assertFalse(range.getValue(n) instanceof Integer);
        }
    }
}
