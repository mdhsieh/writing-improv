package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import timber.log.Timber

/**
 * Displays selected writing submission details.
 */
class SentChallengeDetailsFragment:Fragment(R.layout.fragment_sent_challenge_details) {

    private val args: SentChallengeDetailsFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageView = view.findViewById<ImageView>(R.id.iv_image)
        val progressBar = view.findViewById<ProgressBar>(R.id.pb_loading_image)
        val errorText = view.findViewById<TextView>(R.id.tv_error)
        val timeText = view.findViewById<TextView>(R.id.tv_time)
        val promptText = view.findViewById<TextView>(R.id.tv_writing_prompt)
        val writingText = view.findViewById<TextView>(R.id.tv_writing)
        val statusText = view.findViewById<TextView>(R.id.tv_status)
        val reviewEditText = view.findViewById<EditText>(R.id.et_review)
        val submitReviewButton = view.findViewById<Button>(R.id.btn_submit_review)

        val item = args.writingItem
        timeText.text = item.time
        promptText.text = item.prompt
        writingText.text = item.writing

        // Hide error message
        errorText.visibility = View.GONE
        loadImage(item.url, imageView, progressBar, errorText)

        // If text is blank, then set status to incomplete and hide review EditText
        // to indicate this challenge has not been submitted by the other user yet
        if (item.writing.isEmpty()) {
            statusText.text = "Incomplete"
            reviewEditText.visibility = View.GONE
            submitReviewButton.visibility = View.GONE
        } else {
            statusText.text = "Completed"
            reviewEditText.visibility = View.VISIBLE
            submitReviewButton.visibility = View.VISIBLE
        }
    }

    /**
     * Display an image from a URL.
     * Otherwise, show placeholder error icon.
     * @param url The url of the image
     * @param imageView The ImageView which displays the image
     * @param progressBar ProgressBar to tell user image is loading
     * @param textView TextView informing user an error occurred
     */
    private fun loadImage(url:String, imageView:ImageView, progressBar:ProgressBar, textView:TextView) {
        Picasso.get().load(url)
            .error(R.drawable.ic_error_outline_72)
            .into(imageView, object : Callback {
                override fun onSuccess() {
                    Timber.d("finished getting image")
                    // Hide progress bar
                    progressBar.visibility = View.GONE
                }

                override fun onError(e: Exception?) {
                    // log error message
                    Timber.e(e)
                    // Show error message
                    textView.visibility = View.VISIBLE
                    // Hide progress bar
                    progressBar.visibility = View.GONE
                }

            })
    }
}