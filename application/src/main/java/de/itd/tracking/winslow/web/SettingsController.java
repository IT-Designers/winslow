package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.auth.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
public class SettingsController {

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
            @RequestParam("env") Map<String, String> env) throws IOException {
        if (!canUserAccess(user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } else {
            this.winslow.getSettingsRepository().updateGlobalEnvironmentVariables(stored -> {
                stored.clear();
                stored.putAll(env);
            });
        }
    }

    private static boolean canUserAccess(@Nonnull User user) {
        return user.hasSuperPrivileges();
    }
}
