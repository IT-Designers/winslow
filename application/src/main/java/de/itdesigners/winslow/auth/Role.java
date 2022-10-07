package de.itdesigners.winslow.auth;

/**
 * The roles are ordered descendingly in level of their privileges. The first entry (@link #OWNER) represents the most
 * privileges, while the last entry represents the least privileges.
 */
public enum Role {
    OWNER,
    MEMBER,
}
