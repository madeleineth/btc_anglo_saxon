package net.mdln.englisc;

import android.annotation.SuppressLint;
import android.content.Context;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * We may need to copy a large dictionary resource to the file system before we can use a Dict,
 * so when this object is created, start a background task to do that if necessary. Callers
 * can then call {@link #get} (when not on the UI thread) to get a {@link Dict}, blocking if it
 * is not ready yet.
 */
class LazyDict implements AutoCloseable {
    private final Future<Dict> dict;

    @SuppressLint("StaticFieldLeak")
    LazyDict(final Context ctx) {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        dict = ex.submit(new Callable<Dict>() {
            @Override
            public Dict call() {
                return new Dict(DictDB.get(ctx));
            }
        });
        ex.shutdown();
    }

    /**
     * Get a {@link Dict}, blocking if it's not ready yet.
     */
    Dict get() {
        try {
            return dict.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Can't create dictionary in background.", e);
        }
    }

    @Override
    public void close() {
        get().close();
    }
}
