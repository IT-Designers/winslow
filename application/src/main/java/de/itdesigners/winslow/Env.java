package de.itdesigners.winslow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Env {

    private static final int LOCK_DURATION_MIN_MS     = 10 * 1_000; // 10s
    private static final int LOCK_DURATION_DEFAULT_MS = 5 * 60 * 1_000; // 5min

    public static final String SELF_PREFIX        = "WINSLOW";
    public static final String DEV_ENV            = SELF_PREFIX + "_DEV_ENV";
    public static final String DEV_REMOTE_USER    = SELF_PREFIX + "_DEV_REMOTE_USER";
    public static final String WORK_DIRECTORY     = SELF_PREFIX + "_WORK_DIRECTORY";
    public static final String NODE_NAME          = SELF_PREFIX + "_NODE_NAME";
    public static final String STATIC_HTML        = SELF_PREFIX + "_STATIC_HTML";
    public static final String API_PATH           = SELF_PREFIX + "_API_PATH";
    public static final String WEBSOCKET_PATH     = SELF_PREFIX + "_WEBSOCKET_PATH";
    public static final String NO_STAGE_EXECUTION = SELF_PREFIX + "_NO_STAGE_EXECUTION";
    public static final String NO_GPU_USAGE       = SELF_PREFIX + "_NO_GPU_USAGE";
    public static final String NO_WEB_API         = SELF_PREFIX + "_NO_WEB_API";
    public static final String DEV_ENV_IP         = SELF_PREFIX + "_DEV_ENV_IP";
    public static final String WEB_REQUIRE_SECURE = SELF_PREFIX + "_WEB_REQUIRE_SECURE";
    public static final String LOCK_DURATION_MS   = SELF_PREFIX + "_LOCK_DURATION_MS";

    public static final String LDAP_URL = SELF_PREFIX + "_LDAP_URL";
    // public static final String LDAP_MANAGER_DN          = SELF_PREFIX + "_LDAP_MANAGER_DN";
    // public static final String LDAP_MANAGER_PASSWORD    = SELF_PREFIX + "_LDAP_MANAGER_PASSWORD";
    // public static final String LDAP_USER_SEARCH_BASE    = SELF_PREFIX + "_LDAP_USER_SEARCH_FILTER";
    // public static final String LDAP_USER_SEARCH_FILTER  = SELF_PREFIX + "_LDAP_USER_SEARCH_FILTER";
    // public static final String LDAP_GROUP_SEARCH_BASE   = SELF_PREFIX + "_LDAP_GROUP_SEARCH_FILTER";
    // public static final String LDAP_GROUP_SEARCH_FILTER = SELF_PREFIX + "_LDAP_GROUP_SEARCH_FILTER";

    public static final String ROOT_USERS = SELF_PREFIX + "_ROOT_USERS";

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

    @Nonnull
    public static String getWebsocketPath() {
        return System.getenv().getOrDefault(WEBSOCKET_PATH, "/ws/v1/");
    }

    public static boolean isNoStageExecutionSet() {
        return isTrueOr1(System.getenv().get(NO_STAGE_EXECUTION));
    }

    public static boolean isNoGpuUsageSet() {
        return isTrueOr1(System.getenv().get(NO_GPU_USAGE));
    }

    public static boolean isNoWebApiSet() {
        return isTrueOr1(System.getenv().get(NO_WEB_API));
    }

    public static boolean requireSecure() {
        // 'SECURITY_REQUIRE_SSL' is an old and deprecated springboot property but might be used here and there
        return isTrueOr1(System.getenv("SECURITY_REQUIRE_SSL")) || isTrueOr1(System.getenv(WEB_REQUIRE_SECURE));
    }

    public static int lockDurationMs() {
        try {
            var duration = Integer.parseInt(System.getenv().get(LOCK_DURATION_MS));
            return Integer.max(LOCK_DURATION_MIN_MS, duration);
        } catch (Throwable t) {
            return LOCK_DURATION_DEFAULT_MS;
        }
    }

    public static boolean isDevEnv() {
        return isTrueOr1(System.getenv(DEV_ENV));
    }

    private static boolean isTrueOr1(@Nullable String env) {
        return "1".equals(env) || Boolean.parseBoolean(env);
    }

    public static boolean isLdapAuthEnabled() {
        return !isDevEnv() && System.getenv(LDAP_URL) != null;
    }

    @Nullable
    public static String getDevEnvIp() {
        return System.getenv(DEV_ENV_IP);
    }

    @Nonnull
    public static String[] getRootUsers() {
        var users = System.getenv(ROOT_USERS);
        if (users != null) {
            return users.split(",");
        } else {
            return new String[0];
        }
    }
}
