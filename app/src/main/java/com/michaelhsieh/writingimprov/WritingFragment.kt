package com.michaelhsieh.writingimprov

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import es.dmoral.toasty.Toasty
import timber.log.Timber

/**
 * Load random image and start countdown timer if loaded successfully.
 *
 */
private const val KEY_MILLIS_LEFT:String = "millisLeft"
private const val KEY_IMAGE_URL:String = "imageUrl"

class WritingFragment:Fragment(R.layout.fragment_writing) {

    // countdown start time
    private var startTimeInMillis:Long = 0
    // time left in the countdown
    private var timeLeftInMillis:Long = startTimeInMillis
    // end time in milliseconds = current time in milliseconds + time left in countdown
    private var endTime:Long = 0

    private lateinit var timerText: TextView
    private lateinit var countDownTimer: CountDownTimer

    private lateinit var imageUrl:String

    private val args: WritingFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the prompt
        val promptText = view.findViewById<TextView>(R.id.tv_writing_prompt)
        promptText.text = args.prompt

//        Timber.d("onViewCreated, promptText initialized")

        // Create progress bar so user knows image is loading
        val progressBar = view.findViewById<ProgressBar>(R.id.pb_loading_image)
        progressBar.visibility = View.VISIBLE

        // Set minutes of timer
        val minutes = args.minutes
        startTimeInMillis = minutes.toLong() * 60 * 1000
        timeLeftInMillis = startTimeInMillis

        timerText = view.findViewById(R.id.tv_timer)

        // Load the random image
        if (savedInstanceState != null) {
            val savedUrl = savedInstanceState.getString(KEY_IMAGE_URL)
            if (savedUrl != null) {
                imageUrl = savedUrl
            }
        } else {
            imageUrl = getRandomImageUrl()
        }

        // If savedInstanceState is set,
        // set time left to saved milliseconds
        if (savedInstanceState != null) {
            timeLeftInMillis = savedInstanceState.getLong(KEY_MILLIS_LEFT)
        }

        val image = view.findViewById<ImageView>(R.id.iv_image)
        Picasso.get().load(imageUrl)
            .error(R.drawable.ic_error_outline_72)
            .into(image, object : Callback {
                override fun onSuccess() {
                    //  hide progress bar
                    progressBar.visibility = View.GONE
                }

                override fun onError(e: Exception?) {
                    // display error message
                    Toasty.error(this@WritingFragment.requireContext(), R.string.error_loading_image, Toast.LENGTH_LONG,true).show()
                    Timber.e(e)
                    //  hide progress bar
                    progressBar.visibility = View.GONE
                }

            })

        val submitButton = view.findViewById<Button>(R.id.btn_submit)
        submitButton.setOnClickListener {
            // stop timer
            countDownTimer.cancel()
            val action = WritingFragmentDirections.actionWritingFragmentToCompletedOnTimeFragment()
            findNavController().navigate(action)
        }
    }

    /**
     * Create a new countdown timer and start it.
     */
    private fun startTimer() {

        endTime = System.currentTimeMillis() + timeLeftInMillis

        countDownTimer = object: CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateCountDownText()
            }

            override fun onFinish() {
                val action = WritingFragmentDirections.actionWritingFragmentToOutOfTimeFragment()
                findNavController().navigate(action)
            }
        }.start()

    }

    /**
     * Update timer TextView.
     */
    private fun updateCountDownText() {
        val minutes:Int = (timeLeftInMillis.toInt() / 1000) / 60
        val seconds:Int = (timeLeftInMillis.toInt() / 1000) % 60

        val timeLeftFormatted:String = String.format("%02d:%02d", minutes, seconds)

        timerText.text = timeLeftFormatted

        // warn user when time is almost out

        // time to display first info toast
        val secsFirstToast = 30
        // time to display second info toast and change text to red
        val secsSecondToast = 10
        if (minutes == 0 && seconds == secsFirstToast) {
            Toasty.info(this@WritingFragment.requireContext(), getString(R.string.time_left_secs, seconds), Toast.LENGTH_LONG, true).show()
        } else if (minutes == 0 && seconds == secsSecondToast) {
            Toasty.info(this@WritingFragment.requireContext(), getString(R.string.time_left_secs, seconds), Toast.LENGTH_LONG, true).show()
        }

        // change countdown text to red
        if (minutes == 0 && seconds <= secsSecondToast) {
            timerText.setTextColor(ContextCompat.getColor(this@WritingFragment.requireContext(), R.color.errorColor))
        }
    }

    /** Returns a random image URL. */
    private fun getRandomImageUrl():String {
        val imageUrls = arrayOf(
            "https://images.unsplash.com/photo-1617721042477-7c5c498e7dbf?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&h=800&ixlib=rb-1.2.1&q=80&w=800",
            "https://images.unsplash.com/photo-1617386564901-be7cfcaa4c60?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&h=800&ixlib=rb-1.2.1&q=80&w=800",
            "https://images.unsplash.com/photo-1618085579752-d666c8ad12b6?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&h=800&ixlib=rb-1.2.1&q=80&w=800",
            "https://images.unsplash.com/photo-1619440482145-3133e2abb77d?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&h=800&ixlib=rb-1.2.1&q=80&w=800",
            "https://images.unsplash.com/photo-1618053448492-2b629c2c912c?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&h=800&ixlib=rb-1.2.1&q=80&w=800"
        )
        // generated random number from 0 to last index included
        val randNum = (imageUrls.indices).random()
        return imageUrls[randNum]
    }

    /**
     * Create and start a new countdown timer
     */
    override fun onStart() {
        super.onStart()

        startTimer()
    }

    /**
     * Cancel existing timer
     */
    override fun onStop() {
        super.onStop()

        countDownTimer.cancel()
    }

    /**
     * Keep time the same after configuration change, example on rotation,
     * by saving time variables.
     * The current timer is canceled in onStop() and
     * variables will be used by a new CountDownTimer in onRestoreInstanceState().
     *
     * Also save image URL.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_MILLIS_LEFT, timeLeftInMillis)

        outState.putString(KEY_IMAGE_URL, imageUrl)
    }
}