package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class PipelineExecutor implements Poll<Boolean, OrchestratorException> {

    private final Pipeline pipeline;
    private final Orchestrator orchestrator;
    private final Environment environment;

    private RunningStage runningStage;
    private Path         workspaceDirectory;
    private int          index = 0;

    public PipelineExecutor(Pipeline pipeline, Orchestrator orchestrator, Environment environment) {
        this.pipeline = pipeline;
        this.orchestrator = orchestrator;
        this.environment = environment;
    }

    @Nonnull
    public Optional<RunningStage> getCurrentStage() {
        return Optional.ofNullable(this.runningStage);
    }

    @Nonnull
    @Override
    public Optional<Boolean> poll() throws OrchestratorException {
        if (runningStage == null && index == 0) {
            this.init();
        }
        if (runningStage != null) {
            switch (runningStage.getState()) {
                default:
                    System.err.println("Unexpected State: " + runningStage.getState());
                case Preparing:
                case Running:
                    return Optional.empty();
                case Succeeded:
                    if (hasStagesRemaining()) {
                        this.startNextStage();
                        return Optional.empty();
                    } else {
                        return Optional.of(true);
                    }
                case Failed:
                    return Optional.of(false);
            }
        } else {
            return Optional.of(false);
        }
    }

    private void init() throws OrchestratorException {
        this.startNextStage();
    }

    private void startNextStage() throws OrchestratorException {
        if (hasStagesRemaining()) {
            Stage stage = this.pipeline.getStages().get(index);
            var prepared = this.orchestrator.prepare(this.pipeline, stage, this.environment);

            try {
                this.prepareNextWorkspaceDirectory(prepared.getWorkspaceDirectory());
            } catch (IOException e) {
                e.printStackTrace();
                throw new OrchestratorException("Failed to prepare workspace directory", e);
            }

            this.runningStage = prepared.start().orElseThrow();
            this.workspaceDirectory = prepared.getWorkspaceDirectory();
            this.index += 1;
        } else {
            this.runningStage = null;
        }
    }

    private void prepareNextWorkspaceDirectory(Path workspaceDirectory) throws IOException {
        if (this.workspaceDirectory != null) {
            var iterator = Files.list(this.workspaceDirectory).iterator();
            while (iterator.hasNext()) {
                var sourceAbsolute = iterator.next();
                var sourceRelative = this.workspaceDirectory.relativize(sourceAbsolute);
                var destination    = workspaceDirectory.resolve(sourceRelative);
                Files.copy(sourceAbsolute, destination);
            }
        }
    }

    private boolean hasStagesRemaining() {
        return this.index + 1 <= this.pipeline.getStages().size();
    }
}
