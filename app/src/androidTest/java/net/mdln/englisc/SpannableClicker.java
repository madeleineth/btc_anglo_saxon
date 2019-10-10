package net.mdln.englisc;

import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * I (Madeleine) was unable to get {@link androidx.test.espresso.matcher.ViewMatchers#openLinkWithText}
 * working on {@link DefnActivity} due to an exception saying its TextView didn't have links. So,
 * work around it with {@link #clickClickableSpan}.
 * <p>
 * Based on https://stackoverflow.com/a/40524247 by Lavekush Agrawal.
 */
final class SpannableClicker implements ViewAction {

    private final String textToClick;

    private SpannableClicker(String textToClick) {
        this.textToClick = textToClick;
    }

    static ViewAction clickClickableSpan(String textToClick) {
        return new SpannableClicker(textToClick);
    }

    @Override
    public Matcher<View> getConstraints() {
        return Matchers.instanceOf(TextView.class);
    }

    @Override
    public String getDescription() {
        return "clicking on a ClickableSpan";
    }

    @Override
    public void perform(UiController uiController, View view) {
        TextView textView = (TextView) view;
        SpannableString spannableString = (SpannableString) textView.getText();
        if (spannableString.length() == 0) {
            // TextView is empty, nothing to do
            throw new NoMatchingViewException.Builder()
                    .includeViewHierarchy(true)
                    .withRootView(textView)
                    .build();
        }
        // Get the links inside the TextView and check if we find textToClick
        ClickableSpan[] spans = spannableString.getSpans(0, spannableString.length(), ClickableSpan.class);
        if (spans.length > 0) {
            ClickableSpan spanCandidate;
            for (ClickableSpan span : spans) {
                spanCandidate = span;
                int start = spannableString.getSpanStart(spanCandidate);
                int end = spannableString.getSpanEnd(spanCandidate);
                CharSequence sequence = spannableString.subSequence(start, end);
                if (textToClick.equals(sequence.toString())) {
                    span.onClick(textView);
                    return;
                }
            }
        }
        // textToClick not found in TextView
        throw new NoMatchingViewException.Builder()
                .includeViewHierarchy(true)
                .withRootView(textView)
                .build();
    }
}
