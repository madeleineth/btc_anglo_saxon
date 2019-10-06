package net.mdln.englisc;

import com.google.auto.value.AutoValue;

/**
 * Represents a result of a user's search. See {@link Dict}.
 */
@AutoValue
abstract class Term {
    static Term create(String title, String html, int nid, double score) {
        return new AutoValue_Term(title, html, nid, score);
    }

    abstract String title();

    abstract String html();

    abstract int nid();

    abstract double score();
}
