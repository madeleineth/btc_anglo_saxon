package net.mdln.englisc;

import android.content.Context;
import android.content.Intent;
import android.net.MailTo;
import android.widget.EditText;

import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;

import java.util.Objects;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FeedbackTest {

    @Rule
    public IntentsTestRule<MainActivity> intentsTestRule = new IntentsTestRule<>(MainActivity.class, true, true);

    @Test
    public void feedbackMenuItem() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        onView(isAssignableFrom(EditText.class)).perform(typeText("words"), closeSoftKeyboard());
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText("Feedback")).perform(click());
        assertEquals(1, Intents.getIntents().size());
        Intent intent = Intents.getIntents().get(0);
        assertEquals(Intent.ACTION_SENDTO, intent.getAction());
        MailTo mailto = MailTo.parse(Objects.requireNonNull(intent.getData()).toString());
        assertEquals(ctx.getString(R.string.feedback_email), mailto.getTo());
        assertThat(mailto.getBody(), containsString("words"));
    }
}