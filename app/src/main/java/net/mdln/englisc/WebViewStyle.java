package net.mdln.englisc;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.util.Base64;
import android.webkit.WebView;

import androidx.webkit.WebSettingsCompat;

public class WebViewStyle {
    private WebViewStyle() {
    }

    /**
     * Apply the app's style to {@code view} and set its HTML to {@code html}.
     */
    public static void apply(Activity activity, WebView view, String html) {
        WebView.setWebContentsDebuggingEnabled(true);
        view.setBackgroundColor(Color.TRANSPARENT);  // Otherwise it flashes white before rendering in dark mode.
        view.getSettings().setJavaScriptEnabled(BuildConfig.DEBUG); // Espresso needs JavaScript.
        boolean night = inNightMode(activity);
        String encodedHtml = Base64.encodeToString(styledHtml(activity, html, night).getBytes(), Base64.NO_PADDING);
        view.loadData(encodedHtml, "text/html; charset=utf-8", "base64");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.getSettings(), true);
        }
    }

    private static String styledHtml(Activity activity, String html, boolean night) {
        String headBlock = "<head><style type=\"text/css\">" + Streams.readUtf8Resource(activity, R.raw.defn) + "</style></head>";
        String styleClass = night ? "dark" : "light";
        String bodyBlock = "<body class=\"" + styleClass + "\">" + html + "</body>";
        return "<html>" + headBlock + bodyBlock + "</html>";
    }

    private static boolean inNightMode(Activity activity) {
        Configuration cfg = activity.getResources().getConfiguration();
        int nightMode = cfg.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }
}
