package net.mdln.englisc;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import org.jetbrains.annotations.NotNull;

/**
 * An activity for viewing definitions. In the intent that starts it, it must be passed
 * {@link #EXTRA_BTC_URL}, which is of the form https://btc.invalid/N where N is the nid of the
 * term. The weird URL format is to ensure Android's WebView triggers a navigation.
 */
public class DefnActivity extends AppCompatActivity {
    static final String EXTRA_BTC_URL = "net.mdln.englisc.DefnActivity.BTC_URL";
    static final String BTC_URL_PREFIX = "https://btc.invalid/";
    private LazyDict dict;
    private Term term;

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
        term = dict.get().loadNid(urlToNid(btcUrl));

        setContentView(R.layout.activity_defn);
        Toolbar toolbar = findViewById(R.id.defn_toolbar);
        toolbar.setTitle(term.title());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        WebView.setWebContentsDebuggingEnabled(true);
        WebView content = findViewById(R.id.defn_content);
        content.getSettings().setJavaScriptEnabled(BuildConfig.DEBUG); // Espresso needs JavaScript.

        String css = Streams.readUtf8Resource(this, R.raw.defn);
        String cssBlock = "<style type=\"text/css\">" + Html.escapeHtml(css) + "</style>";
        String encodedHtml = Base64.encodeToString((cssBlock + term.html()).getBytes(), Base64.NO_PADDING);
        content.loadData(encodedHtml, "text/html", "base64");

        content.setWebViewClient(new WebViewClient() {
            // Don't use the WebResourceRequest version of shouldOverrideUrlLoading; it doesn't work before API 24.
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(BTC_URL_PREFIX)) {
                    Log.i("DefnActivity", "Initiating DefnActivity with URL " + url);
                    Intent intent = new Intent(DefnActivity.this, DefnActivity.class);
                    intent.putExtra(DefnActivity.EXTRA_BTC_URL, url);
                    DefnActivity.this.startActivity(intent);
                    return true;
                } else {
                    return false;
                }
            }
        });
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) && inNightMode()) {
            WebSettingsCompat.setForceDark(content.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
        }
    }

    private boolean inNightMode() {
        Configuration cfg = getResources().getConfiguration();
        int nightMode = cfg.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
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
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        return false;
    }
}
