package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.*;

public class StageDefinition {

    private final @Nonnull UUID                id;
    private final @Nonnull String              name;
    private final @Nonnull String              desc;
    private final @Nonnull Image               image;
    private final @Nonnull Requirements        requires;
    private final @Nonnull UserInput           userInput;
    private final @Nonnull Map<String, String> env;
    private final @Nonnull Highlight           highlight;
    private final          boolean             discardable;
    private final          boolean             privileged;
    private final @Nonnull List<LogParser>     logParsers;
    private final          boolean             ignoreFailuresWithinExecutionGroup;
    private final @Nonnull List<String>        tags;
    private final @Nonnull Map<String, String> result;
    private final @Nonnull StageType           type;
    private final @Nonnull List<UUID>          nextStages;

    @ConstructorProperties({"id", "name", "description", "image", "requirements", "requires", "environment", "highlight", "discardable", "privileged", "logParsers", "ignoreFailuresWithinExecutionGroup", "tags", "result", "type", "nextStages"})
    public StageDefinition(
            // null-able for backwards compatibility
            @Nullable UUID id,
            @Nonnull String name,
            @Nullable String description,
            @Nullable Image image,
            @Nullable Requirements requirements,
            @Nullable UserInput requires,
            @Nullable Map<String, String> environment,
            @Nullable Highlight highlight,
            @Nullable Boolean discardable,
            @Nullable Boolean privileged,
            @Nullable List<LogParser> logParsers,
            @Nullable Boolean ignoreFailuresWithinExecutionGroup,
            @Nullable List<String> tags,
            @Nullable Map<String, String> result,
            @Nullable StageType type,
            @Nullable List<UUID> nextStages) {
        this.id                                 = id != null ? id : idFromName(name);
        this.name                               = name;
        this.desc                               = description != null ? description : "";
        this.image                              = image != null ? image : new Image();
        this.requires                           = requirements != null ? requirements : new Requirements(
                null,
                null,
                null
        );
        this.userInput                          = requires != null ? requires : new UserInput(null, null);
        this.env                                = environment != null ? environment : Collections.emptyMap();
        this.highlight                          = highlight != null ? highlight : new Highlight();
        this.discardable                        = discardable != null && discardable;
        this.privileged                         = privileged != null && privileged;
        this.logParsers                         = Optional
                .ofNullable(logParsers)
                .map(Collections::unmodifiableList)
                .orElseGet(Collections::emptyList);
        this.ignoreFailuresWithinExecutionGroup = ignoreFailuresWithinExecutionGroup != null && ignoreFailuresWithinExecutionGroup;
        this.tags                               = tags != null
                                                  ? Collections.unmodifiableList(tags)
                                                  : Collections.emptyList();
        this.result                             = result != null ? result : Collections.emptyMap();
        this.type                               = type != null ? type : StageType.Execution;
        this.nextStages                         = nextStages != null ? nextStages : Collections.emptyList();
        this.check();
    }

    public static UUID idFromName(String name) {
        return new UUID(name.hashCode(), name.length());
    }

    public void check() {
        Objects.requireNonNull(name, "The name of a stage must be set");
    }


    @Nonnull
    public UUID getId() {
        return id;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public String getDescription() {
        return desc;
    }

    @Nonnull
    public Image getImage() {
        return image;
    }

    @Nonnull
    public Requirements getRequirements() {
        return requires;
    }

    @Nonnull
    public UserInput getRequires() {
        return userInput;
    }

    @Nonnull
    public Map<String, String> getEnvironment() {
        return env;
    }

    @Nonnull
    public Highlight getHightlight() {
        return highlight;
    }

    /**
     * @return Whether associated resources (like the workspace) that were used when executing this
     * stage are allowed to be discarded as soon as the next stage succeeded in execution
     */
    public boolean getDiscardable() {
        return discardable;
    }

    /**
     * @return Whether this stages requires to be executed in a privileged environment
     */
    public boolean getPrivileged() {
        return privileged;
    }

    @Nonnull
    public List<LogParser> getLogParsers() {
        return logParsers;
    }

    public boolean getIgnoreFailuresWithinExecutionGroup() {
        return this.ignoreFailuresWithinExecutionGroup;
    }

    @Nonnull
    public Map<String, String> getResult() {
        return result;
    }

    @Nonnull
    public StageType getType() {
        return type;
    }

    @Nonnull
    public List<UUID> getNextStages() {
        return nextStages;
    }

    @Override
    public String toString() {
        return getClass()
                .getSimpleName() + "@{name='" + this.name + "',desc='" + this.desc + "',image=" + this.image + ",userInput=" + this.userInput + ",type='" + this.type + "'}#" + this
                .hashCode();
    }

    @Nonnull
    public List<String> getTags() {
        return this.tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StageDefinition stageDefinition = (StageDefinition) o;
        return Objects.equals(
                id,
                stageDefinition.id
        ) && Objects.equals(
                name,
                stageDefinition.name
        ) && Objects.equals(
                desc,
                stageDefinition.desc
        ) && Objects.equals(
                image,
                stageDefinition.image
        ) && Objects.equals(
                requires,
                stageDefinition.requires
        ) && Objects.equals(
                userInput,
                stageDefinition.userInput
        ) && Objects.equals(
                env,
                stageDefinition.env
        ) && Objects.equals(
                highlight,
                stageDefinition.highlight
        ) && Objects.equals(
                discardable,
                stageDefinition.discardable
        ) && Objects.equals(
                ignoreFailuresWithinExecutionGroup,
                stageDefinition.ignoreFailuresWithinExecutionGroup
        ) && Objects.equals(
                tags,
                stageDefinition.tags
        ) && Objects.equals(
                nextStages,
                stageDefinition.nextStages
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                name,
                desc,
                image,
                requires,
                userInput,
                env,
                highlight,
                discardable,
                ignoreFailuresWithinExecutionGroup,
                tags,
                nextStages
        );
    }

}
