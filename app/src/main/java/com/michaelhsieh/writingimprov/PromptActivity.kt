package com.michaelhsieh.writingimprov

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

private const val TAG:String = "PromptActivity"

class PromptActivity : AppCompatActivity() {

    // need to use key when passing time and prompt to WritingActivity
    companion object {
        const val KEY_MINUTES = "minutes"
        const val KEY_PROMPT = "prompt"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prompt)

        val minutesInput = findViewById<EditText>(R.id.et_time)

        val promptText = findViewById<TextView>(R.id.tv_prompt)
        promptText.text = createRandomPrompt()

        val goButton = findViewById<Button>(R.id.btn_go)
        goButton.setOnClickListener {
            val intent = Intent(this, WritingActivity::class.java)
            val minutes = minutesInput.text.toString()
            if (minutes.trim().isNotEmpty()) {
                intent.putExtra(KEY_MINUTES, minutes.toInt())
                intent.putExtra(KEY_PROMPT, promptText.text.toString())
                startActivity(intent)
            } else {
                Toast.makeText(this@PromptActivity, R.string.error_minutes, Toast.LENGTH_LONG).show()
            }

        }
    }

    /** Generates a random prompt using String resources. */
    private fun createRandomPrompt():String {
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
        Log.d(TAG, promptArray[randNum])
        return promptArray[randNum]
    }
}