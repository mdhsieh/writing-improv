package com.michaelhsieh.writingimprov

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.michaelhsieh.writingimprov.HomeFragment.Companion.COLLECTION_USERS
import com.michaelhsieh.writingimprov.HomeFragment.Companion.COLLECTION_WRITING
import es.dmoral.toasty.Toasty
import timber.log.Timber


/**
 * Displays all writing user has submitted.
 * Displays a Toast if user has submitted writing from previous Fragment.
 */
class MyWritingFragment : Fragment(R.layout.fragment_my_writing), MyWritingAdapter.ItemClickListener {

    private val args: MyWritingFragmentArgs by navArgs()

    var db = FirebaseFirestore.getInstance()

    private lateinit var adapter: MyWritingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val successIcon:Drawable? = ResourcesCompat.getDrawable(requireActivity().resources, R.drawable.ic_check_circle_outline_72, null)
        val failIcon:Drawable? = ResourcesCompat.getDrawable(requireActivity().resources, R.drawable.ic_highlight_off_72, null)

        // Get booleans from previous Fragment.
        // Show Toast only if writing was submitted from a challenge.
        // If user completed by time limit, show success Toast
        // Otherwise, show fail Toast
        if (args.writingItem != null) {
            if (args.isCompletedOnTime) {
                Toasty.normal(this.requireContext(), getString(R.string.on_time), Toast.LENGTH_LONG, successIcon).show()
            } else {
                Toasty.normal(this.requireContext(), getString(R.string.out_of_time), Toast.LENGTH_LONG, failIcon).show()
            }
        }

        // data to populate the RecyclerView with
        val writingItems: ArrayList<WritingItem> = ArrayList()

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

            collection
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
        val action = MyWritingFragmentDirections.actionMyWritingFragmentToMyWritingDetailsFragment(item)
        findNavController().navigate(action)
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

    /** Return the user's email if signed in.
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