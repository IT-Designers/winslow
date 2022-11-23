package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.settings.ResourceLimitation;
import de.itdesigners.winslow.auth.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class SettingsController {

    private static final Logger LOG = Logger.getLogger(SettingsController.class.getSimpleName());

    @Nonnull private final Winslow winslow;

    public SettingsController(@Nonnull Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/settings/global-env")
    public Optional<Map<String, String>> getEnvironmentVariables(@Nonnull User user) throws IOException {
        if (canUserAccess(user)) {
            return Optional.of(this.winslow.getSettingsRepository().getGlobalEnvironmentVariables());
        } else {
            return Optional.empty();
        }
    }

    @PostMapping("/settings/global-env")
    public void setEnvironmentVariables(
            @Nonnull User user,
            @RequestBody Map<String, String> env) throws IOException {
        if (!canUserAccess(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } else {
            this.winslow.getSettingsRepository().updateGlobalEnvironmentVariables(stored -> {
                stored.clear();
                stored.putAll(env);
            });
        }
    }

    @GetMapping("/settings/user-res-limit")
    public Optional<ResourceLimitation> getUserResourceLimitation(@Nonnull User user) {
        return Optional
                .of(user)
                .filter(User::hasSuperPrivileges)
                .flatMap(u -> winslow.getSettingsRepository().getUserResourceLimitations().unsafe());
    }

    @PostMapping("/settings/user-res-limit")
    public Optional<ResourceLimitation> setUserResourceLimitation(
            @Nonnull User user,
            @RequestBody ResourceLimitation limit) {
        return Optional
                .of(user)
                .filter(User::hasSuperPrivileges)
                .flatMap(u -> {
                    try {
                        winslow.getSettingsRepository().updateUserResourceLimitations(new ResourceLimitation(
                                Optional.ofNullable(limit.cpu).map(l -> Math.max(1, l)).orElse(null),
                                Optional.ofNullable(limit.mem).map(l -> Math.max(100, l)).orElse(null),
                                Optional.ofNullable(limit.gpu).map(l -> Math.max(1, l)).orElse(null)
                        ));
                        return getUserResourceLimitation(u);
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Failed to update userResourceLimitations", e);
                        return Optional.empty();
                    }
                });
    }

    private static boolean canUserAccess(@Nonnull User user) {
        return user.hasSuperPrivileges();
    }
}
