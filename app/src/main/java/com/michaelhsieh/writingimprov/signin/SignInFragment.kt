package com.michaelhsieh.writingimprov.signin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.michaelhsieh.writingimprov.R
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

    /**
     * Display this Fragment's own separate options menu,
     * since need to hide options if user not signed in.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * Hide settings option when user not signed in.
     */
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val item:MenuItem = menu.findItem(R.id.settingsFragment)
        item.isVisible = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sign in when button clicked
        val signInButton = view.findViewById<Button>(R.id.btn_sign_in)
        signInButton.setOnClickListener {
            signIn()
        }
        // Do exact same sign in when create account button clicked
        // FirebaseUI sign-in by default has create account email, name, password prompt as long as
        // user never signed in before
        val createAccountButton = view.findViewById<Button>(R.id.btn_create_account)
        createAccountButton.setOnClickListener {
            signIn()
        }

        // If user already signed in, go to HomeFragment
        navToHomeIfSignedIn()
    }

    /**
     * Launch sign in Activity.
     */
    private fun signIn() {
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        // Create and launch sign-in intent
        // DISABLE smart lock because causing sign in error
        // 2022-05-20 21:20:28.482 10865-10865/com.michaelhsieh.writingimprov E/AuthUI: A sign-in error occurred.
        //    com.firebase.ui.auth.FirebaseUiException: Error when saving credential.
        //        at com.firebase.ui.auth.viewmodel.smartlock.SmartLockHandler$1.onComplete(SmartLockHandler.java:99)
        // ...
        // Caused by: com.google.android.gms.common.api.ApiException: 16: Cannot find an eligible account.
        // ...
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    /**
     * Go to next Fragment when sign in successful.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                navToHomeIfSignedIn()
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                if (response != null) {
                    Timber.e(response.error?.errorCode.toString())
                    Toasty.error(this@SignInFragment.requireContext(), R.string.error_sign_in, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navToHomeIfSignedIn() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            Timber.d("user display name: %s", user.displayName.toString())
            Timber.d("user email: %s", user.email.toString())
            Toasty.normal(this@SignInFragment.requireContext(), getString(R.string.user_sign_in, user.displayName), Toast.LENGTH_LONG).show()

            // Navigate to home
            val action = SignInFragmentDirections.actionSignInFragmentToHomeFragment()
            findNavController().navigate(action)
        }
    }
}