package net.mdln.englisc;

import android.content.Context;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.widget.Toolbar;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

public class MainActivityTest {

    // TODO: Migrate to https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, true);

    /**
     * Check that you can type text and click on a result to get a dialog. This just tests UI
     * elements. See {@link DictTest} for a test that checks whether reasonable results are found.
     */
    @Test
    public void searchForAWord() {
        IdlingRegistry.getInstance().register(new SearchPendingIdlingResource(activityRule.getActivity()));
        // SearchView has an opaque hierarchy, but there's only one EditText in it, so type
        // "forthmesto" into it. This term is only notable for having a single result, which
        // contains a clickable abbreviation.
        onView(isAssignableFrom(EditText.class)).perform(typeText("forthmesto"), closeSoftKeyboard());
        // Check that the search result has the right text and click on it.
        onView(new RecyclerViewMatcher(R.id.search_results).atPosition(0))
                .check(matches(withText(containsString("forþmest"))))
                .perform(click());
        // In the definition of "forthmesto", click on the abbreviation, "Mt."
        onWebView().withElement(findElement(Locator.LINK_TEXT, "Mt.")).perform(webClick());
        // Check that the screen for "Mt." comes up.
        onWebView().withElement(findElement(Locator.TAG_NAME, "body")).check(
                webMatches(getText(), containsString("Gospel of St. Matthew")));
        // If it worked, there should be a DefnActivity with "up" navigation. Click it.
        onView(allOf(
                isAssignableFrom(ImageButton.class),
                withParent(isAssignableFrom(Toolbar.class)))).perform(click());
        // The last item we viewed was "Mt." Clear the search box so that we see history and check
        // that it's the first item in the history list.
        onView(isAssignableFrom(EditText.class)).perform(clearText());
        onView(new RecyclerViewMatcher(R.id.search_results).atPosition(0)).check(matches(withText(containsString("Mt."))));
    }

    @Test
    public void aboutMenuItem() {
        // Open the action bar menu, open the "About" box, and then close it.
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        openActionBarOverflowOrOptionsMenu(ctx);
        onView(withText("About")).perform(click());
        onWebView().withElement(findElement(Locator.TAG_NAME, "body")).check(
                webMatches(getText(), containsString("Joseph Bosworth")));
    }
}
