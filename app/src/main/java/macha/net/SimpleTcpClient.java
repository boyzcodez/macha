package macha.net;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

public final class SimpleTcpClient implements AutoCloseable {
    private final Consumer<String> onMessage;
    private FramedConnection conn;
    private Thread readerThread;
    private Socket socket;

    public SimpleTcpClient(Consumer<String> onMessage){
        this.onMessage = onMessage;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        conn = new FramedConnection(socket.getInputStream(), socket.getOutputStream());

        readerThread = new Thread(() -> {
            try {
                while (true) {
                    String msg = conn.readText();
                    onMessage.accept(msg);
                }
            } catch (IOException e) {
                onMessage.accept("[Client disconnected] " + e.getMessage());
            }
        }, "tcp-client-reader");

        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void send(String msg) throws IOException {
        if (conn == null) throw new IllegalStateException("Not connected");
        conn.sendText(msg);
    }

    @Override public void close() throws Exception {
        if (conn != null) conn.close();
        if (socket != null) socket.close();
    }

}
