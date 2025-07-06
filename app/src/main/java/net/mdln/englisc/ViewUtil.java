package net.mdln.englisc;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

class ViewUtil {

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
            }
            return windowInsets;
        });
    }
}
