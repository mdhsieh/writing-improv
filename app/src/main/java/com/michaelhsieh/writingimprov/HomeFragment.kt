package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

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

        val myWritingButton = view.findViewById<Button>(R.id.btn_my_writing)
        myWritingButton.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToMyWritingFragment(
                isSubmittedChallenge = false,
                isCompletedOnTime = false
            )
            findNavController().navigate(action)
        }
    }
}