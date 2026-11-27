package net.mdln.englisc;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;
import android.webkit.WebView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

class ViewUtil {

    static private float navInsetCssPx = 0.0f; // fine to be global, it is the same everywhere

    /**
     * Sets toolbar top padding so that it doesn't overlap the OS status bar.
     */
    static void setToolbarPadding(View toolbar) {
        Activity activity = (Activity) toolbar.getContext();
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * Sets layout padding to avoid nav buttons on the left or right in landscape mode. In
     * portrait mode the default behavior, where the text scrolls behind them, is fine, since
     * the buttons are on the bottom.
     */
    static void setLayoutInsets(View rootView) {
        Activity activity = (Activity) rootView.getContext();
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            boolean isLandscape = activity.getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE;
            if (isLandscape) {
                v.setPadding(insets.left, 0, insets.right, insets.bottom);
                return windowInsets.inset(insets.left, 0, insets.right, insets.bottom);
            }
            return windowInsets;
        });
    }


    /**
     * The height of the bottom nav buttons in CSS pixels, as of the last time we were told. Zero
     * until the first window insets arrive.
     */
    static float navInsetCssPx() {
        return navInsetCssPx;
    }

    /**
     * Similar to setLayoutInsets but for the inside of a webview. The inset has to go into the
     * page's CSS (see {@link WebViewStyle}), so this just records it and runs {@code reload}
     * when it differs from what the page was last built with.
     */
    static void setWebViewInsets(WebView webView, Runnable reload) {
        ViewCompat.setOnApplyWindowInsetsListener(webView, (v, insets) -> {
            int px = insets.getInsets(WindowInsetsCompat.Type.tappableElement()).bottom;
            float cssPx = px / v.getResources().getDisplayMetrics().density;
            if (cssPx != navInsetCssPx) {
                navInsetCssPx = cssPx;
                reload.run();
            }
            return insets;
        });
    }
}
