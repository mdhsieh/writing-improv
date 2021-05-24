package com.michaelhsieh.writingimprov

import android.content.pm.ActivityInfo
import android.view.View
import android.widget.TextView
import androidx.test.espresso.*
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class WritingFragmentTest {
    @get:Rule
    val activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)

    /**
     * Go to WritingFragment before running any tests.
     */
    @Before
    fun navWritingFragment() {
        Espresso.onView(ViewMatchers.withId(R.id.btn_practice)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.btn_go))
            .perform(ViewActions.scrollTo())
            .perform(ViewActions.click())

    }

    @Test
    fun test_isWritingFragmentInView() {

        Espresso.onView(ViewMatchers.withId(R.id.scroll_view_writing))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    /**
     * Check random prompt text is visible.
     */
    @Test
    fun test_visibility_promptText() {

        Espresso.onView(ViewMatchers.withId(R.id.tv_writing_prompt))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    /**
     * Check random prompt text is unchanged after
     * device rotation to landscape.
     */
    @Test
    fun test_isSameAfterRotation_landscape_promptText() {

        Espresso.onView(ViewMatchers.withId(R.id.tv_writing_prompt))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // get current prompt text
        val promptTextResult: ViewInteraction = Espresso.onView(ViewMatchers.withId(R.id.tv_writing_prompt))
        val promptText = getText(promptTextResult)

        // rotate to landscape
        activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        // check if same
        Espresso.onView(ViewMatchers.withId(R.id.tv_writing_prompt))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.withText(promptText)))
    }

    /**
     * Check random prompt text is unchanged after
     * device rotation to portrait.
     */
    @Test
    fun test_isSameAfterRotation_portrait_promptText() {

        Espresso.onView(ViewMatchers.withId(R.id.tv_writing_prompt))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        val promptTextResult: ViewInteraction = Espresso.onView(ViewMatchers.withId(R.id.tv_writing_prompt))
        val promptText = getText(promptTextResult)

        // rotate to portrait
        activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        Espresso.onView(ViewMatchers.withId(R.id.tv_writing_prompt))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.withText(promptText)))

    }

    @Test
    fun test_visibility_submitButton() {

        Espresso.onView(ViewMatchers.withId(R.id.btn_submit))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun test_isSubmitButtonTextDisplayed() {

        Espresso.onView(ViewMatchers.withId(R.id.btn_submit))
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.submit)))
    }

    /**
     * Go to CompletedOnTimeFragment when submit button clicked
     */
    @Test
    fun test_navCompletedOnTimeActivity() {

        val someText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."

        Espresso.onView(ViewMatchers.withId(R.id.et_writing))
            .perform(ViewActions.scrollTo())
            .perform(clearText(), typeText(someText))

        Espresso.onView(ViewMatchers.withId(R.id.btn_submit))
            .perform(ViewActions.scrollTo())
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.completed_on_time))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    /**
     * Go to CompletedOnTimeFragment, then go back to WritingFragment
     */
    @Test
    fun test_backPress_toWritingFragment() {

        Espresso.onView(ViewMatchers.withId(R.id.btn_submit))
            .perform(ViewActions.scrollTo())
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.completed_on_time))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.pressBack()

        Espresso.onView(ViewMatchers.withId(R.id.scroll_view_writing))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    }

    /**
     * Get the text of a TextView.
     * Used to check if a TextView value is the same after rotation.
     */
    private fun getText(matcher: ViewInteraction): String {
        var text = String()
        matcher.perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(TextView::class.java)
            }

            override fun getDescription(): String {
                return "Text of the view"
            }

            override fun perform(uiController: UiController, view: View) {
                val tv = view as TextView
                text = tv.text.toString()
            }
        })

        return text
    }
}