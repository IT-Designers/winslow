package de.itdesigners.winslow;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourceAllocationMonitorTest {

    @Test
    public void testCouldReserveWithMatchingRequirements() {
        var set = new ResourceAllocationMonitor.ResourceSet<Long>()
                .with(ResourceAllocationMonitor.StandardResources.GPU, 2L)
                .with(ResourceAllocationMonitor.StandardResources.RAM, 1024L * 1024L * 1024L);
        var monitor = new ResourceAllocationMonitor().withResourcesAvailable(set);
        assertTrue(monitor.couldReserveConsideringReservations(set));
    }

    @Test
    public void testCouldReserveWithTooHighRequirements() {
        var monitor = new ResourceAllocationMonitor()
                .withResourcesAvailable(
                        new ResourceAllocationMonitor.ResourceSet<Long>()
                                .with(ResourceAllocationMonitor.StandardResources.GPU, 2L)
                                .with(ResourceAllocationMonitor.StandardResources.RAM, 1024L * 1024L * 1024L)
                );
        assertFalse(monitor.couldReserveConsideringReservations(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 3L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024L * 1024L * 1024L)
        ));
    }

    @Test
    public void testCouldReserveWithLowerRequirements() {
        var monitor = new ResourceAllocationMonitor()
                .withResourcesAvailable(
                        new ResourceAllocationMonitor.ResourceSet<Long>()
                                .with(ResourceAllocationMonitor.StandardResources.GPU, 2L)
                                .with(ResourceAllocationMonitor.StandardResources.RAM, 1024L * 1024L * 1024L)
                );
        assertTrue(monitor.couldReserveConsideringReservations(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 2L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024L * 1024L)
        ));
    }

    @Test
    public void testCouldReserveWithMultipleRequirements() {
        var token = "the-test-token";
        var monitor = new ResourceAllocationMonitor()
                .withResourcesAvailable(
                        new ResourceAllocationMonitor.ResourceSet<Long>()
                                .with(ResourceAllocationMonitor.StandardResources.GPU, 3L)
                                .with(ResourceAllocationMonitor.StandardResources.RAM, 1024 * 1024L * 1024L)
                );
        var firstSet = new ResourceAllocationMonitor.ResourceSet<Long>()
                .with(ResourceAllocationMonitor.StandardResources.GPU, 1L)
                .with(ResourceAllocationMonitor.StandardResources.RAM, 512 * 1024L * 1024L);
        assertTrue(monitor.couldReserveConsideringReservations(firstSet));
        monitor.reserve(token, firstSet);
        assertTrue(monitor.couldReserveConsideringReservations(firstSet));
        monitor.reserve(token, firstSet);
        assertFalse(monitor.couldReserveConsideringReservations(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 1L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024 * 1024L * 1024L)
        ));
        monitor.free(token, firstSet);
        assertFalse(monitor.couldReserveConsideringReservations(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 1L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024 * 1024L * 1024L)
        ));
        monitor.free(token, firstSet);
        assertTrue(monitor.couldReserveConsideringReservations(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 1L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1024 * 1024L * 1024L)
        ));
    }

    @Test
    public void testAversionOfGpuNodeIsHigherThanWithoutGpuNodeOnJobWithoutGpuRequirement() {
        var node    = createComputeNode();
        var gpuNode = createGpuNode();

        var computeTask = new ResourceAllocationMonitor.ResourceSet<Long>()
                .with(ResourceAllocationMonitor.StandardResources.CPU, 1L)
                .with(ResourceAllocationMonitor.StandardResources.RAM, 512 * 1024 * 1024L);

        assertTrue(node.getAversion(computeTask) < gpuNode.getAversion(computeTask));
    }

    @Test
    public void testAversionOfGpuNodeIsHigherThanWithoutGpuNodeOnJobWithoutGpuRequirementBeingEmpty() {
        var node    = createComputeNode();
        var gpuNode = createGpuNode();

        var computeTask = new ResourceAllocationMonitor.ResourceSet<Long>();

        assertTrue(node.getAversion(computeTask) < gpuNode.getAversion(computeTask));
    }

    @Test
    public void testAversionOfGpuNodeIsLowerThanWithoutGpuNodeOnJobWithGpuRequirement() {
        var node    = createComputeNode();
        var gpuNode = createGpuNode();

        var gpuTask = new ResourceAllocationMonitor.ResourceSet<Long>()
                .with(ResourceAllocationMonitor.StandardResources.GPU, 1L);

        assertTrue(node.getAversion(gpuTask) > gpuNode.getAversion(gpuTask));
    }

    @Test
    public void testAversionOfGpuNodeIsLowerThanWithoutGpuNodeOnJobWithGpuRequirementWhenAllGPUsAreInUse() {
        var node    = createComputeNode();
        var gpuNode = createGpuNode();

        var computeTask = new ResourceAllocationMonitor.ResourceSet<Long>();
        var gpuTask = new ResourceAllocationMonitor.ResourceSet<Long>()
                .with(ResourceAllocationMonitor.StandardResources.GPU, 1L);

        // reserve all GPUs
        gpuNode.reserve(
                "the-token",
                new ResourceAllocationMonitor.ResourceSet<Long>().with(
                        ResourceAllocationMonitor.StandardResources.GPU,
                        100L
                )
        );

        // cannot reserve further GPUs
        assertFalse(gpuNode.couldReserveConsideringReservations(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 1L)
        ));

        // still needs to consider itself as GPU node
        assertTrue(node.getAversion(gpuTask) > gpuNode.getAversion(gpuTask));
        assertTrue(node.getAversion(computeTask) < gpuNode.getAversion(computeTask));
    }

    private ResourceAllocationMonitor createGpuNode() {
        return new ResourceAllocationMonitor()
                .withResourcesAvailable(
                        new ResourceAllocationMonitor.ResourceSet<Long>()
                                .with(ResourceAllocationMonitor.StandardResources.CPU, 4L)
                                .with(ResourceAllocationMonitor.StandardResources.RAM, 1024L * 1024L * 1024L)
                                .with(ResourceAllocationMonitor.StandardResources.GPU, 100L)
                );
    }

    private ResourceAllocationMonitor createComputeNode() {
        return new ResourceAllocationMonitor()
                .withResourcesAvailable(
                        new ResourceAllocationMonitor.ResourceSet<Long>()
                                .with(ResourceAllocationMonitor.StandardResources.CPU, 1L)
                                .with(ResourceAllocationMonitor.StandardResources.RAM, 1024L * 1024L * 1024L)
                );
    }
}
