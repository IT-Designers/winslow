package de.itdesigners.winslow.api.auth;

/**
 * The roles are ordered descendingly in level of their privileges. The first entry (@link #OWNER) represents the most
 * privileges, while the last entry represents the least privileges.
 */
public enum Role {
    /**
     * Allowed to do anything
     */
    OWNER,
    /**
     * Allowed to do nearly anything, except adding or removing >= {@link #MAINTAINER}
     */
    MAINTAINER,
    MEMBER,
}
