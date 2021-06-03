package com.michaelhsieh.writingimprov

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
import java.util.*


/**
 * Load random image and start countdown timer if loaded successfully.
 * User writes and submits their writing here.
 *
 * References:
 * https://codinginflow.com/tutorials/android/countdowntimer/part-1-countdown-timer
 * https://www.youtube.com/watch?v=LMYQS1dqfo8
 * https://www.youtube.com/watch?v=lvibl8YJfGo
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

    private var imageUrl:String = ""
    // show image to user and loading progress
    private lateinit var imageView:ImageView
    private lateinit var progressBar:ProgressBar

    // Smaller image URL which is used later by MyWritingFragment RecyclerView
    private var thumbnailImageUrl:String = ""

    // Show error if image URL can't load
    private lateinit var errorText:TextView

    // EditText where user writes
    private lateinit var writeEditText: EditText

    private val args: WritingFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the prompt
        val promptText = view.findViewById<TextView>(R.id.tv_writing_prompt)
        promptText.text = args.prompt

        // Set minutes of timer
        val minutes = args.minutes
        startTimeInMillis = minutes.toLong() * 60 * 1000
        timeLeftInMillis = startTimeInMillis

        timerText = view.findViewById(R.id.tv_timer)

        // ImageView which will have random image
        imageView = view.findViewById(R.id.iv_image)

        // Create progress bar and show so user knows image is loading
        progressBar = view.findViewById(R.id.pb_loading_image)
        progressBar.visibility = View.VISIBLE

        // Hide error text
        errorText = view.findViewById(R.id.tv_error)
        errorText.visibility = View.GONE

        // EditText
        writeEditText = view.findViewById(R.id.et_writing)

        // Load the random image, or use saved URL after configuration change,
        // example device rotated
        if (savedInstanceState != null) {
            val savedUrl = savedInstanceState.getString(KEY_IMAGE_URL)
            if (savedUrl != null) {
                // configuration change, load the image with the saved URL
                imageUrl = savedUrl
            }
        } else {
            // Set the URL from previous Fragment
            imageUrl = args.url
        }
        loadImage(imageUrl)

        // Set thumbnail URL from previous Fragment
        thumbnailImageUrl = args.thumbUrl

        // If savedInstanceState is set,
        // set time left to saved milliseconds
        if (savedInstanceState != null) {
            timeLeftInMillis = savedInstanceState.getLong(KEY_MILLIS_LEFT)
        }

        val submitButton = view.findViewById<Button>(R.id.btn_submit)
        submitButton.setOnClickListener {
            // stop timer
            countDownTimer.cancel()

            submitWriting(true)
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
                submitWriting(false)
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

    /**
     * Display the image at a URL and
     * remove progress bar if image was loaded successfully.
     * Otherwise, display a Toast error message and remove progress bar.
     * @param url The url of the image
     */
    private fun loadImage(url:String) {
        Picasso.get().load(url)
            .error(R.drawable.ic_error_outline_72)
            .into(imageView, object : Callback {
                override fun onSuccess() {
                    //  hide progress bar
                    progressBar.visibility = View.GONE
                    Timber.d("finished getting image")
                }

                override fun onError(e: Exception?) {
                    // display error message
                    Toasty.error(this@WritingFragment.requireContext(), R.string.error_loading_image, Toast.LENGTH_LONG,true).show()
                    Timber.e(e)
                    //  hide progress bar
                    progressBar.visibility = View.GONE

                    // show error text
                    errorText.visibility = View.VISIBLE
                }

            })
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

    /**
     * Create new WritingItem with all text and URL, then
     * go to next Fragment.
     * @param isOnTime Whether the writing was submitted on time
     */
    private fun submitWriting(isOnTime:Boolean) {
        // Create new WritingItem with all text and URL
        val item = WritingItem(UUID.randomUUID().toString(), "Practice", prompt = args.prompt, time = args.minutes.toString(), url = imageUrl, thumbUrl = thumbnailImageUrl, writing = writeEditText.text.toString())

        Timber.d("Passing: %s", item.toString())

        val action = WritingFragmentDirections.actionWritingFragmentToMyWritingFragment(
            isCompletedOnTime = isOnTime,
            writingItem = item
        )
        findNavController().navigate(action)
    }
}