# Macha

A small JavaFX-based LAN chat app. Devices on the same local network discover each other over UDP, then exchange messages over a direct TCP connection.

## Features

- **Automatic peer discovery** — broadcasts a UDP beacon on port `45454` so other instances on the LAN show up in the peers list automatically.
- **Direct TCP chat** — one side starts a host on a chosen port, the other connects; messages are framed and sent over the socket.
- **Persistent device identity** — a stable `deviceId` (UUID) and `deviceName` are stored in `~/.lanchat/config.properties` on first run.
- **JavaFX UI** — peers list on the left, message log in the middle, host/port + Start Host / Connect controls on top, message input on the bottom.

## Requirements

- JDK 21 (toolchain is configured in [app/build.gradle](app/build.gradle); change to 17 if needed)
- Gradle wrapper is included — no separate Gradle install required
- JavaFX 21 (pulled in automatically via the `org.openjfx.javafxplugin`)

## Running

From the project root:

```
./gradlew run
```

On Windows:

```
gradlew.bat run
```

## How to use

1. Launch the app on two machines on the same LAN (or two instances on the same machine using different ports).
2. Each instance broadcasts its presence — the other should appear in the **peers list** within a couple of seconds.
3. On one instance, click **Start Host** to listen on the configured port.
4. On the other instance, click a peer in the list (this fills in host + port) and click **Connect**.
5. Type a message and click **Send**.

If you don't see peers, make sure UDP port `45454` isn't blocked by your firewall.

## Project layout

- [app/src/main/java/macha/App.java](app/src/main/java/macha/App.java) — JavaFX entry point, loads `main.fxml`.
- [app/src/main/java/macha/ui/MainController.java](app/src/main/java/macha/ui/MainController.java) — UI controller, wires discovery, hosting, connecting, and sending.
- [app/src/main/java/macha/discovery/UdpDiscovery.java](app/src/main/java/macha/discovery/UdpDiscovery.java) — UDP broadcast sender + listener (`LCH1|name|tcpPort` payload).
- [app/src/main/java/macha/discovery/Peer.java](app/src/main/java/macha/discovery/Peer.java) — peer model (name, ip, port, last-seen).
- [app/src/main/java/macha/net/SimpleTcpServer.java](app/src/main/java/macha/net/SimpleTcpServer.java) — accepts clients, broadcasts to all of them.
- [app/src/main/java/macha/net/SimpleTcpClient.java](app/src/main/java/macha/net/SimpleTcpClient.java) — outbound TCP connection.
- [app/src/main/java/macha/net/FramedConnection.java](app/src/main/java/macha/net/FramedConnection.java) — length-prefixed message framing over a socket.
- [app/src/main/java/macha/storage/LocalConfig.java](app/src/main/java/macha/storage/LocalConfig.java) — loads/creates `~/.lanchat/config.properties`.
- [app/src/main/resources/macha/main.fxml](app/src/main/resources/macha/main.fxml) — UI layout.

## Network details

- **Discovery port (UDP):** `45454`
- **Default chat port (TCP):** `45455`
- **Beacon format:** `LCH1|<deviceName>|<tcpPort>` sent every 1.5s to `255.255.255.255`

## Tests

```
./gradlew test
```
