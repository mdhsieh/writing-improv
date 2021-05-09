package com.michaelhsieh.writingimprov

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test

class HomeFragmentTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun test_isHomeFragmentInView() {

        Espresso.onView(ViewMatchers.withId(R.id.home))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun test_visibility_practiceButton() {

        Espresso.onView(ViewMatchers.withId(R.id.btn_practice))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun test_isPracticeButtonTextDisplayed() {

        Espresso.onView(ViewMatchers.withId(R.id.btn_practice))
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.practice)))
    }

    /**
     * Go to PromptFragment when practice button clicked
     */
    @Test
    fun test_navPromptFragment() {

        Espresso.onView(ViewMatchers.withId(R.id.btn_practice)).perform(ViewActions.click())

        // parent ScrollView of PromptFragment
        Espresso.onView(ViewMatchers.withId(R.id.scroll_view_prompt))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    /**
     * Go to PromptFragment, then go back to HomeFragment
     */
    @Test
    fun test_backPress_toHomeFragment() {

        Espresso.onView(ViewMatchers.withId(R.id.btn_practice)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.scroll_view_prompt))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.pressBack()

        Espresso.onView(ViewMatchers.withId(R.id.home))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    }
}