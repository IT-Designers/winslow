package de.itdesigners.winslow.docker;

import com.github.dockerjava.api.DockerClient;
import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.StageId;
import de.itdesigners.winslow.pipeline.Submission;
import de.itdesigners.winslow.pipeline.SubmissionResult;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

public class DockerBackend implements Backend, Closeable, AutoCloseable {

    private final @Nonnull DockerClient dockerClient;

    public DockerBackend(@Nonnull DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }


    @Nonnull
    @Override
    public Stream<String> listStages() throws IOException {
        return null;
    }

    @Nonnull
    @Override
    public Optional<State> getState(@Nonnull StageId stageId) throws IOException {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<State> getState(@Nonnull String pipeline, @Nonnull String stage) throws IOException {
        return Optional.empty();
    }

    @Override
    public void delete(@Nonnull String pipeline, @Nonnull String stage) throws IOException {

    }

    @Override
    public void stop(@Nonnull String stage) throws IOException {

    }

    @Override
    public void kill(@Nonnull String stage) throws IOException {

    }

    @Nonnull
    @Override
    public SubmissionResult submit(@Nonnull Submission submission) throws IOException {
        return null;
    }

    @Override
    public boolean isCapableOfExecuting(@Nonnull StageDefinition stage) {
        return false;
    }

    @Override
    public void close() throws IOException {

    }
}
