package macha.storage;

import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;
import java.util.UUID;

public final class LocalConfig {
    private final Path path;
    private final Properties props = new Properties();

    private String deviceId;
    private String deviceName;

    public LocalConfig(String appName) {
        // Windows: C:\Users\<you>\AppData\Roaming\<appName>\
        // macOS/Linux: user home folder based locations still fine for school projects
        Path dir = Paths.get(System.getProperty("user.home"), "." + appName);
        this.path = dir.resolve("config.properties");
    }

    public void loadOrCreate() throws IOException {
        Files.createDirectories(path.getParent());

        if (Files.exists(path)) {
            try (var in = Files.newInputStream(path)) {
                props.load(in);
            }
        }

        deviceId = props.getProperty("deviceId");
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
            props.setProperty("deviceId", deviceId);
        }

        deviceName = props.getProperty("deviceName");
        if (deviceName == null || deviceName.isBlank()) {
            deviceName = System.getProperty("user.name", "Device");
            props.setProperty("deviceName", deviceName);
        }

        save();
    }

    public void save() throws IOException {
        try (var out = Files.newOutputStream(path)) {
            props.store(out, "LanChat config");
        }
    }

    public String getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }

    public void setDeviceName(String name) {
        this.deviceName = name;
        props.setProperty("deviceName", name);
    }
}
