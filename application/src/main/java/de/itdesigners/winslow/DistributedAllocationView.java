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

    private @Nonnull AllocInfo accountAllocation;
    private @Nonnull AllocInfo projectAllocation;

    private @Nullable ResourceLimitation accountLimit;
    private @Nullable ResourceLimitation projectLimit;

    public DistributedAllocationView(@Nonnull String account, @Nonnull String project) {
        this.accountAllocation = new AllocInfo(account, 0, 0, 0);
        this.projectAllocation = new AllocInfo(project, 0, 0, 0);
    }

    public void setAccountLimit(@Nullable ResourceLimitation accountLimit) {
        this.accountLimit = accountLimit;
    }

    @Nullable
    public ResourceLimitation getAccountLimit() {
        return accountLimit;
    }

    @Nullable
    public ResourceLimitation getProjectLimit() {
        return projectLimit;
    }

    @Nonnull
    public AllocInfo getAccountAllocation() {
        return accountAllocation;
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
                if (Objects.equals(project.getAccountingGroup(), accountAllocation.title())) {
                    this.accountAllocation = this.accountAllocation.add(a);
                }
            });
        });
    }

    public boolean wouldResourcesExceedLimit(@Nonnull ResourceAllocationMonitor.ResourceSet<Long> resources) {
        return wouldResourcesExceedLimit(accountLimit, accountAllocation, resources)
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
