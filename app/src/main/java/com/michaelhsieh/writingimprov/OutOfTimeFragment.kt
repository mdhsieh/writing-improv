package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Writing not submitted on time.
 */
class OutOfTimeFragment:Fragment(R.layout.fragment_out_of_time) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuButton = view.findViewById<Button>(R.id.btn_menu)
        menuButton.setOnClickListener {
            val action = OutOfTimeFragmentDirections.actionOutOfTimeFragmentToHomeFragment()
            findNavController().navigate(action)
        }
    }
}