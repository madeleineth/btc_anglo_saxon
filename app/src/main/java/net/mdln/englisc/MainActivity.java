package net.mdln.englisc;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private final AtomicInteger numPendingSearches = new AtomicInteger(0);
    // `readySemaphore` is has one permit when `dict` and `history` are valid but not in active use.
    // This way, `onDestroy` can wait to close them if they're in use by `getTerms` on another thread.
    private final Semaphore readySemaphore = new Semaphore(0);
    private LazyDict dict = null;
    private ResultsAdapter results = null;
    private SearchView searchBox = null;
    private TermHistory history = null;
    private Runnable searchFinishedCallback = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        searchBox = findViewById(R.id.search_box);

        // We specify search results to the RecyclerView by calling `results.setTerms(...)`.
        results = new ResultsAdapter();
        RecyclerView rv = findViewById(R.id.search_results);
        rv.setAdapter(results);
        rv.setLayoutManager(new LinearLayoutManager(this));

        dict = new LazyDict(this);
        long tenDaysAgoMillis = System.currentTimeMillis() - 10 * 24 * 3600 * 1000;
        history = new TermHistory(this, TermHistory.Location.ON_DISK, tenDaysAgoMillis);

        searchBox.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String text) {
                // Hide the help text when there is a query, so that it doesn't steal useful screen real estate.
                findViewById(R.id.mainHelpView).setVisibility(text.equals("") ? View.VISIBLE : View.GONE);
                // Only hit the database off the UI thread.
                searchInBackground();
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String text) {
                return true;
            }
        });
        readySemaphore.release();
    }

    @Override
    protected void onDestroy() {
        // Wait for all pending searches to complete so that they don't use closed resources.
        readySemaphore.acquireUninterruptibly();
        if (dict != null) {
            dict.close();
        }
        if (history != null) {
            history.close();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        searchBox.requestFocus();
        searchInBackground();  // to update terms from the history, if necessary
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        if (new MenuHandler(this).handleSelection(item, "Current search text: " + searchBox.getQuery().toString())) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    private void searchInBackground() {
        numPendingSearches.incrementAndGet();
        // Get this on the UI thread because SearchView.getQuery is not thread-safe.
        final String qry = searchBox.getQuery().toString();
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.submit(new Runnable() {
            private List<Term> getTerms() {
                // running off the UI thread
                if (qry.length() >= 2) {
                    return dict.get().search(qry, 50);
                } else if (qry.length() == 0) {
                    return historyTerms();
                } else {
                    return Collections.emptyList();
                }
            }

            @Override
            public void run() {
                boolean wasReady = readySemaphore.tryAcquire();
                if (!wasReady) {
                    Log.w("MainActivity", "Aborting search because Activity was not ready.");
                    return;
                }
                final List<Term> t;
                try {
                    t = getTerms();
                } finally {
                    readySemaphore.release();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        results.setTerms(t);
                        // Show or hide the "recent:" label.
                        String q = ((SearchView) findViewById(R.id.search_box)).getQuery().toString();
                        boolean historyActive = q.equals("") && results.getItemCount() > 0;
                        findViewById(R.id.recentLabel).setVisibility(historyActive ? View.VISIBLE : View.GONE);
                        numPendingSearches.decrementAndGet();
                        if (searchFinishedCallback != null) {
                            searchFinishedCallback.run();
                        }
                    }
                });
            }
        });
        ex.shutdown();
    }

    private List<Term> historyTerms() {
        final int numHistoryTermsToShow = 20;
        List<Term> terms = new ArrayList<>();
        for (int nid : history.getIds(numHistoryTermsToShow)) {
            terms.add(dict.get().loadNid(nid));
        }
        return terms;
    }

    int pendingSearches() {
        return numPendingSearches.get();
    }

    /**
     * Registers {@code fn} to be called whenever a search finishes.
     */
    void onSearchFinished(Runnable fn) {
        if (searchFinishedCallback != null) {
            throw new RuntimeException("Only one search-finished callback may be specified.");
        }
        searchFinishedCallback = fn;
    }
}
