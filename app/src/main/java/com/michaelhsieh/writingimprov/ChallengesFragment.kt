package com.michaelhsieh.writingimprov

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.michaelhsieh.writingimprov.HomeFragment.Companion.COLLECTION_CHALLENGES
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import es.dmoral.toasty.Toasty
import timber.log.Timber
import java.util.*


/**
 * Display clickable list of challenges displaying who challenge is from and completion status.
 *
 */

class ChallengesFragment:Fragment(R.layout.fragment_challenges), ChallengesAdapter.ItemClickListener {

    private lateinit var adapter: ChallengesAdapter

    var db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // data to populate the RecyclerView with
        val challengeItems: ArrayList<ChallengeItem> = ArrayList()

        val context = this.requireContext()
        // set up the RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.rv_challenges)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ChallengesAdapter(context, challengeItems)
        adapter.setClickListener(this)
        recyclerView.adapter = adapter

        // add a divider between rows
        val dividerItemDecoration = DividerItemDecoration(recyclerView.context,
            (recyclerView.layoutManager as LinearLayoutManager).orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

        val progressBar = view.findViewById<ProgressBar>(R.id.pb_loading_challenges)
        // Get reference to TextView
        val emptyChallengesText = view.findViewById<TextView>(R.id.tv_challenges_empty)

        // Get existing users from Firestore
        val collection = db.collection(HomeFragment.COLLECTION_USERS)

        // get current user ID to get challenges collection
        // ID is same as FirebaseUI email
        val email = getEmail()
        if (email != null) {
            collection
                .document(email)
                .collection(COLLECTION_CHALLENGES)
                .get()
                .addOnSuccessListener {
                    if (it.isEmpty) {
                        Timber.d("Empty list")
                        //Toasty.info(this@AuthorsFragment.requireContext(), "empty list", Toast.LENGTH_LONG).show()
                    } else {
                        // Convert the whole Query Snapshot to a list
                        // of objects directly
                        val items: List<ChallengeItem> =
                            it.toObjects(ChallengeItem::class.java)

                        // Set to list
                        challengeItems.clear()
                        challengeItems.addAll(items)
                        // Timber.d("onSuccess: %s", challengeItems)

                        // Reload RecyclerView
                        adapter.notifyDataSetChanged()

                    }
                    // Hide progress bar
                    progressBar.visibility = View.GONE
                    // Show or hide no writing text
                    setEmptyTextVisibility(challengeItems.size, emptyChallengesText)

                }
                .addOnFailureListener {
                    Timber.e(it)
                    Toasty.error(this.requireContext(), R.string.error_loading_challenges, Toast.LENGTH_LONG).show()

                    // Hide progress bar
                    progressBar.visibility = View.GONE
                }
        }

    }

    /**
     * Show text to inform user if no challenges available
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
        // Toasty.info(this@ChallengesFragment.requireContext(), "You clicked " + adapter.getItem(position).name, Toast.LENGTH_LONG).show()
        val item = adapter.getItem(position)
        val action = ChallengesFragmentDirections.actionChallengesFragmentToWritingFragment(item.time.toInt(), item.prompt, item.url, item.thumbUrl, true, item.name, item.id)
        findNavController().navigate(action)
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
}