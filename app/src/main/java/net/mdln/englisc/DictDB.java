package net.mdln.englisc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A factory class for the SQLiteDatabase embedded as the "dictdb" resource. See the "get" method for details.
 */
final class DictDB {
    // This keeps us from interleaving expansions of resources.
    private static final ReentrantLock lock = new ReentrantLock();

    private DictDB() {
    }

    private static boolean needsCopy(Context appCtx, File revPath) {
        try (InputStream in1 = appCtx.getResources().openRawResource(R.raw.dictdb_rev);
             InputStream in2 = new FileInputStream(revPath)) {
            return !Arrays.equals(Streams.toByteArray(in1), Streams.toByteArray(in2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyResourceToFile(Context appCtx, int resourceId, File f) {
        try (InputStream in = appCtx.getResources().openRawResource(resourceId);
             OutputStream out = new FileOutputStream(f)) {
            pipe(in, out);
        } catch (IOException e) {
            throw new RuntimeException("can't copy resource to " + f.toString(), e);
        }
    }

    private static void pipe(InputStream in, OutputStream out) {
        final int bufSize = 8196;
        try {
            byte[] buf = new byte[bufSize];
            while (true) {
                int n = in.read(buf);
                if (n == -1) {
                    return;
                }
                out.write(buf, 0, n);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The build packages a "dictdb" resource and an associated "dictdb_rev" file. These are
     * copied to files named "dict.db" and "dict.rev" the first time the app runs. On subsequent
     * runs, we call {@link #needsCopy} to compare "dict.dev" and "dictdb_rev"; if they differ (or
     * if "dict.rev" does not exist because it's the first run), we refresh "dict.db".
     * <p>
     * We can't use "dictdb" directly because resources aren't files, and sqlite can only open
     * files. The database is around 71M, so we don't want to copy it every time, nor do we want to
     * hash it. We don't trust file modification times. So, we use a nonce in "dictdb_rev".
     */
    static SQLiteDatabase get(Context appCtx) {
        File dictDBPath = new File(appCtx.getNoBackupFilesDir(), "dict.db");
        File revPath = new File(appCtx.getNoBackupFilesDir(), "dict.rev");
        try {
            lock.lock();
            if (!dictDBPath.canRead() || !revPath.canRead() || needsCopy(appCtx, revPath)) {
                long startCopyMillis = System.currentTimeMillis();
                copyResourceToFile(appCtx, R.raw.dictdb, dictDBPath);
                copyResourceToFile(appCtx, R.raw.dictdb_rev, revPath);
                long copyMillis = System.currentTimeMillis() - startCopyMillis;
                Log.i("DictDB", "Installed new dictionary at '" + dictDBPath + "' in " + copyMillis + "ms.");
            }
            return SQLiteDatabase.openDatabase(dictDBPath.toString(), null, SQLiteDatabase.OPEN_READONLY);
        } finally {
            lock.unlock();
        }
    }
}
