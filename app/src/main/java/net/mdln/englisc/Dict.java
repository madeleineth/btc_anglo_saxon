package net.mdln.englisc;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.VisibleForTesting;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Uses a SQLite database to answer search queries. See {@link #search}.
 */
final class Dict {
    // Scoring happens in Java, but we want to keep from returning a huge number of low-quality
    // results. So, we return all matches on "terms", but put a a LIMIT on the number of matches
    // in the rest of the entry (effectively, the "html" column).
    private static final String QRY =
            "SELECT * FROM ( SELECT title, html, rowid, terms, entry_type, offsets(defn_idx) FROM defn_idx WHERE defn_idx MATCH ? LIMIT ? ) UNION SELECT title, html, rowid, terms, entry_type, offsets(defn_idx) FROM defn_idx WHERE terms MATCH ?";

    private static final double MINIMUM_SCORE = 0.003;

    private final SQLiteDatabase db;

    Dict(@NotNull SQLiteDatabase db) {
        this.db = db;
    }

    /**
     * Exact term matches and abbreviations go first. Then we rank by how early in the HTML the query terms were.
     */
    private static double scoreTerm(boolean termMatch, String entryType, List<MatchOffset> offsets) {
        if (!entryType.equals("a") /* abbrev */ && !entryType.equals("e") /* entry */) {
            throw new IllegalArgumentException("invalid entry type: '" + entryType + "'");
        }
        double score = (termMatch ? 2.0 : 0.0) + (entryType.equals("a") ? 1.0 : 0.0);
        if (offsets.size() > 0) {
            score += 1.0 / offsets.get(0).offset();
        }
        return score;
    }

    /**
     * Matches {@code ascify} in {@code db/normalize.py}.
     */
    @VisibleForTesting
    static String normalizeQuery(String q) {
        String term = q.toLowerCase().trim();
        term = term.replaceAll("[ðþ]", "th");
        term = term.replaceAll("æ", "ae");
        return term;
    }

    /**
     * See https://www.sqlite.org/fts3.html#offsets.
     */
    @VisibleForTesting
    static List<MatchOffset> parseOffsets(String offsets) {
        String[] e = offsets.split(" ");
        if (e.length % 4 != 0) {
            throw new RuntimeException("could not parse offsets: " + offsets);
        }
        ArrayList<MatchOffset> ret = new ArrayList<>();
        for (int i = 0; i < e.length; i += 4) {
            MatchOffset off = MatchOffset.create(
                    Integer.valueOf(e[i]),
                    Integer.valueOf(e[i + 1]),
                    Integer.valueOf(e[i + 2]),
                    Integer.valueOf(e[i + 3]));
            ret.add(off);
        }
        return ret;
    }

    private static Term queryMatchToTerm(String query, Cursor cursor) {
        String title = cursor.getString(0);
        String html = cursor.getString(1);
        int rowId = cursor.getInt(2);
        String terms = cursor.getString(3);
        // The "terms" column is of the form "/form1/form2/.../" so if we see "/query/" then we got
        // an exact term match.
        boolean termMatch = terms.contains("/" + query + "/");
        String entryType = cursor.getString(4);
        List<MatchOffset> offsets = parseOffsets(cursor.getString(5));
        double score = scoreTerm(termMatch, entryType, offsets);
        return Term.create(title, html, rowId, score);
    }

    private static void sortByDescendingScore(List<Term> terms) {
        Collections.sort(terms, new Comparator<Term>() {
            @Override
            public int compare(Term t1, Term t2) {
                return -1 * Double.compare(t1.score(), t2.score());
            }
        });
    }

    private static List<Term> removeLowScoringTerms(List<Term> terms) {
        List<Term> retVal = new ArrayList<>();
        for (Term t : terms) {
            if (t.score() >= MINIMUM_SCORE) {
                retVal.add(t);
            }
        }
        return retVal;
    }

    private static List<Term> removeDuplicates(List<Term> terms) {
        Set<String> seenTitles = new HashSet<>();
        List<Term> retVal = new ArrayList<>();
        for (Term t : terms) {
            if (!seenTitles.contains(t.title())) {
                seenTitles.add(t.title());
                retVal.add(t);
            }
        }
        return retVal;
    }

    /**
     * Search the database for inflected terms or HTML phrases that match {@code query}. At most
     * {@code limit} HTML phrase matches are considered. If there are any spaces in {@code query},
     * we can't be matching a word, so we do a phrase search. If there are no spaces, we do a word
     * search of "html" and a prefix search in "terms".
     *
     * Returns a list of terms in descending score order.
     */
    List<Term> search(@NotNull String query, int limit) {
        String term = normalizeQuery(query);
        String ftsQuery = term.contains(" ") ? "\"" + term + "\"" : "html:" + term + " OR terms:" + term + "*";
        String[] args = new String[]{ftsQuery, String.valueOf(limit), term};
        List<Term> retVal = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(QRY, args)) {
            while (cursor.moveToNext()) {
                // Is this a bug? Maybe we should pass `term`, not `query`.
                retVal.add(queryMatchToTerm(query, cursor));
            }
        }
        sortByDescendingScore(retVal);
        retVal = removeLowScoringTerms(retVal);
        retVal = removeDuplicates(retVal);
        return retVal;
    }
}
