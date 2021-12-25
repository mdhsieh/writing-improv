package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import es.dmoral.toasty.Toasty
import timber.log.Timber

/**
 * Displays selected writing submission details.
 */
class SentChallengeDetailsFragment:Fragment(R.layout.fragment_sent_challenge_details) {

    private val args: SentChallengeDetailsFragmentArgs by navArgs()

    var db = FirebaseFirestore.getInstance()

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

        // If text is blank, then set status and hide review EditText and button
        // to indicate this challenge has not been submitted by the other user yet
        if (item.writing.isEmpty()) {
            statusText.text = getText(R.string.sent_challenge_not_submitted_short)
            reviewEditText.visibility = View.GONE
            submitReviewButton.visibility = View.GONE
        } else {
            statusText.text = getText(R.string.sent_challenge_submitted_short)
            reviewEditText.visibility = View.VISIBLE
            submitReviewButton.visibility = View.VISIBLE

            // Set the review text to the last updated review from Firestore.
            // Default review text is empty if no review has been submitted yet
            getReviewText(getEmail(), item, reviewEditText)
        }

        // Submit review to other user, show confirmation Toast, and go back to previous screen
        submitReviewButton.setOnClickListener {
            val email = getEmail()
            val reviewText = reviewEditText.text.toString()
            updateReview(email, item, reviewText)
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

    /**
     * Return the user's email if signed in.
     * Otherwise, return null.
     */
    private fun getEmail():String? {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            return user.email
        }
        return null
    }

    /** Update review text of writing with ID in Firestore.
     * Assumes the review field already exists.
     */
    private fun updateReview(email:String?, item:WritingItem, reviewText:String) {
        // Find the writing submitted for this challenge
        if (email != null) {
            db.collection(HomeFragment.COLLECTION_USERS)
                .document(email)
                .collection(HomeFragment.COLLECTION_WRITING)
                .whereEqualTo("id", item.id)
                .get()
                .addOnSuccessListener {

                    // Should only be 1 writing with the ID
                    // Update review field to EditText text
                    for (doc in it.documents) {
                        // Find auto-generated Firestore ID to do update
                        // Need to get Firestore ID to get the document itself
                        val docFirestoreId = doc.id

                        // Now update the writing review field
                        // Now update the challenge document with this Firestore ID
                        db.collection(HomeFragment.COLLECTION_USERS)
                            .document(email)
                            .collection(HomeFragment.COLLECTION_WRITING)
                            .document(docFirestoreId)
                            .update("review", reviewText)
                            .addOnSuccessListener {
                                Toasty.info(this.requireContext(), R.string.review_submitted, Toast.LENGTH_LONG)

                                findNavController().popBackStack()
                            }
                            .addOnFailureListener {
                                Timber.e(it)
                                Toasty.error(this.requireContext(), R.string.error_submitting_review, Toast.LENGTH_LONG).show()
                            }
                    }

                }
                .addOnFailureListener {
                    Timber.e(it)
                    Toasty.error(this.requireContext(), R.string.error_submitting_review, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun getReviewText(email:String?, item:WritingItem, editText:EditText) {
        // Find the writing submitted for this challenge
        if (email != null) {
            db.collection(HomeFragment.COLLECTION_USERS)
                .document(email)
                .collection(HomeFragment.COLLECTION_WRITING)
                .whereEqualTo("id", item.id)
                .get()
                .addOnSuccessListener {

                    // Should only be 1 writing with the ID
                    // Update review field to EditText text
                    for (doc in it.documents) {
                        editText.setText(doc.data?.get("review").toString())
                    }

                }
                .addOnFailureListener {
                    Timber.e(it)
                    Toasty.error(this.requireContext(), R.string.error_submitting_review, Toast.LENGTH_LONG).show()
                }
        }
    }
}