package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import timber.log.Timber

/**
 * Displays question icon, random time limit text in minutes, random prompt text,
 * button to go to writing screen.
 */
const val KEY_MINUTES = "minutes"
const val KEY_PROMPT = "prompt"

class PromptFragment:Fragment(R.layout.fragment_prompt) {

    private lateinit var promptText: TextView
    private lateinit var minutesText: TextView

    private val MIN_MINUTES:Int = 1
    private val MAX_MINUTES:Int = 3

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        promptText = view.findViewById(R.id.tv_prompt)
        minutesText = view.findViewById(R.id.tv_time)

        Timber.d("onViewCreated, promptText initialized")

        if (savedInstanceState != null) {
            promptText.text = savedInstanceState.getString(KEY_PROMPT)
            minutesText.text = savedInstanceState.getString(KEY_MINUTES)
        } else {
            promptText.text = getRandomPrompt()
            minutesText.text = getRandomTime(MIN_MINUTES, MAX_MINUTES).toString()
        }

        val goButton = view.findViewById<Button>(R.id.btn_go)
        goButton.setOnClickListener {

            val minutes = minutesText.text.toString()

            val action = PromptFragmentDirections.actionPromptFragmentToWritingFragment(minutes.toInt(), promptText.text.toString())
            findNavController().navigate(action)

        }
    }

    // Save prompt and minutes on configuration change, example screen rotate
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Timber.d("promptText initialized? " + this::promptText.isInitialized)
//        Timber.d("minutesText initialized? " + this::minutesText.isInitialized)
        if (this::promptText.isInitialized && this::minutesText.isInitialized) {
            outState.putString(KEY_PROMPT, promptText.text.toString())
            outState.putString(KEY_MINUTES, minutesText.text.toString())
        }
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
}