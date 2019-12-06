package net.mdln.englisc;

import android.annotation.SuppressLint;
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

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private LazyDict dict = null;
    private ResultsAdapter results = null;
    private SearchView search = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        search = findViewById(R.id.searchView);

        // We specify search results to the RecyclerView by calling `results.setTerms(...)`.
        results = new ResultsAdapter();
        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setAdapter(results);
        rv.setLayoutManager(new LinearLayoutManager(this));

        dict = new LazyDict(this);

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
    }

    @Override
    protected void onDestroy() {
        dict.close();
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

    @SuppressLint("StaticFieldLeak")
    private void searchInBackground() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                String q = search.getQuery().toString();
                final List<Term> t = q.length() >= 2 ?
                        dict.get().search(q, 50) :
                        Collections.<Term>emptyList();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        results.setTerms(t);
                    }
                });
            }
        });
    }
}
