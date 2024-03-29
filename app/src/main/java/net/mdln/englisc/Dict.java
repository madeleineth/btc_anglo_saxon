package net.mdln.englisc;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import org.jetbrains.annotations.NotNull;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Uses a SQLite database to answer search queries. See {@link #search}.
 */
final class Dict implements AutoCloseable {
    // Scoring happens in Java, but we want to keep from returning a huge number of low-quality
    // results. So, we return all matches on "terms", but put a a LIMIT on the number of matches
    // in the rest of the entry (effectively, the "html" column).
    //
    // The weird subexpression "ORDER BY CAST(substr(offsets(defn_idx), 5) AS INTEGER)" is a way of
    // saying "strip off the first two numbers in the offsets (which are always one digit) extract
    // the byte-offset of the match, and sort the results with early matches first." This is done by
    // the scoring algorithm later in Java, but unless we do a rough version of it in SQL, we may
    // never see some results that would otherwise score highly.
    private static final String select = "SELECT title, html, conj_html, mod_e, rowid, terms, entry_type, offsets(defn_idx) FROM defn_idx";
    private static final String Q1 = select + " WHERE defn_idx MATCH ? ORDER BY CAST(substr(offsets(defn_idx), 5) AS INTEGER) LIMIT ?";
    private static final String Q2 = select + " WHERE terms MATCH ?";
    private static final String Q3 = select + " WHERE mod_e MATCH ?";
    private static final String QRY = "SELECT * FROM (  " + Q1 + ") UNION " + Q2 + " UNION " + Q3;

    private static final double MINIMUM_SCORE = 0.003;

    private final SQLiteDatabase db;

    Dict(@NotNull SQLiteDatabase db) {
        this.db = db;
    }

    /**
     * Matches for Modern English equivalents go first, then exact term matches
     * and abbreviations, then we rank by how early in the HTML the query terms
     * were.
     */
    private static double scoreTerm(boolean termMatch, boolean modEngMatch, boolean goodEntry, String entryType, List<MatchOffset> offsets) {
        if (!entryType.equals("a") /* abbrev */ && !entryType.equals("e") /* entry */) {
            throw new IllegalArgumentException("invalid entry type: '" + entryType + "'");
        }
        double score = (termMatch ? 2.0 : 0.0) + (entryType.equals("a") ? 1.0 : 0.0);
        if (modEngMatch) {
            score += 5;
        }
        if (goodEntry) {
            score += 0.5;
        }
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
        term = Normalizer.normalize(term, Normalizer.Form.NFKD);
        term = term.replaceAll("[ðþ]", "th");
        term = term.replaceAll("æ", "ae");
        term = term.replaceAll("[^a-z ]", "");
        term = term.replaceAll("  +", " ");
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
                    Integer.parseInt(e[i]),
                    Integer.parseInt(e[i + 1]),
                    Integer.parseInt(e[i + 2]),
                    Integer.parseInt(e[i + 3]));
            ret.add(off);
        }
        return ret;
    }

    private static Term queryMatchToTerm(String query, Cursor cursor) {
        String title = cursor.getString(0);
        String defnHtml = cursor.getString(1);
        String conjHtml = cursor.getString(2);
        String modE = cursor.getString(3);
        int rowId = cursor.getInt(4);
        String terms = cursor.getString(5);
        String entryType = cursor.getString(6);
        // The "terms" column is of the form "/form1/form2/.../" so if we see "/query/" then we got
        // an exact term match.
        boolean termMatch = terms.contains("/" + query + "/");
        boolean modEngMatch = modE != null && modE.contains(query.toLowerCase());
        boolean goodEntry = modE != null;
        List<MatchOffset> offsets = parseOffsets(cursor.getString(7));
        double score = scoreTerm(termMatch, modEngMatch, goodEntry, entryType, offsets);
        return Term.create(title, defnHtml, conjHtml, modE, rowId, score);
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

    @Override
    public void close() {
        db.close();
    }

    /**
     * Search the database for inflected terms or HTML phrases that match {@code query}. At most
     * {@code limit} HTML phrase matches are considered. If there are any spaces in {@code query},
     * we can't be matching a word, so we do a phrase search. If there are no spaces, we do a word
     * search of "html" and a prefix search in "terms".
     * <p>
     * Returns a list of terms in descending score order.
     */
    List<Term> search(@NotNull String query, int limit) {
        String term = normalizeQuery(query);
        String ftsQuery = term.contains(" ") ? "\"" + term + "\"" : "html:" + term + " OR terms:" + term + "*";
        String[] args = new String[]{ftsQuery, String.valueOf(limit), term, term};
        List<Term> retVal = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(QRY, args)) {
            Log.d("Dict", "Got " + cursor.getCount() + " results for '" + query + "'");
            while (cursor.moveToNext()) {
                retVal.add(queryMatchToTerm(term, cursor));
            }
        }
        sortByDescendingScore(retVal);
        retVal = removeLowScoringTerms(retVal);
        retVal = removeDuplicates(retVal);
        return retVal;
    }

    /**
     * Returns the term in the database with the specified row id, or null if none exists.
     */
    Term loadNid(int nid) {
        try (Cursor cursor = db.rawQuery("SELECT title, html, conj_html, mod_e FROM defn_idx WHERE rowid = ?", new String[]{String.valueOf(nid)})) {
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToNext();
            String title = cursor.getString(0);
            String defnHtml = cursor.getString(1);
            String conjHtml = cursor.getString(2);
            String modE = cursor.getString(3);
            return Term.create(title, defnHtml, conjHtml, modE, nid, 0.0);
        }
    }
}
