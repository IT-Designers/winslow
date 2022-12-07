package de.itdesigners.winslow.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Pattern;

public class NamedId {

    private static final Pattern INVALID_BACKEND_CHARACTER = Pattern.compile("[^a-zA-Z0-9\\-_]");
    private static final Pattern MULTI_UNDERSCORE          = Pattern.compile("_[_]+");


    private static String replaceInvalidCharactersInName(@Nonnull String name) {
        return MULTI_UNDERSCORE
                .matcher(INVALID_BACKEND_CHARACTER.matcher(name.toLowerCase()).replaceAll("_"))
                .replaceAll("_");
    }

    private static String getExecutionGroupId(@Nonnull String pipelineId, int number, @Nonnull String groupName) {
        return replaceInvalidCharactersInName(String.format(
                "%s_%04d_%s",
                pipelineId,
                number,
                groupName
        ));
    }

    private static String getStageId(@Nonnull String groupId, int number, @Nonnull String stageName) {
        return replaceInvalidCharactersInName(String.format(
                "%s_%04d_%s",
                groupId,
                number,
                stageName
        ));
    }

    @Nonnull
    public static String buildStageId(
            @Nullable String projectId,
            int groupNumberWithinProject,
            @Nullable String humanReadableGroupHint,
            @Nullable Integer stageNumberWithinGroup) {
        return buildIdentifierString(
                projectId,
                groupNumberWithinProject,
                humanReadableGroupHint,
                stageNumberWithinGroup
        );
    }

    @Nonnull
    public static String buildExecutionGroupId(
            @Nullable String projectId,
            int groupNumberWithinProject,
            @Nullable String humanReadableGroupHint) {
        return buildIdentifierString(projectId, groupNumberWithinProject, humanReadableGroupHint, null);
    }

    private static String buildIdentifierString(
            @Nullable String projectId,
            int groupNumberWithinProject,
            @Nullable String humanReadableGroupHint,
            @Nullable Integer stageNumberWithinGroup) {
        var builder = new StringBuilder();

        if (projectId != null) {
            builder.append(projectId);
            builder.append('_');
        }

        builder.append(String.format("%04d", groupNumberWithinProject));

        if (humanReadableGroupHint != null) {
            builder.append('_');
            builder.append(replaceInvalidCharactersInName(humanReadableGroupHint));
        }

        if (stageNumberWithinGroup != null) {
            builder.append(String.format("_%04d", stageNumberWithinGroup));
        }

        return builder.toString();
    }

    @Nonnull
    public static ExecutionGroupId parseLegacyExecutionGroupId(@Nonnull String identifier) {
        var split = identifier.split("_", 3);

        if (split.length != 3) {
            throw new IllegalArgumentException("Invalid group id: " + identifier);
        }

        return new ExecutionGroupId(
                split[0],
                Integer.parseInt(split[1]),
                split[2]
        );
    }
}
