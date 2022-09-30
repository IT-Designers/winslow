package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InvalidNameException extends Exception {

    private static final @Nonnull String REGEX_PATTERN_SIMPLE   = "[a-zA-Z0-9_]";
    private static final @Nonnull String REGEX_PATTERN_EXTENDED = "[a-zA-Z0-9_\\-.]";
    private static final @Nonnull String REGEX_PATTERN          =
            REGEX_PATTERN_SIMPLE + REGEX_PATTERN_EXTENDED + "*(::" + REGEX_PATTERN_SIMPLE + ")?" + REGEX_PATTERN_EXTENDED + '*';

    public InvalidNameException(@Nonnull String name) {
        super("The name '" + name + "' does not match the pattern '" + REGEX_PATTERN + "'");
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
