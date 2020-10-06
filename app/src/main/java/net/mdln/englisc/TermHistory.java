package net.mdln.englisc;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of term ids viewed and their timestamps so we can show recently viewed items
 * in {@link MainActivity}.
 */
public class TermHistory implements AutoCloseable {

    private static final int SCHEMA_VERSION = 1;

    private final SQLiteOpenHelper databaseHelper;

    TermHistory(Context ctx, Location loc, long deleteHistoryBeforeMillis) {
        this.databaseHelper = new OpenHelper(ctx, loc, deleteHistoryBeforeMillis / 1000.0);
    }

    void recordId(int nid, long timeMillis) {
        String sql = "INSERT INTO history (nid, timestamp_secs) VALUES (?, ?)";
        databaseHelper.getWritableDatabase().execSQL(sql, new Object[]{nid, timeMillis / 1000.0});
    }

    List<Integer> getIds(int limit) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT nid FROM ( SELECT nid, MAX(timestamp_secs) AS t FROM history GROUP by nid ) AS tb " +
                "ORDER BY t DESC LIMIT " + limit;
        try (Cursor cursor = db.rawQuery(sql, new String[]{})) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getInt(0));
            }
        }
        return ids;
    }

    @Override
    public void close() {
        databaseHelper.close();
    }

    enum Location {ON_DISK, IN_MEMORY}

    private static final class OpenHelper extends SQLiteOpenHelper {

        private final double deleteHistoryBeforeSecs;

        public OpenHelper(Context context, Location loc, double deleteHistoryBeforeSecs) {
            super(context, getPath(context, loc), null, SCHEMA_VERSION);
            this.deleteHistoryBeforeSecs = deleteHistoryBeforeSecs;
        }

        @Nullable
        private static String getPath(Context context, Location loc) {
            return loc == Location.IN_MEMORY ? null :
                    new File(context.getNoBackupFilesDir(), "history.db").toString();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE history (nid INTEGER, timestamp_secs REAL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            if (deleteHistoryBeforeSecs > 0) {
                db.execSQL("DELETE FROM history WHERE timestamp_secs < ?", new String[]{String.valueOf(deleteHistoryBeforeSecs)});
            }
        }
    }
}
