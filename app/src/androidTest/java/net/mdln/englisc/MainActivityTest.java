package net.mdln.englisc;

import android.content.Context;
import android.widget.EditText;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.mdln.englisc.SpannableClicker.clickClickableSpan;

public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    /**
     * Check that you can type text and click on a result to get a dialog. This just tests UI
     * elements. See {@link DictTest} for a test that checks whether reasonable results are found.
     */
    @Test
    public void searchForAWord() {
        // SearchView has an opaque hierarchy, but there's only one EditText in it, so type
        // "forthmesto" into it. This term is only notable for having a single result, which
        // contains a clickable abbreviation.
        onView(isAssignableFrom(EditText.class)).perform(typeText("forthmesto"), closeSoftKeyboard());
        // Click on the one search result.
        onView(withId(R.id.results_row)).perform(click());
        // In the definition of "forthmesto", click on the abbreviation, "Mt."
        onView(withId(R.id.defn_content)).perform(clickClickableSpan("Mt."));
        // Check that the screen for "Mt." comes up.
        onView(withId(R.id.defn_content)).check(matches(withSubstring("Gospel of St. Matthew")));
        // If it worked, there should be a DefnActivity with "up" navigation. Click it.
        onView(withContentDescription("Navigate up")).perform(click());
    }

    @Test
    public void aboutMenuItem() {
        // Open the action bar menu, open the "About" box, and then close it.
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText("About")).perform(click());
        onView(withText("Close")).inRoot(isDialog()).perform(click());
    }
}
