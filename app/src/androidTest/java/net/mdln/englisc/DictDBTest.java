package net.mdln.englisc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

public class DictDBTest {

    /**
     * Check that we can get a handle to the dictionary and query for a word we know should be there.
     * <p>
     * More serious testing is done in {@link DictTest}.
     */
    @Test
    public void get() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (SQLiteDatabase db = DictDB.get(ctx)) {
            try (Cursor cursor = db.rawQuery("SELECT title FROM defn_idx WHERE terms MATCH 'wicingum'", new String[]{})) {
                assertTrue(cursor.moveToFirst());
                String title = cursor.getString(0);
                assertEquals("wicing", title);
            }
        }
    }
}
