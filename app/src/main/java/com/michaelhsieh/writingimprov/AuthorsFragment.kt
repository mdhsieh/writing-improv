package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
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

    var db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // data to populate the RecyclerView with
        val authorItems: ArrayList<AuthorItem> = ArrayList()

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
        // Get reference to TextView
        val emptyAuthorsText = view.findViewById<TextView>(R.id.tv_authors_empty)

        // Get existing users from Firestore
        val collection = db.collection(HomeFragment.COLLECTION_USERS)

        collection
            .get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    Timber.d("Empty list")
                    //Toasty.info(this@AuthorsFragment.requireContext(), "empty list", Toast.LENGTH_LONG).show()
                } else {

                    // Add item with user name and email
                    for (doc in it.documents) {
                        authorItems.add(AuthorItem(doc.id, doc.data?.get("username") as String))
                    }
                    // Reload RecyclerView
                    adapter.notifyDataSetChanged()

                }
                // Hide progress bar
                progressBar.visibility = View.GONE
                // Show or hide no writing text
                setEmptyTextVisibility(authorItems.size, emptyAuthorsText)

            }
            .addOnFailureListener {
                Timber.e(it)
                Toasty.error(this.requireContext(), R.string.error_loading_authors, Toast.LENGTH_LONG).show()

                // Hide progress bar
                progressBar.visibility = View.GONE
            }
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
        // Toasty.info(this@AuthorsFragment.requireContext(), "You clicked " + adapter.getItem(position).name, Toast.LENGTH_LONG).show()
        val item = adapter.getItem(position)
        val action = AuthorsFragmentDirections.actionAuthorsFragmentToChallengePromptFragment(item)
        findNavController().navigate(action)
    }
}