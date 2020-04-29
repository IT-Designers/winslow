package de.itdesigners.winslow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class Env {

    public static final String SELF_PREFIX        = "WINSLOW";
    public static final String DEV_ENV            = SELF_PREFIX + "_DEV_ENV";
    public static final String DEV_REMOTE_USER    = SELF_PREFIX + "_DEV_REMOTE_USER";
    public static final String WORK_DIRECTORY     = SELF_PREFIX + "_WORK_DIRECTORY";
    public static final String NODE_NAME          = SELF_PREFIX + "_NODE_NAME";
    public static final String STATIC_HTML        = SELF_PREFIX + "_STATIC_HTML";
    public static final String API_PATH           = SELF_PREFIX + "_API_PATH";
    public static final String NO_STAGE_EXECUTION = SELF_PREFIX + "_NO_STAGE_EXECUTION";
    public static final String NO_GPU_USAGE       = SELF_PREFIX + "_NO_GPU_USAGE";
    public static final String NO_WEB_API         = SELF_PREFIX + "_NO_WEB_API";
    public static final String DEV_ENV_IP         = SELF_PREFIX + "_DEV_ENV_IP";

    private Env() {
    }

    @Nonnull
    public static String getNodeName() throws UnknownHostException {
        return System.getenv().getOrDefault(NODE_NAME, InetAddress.getLocalHost().getHostName());
    }

    @Nonnull
    public static String getWorkDirectory() {
        return System.getenv().getOrDefault(WORK_DIRECTORY, "/winslow/");
    }

    @Nullable
    public static String getStaticHtml() {
        return System.getenv(STATIC_HTML);
    }

    @Nonnull
    public static String getApiPath() {
        return System.getenv().getOrDefault(API_PATH, "/api/v1/");
    }

    public static boolean isNoStageExecutionSet() {
        return !Optional
                .ofNullable(System.getenv().get(NO_STAGE_EXECUTION))
                .map(Boolean::parseBoolean)
                .orElse(Boolean.TRUE);
    }

    public static boolean isNoGpuUsageSet() {
        return !Optional
                .ofNullable(System.getenv().get(NO_GPU_USAGE))
                .map(Boolean::parseBoolean)
                .orElse(Boolean.TRUE);
    }

    public static boolean isNoWebApiSet() {
        return !Optional
                .ofNullable(System.getenv().get(NO_WEB_API))
                .map(Boolean::parseBoolean)
                .orElse(Boolean.TRUE);
    }

    @Nullable
    public static String getDevEnvIp() {
        return System.getenv(DEV_ENV_IP);
    }
}
