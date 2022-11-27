package net.mdln.englisc;

import androidx.test.espresso.IdlingResource;

import java.util.ArrayList;

/**
 * An {@link androidx.test.espresso.IdlingResource} implementation that is idle only if
 * {@link MainActivity} does not have any pending searches. This way {@link MainActivityTest} can
 * be sure that when it searches for a word, it doesn't look at the top search result until the
 * searches are all completed.
 */
class SearchPendingIdlingResource implements IdlingResource {
    private final MainActivity activity;
    private final ArrayList<ResourceCallback> callbacks = new ArrayList<>();

    SearchPendingIdlingResource(MainActivity activity) {
        this.activity = activity;
        activity.onSearchFinished(() -> {
            if (isIdleNow()) {
                for (ResourceCallback cb : callbacks) {
                    cb.onTransitionToIdle();
                }
            }
        });
    }

    @Override
    public String getName() {
        return "SearchPending";
    }

    @Override
    public boolean isIdleNow() {
        return activity.pendingSearches() == 0;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        callbacks.add(callback);
    }
}