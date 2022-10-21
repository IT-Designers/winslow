package de.itdesigners.winslow;

import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.node.AllocInfo;
import de.itdesigners.winslow.api.settings.ResourceLimitation;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.project.Project;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class DistributedAllocationViewTests {

    public static final String      USER_ID     = "the-viewed-user-id";
    public static final String      PROJECT_ID  = "the-viewed-project-id";
    public static final AllocInfo[] ALLOCATIONS = new AllocInfo[]{
            new AllocInfo(
                    PROJECT_ID,
                    2,
                    1024,
                    0
            ),
            new AllocInfo(
                    "not-" + PROJECT_ID + "-1",
                    5,
                    4096,
                    1
            ),
            new AllocInfo(
                    "not-" + PROJECT_ID + "-2",
                    7,
                    1024,
                    2
            )
    };

    private Stream<AllocInfo> getAllocations() {
        return Stream.of(ALLOCATIONS);
    }

    @Test
    public void testCorrectSummary() {
        var view = new DistributedAllocationView(USER_ID, PROJECT_ID);
        view.loadAllocInfo(getAllocations(), projectId -> {
            if (PROJECT_ID.equals(projectId) || ("not-" + PROJECT_ID + "-1").equals(projectId)) {
                return Optional.of(getBasicProject(USER_ID, projectId, null));
            } else {
                return Optional.empty();
            }
        });

        assertEquals(new AllocInfo(PROJECT_ID, 2, 1024, 0), view.getProjectAllocation());
        assertEquals(new AllocInfo(USER_ID, 7, 5120, 1), view.getUserAllocation());
    }

    @Test
    public void testLoadsProjectResourceLimit() {
        var view = new DistributedAllocationView(USER_ID, PROJECT_ID);
        view.loadAllocInfo(getAllocations(), projectId -> {
            if (PROJECT_ID.equals(projectId)) {
                return Optional.of(getBasicProject(USER_ID, projectId, new ResourceLimitation(1L, 2L, 3L)));
            } else {
                return Optional.empty();
            }
        });

        assertEquals(new ResourceLimitation(1L, 2L, 3L), view.getProjectLimit());
    }

    @Test
    public void testExceedWithoutLimit() {
        var view = new DistributedAllocationView(USER_ID, PROJECT_ID);
        view.loadAllocInfo(getAllocations(), projectId -> {
            if (PROJECT_ID.equals(projectId)) {
                return Optional.of(getBasicProject(USER_ID, projectId, null));
            } else {
                return Optional.empty();
            }
        });

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.CPU, Long.MAX_VALUE)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, Long.MAX_VALUE)
                        .with(ResourceAllocationMonitor.StandardResources.GPU, Long.MAX_VALUE)
        ));
    }

    @Test
    public void testExceedWithoutLimit0NoGpu() {
        var view = new DistributedAllocationView(USER_ID, PROJECT_ID);
        view.loadAllocInfo(getAllocations(), projectId -> {
            if (PROJECT_ID.equals(projectId)) {
                return Optional.of(getBasicProject(USER_ID, projectId, null));
            } else {
                return Optional.empty();
            }
        });

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.CPU, 0L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 0L)
        ));
    }

    @Test
    public void testExceedWithoutLimit0() {
        var view = new DistributedAllocationView(USER_ID, PROJECT_ID);
        view.loadAllocInfo(getAllocations(), projectId -> {
            if (PROJECT_ID.equals(projectId)) {
                return Optional.of(getBasicProject(USER_ID, projectId, null));
            } else {
                return Optional.empty();
            }
        });

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.CPU, 0L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 0L)
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 0L)
        ));
    }

    @Test
    public void testExceedWithoutLimit1() {
        var view = new DistributedAllocationView(USER_ID, PROJECT_ID);
        view.loadAllocInfo(getAllocations(), projectId -> {
            if (PROJECT_ID.equals(projectId)) {
                return Optional.of(getBasicProject(USER_ID, projectId, null));
            } else {
                return Optional.empty();
            }
        });

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.CPU, 1L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 1L)
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 1L)
        ));
    }

    @Test
    public void testExceedWithShallowLimit() {
        var view = new DistributedAllocationView(USER_ID, PROJECT_ID);
        view.loadAllocInfo(getAllocations(), projectId -> {
            if (PROJECT_ID.equals(projectId)) {
                return Optional.of(getBasicProject(USER_ID, projectId, new ResourceLimitation(null, null, null)));
            } else {
                return Optional.empty();
            }
        });

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.CPU, Long.MAX_VALUE)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, Long.MAX_VALUE)
                        .with(ResourceAllocationMonitor.StandardResources.GPU, Long.MAX_VALUE)
        ));
    }

    @Test
    public void testExceedProjectLimit() {
        var view = new DistributedAllocationView(USER_ID, PROJECT_ID);
        view.loadAllocInfo(getAllocations(), projectId -> {
            if (ALLOCATIONS[0].getTitle().equals(projectId)) {
                return Optional.of(getBasicProject(USER_ID, projectId, new ResourceLimitation(
                        // the only associated AllocInfo should be the one with the exact id-match
                        ALLOCATIONS[0].getCpu() + 2L,
                        ALLOCATIONS[0].getMemory() + 3L,
                        ALLOCATIONS[0].getGpu() + 4L
                )));
            } else {
                return Optional.empty();
            }
        });

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>().with(
                        ResourceAllocationMonitor.StandardResources.CPU,
                        2L
                )
        ));
        assertTrue(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>().with(
                        ResourceAllocationMonitor.StandardResources.CPU,
                        3L
                )
        ));

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 3L)
        ));
        assertTrue(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 4L)
        ));

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 4L)
        ));
        assertTrue(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 5L)
        ));

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.CPU, 2L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 3L)
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 4L)
        ));
        assertTrue(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.CPU, 3L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 4L)
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 5L)
        ));
    }

    @Test
    public void testExceedUserLimit() {
        var view = new DistributedAllocationView(USER_ID, PROJECT_ID);
        view.loadAllocInfo(getAllocations(), projectId -> {
            if (PROJECT_ID.equals(projectId)) {
                return Optional.of(getBasicProject(USER_ID, projectId, null));
            } else {
                return Optional.empty();
            }
        });

        view.setUserLimit(new ResourceLimitation(
                // the only associated AllocInfo should be the one with the exact id-match
                ALLOCATIONS[0].getCpu() + 2L,
                ALLOCATIONS[0].getMemory() + 3L,
                ALLOCATIONS[0].getGpu() + 4L
        ));

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>().with(
                        ResourceAllocationMonitor.StandardResources.CPU,
                        2L
                )
        ));
        assertTrue(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>().with(
                        ResourceAllocationMonitor.StandardResources.CPU,
                        3L
                )
        ));

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 3L)
        ));
        assertTrue(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 4L)
        ));

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 4L)
        ));
        assertTrue(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 5L)
        ));

        assertFalse(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.CPU, 2L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 3L)
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 4L)
        ));
        assertTrue(view.wouldResourcesExceedLimit(
                new ResourceAllocationMonitor.ResourceSet<Long>()
                        .with(ResourceAllocationMonitor.StandardResources.CPU, 3L)
                        .with(ResourceAllocationMonitor.StandardResources.RAM, 4L)
                        .with(ResourceAllocationMonitor.StandardResources.GPU, 5L)
        ));
    }

    @Nonnull
    private Project getBasicProject(
            @Nonnull String ownerId,
            @Nonnull String projectId,
            @Nullable ResourceLimitation resourceLimitation) {
        return new Project(
                projectId,
                ownerId,
                (Iterable<Link>) null,
                null,
                "name-of-" + projectId,
                null,
                new PipelineDefinition(
                        "pipeline-definition-of-" + projectId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                resourceLimitation
        );
    }

}
