package net.mdln.englisc;

import android.app.Activity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

    static String readUtf8Resource(Activity activity, int id) {
        try (InputStream stream = activity.getResources().openRawResource(id)) {
            return new String(toByteArray(stream), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("can't load raw resource " + id, e);
        }
    }
}
