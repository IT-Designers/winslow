package de.itdesigners.winslow;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourceAllocationMonitorTest {

    @Test
    public void testCouldReserveWithMatchingRequirements() {
        var set = new ResourceAllocationMonitor.Set<Long>()
                .with(ResourceAllocationMonitor.StandardResources.GPU, 2L)
                .with(ResourceAllocationMonitor.StandardResources.RAM, 1024L * 1024L * 1024L);
        var monitor = new ResourceAllocationMonitor(set);
        assertTrue(monitor.couldReserveConsideringReservations(set));
    }

    @Test
    public void testCouldReserveWithTooHighRequirements() {
        var monitor = new ResourceAllocationMonitor(
                new ResourceAllocationMonitor.Set<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 2L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024L * 1024L * 1024L));
        assertFalse(monitor.couldReserveConsideringReservations(
                new ResourceAllocationMonitor.Set<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 3L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024L * 1024L * 1024L)
        ));
    }

    @Test
    public void testCouldReserveWithLowerRequirements() {
        var monitor = new ResourceAllocationMonitor(
                new ResourceAllocationMonitor.Set<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 2L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024L * 1024L * 1024L));
        assertTrue(monitor.couldReserveConsideringReservations(
                new ResourceAllocationMonitor.Set<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 2L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024L * 1024L)
        ));
    }

    @Test
    public void testCouldReserveWithMultipleRequirements() {
        var monitor = new ResourceAllocationMonitor(
                new ResourceAllocationMonitor.Set<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 3L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024 * 1024L * 1024L)
        );
        var firstSet = new ResourceAllocationMonitor.Set<Long>()
                .with(ResourceAllocationMonitor.StandardResources.GPU, 1L)
                .with(ResourceAllocationMonitor.StandardResources.RAM, 512 * 1024L * 1024L);
        assertTrue(monitor.couldReserveConsideringReservations(firstSet));
        monitor.reserve(firstSet);
        assertTrue(monitor.couldReserveConsideringReservations(firstSet));
        monitor.reserve(firstSet);
        assertFalse(monitor.couldReserveConsideringReservations(
                new ResourceAllocationMonitor.Set<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 1L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024 * 1024L * 1024L)
        ));
        monitor.free(firstSet);
        assertFalse(monitor.couldReserveConsideringReservations(
                new ResourceAllocationMonitor.Set<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 1L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024 * 1024L * 1024L)
        ));
        monitor.free(firstSet);
        assertTrue(monitor.couldReserveConsideringReservations(
                new ResourceAllocationMonitor.Set<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 1L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024 * 1024L * 1024L)
        ));
    }

}
