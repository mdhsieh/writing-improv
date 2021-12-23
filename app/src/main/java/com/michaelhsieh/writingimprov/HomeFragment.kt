package com.michaelhsieh.writingimprov

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import es.dmoral.toasty.Toasty
import timber.log.Timber

/**
 * Home menu.
 * Displays user option buttons such as view user's writing and practice writing.
 * Has sign in functionality.
 */
class HomeFragment:Fragment(R.layout.fragment_home) {

    var db = FirebaseFirestore.getInstance()

    companion object {
        val MAP_USERNAME = "username"
        val COLLECTION_USERS = "users"
        val COLLECTION_WRITING = "writing"
        val COLLECTION_CHALLENGES = "challenges"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val practiceButton = view.findViewById<Button>(R.id.btn_practice)
        practiceButton.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToPromptFragment()
            findNavController().navigate(action)
        }

        // WritingItem is null since user is not submitting any writing
        val myWritingButton = view.findViewById<Button>(R.id.btn_my_writing)
        myWritingButton.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToMyWritingFragment(
                isCompletedOnTime = false,
                writingItem = null
            )
            findNavController().navigate(action)
        }

        val sendChallengeButton = view.findViewById<Button>(R.id.btn_send_challenge)
        sendChallengeButton.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToAuthorsFragment()
            findNavController().navigate(action)
        }

        // user view challenges sent to him or herself
        val challengesButton = view.findViewById<Button>(R.id.btn_challenges)
        challengesButton.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToChallengesFragment()
            findNavController().navigate(action)
        }

        // Logout when user presses button
        val logoutButton = view.findViewById<Button>(R.id.btn_logout)
        logoutButton.setOnClickListener {
            signOut()
        }

        // Get email and username
        val email = getEmail()
        // Username is same as FirebaseUI display name
        val username = getUsername()

        if (email != null && username != null) {
            // Add user to Firestore
            // Create a new user with ID and username
            val user = hashMapOf(
                MAP_USERNAME to username
            )
            // User document ID is same as email
            // Add a new document with a generated ID
            db.collection(COLLECTION_USERS)
                .document(email)
                .set(user)
                .addOnSuccessListener {
                    Timber.d("user added: %s", user.get(MAP_USERNAME))
                }
                .addOnFailureListener { e ->
                    Timber.w(e, "Error adding user")
                }
        } else {
            Timber.d("display name: %s", username)
            Timber.d("email: %s", email)
            Toasty.error(this@HomeFragment.requireContext(), R.string.error_user_info, Toast.LENGTH_LONG).show()
        }
    }

    private fun signOut() {
        AuthUI.getInstance()
            .signOut(this@HomeFragment.requireContext())
            .addOnCompleteListener {
                Toasty.normal(this@HomeFragment.requireContext(), R.string.user_sign_out, Toast.LENGTH_LONG).show()
                Timber.d("logged out")

                // Go to sign in
                val action = HomeFragmentDirections.actionHomeFragmentToSignInFragment()
                findNavController().navigate(action)
            }
            .addOnFailureListener() {
                Timber.e(it)
                Toasty.error(this@HomeFragment.requireContext(), R.string.error_sign_out, Toast.LENGTH_LONG).show()
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
}