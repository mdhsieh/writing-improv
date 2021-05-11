package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Submitted writing on time.
 */
class CompletedOnTimeFragment:Fragment(R.layout.fragment_completed_on_time) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // When press back, go to menu instead of writing
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val action = CompletedOnTimeFragmentDirections.actionCompletedOnTimeFragmentToHomeFragment()
                findNavController().navigate(action)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuButton = view.findViewById<Button>(R.id.btn_menu)
        menuButton.setOnClickListener {
            val action = CompletedOnTimeFragmentDirections.actionCompletedOnTimeFragmentToHomeFragment()
            findNavController().navigate(action)
        }
    }
}