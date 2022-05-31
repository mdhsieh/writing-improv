package com.michaelhsieh.writingimprov.practice

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.michaelhsieh.writingimprov.PromptItem
import com.michaelhsieh.writingimprov.R
import com.michaelhsieh.writingimprov.WritingItem
import com.michaelhsieh.writingimprov.home.HomeFragment
import com.michaelhsieh.writingimprov.home.HomeFragment.Companion.COLLECTION_PROMPTS
import com.michaelhsieh.writingimprov.httprequest.JsonUnsplashApi
import com.michaelhsieh.writingimprov.httprequest.UnsplashImage
import com.michaelhsieh.writingimprov.settings.SettingsFragment
import com.michaelhsieh.writingimprov.testresource.CountingIdlingResourceSingleton
import es.dmoral.toasty.Toasty
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.*


/**
 * Displays question icon, random time limit text in minutes, random prompt text,
 * button to go to writing screen.
 */
const val KEY_MINUTES = "minutes"
const val KEY_PROMPT = "prompt"

const val KEY_URL = "url"
const val KEY_THUMB_URL = "thumb"

class PromptFragment:Fragment(R.layout.fragment_prompt) {

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
    private lateinit var goButton:Button

    private lateinit var shuffleButton:Button
    private lateinit var editTimesButton:Button
    private lateinit var editPromptsButton:Button

    var db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if saved times already exist
        val sp: SharedPreferences = requireActivity().getSharedPreferences(SettingsFragment.KEY_PREFS, Activity.MODE_PRIVATE)
        var savedMin = sp.getInt(SettingsFragment.KEY_MIN_MINUTES, -1)
        var savedMax = sp.getInt(SettingsFragment.KEY_MAX_MINUTES, -1)

        // Default 1 and 3
        if (savedMin == -1) {
            savedMin = 1
        }
        if (savedMax == -1) {
            savedMax = 3
        }

        if (savedInstanceState != null) {
            prompt = savedInstanceState.getString(KEY_PROMPT)!!
            minutes = savedInstanceState.getString(KEY_MINUTES)!!

            url = savedInstanceState.getString(KEY_URL)!!
            Timber.d("After config change, url: %s", url)
            thumbUrl = savedInstanceState.getString(KEY_THUMB_URL)!!
            Timber.d("After config change, thumbnail url: %s", thumbUrl)
        } else {
            /*
            prompt = getRandomPrompt()
            */
            minutes = getRandomTime(savedMin, savedMax).toString()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val promptText: TextView = view.findViewById(R.id.tv_prompt)
        val minutesText: TextView = view.findViewById(R.id.tv_time)

        // Don't get random prompt again if after configuration change
        if (savedInstanceState == null) {
            getUserPrompts(promptText)
        } else {
            // prompt was already set
            // Make the textview match
            promptText.text = prompt
        }
        /*
        promptText.text = prompt
         */
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

        // Pass values to WritingFragment
        goButton.setOnClickListener {
//            val action = PromptFragmentDirections.actionPromptFragmentToWritingFragment(minutes.toInt(), prompt, url, thumbUrl, false, "Practice")
            // empty string challenge id because it's practice
            val action = PromptFragmentDirections.actionPromptFragmentToWritingFragment(minutes.toInt(), prompt, url, thumbUrl, false, "Practice", "")
            findNavController().navigate(action)
        }

        // Get prompt and time again if user clicks shuffle
        shuffleButton = view.findViewById(R.id.btn_shuffle)
        shuffleButton.setOnClickListener {
            // Duplicate code as before
            // Check if saved times already exist
            val sp: SharedPreferences = requireActivity().getSharedPreferences(SettingsFragment.KEY_PREFS, Activity.MODE_PRIVATE)
            var savedMin = sp.getInt(SettingsFragment.KEY_MIN_MINUTES, -1)
            var savedMax = sp.getInt(SettingsFragment.KEY_MAX_MINUTES, -1)

            // Default 1 and 3
            if (savedMin == -1) {
                savedMin = 1
            }
            if (savedMax == -1) {
                savedMax = 3
            }
            minutes = getRandomTime(savedMin, savedMax).toString()
            minutesText.text = minutes
            getUserPrompts(promptText)
        }

        // Navigate to settings screen to change random time range
        editTimesButton = view.findViewById(R.id.btn_edit_times)
        editTimesButton.setOnClickListener {
            val action = PromptFragmentDirections.actionPromptFragmentToSettingsFragment()
            findNavController().navigate(action)
        }
        // Navigate to edit prompts screen to add/delete custom prompts
        editPromptsButton = view.findViewById(R.id.btn_edit_prompts)
        editPromptsButton.setOnClickListener {
            val action = PromptFragmentDirections.actionPromptFragmentToEditPromptsFragment()
            findNavController().navigate(action)
        }
    }

    // Save prompt and minutes and URL on configuration change, example screen rotate
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PROMPT, prompt)
        outState.putString(KEY_MINUTES, minutes)

        outState.putString(KEY_URL, url)
        outState.putString(KEY_THUMB_URL, thumbUrl)
    }

    /** Generates a random prompt from user's custom prompts collection, or
     * if not available, generates from a newly created set of default prompts. */
    private fun getUserPrompts(textView: TextView) {
        val email = getEmail()
        if (email != null) {
            db.collection(HomeFragment.COLLECTION_USERS)
                .document(email)
                .collection(COLLECTION_PROMPTS)
                .get()
                .addOnCompleteListener(OnCompleteListener<QuerySnapshot?> { task ->
                    if (task.isSuccessful) {
                        if (task.result?.size()!! > 0) {
                            for (document in task.result!!) {
                                Timber.d("Prompts already exist, get from Firestore")
                                getPromptsFromFirestore(email, textView)
                            }
                        } else {
                            Timber.d("No prompts exist, create a new prompts collection")
                            createDefaultPrompts(email, textView)
                        }
                    } else {
                        Timber.d("Error getting practice prompts: %s", task.exception)
                        Toasty.error(this.requireContext(), getString(R.string.error_loading_prompts), Toast.LENGTH_LONG).show()
                    }
                })
        }
    }

    /**
     * Creates a new collection in Firestore containing default prompts from String resources.
     * Then generates a random prompt from this collection.
     */
    private fun createDefaultPrompts(userId:String, textView: TextView) {
        val promptArray = arrayOf(
            getString(R.string.prompt_feel),
            getString(R.string.prompt_story),
            getString(R.string.prompt_mystery),
            getString(R.string.prompt_action),
            getString(R.string.prompt_thriller),
            getString(R.string.prompt_comedy)
        )

        val collection = db.collection(HomeFragment.COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_PROMPTS)

        var counter = 0
        for (text in promptArray) {
            val promptItem = PromptItem(
                id = UUID.randomUUID().toString(),
                prompt = text,
                timestamp = System.currentTimeMillis() / 1000
            )
            collection.add(promptItem).addOnCompleteListener {
                if (counter == promptArray.size) {
                    Timber.d("Done adding default prompts")
                    getPromptsFromFirestore(userId, textView)
                }
                counter += 1
            }
        }

    }

    private fun getPromptsFromFirestore(userId: String, textView: TextView) {
        val collection = db.collection(HomeFragment.COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_PROMPTS)

        collection
            .get()
            .addOnSuccessListener {
                // Convert the whole Query Snapshot to a list
                // of objects directly
                val items: List<PromptItem> =
                    it.toObjects(PromptItem::class.java)
                prompt = getRandomPrompt(items)
                textView.text = prompt
            }.addOnFailureListener {
                Timber.e(it)
                Toasty.error(this.requireContext(), R.string.error_loading_prompts, Toast.LENGTH_LONG).show()
            }
    }

    /** Generates a random prompt from array.
     * @param prompts The List of possible PromptItems */
    private fun getRandomPrompt(prompts: List<PromptItem>):String {
        val promptTextList = prompts.map { it.prompt }
        val promptArray = promptTextList.toTypedArray()
        Timber.d("Final prompt array to pick from: %s", promptArray.joinToString())
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
                    thumbUrl = thumbnailUrl.asString
                    Timber.d("finished getting thumbnail url: %s", thumbUrl)

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
}