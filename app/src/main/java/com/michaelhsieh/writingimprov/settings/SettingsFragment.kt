package com.michaelhsieh.writingimprov.settings

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.michaelhsieh.writingimprov.MainActivity
import com.michaelhsieh.writingimprov.home.HomeFragment
import com.michaelhsieh.writingimprov.R
import com.michaelhsieh.writingimprov.practice.PromptFragmentDirections
import es.dmoral.toasty.Toasty
import timber.log.Timber


class SettingsFragment : Fragment(R.layout.fragment_settings) {

    var db = FirebaseFirestore.getInstance()

    companion object {
        const val KEY_PREFS = "prefs"
        const val KEY_MIN_MINUTES = "min-minutes"
        const val KEY_MAX_MINUTES = "max-minutes"
        const val KEY_BOT_DAILY_NOTIFICATIONS = "daily-notifications"
        const val KEY_BOT_RANDOM_PROMPTS = "random-prompts"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usernameEditText = view.findViewById<EditText>(R.id.et_username)
        val saveUsernameButton = view.findViewById<Button>(R.id.btn_change_username)

        // Username is same as FirebaseUI display name
        val username = getUsername()
        if (username != null) {
            usernameEditText.setText(username)
        } else {
            Timber.d("display name: %s", username)
            Toasty.error(this@SettingsFragment.requireContext(), R.string.error_user_info, Toast.LENGTH_LONG).show()
        }

        saveUsernameButton.setOnClickListener {
            val changedUsername = usernameEditText.text.toString()
            updateUsername(changedUsername)
        }

        val editPromptsButton = view.findViewById<Button>(R.id.btn_edit_prompts)
        editPromptsButton.setOnClickListener {
            val action = SettingsFragmentDirections.actionSettingsFragmentToEditPromptsFragment()
            findNavController().navigate(action)
        }

        val minTimeEditText:EditText = view.findViewById(R.id.et_min_time)
        val maxTimeEditText:EditText = view.findViewById(R.id.et_max_time)
        val saveTimesButton:Button = view.findViewById(R.id.btn_change_practice_times)

        // Check if saved times already exist
        val sp: SharedPreferences = requireActivity().getSharedPreferences(KEY_PREFS, Activity.MODE_PRIVATE)
        val savedMin = sp.getInt(KEY_MIN_MINUTES, -1)
        val savedMax = sp.getInt(KEY_MAX_MINUTES, -1)

        // Default 1 and 3
        if (savedMin == -1) {
            minTimeEditText.setText("1")
        }
        if (savedMax == -1) {
            maxTimeEditText.setText("3")
        }

        saveTimesButton.setOnClickListener {
            val minTime:Int = minTimeEditText.text.toString().toInt()
            val maxTime:Int = maxTimeEditText.text.toString().toInt()
            savePracticeTimes(minTime, maxTime)
        }

        val requestChallengeButton = view.findViewById<Button>(R.id.btn_request_challenge)
        requestChallengeButton.setOnClickListener {
            (this.activity as MainActivity).generateBotChallenge()
        }

        // Check if daily notification or random prompt settings already set
        // Default daily notification true
        val isBotDailyNotifications = sp.getBoolean(KEY_BOT_DAILY_NOTIFICATIONS, true)
        // Default random prompts false
        val isBotRandomPrompts = sp.getBoolean(KEY_BOT_RANDOM_PROMPTS, false)
        // Set the switches to whatever was already set
        val dailyNotificationSwitch = view.findViewById<SwitchCompat>(R.id.switch_bot_notification)
        val randomPromptsSwitch = view.findViewById<SwitchCompat>(R.id.switch_bot_random)
        dailyNotificationSwitch.isChecked = isBotDailyNotifications
        randomPromptsSwitch.isChecked = isBotRandomPrompts
        // Change SharedPrefs if switches are toggled
        dailyNotificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // The toggle is enabled
                val editor = sp.edit()
                editor.putBoolean(KEY_BOT_DAILY_NOTIFICATIONS, true)
                editor.apply()
                Toasty.normal(this@SettingsFragment.requireContext(),
                    R.string.switch_daily_on, Toast.LENGTH_LONG).show()
            } else {
                // The toggle is disabled
                val editor = sp.edit()
                editor.putBoolean(KEY_BOT_DAILY_NOTIFICATIONS, false)
                editor.apply()
                Toasty.normal(this@SettingsFragment.requireContext(),
                    R.string.switch_daily_off, Toast.LENGTH_LONG).show()
            }
        }
        randomPromptsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // The toggle is enabled
                val editor = sp.edit()
                editor.putBoolean(KEY_BOT_RANDOM_PROMPTS, true)
                editor.apply()
                Toasty.normal(this@SettingsFragment.requireContext(),
                    R.string.switch_random_prompts_on, Toast.LENGTH_LONG).show()
            } else {
                // The toggle is disabled
                val editor = sp.edit()
                editor.putBoolean(KEY_BOT_RANDOM_PROMPTS, false)
                editor.apply()
                Toasty.normal(this@SettingsFragment.requireContext(),
                    R.string.switch_random_prompts_off, Toast.LENGTH_LONG).show()
            }
        }

    }

    /**
     * Return the user's display name if signed in.
     * Otherwise, return null.
     */
    private fun getUsername():String? {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            return user.displayName
        }
        return null
    }

    /**
     * Change FirebaseUI Auth display name.
     * @param name The new name
     */
    private fun updateUsername(name:String) {
        val user = FirebaseAuth.getInstance().currentUser

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()

        user!!.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.d("User profile updated")
                    Toasty.normal(this@SettingsFragment.requireContext(),
                        R.string.updated_username, Toast.LENGTH_LONG).show()
                }
            }

        // Also update the username in Firestore
        val email = getEmail()
        if (email != null) {
            db.collection(HomeFragment.COLLECTION_USERS)
                .document(email)
                .update("username", name)
                .addOnSuccessListener {
                    Timber.d("username updated in Firestore: %s", user)
                }
                .addOnFailureListener { e ->
                    Timber.w(e, "Error updating username in Firestore")
                }
        }
    }

    /**
     * Save min and max practice times user entered in EditTexts
     * @param min The minimum time
     * @param max The maximum time
     */
    private fun savePracticeTimes(min:Int, max:Int) {

        if (min > 0 && max > 0 && min <= max) {
            val sp: SharedPreferences =
                requireActivity().getSharedPreferences(KEY_PREFS, Activity.MODE_PRIVATE)
            val editor = sp.edit()
            editor.putInt(KEY_MIN_MINUTES, min)
            editor.putInt(KEY_MAX_MINUTES, max)
            editor.apply()

            Toasty.normal(
                this@SettingsFragment.requireContext(),
                R.string.updated_practice_times,
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toasty.error(
                this@SettingsFragment.requireContext(),
                R.string.error_practice_times,
                Toast.LENGTH_LONG
            ).show()
        }
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