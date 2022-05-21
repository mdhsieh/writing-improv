package com.michaelhsieh.writingimprov


import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
//import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
//import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.michaelhsieh.writingimprov.testresource.CountingIdlingResourceSingleton
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sign in, go to practice, submit practice writing, go back to main menu, and then sign out.
 *
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivitySignInPracticeTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule(MainActivity::class.java)

    // access string resources
    private var resources: Resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

    /**
     * Register the idling resource with the espresso test runner,
     * so it can observe the count variable and wait accordingly during execution of the test.
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(CountingIdlingResourceSingleton.countingIdlingResource)
    }

    /**
     * Unregister after test finishes.
     *
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(CountingIdlingResourceSingleton.countingIdlingResource)
    }

    @Test
    fun mainActivitySignInPracticeTest2() {
        val appCompatButton = onView(
            allOf(
                withId(R.id.btn_sign_in), withText("Sign In"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.sign_in),
                        0
                    ),
                    2
                )
            )
        )

        appCompatButton.perform(scrollTo(), click())

        val textInputEditText = onView(
            allOf(
                withId(R.id.email),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.email_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText.perform(
            scrollTo(),
            replaceText(resources.getString(R.string.test_username)),
            closeSoftKeyboard()
        )

        val appCompatButton2 = onView(
            allOf(
                withId(R.id.button_next), withText("Next"),
                childAtPosition(
                    allOf(
                        withId(R.id.email_top_layout),
                        childAtPosition(
                            withClassName(`is`("android.widget.ScrollView")),
                            0
                        )
                    ),
                    2
                )
            )
        )
        appCompatButton2.perform(scrollTo(), click())

        val textInputEditText2 = onView(
            allOf(
                withId(R.id.password),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.password_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText2.perform(scrollTo(), replaceText(resources.getString(R.string.test_password)), closeSoftKeyboard())

        val appCompatButton3 = onView(
            allOf(
                withId(R.id.button_done), withText("Sign in"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    4
                )
            )
        )
        appCompatButton3.perform(scrollTo(), click())

        val appCompatButton4 = onView(
            allOf(
                withId(R.id.btn_practice), withText("Practice"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.home),
                        0
                    ),
                    0
                )
            )
        )
        appCompatButton4.perform(scrollTo(), click())

        // Wait until the go button is visible


        val appCompatButton5 = onView(
            allOf(
                withId(R.id.btn_go), withText("Go"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.scroll_view_prompt),
                        0
                    ),
                    4
                )
            )
        )
        appCompatButton5.perform(scrollTo(), click())

        val appCompatEditText = onView(
            allOf(
                withId(R.id.et_writing),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.scroll_view_writing),
                        0
                    ),
                    5
                )
            )
        )
        appCompatEditText.perform(
            scrollTo(),
            replaceText("testing beep boop 123. this not a very very long text"),
            closeSoftKeyboard()
        )

        val appCompatButton6 = onView(
            allOf(
                withId(R.id.btn_submit), withText("Submit"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.scroll_view_writing),
                        0
                    ),
                    6
                )
            )
        )
        appCompatButton6.perform(scrollTo(), click())

        /*
        val recyclerView = onView(
            allOf(
                withId(R.id.rv_my_writing),
                childAtPosition(
                    withId(R.id.my_writing),
                    1
                )
            )
        )
        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(2, click()))


        val appCompatImageButton = onView(
            allOf(
                withContentDescription("Navigate up"),
                childAtPosition(
                    allOf(
                        withId(R.id.toolbar),
                        childAtPosition(
                            withId(R.id.main),
                            0
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatImageButton.perform(click())
        */

        val appCompatImageButton2 = onView(
            allOf(
                withContentDescription("Navigate up"),
                childAtPosition(
                    allOf(
                        withId(R.id.toolbar),
                        childAtPosition(
                            withId(R.id.main),
                            0
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatImageButton2.perform(click())

        val appCompatImageButton3 = onView(
            allOf(
                withContentDescription("Navigate up"),
                childAtPosition(
                    allOf(
                        withId(R.id.toolbar),
                        childAtPosition(
                            withId(R.id.main),
                            0
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatImageButton3.perform(click())

        val appCompatButton7 = onView(
            allOf(
                withId(R.id.btn_logout), withText("Sign Out"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.home),
                        0
                    ),
                    7
                )
            )
        )
        appCompatButton7.perform(scrollTo(), click())

    }


    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
