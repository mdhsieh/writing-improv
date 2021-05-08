package com.michaelhsieh.writingimprov

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class MainActivityTest {

    @get:Rule
    var activityScenario: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun test_isActivityInView() {

        onView(withId(R.id.main)).check(matches(isDisplayed()))
    }

    @Test
    fun test_visibility_practiceButton() {

        onView(withId(R.id.btn_practice))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_isPracticeButtonTextDisplayed() {

        onView(withId(R.id.btn_practice))
            .check(matches(withText(R.string.practice)))
    }

    // go to PromptActivity when practice button clicked
    @Test
    fun test_navPromptActivity() {

        onView(withId(R.id.btn_practice)).perform(click())

        onView(withId(R.id.scroll_view_prompt)).check(matches(isDisplayed()))
    }
}