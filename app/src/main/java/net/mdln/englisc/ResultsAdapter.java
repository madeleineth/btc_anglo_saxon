package net.mdln.englisc;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a list of results in the search-results RecyclerView owned by {@link MainActivity}.
 * {@link MainActivity} will call {@link #setTerms} when the results need to be updated.
 */
final class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ViewHolder> {

    private List<Term> terms = Collections.emptyList();

    ResultsAdapter() {
        // Search result rows are uniquely identified by Term.nid.
        setHasStableIds(true);
    }

    void setTerms(Collection<Term> terms) {
        this.terms = Collections.unmodifiableList(new ArrayList<>(terms));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final Context ctx = parent.getContext();
        return new ViewHolder(ctx, LayoutInflater.from(ctx).inflate(R.layout.results_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setTerm(terms.get(position));
    }

    @Override
    public int getItemCount() {
        return terms.size();
    }

    @Override
    public long getItemId(int item) {
        return terms.get(item).nid();
    }

    /**
     * A single row in the search results RecyclerView, representing a single {@link Term}.
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView view;
        private Term term = null;

        ViewHolder(final Context ctx, View itemView) {
            super(itemView);
            view = itemView.findViewById(R.id.results_row);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFullDefinition(ctx);
                }
            });
        }

        void setTerm(Term term) {
            this.term = term;
            view.setText(HtmlCompat.fromHtml(Term.unlinkifyTermHtml(term.html()), HtmlCompat.FROM_HTML_MODE_COMPACT));
        }

        private void openFullDefinition(Context ctx) {
            if (term == null) {
                return;
            }
            Intent intent = new Intent(ctx, DefnActivity.class);
            intent.putExtra(DefnActivity.EXTRA_BTC_URL, DefnActivity.BTC_URL_PREFIX + term.nid());
            ctx.startActivity(intent);
        }
    }
}
