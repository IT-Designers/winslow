package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.fs.LockBus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

@RestController
public class SettingsController {

    @Nonnull private final Winslow winslow;

    public SettingsController(@Nonnull Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/settings/global-env")
    public Map<String, String> getEnvironmentVariables() throws IOException {
        return this.winslow.getSettingsRepository().getGlobalEnvironmentVariables();
    }
}
