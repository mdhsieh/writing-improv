package com.michaelhsieh.writingimprov

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class HomeFragment:Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val practiceButton = view.findViewById<Button>(R.id.btn_practice)
        practiceButton.setOnClickListener {
            val intent = Intent(activity, PromptActivity::class.java)
            startActivity(intent)
        }
    }
}