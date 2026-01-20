package macha.net;

import java.io.*;
import java.nio.charset.StandardCharsets;;

public final class FramedConnection implements Closeable {
    private final DataInputStream in;
    private final DataOutputStream out;

    public FramedConnection(InputStream in, OutputStream out){
        this.in = new DataInputStream(new BufferedInputStream(in));
        this.out = new DataOutputStream(new BufferedOutputStream(out));
    }

    public synchronized void sendText(String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    public String readText() throws IOException {
        int len = in.readInt();
        if (len < 0 || len > 10_000_000) throw new IOException("Bad frame length: " + len);
        byte[] data = in.readNBytes(len);
        return new String(data, StandardCharsets.UTF_8);
    }


    @Override public void close() throws IOException {
        in.close();
        out.close();
    }
}
