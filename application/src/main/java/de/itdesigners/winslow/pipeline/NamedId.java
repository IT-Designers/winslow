package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

public class NamedId {

    private static final Pattern INVALID_NOMAD_CHARACTER = Pattern.compile("[^a-zA-Z0-9\\-_]");
    private static final Pattern MULTI_UNDERSCORE        = Pattern.compile("_[_]+");


    public static String replaceInvalidCharactersInName(@Nonnull String name) {
        return MULTI_UNDERSCORE
                .matcher(INVALID_NOMAD_CHARACTER.matcher(name.toLowerCase()).replaceAll("_"))
                .replaceAll("_");
    }

    public static String getExecutionGroupId(@Nonnull String pipelineId, int number, @Nonnull String groupName) {
        return replaceInvalidCharactersInName(String.format(
                "%s_%04d_%s",
                pipelineId,
                number,
                groupName
        ));
    }

    public static String getStageId(@Nonnull String groupId, int number, @Nonnull String stageName) {
        return replaceInvalidCharactersInName(String.format(
                "%s_%04d_%s",
                groupId,
                number,
                stageName
        ));
    }
}
