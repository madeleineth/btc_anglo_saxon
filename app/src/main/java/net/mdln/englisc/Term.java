package net.mdln.englisc;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a result of a user's search. See {@link Dict}.
 */
@AutoValue
abstract class Term {
    static Term create(@NotNull String title, @NotNull String defnHtml, String conjHtml, String modE, int nid, double score) {
        return new AutoValue_Term(title, defnHtml, conjHtml, modE, nid, score);
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

    @Nullable
    abstract String conjHtml();

    @Nullable
    abstract String modE();

    abstract int nid();

    abstract double score();
}
