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
 * First screen shown to user.
 * Has sign in functionality.
 */
class SignInFragment:Fragment(R.layout.fragment_sign_in) {

    // Sign in response code
    companion object {
        private const val RC_SIGN_IN = 123
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sign in when button clicked
        val signInButton = view.findViewById<Button>(R.id.btn_sign_in)
        signInButton.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
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
                    Toasty.normal(this@SignInFragment.requireContext(), getString(R.string.user_sign_in, user.displayName), Toast.LENGTH_LONG).show()

                    // Navigate to home
                    val action = SignInFragmentDirections.actionSignInFragmentToHomeFragment()
                    findNavController().navigate(action)
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
}