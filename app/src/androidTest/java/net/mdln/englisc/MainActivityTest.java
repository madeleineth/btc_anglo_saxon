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
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    /**
     * Check that you can type text and click on a result to get a dialog. This just tests UI
     * elements. See {@link DictTest} for a test that checks whether reasonable results are found.
     */
    @Test
    public void searchForAWord() {
        // SearchView has an opaque hierarchy, but there's only one EditText in it, so type "rimere"
        // into it. This term is only notable for having a single result.
        onView(isAssignableFrom(EditText.class)).perform(typeText("rimere"), closeSoftKeyboard());
        // Click on the one search result.
        onView(withId(R.id.results_row)).perform(click());
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
