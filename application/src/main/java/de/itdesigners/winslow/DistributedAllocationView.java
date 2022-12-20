package de.itdesigners.winslow;

import de.itdesigners.winslow.api.node.AllocInfo;
import de.itdesigners.winslow.api.settings.ResourceLimitation;
import de.itdesigners.winslow.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class DistributedAllocationView {

    private @Nonnull AllocInfo userAllocation;
    private @Nonnull AllocInfo projectAllocation;

    private @Nullable ResourceLimitation userLimit;
    private @Nullable ResourceLimitation projectLimit;

    public DistributedAllocationView(@Nonnull String user, @Nonnull String project) {
        this.userAllocation    = new AllocInfo(user, 0, 0, 0);
        this.projectAllocation = new AllocInfo(project, 0, 0, 0);
    }

    public void setUserLimit(@Nullable ResourceLimitation userLimit) {
        this.userLimit = userLimit;
    }

    @Nullable
    public ResourceLimitation getUserLimit() {
        return userLimit;
    }

    @Nullable
    public ResourceLimitation getProjectLimit() {
        return projectLimit;
    }

    @Nonnull
    public AllocInfo getUserAllocation() {
        return userAllocation;
    }

    @Nonnull
    public AllocInfo getProjectAllocation() {
        return projectAllocation;
    }

    public void loadAllocInfo(
            @Nonnull Stream<AllocInfo> allocated,
            @Nonnull Function<String, Optional<Project>> projects) {
        allocated.forEach(a -> {
            if (Objects.equals(a.title(), projectAllocation.title())) {
                this.projectAllocation = this.projectAllocation.add(a);
            }

            projects.apply(a.title()).ifPresent(project -> {
                if (Objects.equals(a.title(), projectAllocation.title())) {
                    this.projectLimit = project.getResourceLimitation().orElse(null);
                }
                if (Objects.equals(project.getOwner(), userAllocation.title())) {
                    this.userAllocation = this.userAllocation.add(a);
                }
            });
        });
    }

    public boolean wouldResourcesExceedLimit(@Nonnull ResourceAllocationMonitor.ResourceSet<Long> resources) {
        return wouldResourcesExceedLimit(userLimit, userAllocation, resources)
                || wouldResourcesExceedLimit(projectLimit, projectAllocation, resources);
    }

    private static boolean wouldResourcesExceedLimit(
            @Nullable ResourceLimitation limit,
            @Nonnull AllocInfo allocation,
            @Nonnull ResourceAllocationMonitor.ResourceSet<Long> resources) {
        if (limit != null) {
            var maxUserAllocCpu = (limit.cpu() != null ? limit.cpu() - allocation.cpu() : Long.MAX_VALUE);
            var maxUserAllocMem = (limit.mem() != null ? limit.mem() - allocation.memory() : Long.MAX_VALUE);
            var maxUserAllocGpu = (limit.gpu() != null ? limit.gpu() - allocation.gpu() : Long.MAX_VALUE);

            return resources.getOrDefault(ResourceAllocationMonitor.StandardResources.CPU, 0L) > maxUserAllocCpu
                    || resources.getOrDefault(ResourceAllocationMonitor.StandardResources.RAM, 0L) > maxUserAllocMem
                    || resources.getOrDefault(ResourceAllocationMonitor.StandardResources.GPU, 0L) > maxUserAllocGpu;
        } else {
            return false;
        }
    }
}
