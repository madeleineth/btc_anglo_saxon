package net.mdln.englisc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.MenuItem;

import org.jetbrains.annotations.NotNull;

/**
 * This class factors out the code for handling the hamburger menu that is common between
 * {@link MainActivity} and {@link DefnActivity}.
 */
final class MenuHandler {
    final private Activity activity;

    MenuHandler(Activity activity) {
        this.activity = activity;
    }

    /**
     * Returns true if the item was successfully handled.
     */
    boolean handleSelection(@NotNull MenuItem item, String contextString) {
        switch (item.getItemId()) {
            case R.id.main_menu_info:
                Intent intent = new Intent(activity, DefnActivity.class);
                intent.putExtra(DefnActivity.EXTRA_BTC_URL, DefnActivity.BTC_ABOUT_URL);
                activity.startActivity(intent);
                return true;
            case R.id.main_menu_feedback:
                sendFeedback(contextString);
                return true;
            case R.id.main_menu_find:
                findInDefn();
            default:
                return false;
        }
    }

    private void sendFeedback(String contextString) {
        String body = activity.getString(R.string.type_feedback_here) + "\n\n" +
                "Hardware: " + Build.BRAND + " / " + Build.MODEL + "\n" +
                "Android version: " + Build.VERSION.RELEASE + "\n" +
                "App version: " + BuildConfig.VERSION_NAME + "\n" +
                contextString;
        String subject = String.format(activity.getString(R.string.feedback_subject), activity.getString(R.string.long_app_name));
        String mailto = "mailto:" + activity.getString(R.string.feedback_email) +
                "?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body);
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(mailto));
        activity.startActivity(emailIntent);
    }

    private void findInDefn() {
        if (!(activity instanceof DefnActivity)) {
            Log.e("MenuHandler", "findInDefn called when the activity is not a DefnActivity.");
            return;
        }
        ((DefnActivity) activity).toggleSearchBox();
    }
}
