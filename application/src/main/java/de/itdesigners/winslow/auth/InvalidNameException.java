package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InvalidNameException extends Exception {

    /**
     * Chosen by the mystical Gods
     */
    public static final          int    MAX_LENGTH             = 20;
    public static final @Nonnull String REGEX_PATTERN_SIMPLE   = "[a-zA-Z_]";
    public static final @Nonnull String REGEX_PATTERN_EXTENDED = "[a-zA-Z0-9_\\-.]";
    public static final @Nonnull String REGEX_PATTERN          =
            "(" + REGEX_PATTERN_SIMPLE + REGEX_PATTERN_EXTENDED + "{0," + (MAX_LENGTH - 1) + "}" + Prefix.SEPARATOR + ")?" +
                    REGEX_PATTERN_SIMPLE + REGEX_PATTERN_EXTENDED + "{0," + (MAX_LENGTH - 1) + "}";
    
    public static final @Nonnull String REGEX_PATTERN_DESCRIPTION = "English letters, numbers, underscore, minus and dot";

    public InvalidNameException(@Nonnull String name) {
        super("The name '" + name + "' does not match the pattern '" + REGEX_PATTERN + "' or exceeds the maximum length of " + MAX_LENGTH + " characters");
    }

    @Nonnull
    public static String ensureValid(@Nonnull String name) throws InvalidNameException {
        Pattern pattern = Pattern.compile(REGEX_PATTERN);
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches()) {
            throw new InvalidNameException(name);
        } else {
            return name;
        }
    }
}
