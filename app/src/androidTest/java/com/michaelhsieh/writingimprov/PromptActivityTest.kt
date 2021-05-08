package com.michaelhsieh.writingimprov

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test

class PromptActivityTest {

    @get:Rule
    var activityScenario: ActivityScenarioRule<PromptActivity>
            = ActivityScenarioRule(PromptActivity::class.java)

    @Test
    fun test_isPromptActivityInView() {

        Espresso.onView(ViewMatchers.withId(R.id.scroll_view_prompt))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun test_visibility_image() {

        Espresso.onView(ViewMatchers.withId(R.id.iv_image))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun test_visibility_timeText() {

        Espresso.onView(ViewMatchers.withId(R.id.tv_time))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun test_visibility_promptText() {

        Espresso.onView(ViewMatchers.withId(R.id.tv_prompt))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
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

    // go to PromptActivity when practice button clicked
//    @Test
//    fun test_navPromptActivity() {
//
//        Espresso.onView(ViewMatchers.withId(R.id.btn_practice)).perform(ViewActions.click())
//
//        Espresso.onView(ViewMatchers.withId(R.id.scroll_view_prompt))
//            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
//    }

    // go to PromptActivity, then go back to HomeFragment
//    @Test
//    fun test_backPress_toHomeFragment() {
//
//        Espresso.onView(ViewMatchers.withId(R.id.btn_practice)).perform(ViewActions.click())
//
//        Espresso.onView(ViewMatchers.withId(R.id.scroll_view_prompt))
//            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
//
//        Espresso.pressBack()
//
//        Espresso.onView(ViewMatchers.withId(R.id.home))
//            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
//
//    }
}