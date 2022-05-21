package com.michaelhsieh.writingimprov.mychallenges

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.michaelhsieh.writingimprov.ChallengeItem
import com.michaelhsieh.writingimprov.home.HomeFragment
import com.michaelhsieh.writingimprov.home.HomeFragment.Companion.COLLECTION_CHALLENGES
import com.michaelhsieh.writingimprov.R
import es.dmoral.toasty.Toasty
import timber.log.Timber
import java.util.*


/**
 * Display clickable list of challenges displaying who challenge is from and completion status.
 *
 */

class ChallengesFragment:Fragment(R.layout.fragment_challenges),
    ChallengesAdapter.ItemClickListener {

    private lateinit var adapter: ChallengesAdapter

    var db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // data to populate the RecyclerView with
        val challengeItems: ArrayList<ChallengeItem> = ArrayList()

        // Swipe to delete
        val itemTouchHelperCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val email = getEmail()
                    if (email != null) {
                        val challengeToDelete = adapter.getItem(viewHolder.adapterPosition)
                        createDeleteConfirmationDialog(challengeToDelete, viewHolder, challengeItems, email)
                    } else {
                        Timber.d("Email is null")
                        Toasty.error(this@ChallengesFragment.requireContext(),
                            R.string.error_email_deleted_writing
                        ).show()
                    }
                }

            }

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

        // swipe to delete
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

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
                .orderBy("timestamp")
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
        val action = ChallengesFragmentDirections.actionChallengesFragmentToWritingFragment(
            item.time.toInt(),
            item.prompt,
            item.url,
            item.thumbUrl,
            true,
            item.name,
            item.id
        )
        findNavController().navigate(action)
    }

    /**
     * Display a dialog warning user they are deleting a challenge.
     * If user selects yes, remove item from list, update RecyclerView,
     * then remove from Firestore,
     * then display success or failure Toast.
     * If no, then don't remove
     * @param challengeItem: Challenge to delete
     * @param itemViewHolder: RecyclerView ViewHolder
     * @param items: List of challenge items
     * @param userId: Firestore document user ID which is email
     */
    private fun createDeleteConfirmationDialog(challengeItem: ChallengeItem, itemViewHolder: RecyclerView.ViewHolder, items: ArrayList<ChallengeItem>, userId:String) {
        val builder = AlertDialog.Builder(this@ChallengesFragment.requireContext())
        builder.setMessage(getString(R.string.delete_confirmation, challengeItem.name, challengeItem.prompt))
            .setCancelable(false)
            .setPositiveButton(R.string.yes) { dialog, id ->
                deleteChallenge(challengeItem, itemViewHolder, items, userId)
            }
            .setNegativeButton(R.string.no) { dialog, id ->
                // Dismiss the dialog
                dialog.dismiss()

                // Make swiped out view animate back to original position
                adapter.notifyItemChanged(itemViewHolder.adapterPosition)
            }
        val alert = builder.create()
        alert.show()
    }

    /**
     * Remove selected writing item from list, update RecyclerView,
     * remove from Firestore,
     * then display success or failure Toast.
     *
     * @param challengeToDelete: Challenge to delete
     * @param viewHolder: RecyclerView ViewHolder
     * @param challengeItems: List of challenge items
     * @param email: Firestore document user ID which is email
     */
    private fun deleteChallenge(
        challengeToDelete: ChallengeItem, viewHolder:RecyclerView.ViewHolder,
        challengeItems:ArrayList<ChallengeItem>, email:String) {
        challengeItems.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)

        // Start delete from Firestore
        val collection = db.collection(HomeFragment.COLLECTION_USERS)
            .document(email)
            .collection(HomeFragment.COLLECTION_CHALLENGES)

        collection.whereEqualTo("id", challengeToDelete.id)
            .get()
            .addOnSuccessListener {
                for (doc in it.documents) {
                    // Get document ID which is different from writing ID field
                    // and use that to delete document instead
                    val docRefId = doc.id
                    collection.document(docRefId).delete()
                        .addOnSuccessListener {
                            Toasty.info(
                                this@ChallengesFragment.requireContext(),
                                getString(R.string.deleted_challenge, challengeToDelete.name),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Timber.d(it)
                            Toasty.info(
                                this@ChallengesFragment.requireContext(),
                                getString(R.string.error_deleted_challenge, challengeToDelete.name),
                                Toast.LENGTH_SHORT
                            ).show()
                            // Make swiped out view animate back to original position
                            adapter.notifyItemChanged(viewHolder.adapterPosition)
                        }
                }
            }
            .addOnFailureListener {
                Timber.d("Error getting docs where equal to")
                Timber.d(it)

                Toasty.info(
                    this@ChallengesFragment.requireContext(),
                    getString(R.string.error_deleted_challenge, challengeToDelete.name),
                    Toast.LENGTH_SHORT
                ).show()
                // Make swiped out view animate back to original position
                adapter.notifyItemChanged(viewHolder.adapterPosition)
            }
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