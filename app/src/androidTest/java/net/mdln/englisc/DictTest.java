package net.mdln.englisc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class DictTest {

    @Test
    public void search() {
        final String[][] expectedResults = {
                /* inflected form */ {"healp", "helpan"},
                /* phrase search in modern English */ {"to write", "mis-writan"},
                /* frequent word that should match canonical entry */ {"thaet", "þæt"},
                /* word with non-ASCII characters in search */ {"þrittig", "þritig"},
                /* variant spelling of very common word */ {"þonne", "þanne"},
                /* manually-added variant */ {"Swylce", "swilc"},
        };
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (SQLiteDatabase db = DictDB.get(ctx)) {
            Dict d = new Dict(db);
            for (String[] ex : expectedResults) {
                String query = ex[0];
                String expectedTerm = ex[1];
                List<Term> t = d.search(query, 100);
                String msg = "query: '" + query + "'";
                assertThat(msg, t.size(), greaterThan(0));
                assertThat(msg, t.size(), lessThan(120));
                assertEquals(msg, expectedTerm, t.get(0).title());
            }
        }
    }

    @Test
    public void parseOffets() {
        List<MatchOffset> offsets = Dict.parseOffsets("1 2 3 4");
        List<MatchOffset> expected = Collections.singletonList(MatchOffset.create(1, 2, 3, 4));
        assertEquals(expected, offsets);
    }

    @Test
    public void normalizeQuery() {
        assertEquals("thaet", Dict.normalizeQuery("þæt"));
    }

    @Test
    public void normalizeAccentedQuery() {
        assertEquals("aela", Dict.normalizeQuery("\u01fd-l\u00e1!"));
    }
}
