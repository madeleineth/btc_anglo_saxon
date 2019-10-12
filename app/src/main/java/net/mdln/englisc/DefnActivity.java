package net.mdln.englisc;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;

import org.jetbrains.annotations.NotNull;

/**
 * An activity for viewing definitions. In the intent that starts it, it must be passed
 * {@link #EXTRA_BTC_URL}, which is of the form btc://N where N is the nid of the term.
 */
public class DefnActivity extends AppCompatActivity {
    static final String EXTRA_BTC_URL = "net.mdln.englisc.DefnActivity.BTC_URL";
    static final String BTC_URL_PREFIX = "btc://";
    private LazyDict dict;

    /**
     * Given a URL like "btc://345" returns the int 345.
     */
    private static int urlToNid(String url) {
        if (!url.startsWith(BTC_URL_PREFIX)) {
            throw new IllegalArgumentException("not a BTC URL: " + url);
        }
        String nidPart = url.substring(BTC_URL_PREFIX.length());
        try {
            return Integer.valueOf(nidPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid BTC URL: " + url);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String btcUrl = intent.getStringExtra(EXTRA_BTC_URL);
        if (btcUrl == null) {
            throw new RuntimeException("expected " + EXTRA_BTC_URL);
        }
        dict = new LazyDict(this);
        Term term = dict.get().loadNid(urlToNid(btcUrl));

        setContentView(R.layout.activity_defn);
        Toolbar toolbar = findViewById(R.id.defn_toolbar);
        toolbar.setTitle(term.title());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView content = findViewById(R.id.defn_content);
        content.setText(linkifyHtml(term.html()));
        content.setMovementMethod(LinkMovementMethod.getInstance());  //  clickable links
    }

    /**
     * Return a spanned string of {@code html} with URLs of the form "btc://N" set up to trigger
     * an intent to open a nested {@link DefnActivity} instead of opening a browser.
     */
    Spannable linkifyHtml(String html) {
        Spanned spannedHtml = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_HEADING);
        SpannableStringBuilder seq = new SpannableStringBuilder(spannedHtml);
        for (URLSpan span : seq.getSpans(0, seq.length(), URLSpan.class)) {
            final String url = span.getURL();
            if (!url.startsWith(BTC_URL_PREFIX)) {
                continue;
            }
            ClickableSpan replacementSpan = new ClickableSpan() {
                public void onClick(@NotNull View view) {
                    Intent intent = new Intent(DefnActivity.this, DefnActivity.class);
                    intent.putExtra(DefnActivity.EXTRA_BTC_URL, url);
                    DefnActivity.this.startActivity(intent);
                }
            };
            seq.setSpan(replacementSpan, seq.getSpanStart(span), seq.getSpanEnd(span), seq.getSpanFlags(span));
            seq.removeSpan(span);
        }
        return seq;
    }

    @Override
    protected void onDestroy() {
        dict.close();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        return false;
    }
}
