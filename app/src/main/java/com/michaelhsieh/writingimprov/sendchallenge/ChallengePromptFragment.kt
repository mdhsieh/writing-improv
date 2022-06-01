package com.michaelhsieh.writingimprov.sendchallenge

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.michaelhsieh.writingimprov.ChallengeItem
import com.michaelhsieh.writingimprov.R
import com.michaelhsieh.writingimprov.httprequest.JsonUnsplashApi
import com.michaelhsieh.writingimprov.httprequest.UnsplashImage
import com.michaelhsieh.writingimprov.practice.KEY_THUMB_URL
import com.michaelhsieh.writingimprov.practice.KEY_URL
import com.michaelhsieh.writingimprov.testresource.CountingIdlingResourceSingleton
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import es.dmoral.toasty.Toasty
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.*

/**
 * Displays newly created challenge's
 * random image, user input time limit in minutes,
 * user input prompt text,
 * and button to send challenge to author.
 */
//const val KEY_MINUTES = "minutes"
//const val KEY_PROMPT = "prompt"
//
//const val KEY_URL = "url"
//const val KEY_THUMB_URL = "thumb"

class ChallengePromptFragment:Fragment(R.layout.fragment_challenge_prompt) {

    var db = FirebaseFirestore.getInstance()

    // get the author to challenge info like username to display
    private val args: ChallengePromptFragmentArgs by navArgs()

    private lateinit var prompt: String
    private lateinit var minutes: String

    private val BASE_URL:String = "https://api.unsplash.com/"
    // image URL
    private var url:String = ""

    // image thumbnail URL
    private var thumbUrl:String = ""

    // Show progress bar while getting image URL
    private lateinit var progressBar: ProgressBar
    // Show error if can't get image URL
    private lateinit var errorText:TextView
    private lateinit var sendChallengeButton:Button
    private lateinit var imageView:ImageView
    private lateinit var randomImageButton:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            url = savedInstanceState.getString(KEY_URL)!!
            Timber.d("After config change, url: %s", url)
            thumbUrl = savedInstanceState.getString(KEY_THUMB_URL)!!
            Timber.d("After config change, thumbnail url: %s", thumbUrl)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val promptEditText: EditText = view.findViewById(R.id.et_prompt)
        val minutesEditText: TextView = view.findViewById(R.id.et_time)

        // Create progress bar, error text, go button
        progressBar = view.findViewById(R.id.pb_loading_url)
        errorText = view.findViewById(R.id.tv_error_url)
        randomImageButton = view.findViewById(R.id.btn_random_image)
        sendChallengeButton = view.findViewById(R.id.btn_send_challenge)

        imageView = view.findViewById(R.id.iv_image)
        // Item is info of the user to send a challenge to
        val item = args.authorToChallenge
        if (item != null) {
            sendChallengeButton.text = getString(R.string.send_challenge_to_user, item.name)
        }

        // Hide error text
        errorText.visibility = View.GONE
        // If error occurred before config change, url should be default value which is empty String
        if (savedInstanceState != null && url.isNotEmpty()) {
            progressBar.visibility = View.GONE
            sendChallengeButton.visibility = View.VISIBLE

            // Load image from before config change, example after rotation
            loadImage(url)
        } else {
            // Show progress bar so user knows getting url
            progressBar.visibility = View.VISIBLE

            // hide send challenge button until got the url
            sendChallengeButton.visibility = View.GONE

            // while getting url button is not visible, so increment
            CountingIdlingResourceSingleton.increment()

            getRandomImageUrl()
        }

        randomImageButton.setOnClickListener {
            getRandomImageUrl()
        }

        sendChallengeButton.setOnClickListener {
            if (item != null) {
                // Get author ID, which is email, and username to add challenge to his or her collection
                // and display success or error Toasty
                val id = item.id
                val authorName = item.name

                // Get username of whoever is sending the challenge
                val user = FirebaseAuth.getInstance().currentUser
                val myUsername = user?.displayName.toString()
                val myEmail = user?.email.toString()
                // Title of writing to inform author is challenge from current user
//                val writingName = "Challenge from " + myUsername
                val writingName = getString(R.string.challenge_from, myUsername)

                // Get prompt, time from EditText
                prompt = promptEditText.text.toString()
                minutes = minutesEditText.text.toString()

                // Create new challenge and add to Firestore
                val challengeItem = ChallengeItem(
                    UUID.randomUUID().toString(),
                    writingName,
                    prompt = prompt,
                    time = minutes,
                    url = url,
                    thumbUrl = thumbUrl,
                    completed = false,
                    senderId = myEmail,
                    receiverId = item.id,
                    receiverUsername = item.name,
                    timestamp = System.currentTimeMillis() / 1000
                )
                db.collection("users").document(id).collection("challenges").add(challengeItem)
                    // Show success or error Toasty
                    .addOnSuccessListener {
                        Toasty.info(
                            this@ChallengePromptFragment.requireContext(),
                            getString(R.string.sent_challenge_success, authorName),
                            Toast.LENGTH_LONG,
                            true
                        ).show()

                        // Go back to author list
                        findNavController().popBackStack()
                    }
                    .addOnFailureListener() {
                        Toasty.error(
                            this@ChallengePromptFragment.requireContext(),
                            getString(R.string.sent_challenge_error, authorName),
                            Toast.LENGTH_LONG,
                            true
                        ).show()
                    }
            } else {
                Timber.e("Author info is null")

                Toasty.error(
                    this@ChallengePromptFragment.requireContext(),
                    getString(R.string.sent_challenge_error, "Unknown"),
                    Toast.LENGTH_LONG,
                    true
                ).show()
            }
        }
    }

    // Save image URL on configuration change, example screen rotate
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_URL, url)
        outState.putString(KEY_THUMB_URL, thumbUrl)
    }

    /**
     *  Gets a random image URL and sets variable if successful.
     *  Otherwise, shows an error Toast.
     *
     */
    private fun getRandomImageUrl() {

        Timber.d("starting get url")

        // Create Retrofit to get random image
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val jsonUnsplashApi: JsonUnsplashApi = retrofit.create(JsonUnsplashApi::class.java)

        // pass in access key
        val call: Call<UnsplashImage> = jsonUnsplashApi.getRandomImage(getString(R.string.access_key))

        call.enqueue(object : retrofit2.Callback<UnsplashImage> {
            override fun onFailure(call: Call<UnsplashImage>, t: Throwable) {
                Timber.e(t.message)
                Toasty.error(this@ChallengePromptFragment.requireContext(),
                    R.string.error_loading_url, Toast.LENGTH_LONG,true).show()

                //  hide progress bar
                progressBar.visibility = View.GONE
                // show error text
                errorText.visibility = View.VISIBLE
            }

            override fun onResponse(call: Call<UnsplashImage>, response: Response<UnsplashImage>) {
                if (!response.isSuccessful) {
                    Timber.d("Code: %s", response.code())
                    // Show error Toasty
                    Toasty.error(this@ChallengePromptFragment.requireContext(),
                        R.string.error_loading_url, Toast.LENGTH_LONG,true).show()

                    //  hide progress bar
                    progressBar.visibility = View.GONE
                    // show error text
                    errorText.visibility = View.VISIBLE
                    return
                }

                val image: UnsplashImage? = response.body()

                if (image != null) {

                    val regularUrl = image.urls.asJsonObject.get("regular")
                    val thumbnailUrl = image.urls.asJsonObject.get("thumb")

                    // Set var url to the new image URL
                    url = regularUrl.asString
                    Timber.d("finished getting url: %s", url)

                    // Set var thumb url to the new image thumbnail URL
                    thumbUrl = thumbnailUrl.asString
                    Timber.d("finished getting thumbnail url: %s", thumbUrl)

                    //  hide progress bar
                    progressBar.visibility = View.GONE
                    // Make go button visible
                    sendChallengeButton.visibility = View.VISIBLE

                    // finished getting url so decrement
                    CountingIdlingResourceSingleton.decrement()


                    // show image for user
                    loadImage(url)
                }
            }

        })
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
                    Toasty.error(this@ChallengePromptFragment.requireContext(),
                        R.string.error_loading_image, Toast.LENGTH_LONG,true).show()
                    Timber.e(e)
                    //  hide progress bar
                    progressBar.visibility = View.GONE

                    // show error text
                    errorText.visibility = View.VISIBLE
                }

            })
    }
}