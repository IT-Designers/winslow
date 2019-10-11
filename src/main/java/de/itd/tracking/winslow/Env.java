package de.itd.tracking.winslow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Env {

    public static final String SELF_PREFIX     = "WINSLOW";
    public static final String DEV_ENV         = SELF_PREFIX + "_DEV_ENV";
    public static final String DEV_REMOTE_USER = SELF_PREFIX + "_DEV_REMOTE_USER";
    public static final String WORK_DIRECTORY  = SELF_PREFIX + "_WORK_DIRECTORY";
    public static final String NODE_NAME       = SELF_PREFIX + "_NODE_NAME";
    public static final String STATIC_HTML     = SELF_PREFIX + "_STATIC_HTML";
    public static final String API_PATH        = SELF_PREFIX + "_API_PATH";

    private Env() {
    }

    @Nullable
    public static String getStaticHtml() {
        return System.getenv(STATIC_HTML);
    }

    @Nonnull
    public static String getApiPath() {
        return System.getenv().getOrDefault(API_PATH, "/api/v1/");
    }
}
