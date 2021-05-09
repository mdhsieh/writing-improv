package com.michaelhsieh.writingimprov

import android.content.pm.ActivityInfo
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * References:
 * https://stackoverflow.com/questions/45597008/espresso-get-text-of-element
 */

private const val TAG:String = "PromptFragmentTest"

@RunWith(AndroidJUnit4ClassRunner::class)
class PromptFragmentTest {

    @get:Rule
    val activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)

    /**
     * Go to PromptFragment before running any tests.
     */
    @Before
    fun navPromptFragment() {
        Espresso.onView(ViewMatchers.withId(R.id.btn_practice)).perform(ViewActions.click())
    }

    @Test
    fun test_isPromptActivityInView() {

        Espresso.onView(ViewMatchers.withId(R.id.scroll_view_prompt))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    /**
     * Check question mark image, random time text, and random prompt text are visible.
     */
    @Test
    fun test_visibility_image_timeText_promptText() {

        Espresso.onView(ViewMatchers.withId(R.id.iv_image))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withId(R.id.tv_time))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withId(R.id.tv_prompt))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    /**
     * Check random time text and prompt text are unchanged after
     * device rotation to landscape.
     */
    @Test
    fun test_isSameAfterRotation_landscape_timeText_promptText() {

        Espresso.onView(ViewMatchers.withId(R.id.tv_time))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // get current time text
        val timeTextResult: ViewInteraction = Espresso.onView(ViewMatchers.withId(R.id.tv_time))
        val timeText = getText(timeTextResult)

        // get current prompt text
        val promptTextResult: ViewInteraction = Espresso.onView(ViewMatchers.withId(R.id.tv_prompt))
        val promptText = getText(promptTextResult)

        // rotate to landscape
        activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        // check if same
        Espresso.onView(ViewMatchers.withId(R.id.tv_time))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.withText(timeText)))

        Espresso.onView(ViewMatchers.withId(R.id.tv_prompt))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.withText(promptText)))
    }

    /**
     * Check random time text and prompt text are unchanged after
     * device rotation to portrait.
     */
    @Test
    fun test_isSameAfterRotation_portrait_timeText() {

        Espresso.onView(ViewMatchers.withId(R.id.tv_time))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        val timeTextResult: ViewInteraction = Espresso.onView(ViewMatchers.withId(R.id.tv_time))
        val timeText = getText(timeTextResult)

        val promptTextResult: ViewInteraction = Espresso.onView(ViewMatchers.withId(R.id.tv_prompt))
        val promptText = getText(promptTextResult)

        // rotate to portrait
        activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        Espresso.onView(ViewMatchers.withId(R.id.tv_time))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.withText(timeText)))

        Espresso.onView(ViewMatchers.withId(R.id.tv_prompt))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.withText(promptText)))

    }

    @Test
    fun test_visibility_goButton() {

        Espresso.onView(ViewMatchers.withId(R.id.btn_go))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun test_isGoButtonTextDisplayed() {

        Espresso.onView(ViewMatchers.withId(R.id.btn_go))
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.go)))
    }

    /**
     * Go to WritingActivity when go button clicked
     */
    @Test
    fun test_navWritingActivity() {

        Espresso.onView(ViewMatchers.withId(R.id.btn_go))
            .perform(ViewActions.scrollTo())
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.scroll_view_writing))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    /**
     * Go to WritingActivity, then go back to PromptFragment
     */
    @Test
    fun test_backPress_toHomeFragment() {

        Espresso.onView(ViewMatchers.withId(R.id.btn_go))
            .perform(ViewActions.scrollTo())
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.scroll_view_writing))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // extra back to close keyboard
        Espresso.pressBack()

        Espresso.pressBack()

        Espresso.onView(ViewMatchers.withId(R.id.scroll_view_prompt))
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
                return isAssignableFrom(TextView::class.java)
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