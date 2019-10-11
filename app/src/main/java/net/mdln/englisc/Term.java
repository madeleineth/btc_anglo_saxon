package net.mdln.englisc;

import com.google.auto.value.AutoValue;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a result of a user's search. See {@link Dict}.
 */
@AutoValue
abstract class Term {
    static Term create(@NotNull String title, @NotNull String html, int nid, double score) {
        return new AutoValue_Term(title, html, nid, score);
    }

    @NotNull
    abstract String title();

    @NotNull
    abstract String html();

    abstract int nid();

    abstract double score();
}
