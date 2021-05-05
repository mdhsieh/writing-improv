package com.michaelhsieh.writingimprov

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.security.ProviderInstaller
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

/**
 * Load random image and start countdown timer if loaded successfully
 *
 * References:
 * https://source.unsplash.com/
 * https://codinginflow.com/tutorials/android/countdowntimer/part-1-countdown-timer
 * https://www.youtube.com/watch?v=LMYQS1dqfo8
 * https://www.youtube.com/watch?v=lvibl8YJfGo
 * https://stackoverflow.com/questions/29916962/javax-net-ssl-sslhandshakeexception-javax-net-ssl-sslprotocolexception-ssl-han
 */
private const val TAG:String = "WritingActivity"
private const val KEY_MILLIS_LEFT:String = "millisLeft"
private const val KEY_END_TIME:String = "endTime"
//private const val KEY_TIMER_RUNNING:String = "timerRunning"
private const val KEY_IMAGE_URL:String = "imageUrl"
//private const val PREFS:String = "prefs"

class WritingActivity : AppCompatActivity() {

//    private var timerRunning:Boolean = false

    private var startTimeInMillis:Long = 0
    private var timeLeftInMillis:Long = startTimeInMillis
    private var endTime:Long = 0

    private lateinit var timerText: TextView
    private lateinit var countDownTimer: CountDownTimer

    private lateinit var imageUrl:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing)

        updateAndroidSecurityProvider(this)

        // Set the prompt
        val promptText = findViewById<TextView>(R.id.tv_prompt)
        promptText.text = intent.getStringExtra(PromptActivity.KEY_PROMPT)

        // Create progress bar so user knows image is loading
        val progressBar = findViewById<ProgressBar>(R.id.pb_loading_image)
        progressBar.visibility = View.VISIBLE

        // Set minutes of timer
        val minutes = intent.getIntExtra(PromptActivity.KEY_MINUTES, 0)
        startTimeInMillis = minutes.toLong() * 60 * 1000
        timeLeftInMillis = startTimeInMillis

        timerText = findViewById(R.id.tv_timer)

        // Load the random image
        if (savedInstanceState != null) {
            val savedUrl = savedInstanceState.getString(KEY_IMAGE_URL)
            if (savedUrl != null) {
                imageUrl = savedUrl
            }
        } else {
            imageUrl = getRandomImageUrl()
        }

        val image = findViewById<ImageView>(R.id.iv_image)
        Picasso.get().load(imageUrl)
                        .error(R.drawable.ic_error_outline_72)
                        .into(image, object : Callback {
                            override fun onSuccess() {
                                if (savedInstanceState == null) {
                                    // successfully loaded, start countdown timer
                                    startTimer()
                                }
                                //  hide progress bar
                                progressBar.visibility = View.GONE
                            }

                            override fun onError(e: Exception?) {
                                // display error message
                                Toast.makeText(this@WritingActivity, R.string.error_loading_image, Toast.LENGTH_LONG).show()
                                Log.e(TAG, "error loading image", e)
                                //  hide progress bar
                                progressBar.visibility = View.GONE
                            }

                        })

        val submitButton = findViewById<Button>(R.id.btn_submit)
        submitButton.setOnClickListener {
            // stop timer
            countDownTimer.cancel()
            intent = Intent(this, CompletedOnTimeActivity::class.java)
            startActivity(intent)
        }
    }

    // Create a new countdown timer and start it
    fun startTimer() {

        endTime = System.currentTimeMillis() + timeLeftInMillis

        countDownTimer = object: CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateCountDownText()
            }

            override fun onFinish() {
//                timerRunning = false
                Log.d(TAG, "finished timer " + countDownTimer.toString())
                intent = Intent(this@WritingActivity, OutOfTimeActivity::class.java)
                startActivity(intent)
            }
        }.start()

        Log.d(TAG, "started timer " + countDownTimer.toString())

//        timerRunning = true
    }

    private fun updateCountDownText() {
        val minutes:Int = (timeLeftInMillis.toInt() / 1000) / 60
        val seconds:Int = (timeLeftInMillis.toInt() / 1000) % 60

        val timeLeftFormatted:String = String.format("%02d:%02d", minutes, seconds)

        timerText.text = timeLeftFormatted
    }

    // Update Provider to fix Picasso 504 timeout error on older device,
    // example API 17 tablet
    private fun updateAndroidSecurityProvider(callingActivity: Activity) {
        try {
            ProviderInstaller.installIfNeeded(this)
            Log.d(TAG, "Installed provider if needed")
        } catch (e: GooglePlayServicesRepairableException) {
            // Thrown when Google Play Services is not installed, up-to-date, or enabled
            // Show dialog to allow users to install, update, or otherwise enable Google Play services.
            GooglePlayServicesUtil.getErrorDialog(e.getConnectionStatusCode(), callingActivity, 0)
        } catch (e: GooglePlayServicesNotAvailableException) {
            Log.e("SecurityException", "Google Play Services not available.")
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

    override fun onStop() {
        super.onStop()

        // cancel existing timer
        countDownTimer.cancel()
        Log.d(TAG, "canceled timer " + countDownTimer.toString())
    }

    // Keep timer running after configuration change, example on rotation,
    // and when user closes app
//    override fun onStop() {
//        super.onStop()
//
//        val prefs:SharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
//        val editor:SharedPreferences.Editor = prefs.edit()
//
//        editor.putLong(KEY_MILLIS_LEFT, timeLeftInMillis)
////        editor.putBoolean(KEY_TIMER_RUNNING, timerRunning)
//        editor.putLong(KEY_END_TIME, endTime)
//
//        editor.apply()
//
//        countDownTimer.cancel()
//    }

    // onStart() called after onCreate()
//    override fun onStart() {
//        super.onStart()
//
//        val prefs:SharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
//        // default val set to default starting time
//        timeLeftInMillis = prefs.getLong(KEY_MILLIS_LEFT, startTimeInMillis)
////        timerRunning = prefs.getBoolean(KEY_TIMER_RUNNING, false)
//
//        updateCountDownText()
//
//        if (timerRunning) {
//            endTime = prefs.getLong(KEY_END_TIME, 0)
//            timeLeftInMillis = endTime - System.currentTimeMillis()
//
//            if (timeLeftInMillis < 0) {
//                timeLeftInMillis = 0
////                timerRunning = false
//                updateCountDownText()
//            } else {
//                startTimer()
//            }
//        }
//
//    }

    // Keep timer running after configuration change, example on rotation
    // Save image URL
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_MILLIS_LEFT, timeLeftInMillis)
        outState.putLong(KEY_END_TIME, endTime)

        outState.putString(KEY_IMAGE_URL, imageUrl)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        timeLeftInMillis = savedInstanceState.getLong(KEY_MILLIS_LEFT)
        endTime = savedInstanceState.getLong(KEY_END_TIME)
        timeLeftInMillis = endTime - System.currentTimeMillis()
        updateCountDownText()

        startTimer()
    }
}