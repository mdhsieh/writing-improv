package com.michaelhsieh.writingimprov

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import es.dmoral.toasty.Toasty

/**
 * Displays all writing user has submitted.
 * Displays a Toast if user has submitted writing from previous Fragment.
 */
class MyWritingFragment : Fragment(R.layout.fragment_my_writing) {

    private val args: MyWritingFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val successIcon:Drawable? = ResourcesCompat.getDrawable(requireActivity().resources, R.drawable.ic_check_circle_outline_72, null)
        val failIcon:Drawable? = ResourcesCompat.getDrawable(requireActivity().resources, R.drawable.ic_highlight_off_72, null)

        // Get booleans from previous Fragment.
        // Show Toast only if writing was submitted from a challenge.
        // If user completed by time limit, show success Toast
        // Otherwise, show fail Toast
        if (args.isSubmittedChallenge) {
            if (args.isCompletedOnTime) {
                Toasty.normal(this.requireContext(), getString(R.string.on_time), Toast.LENGTH_LONG, successIcon).show()
            } else {
                Toasty.normal(this.requireContext(), getString(R.string.out_of_time), Toast.LENGTH_LONG, failIcon).show()
            }
        }

        val menuButton = view.findViewById<Button>(R.id.btn_menu)
        menuButton.setOnClickListener {
            val action = MyWritingFragmentDirections.actionMyWritingFragmentToHomeFragment()
            findNavController().navigate(action)
        }
    }
}