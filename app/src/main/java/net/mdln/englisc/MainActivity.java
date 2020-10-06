package net.mdln.englisc;

import android.os.AsyncTask;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {
    private LazyDict dict = null;
    private ResultsAdapter results = null;
    private SearchView search = null;
    private TermHistory history = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        search = findViewById(R.id.searchView);

        // We specify search results to the RecyclerView by calling `results.setTerms(...)`.
        results = new ResultsAdapter();
        RecyclerView rv = findViewById(R.id.search_results);
        rv.setAdapter(results);
        rv.setLayoutManager(new LinearLayoutManager(this));

        dict = new LazyDict(this);
        long tenDaysAgoMillis = System.currentTimeMillis() - 10 * 24 * 3600 * 1000;
        history = new TermHistory(this, TermHistory.Location.ON_DISK, tenDaysAgoMillis);

        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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

        search.requestFocus();
        searchInBackground();  // to show terms from the history
    }

    @Override
    protected void onDestroy() {
        if (dict != null) {
            dict.close();
        }
        if (history != null) {
            history.close();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        if (new MenuHandler(this).handleSelection(item, "Current search text: " + search.getQuery().toString())) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void searchInBackground() {
        // Get this on the UI thread because SearchView.getQuery is not thread-safe.
        final String qry = search.getQuery().toString();
        AsyncTask.execute(new Runnable() {
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
                final List<Term> t = getTerms();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        results.setTerms(t);
                        // Show or hide the "recent:" label.
                        String q = ((SearchView) findViewById(R.id.searchView)).getQuery().toString();
                        boolean historyActive = q.equals("") && results.getItemCount() > 0;
                        findViewById(R.id.recentLabel).setVisibility(historyActive ? View.VISIBLE : View.GONE);
                    }
                });
            }
        });
    }

    private List<Term> historyTerms() {
        final int numHistoryTermsToShow =  20;
        List<Term> terms = new ArrayList<>();
        for (int nid : history.getIds(numHistoryTermsToShow)) {
            terms.add(dict.get().loadNid(nid));
        }
        return terms;
    }
}
