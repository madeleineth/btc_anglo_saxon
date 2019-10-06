package net.mdln.englisc;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;

final class HtmlDialog {
    /**
     * Create a dialog box with the specified title and body HTML and a single "close" button. We
     * have to use a custom layout because there's no better way to enable the
     * {@code android:textIsSelectable} property.
     */
    static void create(Context ctx, String title, String bodyHtml) {
        LayoutInflater factory = LayoutInflater.from(ctx);
        // root == null is correct here; see https://wundermanthompsonmobile.com/2013/05/layout-inflation-as-intended/.
        @SuppressLint("InflateParams") TextView contentView = (TextView) factory.inflate(R.layout.dialog_html_text, null);
        contentView.setText(HtmlCompat.fromHtml(bodyHtml, HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_HEADING));
        // Make HTML links clickable.
        contentView.setMovementMethod(LinkMovementMethod.getInstance());
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(title);
        builder.setView(contentView);
        builder.setNeutralButton(R.string.close_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
}
