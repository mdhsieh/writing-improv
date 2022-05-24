package com.michaelhsieh.writingimprov.home

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.michaelhsieh.writingimprov.MainActivity
import com.michaelhsieh.writingimprov.MainActivity.Companion.isListeningForChallenges
import com.michaelhsieh.writingimprov.R
import es.dmoral.toasty.Toasty
import timber.log.Timber


/**
 * Home menu.
 * Displays user option buttons such as view user's writing and practice writing.
 *
 */
class HomeFragment:Fragment(R.layout.fragment_home) {

    var db = FirebaseFirestore.getInstance()

    companion object {
        val MAP_USERNAME = "username"
        val COLLECTION_USERS = "users"
        val COLLECTION_WRITING = "writing"
        val COLLECTION_CHALLENGES = "challenges"
    }

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = Firebase.analytics

        val practiceButton = view.findViewById<Button>(R.id.btn_practice)
        practiceButton.setOnClickListener {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.ITEM_ID, "home_1")
                param(FirebaseAnalytics.Param.ITEM_NAME, "practice")
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "practice_button")
            }
            val action = HomeFragmentDirections.actionHomeFragmentToPromptFragment()
            findNavController().navigate(action)
        }

        // WritingItem is null since user is not submitting any writing
        val myWritingButton = view.findViewById<Button>(R.id.btn_my_writing)
        myWritingButton.setOnClickListener {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.ITEM_ID, "home_2")
                param(FirebaseAnalytics.Param.ITEM_NAME, "my_writing")
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "my_writing_button")
            }
            val action = HomeFragmentDirections.actionHomeFragmentToMyWritingFragment(
                isCompletedOnTime = false,
                writingItem = null
            )
            findNavController().navigate(action)
        }

        val sendChallengeButton = view.findViewById<Button>(R.id.btn_send_challenge)
        sendChallengeButton.setOnClickListener {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.ITEM_ID, "home_3")
                param(FirebaseAnalytics.Param.ITEM_NAME, "send_challenge")
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "send_challenge_button")
            }
            val action = HomeFragmentDirections.actionHomeFragmentToAuthorsFragment()
            findNavController().navigate(action)
        }

        // user view challenges sent to him or herself
        val challengesButton = view.findViewById<Button>(R.id.btn_challenges)
        challengesButton.setOnClickListener {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.ITEM_ID, "home_4")
                param(FirebaseAnalytics.Param.ITEM_NAME, "my_challenges")
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "my_challenges_button")
            }
            val action = HomeFragmentDirections.actionHomeFragmentToChallengesFragment()
            findNavController().navigate(action)
        }

        // user review writings from  challenges he or she has sent to other users
        val reviewSentChallengesButton = view.findViewById<Button>(R.id.btn_review_sent_challenges)
        reviewSentChallengesButton.setOnClickListener {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.ITEM_ID, "home_5")
                param(FirebaseAnalytics.Param.ITEM_NAME, "review_sent_challenges")
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "review_sent_challenges_button")
            }
            val action = HomeFragmentDirections.actionHomeFragmentToSentChallengesFragment()
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

        // When the user first signs in, listenForChallengesChange() getting email in MainActivity
        // will fail because the first Fragment is SignInFragment which requires entering that email.
        // If first signing in, call listenForChallengesChange() here instead. Then
        // set global MainActivity boolean to false in order to not
        // repeatedly call listenForChallengesChange() every time HomeFragment view is created.
        if (email != null && !isListeningForChallenges) {
            Timber.d("HomeFragment calling listen for changes")
            val activity = this.activity as MainActivity
            activity.listenForChallengesChange(email)
            isListeningForChallenges = true
        } else if (email == null) {
            Timber.d("HomeFragment email is null")
        } else {
            Timber.d("HomeFragment email is not null and isListeningForChallenges is %s", isListeningForChallenges)
        }

        // When user presses back button, sign the user out
        this.requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Timber.d("Fragment back pressed invoked")
                    // Do custom work here
                    signOut()

                    // if you want onBackPressed() to be called as normal afterwards
//                    if (isEnabled) {
//                        isEnabled = false
//                        requireActivity().onBackPressed()
//                    }
                }
            })
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