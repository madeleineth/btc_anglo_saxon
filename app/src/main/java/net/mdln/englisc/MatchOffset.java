package net.mdln.englisc;

import com.google.auto.value.AutoValue;

/**
 * Represents the 4-tuple for a single result in a sqlite "offsets(...)" call.
 * See https://www.sqlite.org/fts3.html#offsets.
 */
@AutoValue
abstract class MatchOffset {
    static MatchOffset create(int colNo, int termNo, int offset, int size) {
        return new AutoValue_MatchOffset(colNo, termNo, offset, size);
    }

    abstract int colNo();

    abstract int termNo();

    abstract int offset();

    abstract int size();
}
