package de.itd.tracking.winslow;

import de.itd.tracking.winslow.project.Project;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface Orchestrator {


    @Nonnull
    Optional<Submission> getSubmission(@Nonnull Project project);

    @Nonnull
    Optional<Submission> getSubmissionUnsafe(@Nonnull Project project);

    boolean canProgress(@Nonnull Project project);

    /**
     * Potentially faster implementation of {@link this#canProgress(Project)}
     * which in return might return true wrongfully.
     *
     * @param project
     * @return
     */
    boolean canProgressLockFree(@Nonnull Project project);

    boolean hasPendingChanges(@Nonnull Project project);

    void updateInternalState(@Nonnull Project project);

    @Nonnull
    Optional<Submission> startNext(@Nonnull Project project, @Nonnull Environment environment);

}
