package de.itd.tracking.winslow;

import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadApiConfiguration;
import de.itd.tracking.winslow.nomad.NomadOrchestrator;

public class Main {
    public static void main(String[] args) {
        new Winslow(new NomadOrchestrator(new NomadApiClient(new NomadApiConfiguration.Builder().setAddress("http://localhost:4646").build()))).run();
    }
}
