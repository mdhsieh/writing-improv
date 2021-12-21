package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import es.dmoral.toasty.Toasty
import timber.log.Timber
import java.util.*


/**
 * Display clickable list of authors which user can challenge.
 *
 */

class AuthorsFragment:Fragment(R.layout.fragment_authors), AuthorsAdapter.ItemClickListener {

    private lateinit var adapter: AuthorsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // data to populate the RecyclerView with
        val authorItems: ArrayList<AuthorItem> = ArrayList()

        authorItems.add(AuthorItem("mdhsieh8@gmail.com", "mhdev"))
        authorItems.add(AuthorItem("michaelhsieh1997@gmail.com", "mh2blue"))

        val context = this.requireContext()
        // set up the RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.rv_authors)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = AuthorsAdapter(context, authorItems)
        adapter.setClickListener(this)
        recyclerView.adapter = adapter

        // add a divider between rows
        val dividerItemDecoration = DividerItemDecoration(recyclerView.context,
            (recyclerView.layoutManager as LinearLayoutManager).orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

        val progressBar = view.findViewById<ProgressBar>(R.id.pb_loading_authors)
        // Hide progress bar
        progressBar.visibility = View.GONE
        // Get reference to TextView
        val emptyAuthorsText = view.findViewById<TextView>(R.id.tv_authors_empty)
        // Show or hide no writing text
        setEmptyTextVisibility(authorItems.size, emptyAuthorsText)
    }

    /**
     * Show text to inform user if no writing available
     * @param numItems Number of items in list
     * @param textView The TextView displayed to user
     */
    private fun setEmptyTextVisibility(numItems:Int, textView: TextView) {
        if (numItems > 0) {
            textView.visibility = View.GONE
        } else {
            textView.visibility = View.VISIBLE
        }
    }

    override fun onItemClick(view: View?, position: Int) {
        Toasty.info(this@AuthorsFragment.requireContext(), "hello clicked", Toast.LENGTH_LONG).show()
    }
}