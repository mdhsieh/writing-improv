package com.michaelhsieh.writingimprov

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.*
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception

private const val RAND_IMAGE_URL:String = "https://source.unsplash.com/random/800x800"
private const val TAG:String = "WritingActivity"
private const val KEY_MILLIS_LEFT:String = "millisLeft"
private const val KEY_END_TIME:String = "endTime"

class WritingActivity : AppCompatActivity() {

    private var startTimeInMillis:Long = 0
    private var timeLeftInMillis:Long = startTimeInMillis
    private var endTime:Long = 0
    private lateinit var timerText: TextView
    private lateinit var countDownTimer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing)

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
        val image = findViewById<ImageView>(R.id.iv_image)
        Picasso.get().load(RAND_IMAGE_URL)
                        .error(R.drawable.ic_error_outline_72)
                        .into(image, object : Callback {
                            override fun onSuccess() {
                                // successfully loaded, start countdown timer
                                startTimer()
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
                intent = Intent(this@WritingActivity, OutOfTimeActivity::class.java)
                startActivity(intent)
            }
        }.start()
    }

    private fun updateCountDownText() {
        val minutes:Int = (timeLeftInMillis.toInt() / 1000) / 60
        val seconds:Int = (timeLeftInMillis.toInt() / 1000) % 60

        val timeLeftFormatted:String = String.format("%02d:%02d", minutes, seconds)

        timerText.text = timeLeftFormatted
    }

    // Keep timer running after configuration change, example on rotation
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_MILLIS_LEFT, timeLeftInMillis)
        outState.putLong(KEY_END_TIME, endTime)
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