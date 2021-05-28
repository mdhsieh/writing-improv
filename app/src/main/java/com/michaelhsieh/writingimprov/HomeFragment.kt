package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

/**
 * Home menu.
 * Displays user option buttons such as practice writing.
 */
class HomeFragment:Fragment(R.layout.fragment_home) {

    var db = FirebaseFirestore.getInstance()
    private val DOC_ID = "my-first-user"
    private val MAP_USERNAME = "username"
    private val MAP_FIRST = "first"
    private val MAP_LAST = "last"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val practiceButton = view.findViewById<Button>(R.id.btn_practice)
        practiceButton.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToPromptFragment()
            findNavController().navigate(action)
        }

        val myWritingButton = view.findViewById<Button>(R.id.btn_my_writing)
        myWritingButton.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToMyWritingFragment(
                isSubmittedChallenge = false,
                isCompletedOnTime = false
            )
            findNavController().navigate(action)
        }

        // Add user to check Firestore works
        // Create a new user with a first and last name
        val user = hashMapOf(
            MAP_USERNAME to "mdhsieh",
            MAP_FIRST to "Michael",
            MAP_LAST to "Hsieh"
        )
        // Add a new document with a generated ID
        db.collection("users")
            .document(DOC_ID)
            .set(user)
            .addOnSuccessListener {
                Timber.d("Document added")
            }
            .addOnFailureListener { e ->
                Timber.w(e, "Error adding document")
            }
    }
}