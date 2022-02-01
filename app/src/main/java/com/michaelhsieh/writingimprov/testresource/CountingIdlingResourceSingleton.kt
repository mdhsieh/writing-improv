package com.michaelhsieh.writingimprov.testresource

import androidx.test.espresso.idling.CountingIdlingResource

/** Class which will provide CountingIdlingResource to the rest of the app.
 *
 */
object CountingIdlingResourceSingleton {

    private const val RESOURCE = "GLOBAL"

    @JvmField val countingIdlingResource = CountingIdlingResource(RESOURCE)

    fun increment() {
        countingIdlingResource.increment()
    }

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }
}