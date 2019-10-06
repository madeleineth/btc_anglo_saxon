package net.mdln.englisc;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    // We may need to copy a large dictionary resource to the file system before we can use a Dict,
    // so when the activity is created, start a background task to do that if necessary. We will
    // only refer to the result of this task in a background search task.
    private AsyncTask<?, ?, Dict> dict = null;

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

        dict = dictTask();

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

    @SuppressLint("StaticFieldLeak")
    private AsyncTask<?, ?, Dict> dictTask() {
        return (new AsyncTask<Void, Void, Dict>() {
            @Override
            protected Dict doInBackground(Void... params) {
                return new Dict(DictDB.get(MainActivity.this));
            }
        }).execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void searchInBackground() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                String q = search.getQuery().toString();
                try {
                    final List<Term> t = q.length() >= 2 ?
                            dict.get().search(q, 50) :
                            Collections.<Term>emptyList();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            results.setTerms(t);
                        }
                    });
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException("Error querying in background.", e);
                }
            }
        });
    }
}
