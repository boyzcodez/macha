package macha.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SimpleTcpServer implements AutoCloseable {
    private final int port;
    private final Consumer<String> onMessage;

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    // thread-safe list you can iterate while removing
    private final List<FramedConnection> clients = new CopyOnWriteArrayList<>();

    public SimpleTcpServer(int port, Consumer<String> onMessage) {
        this.port = port;
        this.onMessage = onMessage;
    }

    public void start() {
        if (running) return;
        running = true;

        acceptThread = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                serverSocket = ss;
                onMessage.accept("[Hosting on port " + port + "]");

                while (running) {
                    Socket socket = ss.accept();
                    FramedConnection conn = new FramedConnection(socket.getInputStream(), socket.getOutputStream());
                    clients.add(conn);

                    onMessage.accept("[Client connected: " + socket.getInetAddress().getHostAddress() + "]");

                    // reader thread per client
                    Thread t = new Thread(() -> readLoop(conn), "server-client-reader");
                    t.setDaemon(true);
                    t.start();
                }
            } catch (IOException e) {
                if (running) onMessage.accept("[Server error] " + e.getMessage());
            }
        }, "tcp-server-accept");

        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void readLoop(FramedConnection conn) {
        try (conn) {
            while (running) {
                String msg = conn.readText();
                onMessage.accept(msg);
            }
        } catch (IOException e) {
            // disconnect
        } finally {
            clients.remove(conn);
            onMessage.accept("[Client disconnected]");
        }
    }

    public void broadcast(String msg) {
        for (FramedConnection c : clients) {
            try {
                c.sendText(msg);
            } catch (IOException e) {
                clients.remove(c);
            }
        }
    }

    public boolean hasClients() {
        return !clients.isEmpty();
    }

    @Override
    public void close() throws Exception {
        running = false;
        if (serverSocket != null) serverSocket.close();
        for (FramedConnection c : clients) {
            try { c.close(); } catch (IOException ignored) {}
        }
        clients.clear();
    }
}
