package net.mdln.englisc;

import com.google.auto.value.AutoValue;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a result of a user's search. See {@link Dict}.
 */
@AutoValue
abstract class Term {
    static Term create(@NotNull String title, @NotNull String defnHtml, @NotNull String conjHtml, int nid, double score) {
        return new AutoValue_Term(title, defnHtml, conjHtml, nid, score);
    }

    /**
     * Remove all opening and closing {@code <a>} tags.
     */
    static String unlinkifyTermHtml(String html) {
        return html.replaceAll("</?[aA][^>]*>", "");
    }

    @NotNull
    abstract String title();

    @NotNull
    abstract String defnHtml();

    @NotNull
    abstract String conjHtml();

    abstract int nid();

    abstract double score();
}
