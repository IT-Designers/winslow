package de.itd.tracking.winslow;

import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadApiConfiguration;
import de.itd.tracking.winslow.com.AutoMesh;
import de.itd.tracking.winslow.com.AutoMeshBuilder;
import de.itd.tracking.winslow.fs.NfsWorkDirectory;
import de.itd.tracking.winslow.nomad.NomadOrchestrator;

import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws SocketException {
        AutoMesh mesh = new AutoMeshBuilder().build();
        try {
            NfsWorkDirectory config = NfsWorkDirectory.loadFromCurrentConfiguration(Path.of("/", "tmp", "local-nfs-mount-test"));
            System.out.println(config);

            new Winslow(new NomadOrchestrator(new NomadApiClient(new NomadApiConfiguration.Builder().setAddress("http://localhost:4646").build())), config).run();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
