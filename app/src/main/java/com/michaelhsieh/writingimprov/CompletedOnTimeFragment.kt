package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Submitted writing on time.
 */
class CompletedOnTimeFragment:Fragment(R.layout.fragment_completed_on_time) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuButton = view.findViewById<Button>(R.id.btn_menu)
        menuButton.setOnClickListener {
            val action = CompletedOnTimeFragmentDirections.actionCompletedOnTimeFragmentToHomeFragment()
            findNavController().navigate(action)
        }
    }
}