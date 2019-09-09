package de.itd.tracking.winslow;

import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadApiConfiguration;
import de.itd.tracking.winslow.com.AutoMesh;
import de.itd.tracking.winslow.com.AutoMeshBuilder;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.NfsWorkDirectory;
import de.itd.tracking.winslow.nomad.NomadOrchestrator;
import de.itd.tracking.winslow.nomad.NomadRepository;
import de.itd.tracking.winslow.web.WebApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Path;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws SocketException {
        LOG.trace("program start at first line within main");
        System.out.println();
        System.out.println("           ____               ");
        System.out.println("         /       \\            ");
        System.out.println("        /   ------\\           ");
        System.out.println("       /___/      |            ");
        System.out.println("       /\\   ^   ^ |                   _           _               ");
        System.out.println("       \\/     ;   /                  (_)         | |              ");
        System.out.println("        /\\   __  /          __      ___ _ __  ___| | _____      __");
        System.out.println("       /   -----            \\ \\ /\\ / / | '_ \\/ __| |/ _ \\ \\ /\\ / /");
        System.out.println("      /\\    /\\  \\            \\ V  V /| | | | \\__ \\ | (_) \\ V  V / ");
        System.out.println("     /--\\   \\/   \\            \\_/\\_/ |_|_| |_|___/_|\\___/ \\_/\\_/  ");
        System.out.println("                                                     v0.0.0                  ");
        System.out.println();
        System.out.println();
        System.out.println("         work-director = /tmp/workdir");
        System.out.println("             log-level = INFO");
        System.out.println("                  mode = STANDALONE");
        System.out.println();
        System.out.println();

        WebApi.Context webApi = null;
        AutoMesh       mesh   = new AutoMeshBuilder().build();

        try {
            LOG.info("Loading NFS configuration for work-directory");
            NfsWorkDirectory config = NfsWorkDirectory.loadFromCurrentConfiguration(Path.of("/home/mi7wa6/mec-view/winslow/nfs-mount"));

            LOG.info("Assembling Winslow");
            var winslow = new Winslow(new NomadOrchestrator(environment, new NomadApiClient(new NomadApiConfiguration.Builder().setAddress("http://localhost:4646").build()),
                    new NomadRepository(
                            // TODO
                            new LockBus(config.getEventsDirectory()),
                            config
                    )
            ), config);

            LOG.info("Starting WebApi");
            webApi = WebApi.start(winslow);
            System.out.println(LoggerFactory.getLogger("global").getClass());

            LOG.info("Letting Winslow run freely");
            winslow.run();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (webApi != null) {
                webApi.stop();
            }
        }


    }
}
