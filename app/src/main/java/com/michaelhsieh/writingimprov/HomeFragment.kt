package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import es.dmoral.toasty.Toasty
import timber.log.Timber

/**
 * Home menu.
 * Displays user option buttons such as practice writing.
 */
class HomeFragment:Fragment(R.layout.fragment_home) {

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

        // Logout when user presses button
        val logoutButton = view.findViewById<Button>(R.id.btn_logout)
        logoutButton.setOnClickListener {
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
}