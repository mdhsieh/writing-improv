package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import es.dmoral.toasty.Toasty
import timber.log.Timber


class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usernameEditText = view.findViewById<EditText>(R.id.et_username)
        val saveButton = view.findViewById<Button>(R.id.btn_change_username)

        // Username is same as FirebaseUI display name
        val username = getUsername()
        if (username != null) {
            usernameEditText.setText(username)
        } else {
            Timber.d("display name: %s", username)
            Toasty.error(this@SettingsFragment.requireContext(), R.string.error_user_info, Toast.LENGTH_LONG).show()
        }

        saveButton.setOnClickListener {
            val changedUsername = usernameEditText.text.toString()
            updateUsername(changedUsername)
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
                    Toasty.normal(this@SettingsFragment.requireContext(), R.string.updated_username, Toast.LENGTH_LONG).show()
                }
            }
    }
}