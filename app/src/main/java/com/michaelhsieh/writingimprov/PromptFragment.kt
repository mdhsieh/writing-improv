package com.michaelhsieh.writingimprov

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

private const val TAG:String = "PromptFragment"

/**
 * Displays question mark icon, random time limit text in minutes, random prompt text,
 * button to go to writing screen.
 */
class PromptFragment:Fragment(R.layout.fragment_prompt) {

    // need to use key when passing time and prompt to WritingActivity
    companion object {
        const val KEY_MINUTES = "minutes"
        const val KEY_PROMPT = "prompt"
    }

    private lateinit var promptText: TextView
    private lateinit var minutesText: TextView

    private val MIN_MINUTES:Int = 1
    private val MAX_MINUTES:Int = 3

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        promptText = view.findViewById(R.id.tv_prompt)
        minutesText = view.findViewById(R.id.tv_time)

        if (savedInstanceState != null) {
            promptText.text = savedInstanceState.getString(KEY_PROMPT)
            minutesText.text = savedInstanceState.getString(KEY_MINUTES)
        } else {
            promptText.text = getRandomPrompt()
            minutesText.text = getRandomTime(MIN_MINUTES, MAX_MINUTES).toString()
        }

        val goButton = view.findViewById<Button>(R.id.btn_go)
        goButton.setOnClickListener {
            val intent = Intent(activity, WritingActivity::class.java)
            val minutes = minutesText.text.toString()
            if (minutes.trim().isNotEmpty()) {
                intent.putExtra(KEY_MINUTES, minutes.toInt())
                intent.putExtra(KEY_PROMPT, promptText.text.toString())
                startActivity(intent)
            } else {
                Toast.makeText(this@PromptFragment.activity, R.string.error_minutes, Toast.LENGTH_LONG).show()
            }

        }
    }

    // Save prompt and minutes on configuration change, example screen rotate
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PROMPT, promptText.text.toString())
        outState.putString(KEY_MINUTES, minutesText.text.toString())
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