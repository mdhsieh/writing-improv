package com.michaelhsieh.writingimprov

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import es.dmoral.toasty.Toasty
import timber.log.Timber

/**
 * Home menu.
 * Displays user option buttons such as view user's writing and practice writing.
 * Has sign in functionality.
 */
class HomeFragment:Fragment(R.layout.fragment_home) {

    // Sign in response code
    companion object {
        private const val RC_SIGN_IN = 123
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
                isSubmittedChallenge = false,
                isCompletedOnTime = false,
                writingItem = null
            )
            findNavController().navigate(action)
        }

        // Sign in when app first started
        login()

        // Logout when user presses button
        val logoutButton = view.findViewById<Button>(R.id.btn_logout)
        logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun login() {
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    Timber.d("user display name: %s", user.displayName)
                    Timber.d("user email: %s", user.email)
                    Toasty.normal(this@HomeFragment.requireContext(), getString(R.string.user_logged_in, user.displayName), Toast.LENGTH_LONG).show()
                }
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                if (response != null) {
                    Timber.e(response.error?.errorCode.toString())
                }
            }
        }
    }

    private fun logout() {
        AuthUI.getInstance()
            .signOut(this@HomeFragment.requireContext())
            .addOnCompleteListener {
                Toasty.normal(this@HomeFragment.requireContext(), R.string.user_logged_out, Toast.LENGTH_LONG).show()
                Timber.d("logged out")
            }
            .addOnFailureListener() {
                Timber.e(it)
            }
    }
}