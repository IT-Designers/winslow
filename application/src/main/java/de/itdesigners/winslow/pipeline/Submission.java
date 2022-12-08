package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.Requirements;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Submission {

    private final @Nonnull StageId id;
    private final          boolean configureOnly;

    private final @Nonnull Map<String, String>    envVarsStage    = new HashMap<>();
    private final @Nonnull Map<String, String>    envVarsPipeline = new HashMap<>();
    private final @Nonnull Map<String, String>    envVarsSystem   = new HashMap<>();
    private final @Nonnull Map<String, String>    envVarsInternal = new HashMap<>();
    private final @Nonnull WorkspaceConfiguration workspaceConfiguration;

    private final @Nonnull Map<Class<? extends Extension>, Extension> extensions = new HashMap<>();

    private @Nullable Requirements hardwareRequirements;
    private @Nullable String       workspaceDirectory;

    public Submission(
            @Nonnull StageId id,
            boolean configureOnly,
            @Nullable Requirements hardwareRequirements,
            @Nonnull WorkspaceConfiguration workspaceConfiguration) {
        this.id                     = Objects.requireNonNull(id);
        this.configureOnly          = configureOnly;
        this.hardwareRequirements   = hardwareRequirements;
        this.workspaceConfiguration = workspaceConfiguration;
    }

    @Nonnull
    @CheckReturnValue
    public StageId getId() {
        return id;
    }

    @CheckReturnValue
    public boolean isConfigureOnly() {
        return this.configureOnly;
    }

    @Nonnull
    @CheckReturnValue
    public Submission withHardwareRequirements(@Nonnull Requirements hardwareRequirements) {
        this.hardwareRequirements = hardwareRequirements;
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public Optional<Requirements> getHardwareRequirements() {
        return Optional.ofNullable(hardwareRequirements);
    }

    @Nonnull
    @CheckReturnValue
    public Submission withWorkspaceDirectory(@Nonnull String workspaceDirectory) {
        this.workspaceDirectory = workspaceDirectory;
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public Optional<String> getWorkspaceDirectory() {
        return Optional.ofNullable(workspaceDirectory);
    }

    @Nonnull
    @CheckReturnValue
    public WorkspaceConfiguration getWorkspaceConfiguration() {
        return workspaceConfiguration;
    }

    @Nonnull
    @CheckReturnValue
    public Iterable<String> getEnvVariableKeys() {
        return Stream
                .of(
                        this.envVarsSystem.keySet().stream(),
                        this.envVarsPipeline.keySet().stream(),
                        this.envVarsStage.keySet().stream(),
                        this.envVarsInternal.keySet().stream()
                )
                .flatMap(v -> v)
                .filter(key -> getEnvVariable(key).isPresent())
                .collect(Collectors.toSet());
    }

    @Nonnull
    @CheckReturnValue
    public Optional<String> getEnvVariable(@Nonnull String key) {
        // enable null-overwrite
        if (this.envVarsInternal.containsKey(key)) {
            return Optional.ofNullable(this.envVarsInternal.get(key));
        } else if (this.envVarsStage.containsKey(key)) {
            return Optional.ofNullable(this.envVarsStage.get(key));
        } else if (this.envVarsPipeline.containsKey(key)) {
            return Optional.ofNullable(this.envVarsPipeline.get(key));
        } else if (this.envVarsSystem.containsKey(key)) {
            return Optional.ofNullable(this.envVarsSystem.get(key));
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public Submission withInternalEnvVariable(@Nonnull String key, @Nonnull String value) {
        this.envVarsInternal.put(key, value);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public Map<String, String> getInternalEnvVariables() {
        return Collections.unmodifiableMap(this.envVarsInternal);
    }

    @Nonnull
    public Submission withPipelineEnvVariables(@Nonnull Map<? extends String, ? extends String> variables) {
        this.envVarsPipeline.putAll(variables);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public Map<String, String> getPipelineEnvVariables() {
        return Collections.unmodifiableMap(this.envVarsPipeline);
    }

    @Nonnull
    public Submission withSystemEnvVariables(@Nonnull Map<? extends String, ? extends String> variables) {
        this.envVarsSystem.putAll(variables);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public Map<String, String> getSystemEnvVariables() {
        return Collections.unmodifiableMap(this.envVarsSystem);
    }

    @Nonnull
    public Submission withStageEnvVariables(@Nonnull Map<? extends String, ? extends String> variables) {
        this.envVarsStage.putAll(variables);
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public Map<String, String> getStageEnvVariables() {
        return Collections.unmodifiableMap(this.envVarsStage);
    }


    @Nonnull
    @CheckReturnValue
    public TreeMap<String, String> getStageEnvVariablesReduced() {
        var envVars = new TreeMap<String, String>();
        this.envVarsStage
                .entrySet()
                .stream()
                .filter(e -> {
                    var key   = e.getKey();
                    var value = e.getValue();
                    return (!envVarsPipeline.containsKey(key) || !Objects.equals(value, envVarsPipeline.get(key)))
                            && (!envVarsSystem.containsKey(key) || !Objects.equals(value, envVarsSystem.get(key)));
                })
                .forEach(entry -> envVars.put(entry.getKey(), entry.getValue()));
        return envVars;
    }

    @Nonnull
    @CheckReturnValue
    public Map<String, String> getEffectiveEnvVariables() {
        var map = new HashMap<String, String>();

        for (var key : getEnvVariableKeys()) {
            getEnvVariable(key).ifPresent(value -> map.put(key, value));
        }

        return map;
    }


    @Nonnull
    @CheckReturnValue
    public Submission withExtension(@Nonnull Extension extension) throws ExtensionAlreadyRegisteredException {
        if (this.extensions.containsKey(extension.getClass())) {
            throw new ExtensionAlreadyRegisteredException(extension);
        } else {
            this.extensions.put(extension.getClass(), extension);
            return this;
        }
    }

    @Nonnull
    @CheckReturnValue
    public <T extends Extension> Optional<T> getExtension(@Nonnull Class<T> type) {
        return Optional.ofNullable(type.cast(this.extensions.get(type)));
    }

    @Nonnull
    @CheckReturnValue
    public Stream<? extends Extension> getExtensions() {
        return this.extensions.values().stream();
    }

    public static class ExtensionAlreadyRegisteredException extends RuntimeException {
        private final @Nonnull Extension extension;

        ExtensionAlreadyRegisteredException(@Nonnull Extension extension) {
            this.extension = extension;
        }

        @Nonnull
        public Extension getExtension() {
            return extension;
        }
    }

    public static class AlreadySubmittedException extends RuntimeException {

    }
}
