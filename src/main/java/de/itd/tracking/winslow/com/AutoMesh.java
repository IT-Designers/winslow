package de.itd.tracking.winslow.com;


import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.Channel;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.SessionChannel;
import net.schmizz.sshj.connection.channel.forwarded.ConnectListener;
import net.schmizz.sshj.connection.channel.forwarded.RemotePortForwarder;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.method.AuthPublickey;
import net.schmizz.sshj.userauth.method.KeyedAuthMethod;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutoMesh implements Runnable {

    private static final Logger LOG = Logger.getLogger(AutoMesh.class.getSimpleName());

    private       boolean        run;
    private       boolean        running;
    private final DatagramSocket socket;
    private final int            reachTimeout;

    public AutoMesh(int port, int reachTimeout) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.reachTimeout = reachTimeout;
        this.run = true;
        this.running = false;
    }

    public synchronized boolean isRunning() {
        return this.running;
    }

    private synchronized boolean keepRunning() {
        return this.run;
    }

    public synchronized void requestStop() {
        this.run = false;
        this.socket.close();
    }

    @Override
    public void run() {
        synchronized (this) {
            this.running = true;
        }
        try {
            DatagramPacket packet = new DatagramPacket(new byte[3], 3);

            while (this.keepRunning()) {
                try {
                    socket.receive(packet);

                    System.out.println(packet.getAddress());

                    InetAddress     address   = packet.getAddress();
                    Optional<Short> port      = getUserSpecifiedPortFromRequest(packet.getData());
                    boolean         reachable = packet.getAddress().isReachable(reachTimeout);

                    if (!reachable) {
                        LOG.log(Level.WARNING, address + " is not reachable withing " + reachTimeout + " ms and therefore being ignored");
                        continue;
                    }

                    addToMesh(address, port, Optional.empty());

                    System.out.println(port);
                    System.out.println(packet.getAddress().isReachable(100));


                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        } finally {
            synchronized (this) {
                this.running = false;
                this.socket.close();
            }
        }
    }

    private static void addToMesh(InetAddress address, Optional<Short> customPort, Optional<String> user) {

        try {
            SSHClient client = new SSHClient();
            client.loadKnownHosts();
            client.useCompression();
            client.addHostKeyVerifier(new PromiscuousVerifier()); // because of publickey one can always accept every fingerprint
            client.connect(address.getHostAddress(), customPort.orElse((short) 22));
            client.authPublickey(user.orElse(System.getProperty("user.name")));




            System.out.println("connected to " + address);

            final String DIRECT_CHANNEL_LOCAL_ADDR = "localhost";
            final int DIRECT_CHANNEL_LOCAL_PORT = 65536;

            var forward = new LocalPortForwarder.DirectTCPIPChannel(
                    client.getConnection(),
                    null,
                    new LocalPortForwarder.Parameters(
                            DIRECT_CHANNEL_LOCAL_ADDR,
                            DIRECT_CHANNEL_LOCAL_PORT,
                            "localhost", // remote host
                            22 // remote ip
                    )
            );

            forward.open();

            var input = forward.getInputStream();
            var output = forward.getOutputStream();

            byte[] data = new byte[1024];
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int available = input.available();
            System.out.println(available);
            input.read(data, 0, available);
            System.out.write(data, 0, available);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Optional<Short> getUserSpecifiedPortFromRequest(@Nonnull byte[] request) {
        if (request.length == 3 && request[0] == 0x01) {
            return Optional.of(ByteBuffer.wrap(request, 1, 2).getShort());
        }
        return Optional.empty();
    }
}
