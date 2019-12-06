package net.mdln.englisc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.MenuItem;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
                HtmlDialog.create(activity, activity.getString(R.string.long_app_name), readUtf8Resource(R.raw.info));
                return true;
            case R.id.main_menu_feedback:
                sendFeedback(contextString);
                return true;
            default:
                return false;
        }
    }

    private void sendFeedback(String contextString) {
        Uri uri = Uri.parse("mailto:" + activity.getString(R.string.feedback_email));
        String body = activity.getString(R.string.type_feedback_here) + "\n\n" +
                "Hardware: " + Build.BRAND + " / " + Build.MODEL + "\n" +
                "Android version: " + Build.VERSION.RELEASE + "\n" +
                "App version: " + BuildConfig.VERSION_NAME + "\n" +
                contextString;
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
        String subject = String.format(activity.getString(R.string.feedback_subject), activity.getString(R.string.long_app_name));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        activity.startActivity(Intent.createChooser(emailIntent, "Send feedback email..."));
    }

    private String readUtf8Resource(int id) {
        try (InputStream stream = activity.getResources().openRawResource(id)) {
            return new String(Streams.toByteArray(stream), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("can't load raw resource " + id, e);
        }
    }

}
