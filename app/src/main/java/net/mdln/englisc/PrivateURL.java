package net.mdln.englisc;

import com.google.auto.value.AutoValue;

import org.jetbrains.annotations.NotNull;

@AutoValue
abstract class PrivateURL {
    static final String BTC_URL_PREFIX = "https://btc.invalid/";
    static final String BTC_CONJ_URL_PREFIX = BTC_URL_PREFIX + "conjugate/";
    static final String BTC_ABOUT_URL = BTC_URL_PREFIX + "about";

    /* Given a URL like "https://btc.invalid/345" return a {@code PrivateURL} representing it. */
    static PrivateURL parse(@NotNull String url) {
        if (url.equals(BTC_ABOUT_URL)) {
            return new AutoValue_PrivateURL(Type.ABOUT, -1);
        }
        if (!url.startsWith(BTC_URL_PREFIX)) {
            throw new IllegalArgumentException("not a BTC URL: " + url);
        }
        String nidPart;
        Type type;
        if (url.startsWith(BTC_CONJ_URL_PREFIX)) {
            nidPart = url.substring((BTC_CONJ_URL_PREFIX.length()));
            type = Type.CONJ;
        } else {
            nidPart = url.substring(BTC_URL_PREFIX.length());
            type = Type.DEFN;
        }
        try {
            int nid = Integer.parseInt(nidPart);
            return new AutoValue_PrivateURL(type, nid);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid BTC URL: " + url);
        }
    }

    static String forDefn(int nid) {
        return BTC_URL_PREFIX + nid;
    }

    static String forConj(int nid) {
        return BTC_CONJ_URL_PREFIX + nid;
    }

    @NotNull
    abstract Type type();

    abstract int nid();

    enum Type {DEFN, CONJ, ABOUT}
}
