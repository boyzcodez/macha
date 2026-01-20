package macha.discovery;

import java.time.Instant;
import java.time.Period;

public final class Peer {
    public final String name;
    public final String ip;
    public final int port;
    public volatile long lastSeenEpochMs;

    public Peer(String name, String ip, int port){
        this.name = name;;
        this.ip = ip;
        this.port = port;
        this.lastSeenEpochMs = Instant.now().toEpochMilli();
    }

    public String key() {
        return ip + ": " + port;
    }

    @Override
    public String toString() {
        return name + " (" + ip + ":" + port + ")";
    }

}
