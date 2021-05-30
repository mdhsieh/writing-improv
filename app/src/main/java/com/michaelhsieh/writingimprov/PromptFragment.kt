package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.michaelhsieh.writingimprov.httprequest.JsonUnsplashApi
import com.michaelhsieh.writingimprov.httprequest.UnsplashImage
import es.dmoral.toasty.Toasty
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

/**
 * Displays question icon, random time limit text in minutes, random prompt text,
 * button to go to writing screen.
 */
const val KEY_MINUTES = "minutes"
const val KEY_PROMPT = "prompt"

const val KEY_URL = "url"

class PromptFragment:Fragment(R.layout.fragment_prompt) {

    private lateinit var prompt: String
    private lateinit var minutes: String

    private val MIN_MINUTES:Int = 1
    private val MAX_MINUTES:Int = 3

    private val BASE_URL:String = "https://api.unsplash.com/"
    // image URL
    private var url:String = ""
    // image thumbnail URL
//    private var thumbUrl:String = ""

    // Show progress bar while getting image URL
    private lateinit var progressBar: ProgressBar
    // Show error if can't get image URL
    private lateinit var errorText:TextView
    private lateinit var goButton:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            prompt = savedInstanceState.getString(KEY_PROMPT)!!
            minutes = savedInstanceState.getString(KEY_MINUTES)!!

            url = savedInstanceState.getString(KEY_URL)!!
            Timber.d("After config change, url: %s", url)
        } else {
            prompt = getRandomPrompt()
            minutes = getRandomTime(MIN_MINUTES, MAX_MINUTES).toString()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val promptText: TextView = view.findViewById(R.id.tv_prompt)
        val minutesText: TextView = view.findViewById(R.id.tv_time)

        promptText.text = prompt
        minutesText.text = minutes


        // Create progress bar, error text, go button
        progressBar = view.findViewById(R.id.pb_loading_url)
        errorText = view.findViewById(R.id.tv_error_url)
        goButton = view.findViewById(R.id.btn_go)
        // Hide error text
        errorText.visibility = View.GONE
        // If error occurred before config change, url should be default value which is empty String
        if (savedInstanceState != null && url.isNotEmpty()) {
            progressBar.visibility = View.GONE
            goButton.visibility = View.VISIBLE
        } else {
            // Show progress bar so user knows getting url
            progressBar.visibility = View.VISIBLE

            // hide go button until got the url
            goButton.visibility = View.GONE

            // while getting url button is not visible, so increment
            CountingIdlingResourceSingleton.increment()

            getRandomImageUrl()
        }

        goButton.setOnClickListener {

            val action = PromptFragmentDirections.actionPromptFragmentToWritingFragment(minutes.toInt(), prompt, url)
            findNavController().navigate(action)

        }
    }

    // Save prompt and minutes and URL on configuration change, example screen rotate
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PROMPT, prompt)
        outState.putString(KEY_MINUTES, minutes)

        outState.putString(KEY_URL, url)
    }

    /** Generates a random prompt using String resources. */
    private fun getRandomPrompt():String {
        val promptArray = arrayOf(
            getString(R.string.prompt_feel),
            getString(R.string.prompt_story),
            getString(R.string.prompt_mystery),
            getString(R.string.prompt_action),
            getString(R.string.prompt_thriller),
            getString(R.string.prompt_comedy)
        )
        // generated random number from 0 to last index included
        val randNum = (promptArray.indices).random()
        return promptArray[randNum]
    }

    /** Generates a random integer time in minutes.
     * @param min The minimum time limit
     * @param max The maximum time limit
     * @return A number from min to max, included */
    private fun getRandomTime(min:Int, max:Int):Int {
        return (min..max).random()
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
                Toasty.error(this@PromptFragment.requireContext(), R.string.error_loading_url, Toast.LENGTH_LONG,true).show()

                //  hide progress bar
                progressBar.visibility = View.GONE
                // show error text
                errorText.visibility = View.VISIBLE
            }

            override fun onResponse(call: Call<UnsplashImage>, response: Response<UnsplashImage>) {
                if (!response.isSuccessful) {
                    Timber.d("Code: %s", response.code())
                    // Show error Toasty
                    Toasty.error(this@PromptFragment.requireContext(), R.string.error_loading_url, Toast.LENGTH_LONG,true).show()

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
//                    thumbUrl = thumbnailUrl.asString
//                    Timber.d("finished getting thumbnail url: %s", thumbUrl)

                    //  hide progress bar
                    progressBar.visibility = View.GONE
                    // Make go button visible
                    goButton.visibility = View.VISIBLE

                    // finished getting url so decrement
                    CountingIdlingResourceSingleton.decrement()
                }
            }

        })
    }
}