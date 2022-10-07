package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.auth.GroupInfo;
import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.auth.Role;
import de.itdesigners.winslow.auth.*;
import de.itdesigners.winslow.web.GroupInfoConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@RestController
public class GroupController {

    private static final Logger LOG = Logger.getLogger(GroupController.class.getSimpleName());

    private final @Nonnull Winslow winslow;

    @Autowired
    public GroupController(@Nonnull Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/groups")
    public Stream<GroupInfo> getGroups(@Nonnull User user) {
        // mask "not found" and "not allowed to see" as empty response
        return winslow
                .getGroupManager()
                .getGroups()
                .stream()
                .filter(group -> isAllowedToSeeGroup(user, group))
                .map(GroupInfoConverter::from);
    }

    @GetMapping("/groups/{name}")
    public Optional<GroupInfo> getGroup(
            @Nonnull User user,
            @PathVariable("name") String name) {
        // mask "not found" and "not allowed to see" as empty response
        return winslow
                .getGroupManager()
                .getGroup(name)
                .filter(group -> isAllowedToSeeGroup(user, group))
                .map(GroupInfoConverter::from);
    }

    @PostMapping("/groups")
    public GroupInfo createGroup(
            @Nullable User user,
            @RequestBody Group group) {
        try {
            ensure(isAllowedToCreateNewGroup(user));

            return GroupInfoConverter.from(
                    winslow.getGroupManager().createGroup(
                        Prefix.unwrap_or_given(group.name()),
                        Stream.concat(
                                Stream.of(new Link(user.name(), Role.OWNER)),
                                group
                                        .members()
                                        .stream()
                                        .filter(link -> !link
                                                .name()
                                                .equals(user.name()))
                        ).toList()
                )
            );
        } catch (InvalidNameException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid name", e);
        } catch (NameAlreadyInUseException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to create a group because of an io-error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/groups/{group}/members")
    public List<Link> getMemberships(
            @Nullable User user,
            @PathVariable("group") String groupName) {
        // mask "not found" and "not allowed to see" as empty value
        return winslow
                .getGroupManager()
                .getGroup(groupName)
                .filter(group -> isAllowedToSeeGroup(user, group))
                .map(Group::members)
                .orElse(Collections.emptyList());
    }

    @PostMapping("/groups/{group}/members")
    public List<Link> addOrUpdateMembership(
            @Nullable User user,
            @PathVariable("group") String groupName,
            @RequestBody Link link) {
        try {
            ensure(isAllowedToAdministrateGroup(user, groupName));

            return winslow.getGroupManager().addOrUpdateMembership(groupName, link.name(), link.role()).members();

        } catch (InvalidNameException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid name", e);
        } catch (NameNotFoundException e) {
            // mask it as forbidden, so that it cannot be distinguished from a real forbidden
            // therefore one cannot find groups by guessing names
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to update group membership because of an io-error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/groups/{group}")
    public void deleteGroup(
            @Nullable User user,
            @PathVariable("group") String groupName) {
        try {
            ensure(isAllowedToAdministrateGroup(user, groupName));

            winslow.getGroupManager().deleteGroup(groupName);
        } catch (NameNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to delete group because of an io-error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/groups/{group}/members/{user}")
    public void deleteGroupMembership(
            @Nullable User user,
            @PathVariable("group") String groupName,
            @PathVariable("user") String userName) {
        try {
            ensure(isAllowedToAdministrateGroup(user, groupName));

            winslow.getGroupManager().deleteMembership(groupName, userName);

        } catch (NameNotFoundException | LinkWithNameNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to delete group membership because of an io-error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void ensure(boolean accessGranted) throws ResponseStatusException {
        if (!accessGranted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private boolean isAllowedToCreateNewGroup(@Nullable User user) {
        // everyone can create a _new_ group
        return user != null;
    }

    private boolean isAllowedToAdministrateGroup(@Nullable User user, @Nonnull String groupName) {
        if (user != null && !user.hasSuperPrivileges()) {
            return winslow
                    .getGroupManager()
                    .getGroup(groupName)
                    .map(group -> group.hasMemberWithRole(user.name(), Role.OWNER))
                    .orElse(Boolean.FALSE);
        } else {
            return false;
        }
    }

    private boolean isAllowedToSeeGroup(@Nullable User user, @Nonnull Group group) {
        // ordered in ascending query complexity
        return user != null && (user.isSuperUser() || group.isMember(user.name()) || user.hasSuperPrivileges());
    }
}
