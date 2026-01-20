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

    private SimpleTcpServer server;
    private SimpleTcpClient client;

    @FXML
    private void initialize() {
        // Instance A: run server (if port already in use, it just won't work — that’s okay for now)
        server = new SimpleTcpServer(45455, msg ->
                Platform.runLater(() -> messagesList.getItems().add("Peer: " + msg)));
        server.start();

        // Client connects to localhost (for now)
        client = new SimpleTcpClient(msg ->
                Platform.runLater(() -> messagesList.getItems().add("Peer: " + msg)));

        new Thread(() -> {
            try {
                client.connect("127.0.0.1", 45455);
                Platform.runLater(() -> messagesList.getItems().add("[Connected to localhost]"));
            } catch (Exception e) {
                Platform.runLater(() -> messagesList.getItems().add("[Connect failed] " + e.getMessage()));
            }
        }, "connect-thread").start();
    }

    @FXML
    private void onSend() {
        String text = messageField.getText();
        if (text == null || text.isBlank()) return;

        String trimmed = text.trim();;
        messagesList.getItems().add("Me: " + trimmed);
        messageField.clear();

        try {
            client.send(trimmed);
        } catch (Exception e) {
            messagesList.getItems().add("[Send failed] " + e.getMessage());
        }
    }
}
