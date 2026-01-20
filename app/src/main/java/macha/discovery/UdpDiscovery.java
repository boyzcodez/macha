package macha.discovery;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.function.Consumer;

public final class UdpDiscovery implements AutoCloseable {
    public static final int DISCOVERY_PORT = 45454;

    private volatile boolean running;
    private Thread senderThread;
    private Thread listenerThread;
    private DatagramSocket sendSocket;
    private DatagramSocket listenSocket;

    // Called when a beacon arrives: (name, ip, tcpPort)
    private final Consumer<Peer> onPeer;

    public UdpDiscovery(Consumer<Peer> onPeer) {
        this.onPeer = onPeer;
    }

    public void start(String deviceName, int tcpPort) {
        if (running) return;
        running = true;

        startSender(deviceName, tcpPort);
        startListener();
    }

    private void startSender(String deviceName, int tcpPort) {
        senderThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                sendSocket = socket;
                socket.setBroadcast(true);

                while (running) {
                    String payload = "LCH1|" + deviceName + "|" + tcpPort;
                    byte[] data = payload.getBytes(StandardCharsets.UTF_8);

                    DatagramPacket packet = new DatagramPacket(
                            data, data.length,
                            InetAddress.getByName("255.255.255.255"),
                            DISCOVERY_PORT
                    );

                    socket.send(packet);
                    Thread.sleep(1500);
                }
            } catch (Exception ignored) {
                // keep it quiet for now; later you can log
            }
        }, "udp-discovery-sender");

        senderThread.setDaemon(true);
        senderThread.start();
    }

    private void startListener() {
        listenerThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
                listenSocket = socket;
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                while (running) {
                    socket.receive(packet);

                    String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    // Format: LCH1|name|port
                    String[] parts = msg.split("\\|");
                    if (parts.length != 3) continue;
                    if (!"LCH1".equals(parts[0])) continue;

                    String name = parts[1].trim();
                    int port;
                    try { port = Integer.parseInt(parts[2].trim()); }
                    catch (NumberFormatException e) { continue; }

                    String ip = packet.getAddress().getHostAddress();
                    Peer peer = new Peer(name, ip, port);
                    peer.lastSeenEpochMs = Instant.now().toEpochMilli();

                    onPeer.accept(peer);
                }
            } catch (IOException ignored) {
            }
        }, "udp-discovery-listener");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @Override
    public void close() {
        running = false;
        if (sendSocket != null) sendSocket.close();
        if (listenSocket != null) listenSocket.close();
    }
}
