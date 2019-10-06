package net.mdln.englisc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class Streams {
    private Streams() {
    }

    static byte[] toByteArray(InputStream is) {
        byte[] buffer = new byte[1024];
        try (ByteArrayOutputStream os = new ByteArrayOutputStream(is.available())) {
            while (true) {
                int n = is.read(buffer);
                if (n <= 0) {
                    break;
                }
                os.write(buffer, 0, n);
            }
            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
