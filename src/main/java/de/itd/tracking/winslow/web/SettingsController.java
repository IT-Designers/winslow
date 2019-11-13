package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.auth.GroupRepository;
import de.itd.tracking.winslow.auth.User;
import de.itd.tracking.winslow.auth.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private static boolean canUserAccess(@Nonnull User user) {
        return UserRepository.SUPERUSER.equals(user.getName())
                || user.getGroups().anyMatch(GroupRepository.SUPERGROUP::equals);
    }
}
