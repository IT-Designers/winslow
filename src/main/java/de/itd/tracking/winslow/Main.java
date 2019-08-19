package de.itd.tracking.winslow;

import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadApiConfiguration;
import de.itd.tracking.winslow.com.AutoMesh;
import de.itd.tracking.winslow.com.AutoMeshBuilder;
import de.itd.tracking.winslow.nomad.NomadOrchestrator;

import java.net.SocketException;

public class Main {
    public static void main(String[] args) throws SocketException {
        AutoMesh mesh = new AutoMeshBuilder().build();

        new Winslow(new NomadOrchestrator(new NomadApiClient(new NomadApiConfiguration.Builder().setAddress("http://localhost:4646").build()))).run();
    }
}
