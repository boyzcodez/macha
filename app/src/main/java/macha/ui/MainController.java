package macha.ui;

import macha.net.SimpleTcpClient;
import macha.net.SimpleTcpServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import macha.discovery.Peer;
import macha.discovery.UdpDiscovery;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MainController {

    @FXML private ListView<String> messagesList;
    @FXML private TextField messageField;

    @FXML private TextField hostField;
    @FXML private TextField portField;

    @FXML private ListView<Peer> peersList;

    private final ObservableList<Peer> peers = FXCollections.observableArrayList();
    private UdpDiscovery discovery;

    private SimpleTcpServer server;
    private SimpleTcpClient client;

    private macha.storage.LocalConfig config;
    private String deviceName;

    @FXML
    private void initialize() {
        hostField.setText("127.0.0.1");
        portField.setText("45455");

        try {
            config = new macha.storage.LocalConfig("lanchat");
            config.loadOrCreate();
            deviceName = config.getDeviceName();
            messagesList.getItems().add("[Device] " + deviceName + " (" + config.getDeviceId() + ")");
        } catch (Exception e) {
            deviceName = System.getProperty("user.name", "Device");
            messagesList.getItems().add("[Config error] " + e.getMessage());
        }

        client = new SimpleTcpClient(msg ->
                Platform.runLater(() -> messagesList.getItems().add("Peer: " + msg)));

        peersList.setItems(peers);

        peersList.setOnMouseClicked(e -> {
            Peer selected = peersList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                hostField.setText(selected.ip);
                portField.setText(Integer.toString(selected.port));
                messagesList.getItems().add("[Selected " + selected + "]");
            }
        });

        int advertisedPort = parsePortSafeDefault(); // you already have this helper
        discovery = new UdpDiscovery(peer ->
                Platform.runLater(() -> upsertPeer(peer))); // you already have upsertPeer(...)
        discovery.start(deviceName, advertisedPort);

        messagesList.getItems().add("[Discovery] Broadcasting + listening on UDP " + UdpDiscovery.DISCOVERY_PORT);
    }

    @FXML
    private void onStartHost() {
        int port = parsePort();
        if (port == -1) return;

        if (server != null) {
            messagesList.getItems().add("[Host already running]");
            return;
        }

        server = new SimpleTcpServer(port, msg ->
                Platform.runLater(() -> messagesList.getItems().add("Peer: " + msg)));
        server.start();

        // Restart discovery to advertise correct tcpPort
        try { discovery.close(); } catch (Exception ignored) {}
        discovery = new UdpDiscovery(peer -> Platform.runLater(() -> upsertPeer(peer)));
        discovery.start(deviceName, port);

        messagesList.getItems().add("[Hosting on port " + port + "]");
    }

    @FXML
    private void onConnect() {
        String host = hostField.getText();
        int port = parsePort();
        if (port == -1) return;

        new Thread(() -> {
            try {
                client.connect(host, port);
                Platform.runLater(() -> messagesList.getItems().add("[Connected to " + host + ":" + port + "]"));
            } catch (Exception e) {
                Platform.runLater(() -> messagesList.getItems().add("[Connect failed] " + e.getMessage()));
            }
        }, "connect-thread").start();
    }

    @FXML
    private void onSend() {
        String text = messageField.getText();
        if (text == null || text.isBlank()) return;

        String trimmed = text.trim();
        messagesList.getItems().add("Me: " + trimmed);
        messageField.clear();

        try {
            // Prefer outbound connection if you used Connect
            if (client != null && client.isConnected()) {
                client.send(trimmed);
            }
            // Otherwise, if you're hosting and someone connected to you, send to them
            else if (server != null && server.hasClients()) {
                server.broadcast(trimmed);
            } else {
                messagesList.getItems().add("[Not connected to anyone]");
            }
        } catch (Exception e) {
            messagesList.getItems().add("[Send failed] " + e.getMessage());
        }
    }

    private int parsePort() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) throw new NumberFormatException();
            return port;
        } catch (Exception e) {
            messagesList.getItems().add("[Invalid port]");
            return -1;
        }
    }

    private void upsertPeer(Peer incoming) {
        // ignore ourselves if we see our own broadcast (optional)
        // if (incoming.ip.equals("127.0.0.1")) return;
        if (isSelfPeer(incoming)) return;

        for (int i = 0; i < peers.size(); i++) {
            if (peers.get(i).key().equals(incoming.key())) {
                peers.get(i).lastSeenEpochMs = incoming.lastSeenEpochMs;
                return;
            }
        }
        peers.add(incoming);
    }

    private int parsePortSafeDefault() {
        try {
            int p = Integer.parseInt(portField.getText().trim());
            if (p >= 1 && p <= 65535) return p;
        } catch (Exception ignored) {}
        return 45455;
    }

    private boolean isSelfPeer(Peer p) {
        try {
            // If it's the same machine IP and same advertised port, treat as self.
            // This works well enough for now.
            String localHost = java.net.InetAddress.getLocalHost().getHostAddress();
            int myPort = parsePortSafeDefault();
            return p.ip.equals(localHost) && p.port == myPort;
        } catch (Exception e) {
            return false;
        }
    }
}
