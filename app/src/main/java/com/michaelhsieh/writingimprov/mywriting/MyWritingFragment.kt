package com.michaelhsieh.writingimprov.mywriting

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.michaelhsieh.writingimprov.home.HomeFragment.Companion.COLLECTION_USERS
import com.michaelhsieh.writingimprov.home.HomeFragment.Companion.COLLECTION_WRITING
import com.michaelhsieh.writingimprov.R
import com.michaelhsieh.writingimprov.WritingItem
import es.dmoral.toasty.Toasty
import timber.log.Timber


/**
 * Displays all writing user has submitted.
 * 
 */
class MyWritingFragment : Fragment(R.layout.fragment_my_writing),
    MyWritingAdapter.ItemClickListener {

    private val args: MyWritingFragmentArgs by navArgs()

    var db = FirebaseFirestore.getInstance()

    private lateinit var adapter: MyWritingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // data to populate the RecyclerView with
        val writingItems: ArrayList<WritingItem> = ArrayList()

        // Swipe to delete
        val itemTouchHelperCallback =
            object :ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
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
                    val writingToDelete = adapter.getItem(viewHolder.adapterPosition)
                    createDeleteConfirmationDialog(writingToDelete, viewHolder, writingItems, email)
                } else {
                    Timber.d("Email is null")
                    Toasty.error(this@MyWritingFragment.requireContext(),
                        R.string.error_email_deleted_writing
                    ).show()
                }
            }

        }

        val context = this.requireContext()
        // set up the RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.rv_my_writing)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = MyWritingAdapter(context, writingItems)
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

        // Make visible
        val progressBar = view.findViewById<ProgressBar>(R.id.pb_loading_my_writing)
        progressBar.visibility = View.VISIBLE
        // Get reference to TextView
        val emptyWritingText = view.findViewById<TextView>(R.id.tv_my_writing_empty)
        emptyWritingText.visibility = View.GONE

        val email = getEmail()

        if (email != null) {
            // Get existing writings by user from Firestore
            val collection = db.collection(COLLECTION_USERS)
                .document(email)
                .collection(COLLECTION_WRITING)

            // Order by time created
            collection
//                .orderBy("timeStamp")
                .get()
                .addOnSuccessListener {

                    if (it.isEmpty) {
                        Timber.d("Empty list")
                    } else {
                        // Convert the whole Query Snapshot to a list
                        // of objects directly
                        val items: List<WritingItem> =
                            it.toObjects(WritingItem::class.java)

                        // Set to list
                        writingItems.clear()
                        writingItems.addAll(items)
                        Timber.d("onSuccess: %s", writingItems)

                    }


                    // Add submitted writing from previous Fragment.
                    // Will be null if previous was HomeFragment.
                    val item = args.writingItem
                    if (item != null) {
                        Timber.d("Receiving: %s", item.toString())

                        // Don't add item if it already exists in list.
                        // add() methods are called again after device rotated
                        if (!isItemIdSame(item, writingItems)) {
                            writingItems.add(item)

                            // Add to user's writing collection
                            collection.add(item)
                        }
                    }
                    // Reload RecyclerView
                    adapter.notifyDataSetChanged()

                    // Show or hide no writing text
                    setEmptyTextVisibility(writingItems.size, emptyWritingText)
                    // Hide progress bar
                    progressBar.visibility = View.GONE
                }
                .addOnFailureListener {
                    Timber.e(it)
                    Toasty.error(this.requireContext(), R.string.error_loading_my_writing, Toast.LENGTH_LONG).show()

                    // Hide progress bar
                    progressBar.visibility = View.GONE
                }
        } else {
            Timber.d("email: %s", email)
            Toasty.error(this@MyWritingFragment.requireContext(), R.string.error_user_info, Toast.LENGTH_LONG).show()
        }
    }

    override fun onItemClick(view: View?, position: Int) {
        val item = adapter.getItem(position)
        val action =
            MyWritingFragmentDirections.actionMyWritingFragmentToMyWritingDetailsFragment(item)
        findNavController().navigate(action)
    }

    /**
     * Display a dialog warning user they are deleting a writing.
     * If user selects yes, remove item from list, update RecyclerView,
     * then remove from Firestore,
     * then display success or failure Toast.
     * If no, then don't remove
     * @param writingItem: Writing to delete
     * @param itemViewHolder: RecyclerView ViewHolder
     * @param items: List of writing items
     * @param userId: Firestore document user ID which is email
     */
    private fun createDeleteConfirmationDialog(writingItem: WritingItem, itemViewHolder: RecyclerView.ViewHolder, items: ArrayList<WritingItem>, userId:String) {
        val builder = AlertDialog.Builder(this@MyWritingFragment.requireContext())
        builder.setMessage(getString(R.string.delete_confirmation, writingItem.name, writingItem.prompt))
            .setCancelable(false)
            .setPositiveButton(R.string.yes) { dialog, id ->
                deleteWriting(writingItem, itemViewHolder, items, userId)
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
     * @param writingToDelete: Writing to delete
     * @param viewHolder: RecyclerView ViewHolder
     * @param writingItems: List of writing items
     * @param email: Firestore document user ID which is email
     */
    private fun deleteWriting(
        writingToDelete: WritingItem, viewHolder:RecyclerView.ViewHolder,
        writingItems:ArrayList<WritingItem>, email:String) {
        writingItems.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)

        // Start delete from Firestore
        val collection = db.collection(COLLECTION_USERS)
            .document(email)
            .collection(COLLECTION_WRITING)

        collection.whereEqualTo("id", writingToDelete.id)
            .get()
            .addOnSuccessListener {
                for (doc in it.documents) {
                    // Get document ID which is different from writing ID field
                    // and use that to delete document instead
                    val docRefId = doc.id
                    collection.document(docRefId).delete()
                        .addOnSuccessListener {
                            Toasty.info(
                                this@MyWritingFragment.requireContext(),
                                getString(R.string.deleted_writing, writingToDelete.name),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Timber.d(it)
                            Toasty.info(
                                this@MyWritingFragment.requireContext(),
                                getString(R.string.error_deleted_writing, writingToDelete.name),
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
                    this@MyWritingFragment.requireContext(),
                    getString(R.string.error_deleted_writing, writingToDelete.name),
                    Toast.LENGTH_SHORT
                ).show()
                // Make swiped out view animate back to original position
                adapter.notifyItemChanged(viewHolder.adapterPosition)
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

    /**
     * Whether an item has the same ID as any item in a list.
     * @param item The item
     * @param itemList The list to compare with
     * @return true if any IDs match, false otherwise
     */
    private fun isItemIdSame(item: WritingItem, itemList:List<WritingItem>):Boolean {
        for (i in itemList) {
            if (i.id == item.id) {
                return true
            }
        }
        return false
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