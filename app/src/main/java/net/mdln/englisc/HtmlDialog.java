package net.mdln.englisc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.webkit.WebView;

final class HtmlDialog {
    /**
     * Create a dialog box with the specified title and body HTML and a single "close" button.
     */
    static void create(Activity parentActivity, String title, String bodyHtml) {
        LayoutInflater factory = LayoutInflater.from(parentActivity);
        // root == null is correct here; see https://wundermanthompsonmobile.com/2013/05/layout-inflation-as-intended/.
        @SuppressLint("InflateParams") WebView view = (WebView) factory.inflate(R.layout.dialog_html_text, null);
        WebViewStyle.apply(parentActivity, view, bodyHtml);
        AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
        builder.setTitle(title);
        builder.setView(view);
        builder.setNeutralButton(R.string.close_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
}
