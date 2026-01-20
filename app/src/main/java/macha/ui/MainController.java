package macha.ui;

import macha.net.SimpleTcpClient;
import macha.net.SimpleTcpServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class MainController {

    @FXML private ListView<String> messagesList;
    @FXML private TextField messageField;

    @FXML private TextField hostField;
    @FXML private TextField portField;

    private SimpleTcpServer server;
    private SimpleTcpClient client;

    @FXML
    private void initialize() {
        hostField.setText("127.0.0.1");
        portField.setText("45455");

        client = new SimpleTcpClient(msg ->
                Platform.runLater(() -> messagesList.getItems().add("Peer: " + msg)));
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
            client.send(trimmed);
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
}
