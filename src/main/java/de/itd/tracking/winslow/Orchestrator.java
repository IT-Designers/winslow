package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;

import java.util.Optional;

public interface Orchestrator {

    String start(Pipeline pipeline, Stage stage, Environment environment);
}
