package net.mdln.englisc;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.MailTo;
import android.widget.EditText;

import androidx.test.espresso.intent.Intents;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Objects;

public class FeedbackTest {

    @SuppressWarnings("deprecation")
    @Rule
    public androidx.test.espresso.intent.rule.IntentsTestRule<MainActivity> intentsTestRule = new androidx.test.espresso.intent.rule.IntentsTestRule<>(MainActivity.class, true, true);

    // Based on https://github.com/android/testing-samples/blob/master/ui/espresso/IntentsBasicSample/app/src/sharedTest/java/com/example/android/testing/espresso/IntentsBasicSample/DialerActivityTest.java.
    @Before
    public void blockExternalIntents() {
        intending(not(isInternal())).respondWith(
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    }

    @Test
    public void feedbackMenuItem() {
        final String typedText = "wordes";
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        onView(isAssignableFrom(EditText.class)).perform(typeText(typedText), closeSoftKeyboard());
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText("Feedback")).perform(click());
        assertEquals(1, Intents.getIntents().size());
        Intent intent = Intents.getIntents().get(0);
        assertEquals(Intent.ACTION_SENDTO, intent.getAction());
        MailTo mailto = MailTo.parse(Objects.requireNonNull(intent.getData()).toString());
        assertEquals(ctx.getString(R.string.feedback_email), mailto.getTo());
        assertThat(mailto.getBody(), containsString(typedText));
    }
}