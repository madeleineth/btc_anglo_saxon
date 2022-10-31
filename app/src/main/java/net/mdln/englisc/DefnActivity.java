package net.mdln.englisc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An activity for viewing definitions. In the intent that starts it, it must be passed
 * {@link #EXTRA_BTC_URL}, which is of the form https://btc.invalid/N where N is the nid of the
 * term. The weird URL format is to ensure Android's WebView triggers a navigation. (As a hack,
 * {@link #BTC_ABOUT_URL} lets us use this Activity for showing the "About" content.)
 */
public class DefnActivity extends AppCompatActivity {
    static final String EXTRA_BTC_URL = "net.mdln.englisc.DefnActivity.BTC_URL";
    static final String BTC_URL_PREFIX = "https://btc.invalid/";
    static final String BTC_ABOUT_URL = BTC_URL_PREFIX + "about";
    private LazyDict dict;
    private Term term;
    private Mode mode;

    /**
     * Given a URL like "https://btc.invalid/345" returns the int 345.
     */
    private static int urlToNid(String url) {
        if (!url.startsWith(BTC_URL_PREFIX)) {
            throw new IllegalArgumentException("not a BTC URL: " + url);
        }
        String nidPart = url.substring(BTC_URL_PREFIX.length());
        try {
            return Integer.parseInt(nidPart);
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
        mode = Mode.DEFN;
        if (btcUrl.equals(BTC_ABOUT_URL)) {
            term = fakeAboutTerm();
        } else {
            int nid = urlToNid(btcUrl);
            term = dict.get().loadNid(nid);
            // Record the fact that we viewed this term in the on-disk history.
            try (TermHistory h = new TermHistory(this, TermHistory.Location.ON_DISK, 0)) {
                h.recordId(nid, System.currentTimeMillis());
            }
        }

        setContentView(R.layout.activity_defn);
        updateHtmlContent();
        setSupportActionBar(findViewById(R.id.defn_toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        WebView.setWebContentsDebuggingEnabled(true);
        setUpEventListeners();
    }

    private void setUpEventListeners() {
        final WebView content = findViewById(R.id.defn_content);
        content.setWebViewClient(new WebViewClient() {
            // Don't use the WebResourceRequest version of shouldOverrideUrlLoading; it doesn't work before API 24.
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(BTC_URL_PREFIX)) {
                    Log.i("DefnActivity", "Initiating DefnActivity with URL " + url);
                    Intent intent = new Intent(DefnActivity.this, DefnActivity.class);
                    intent.putExtra(DefnActivity.EXTRA_BTC_URL, url);
                    DefnActivity.this.startActivity(intent);
                    return true;
                } else if (url.startsWith("data:")) {
                    return false;
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
            }
        });

        final SearchView search = findViewById(R.id.defn_search);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                content.findNext(true);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String searchText) {
                content.findAllAsync(searchText);
                return true;
            }
        });
        search.setOnCloseListener(() -> {
            search.setVisibility(View.GONE);
            return false;
        });
        content.setFindListener((activeMatch, numMatches, done) -> {
            // If an in-page search fails and contains "th", try again after replacing "th" with "þ".
            // We don't need to worry about ð or æ because Android's WebView search matches ð if you
            // type "d" and æ if you type "ae".
            String q1 = search.getQuery().toString();
            String q2 = q1.replaceAll("t[hH]", "þ").replaceAll("T[hH]", "Þ");
            if (numMatches == 0 && !q1.equals(q2)) {
                search.setQuery(q2, false);
            }
        });
    }

    /**
     * Returns a fake {@link Term} that represents the "About" page.
     */
    private Term fakeAboutTerm() {
        String html = Streams.readUtf8Resource(this, R.raw.about);
        String title = this.getString(R.string.about_page_title);
        return Term.create(title, html, "", null /* modE */, 0 /* invalid nid */, 0.0 /* score */);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menu.findItem(R.id.main_menu_find).setVisible(true);
        if (term.conjHtml() != null) {
            menu.findItem(R.id.main_menu_conj).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        if (new MenuHandler(this).handleSelection(item, "Current term: " + term.title())) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        dict.close();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        return false;
    }

    void toggleSearchBox() {
        SearchView view = findViewById(R.id.defn_search);
        if (view.getVisibility() == View.VISIBLE) {
            view.setQuery("", false);
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }

    void setMode(Mode mode) {
        if (mode == this.mode) {
            return;
        }
        this.mode = mode;
        findViewById(R.id.defn_search).setVisibility(View.GONE);
        updateHtmlContent();
        Menu menu = ((Toolbar) findViewById(R.id.defn_toolbar)).getMenu();
        menu.findItem(R.id.main_menu_conj).setVisible(mode == Mode.DEFN);
        menu.findItem(R.id.main_menu_defn).setVisible(mode == Mode.CONJ);
    }

    private void updateHtmlContent() {
        String title = Character.toTitleCase(term.title().charAt(0)) + term.title().substring(1);
        if (mode == Mode.CONJ) {
            title += " " + getString(R.string.conj_suffix);
        }
        ((Toolbar) findViewById(R.id.defn_toolbar)).setTitle(title);
        WebView content = findViewById(R.id.defn_content);
        String html = mode == Mode.DEFN ? term.defnHtml() : term.conjHtml();
        WebViewStyle.apply(this, content, html);
    }

    enum Mode {DEFN, CONJ}
}
