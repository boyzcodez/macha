package macha.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public final class SimpleTcpServer implements AutoCloseable {
    private final int port;
    private final Consumer<String> onMessage;
    private volatile boolean running;
    private Thread thread;
    private ServerSocket serverSocket;

    public SimpleTcpServer(int port, Consumer<String> onMessage){
        this.port = port;
        this.onMessage = onMessage;
    }

    public void start() {
        if (running) return;
        running = true;

        thread = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                serverSocket = ss;
                try (Socket socket = ss.accept();
                    FramedConnection conn = new FramedConnection(socket.getInputStream(), socket.getOutputStream())) {

                        while (running) {
                            String msg = conn.readText();
                            onMessage.accept(msg);
                        }
                    }
            } catch (IOException e) {
                if (running) onMessage.accept("[Server error] " + e.getMessage());
            }
        }, "tcp-server");

        thread.setDaemon(true);
        thread.start();
    }

    @Override public void close() throws Exception {
        running = false;
        if (serverSocket != null) serverSocket.close();
    }
    
}
