package de.itd.tracking.winslow;

public class Env {

    public static final String SELF_PREFIX     = "WINSLOW";
    public static final String DEV_ENV         = SELF_PREFIX + "_DEV_ENV";
    public static final String DEV_REMOTE_USER = SELF_PREFIX + "_DEV_REMOTE_USER";
    public static final String WORK_DIRECTORY  = SELF_PREFIX + "_WORK_DIRECTORY";
    public static final String NODE_NAME       = SELF_PREFIX + "_NODE_NAME";

    private Env() {
    }
}
