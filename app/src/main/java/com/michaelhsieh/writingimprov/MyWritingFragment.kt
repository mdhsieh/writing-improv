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
import com.google.firebase.firestore.FirebaseFirestore
import es.dmoral.toasty.Toasty
import timber.log.Timber


/**
 * Displays all writing user has submitted.
 * Displays a Toast if user has submitted writing from previous Fragment.
 */
class MyWritingFragment : Fragment(R.layout.fragment_my_writing), MyWritingAdapter.ItemClickListener {

    private val args: MyWritingFragmentArgs by navArgs()

    var db = FirebaseFirestore.getInstance()
    private val DOC_ID = "my-first-user"
    private val MAP_USERNAME = "username"
    private val MAP_FIRST = "first"
    private val MAP_LAST = "last"

    private val COLLECTION_WRITING = "writing"

    private lateinit var adapter: MyWritingAdapter

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

        // Add user to check Firestore works
        // Create a new user with a first and last name
        val user = hashMapOf(
            MAP_USERNAME to "mdhsieh",
            MAP_FIRST to "Michael",
            MAP_LAST to "Hsieh"
        )
        // Add a new document with a generated ID
        db.collection("users")
            .document(DOC_ID)
            .set(user)
            .addOnSuccessListener {
                Timber.d("user added")
            }
            .addOnFailureListener { e ->
                Timber.w(e, "Error adding user")
            }

        // Get existing writings by user from Firestore
        val collection = db.collection("users")
            .document(DOC_ID)
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


                // Add submitted writing from previous Fragment
                val item = args.writingItem
                if (item != null) {
                    Timber.d("Receiving: %s", item.toString())

                    Timber.d("list: %s", writingItems)
                    // Don't add item if it already exists in list.
                    // add() methods are called again after device rotated
                    if (!isItemIdSame(item, writingItems)) {
                        Timber.d("%s id is different: %s", item.prompt, item.id)
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
    }

    override fun onItemClick(view: View?, position: Int) {
        Toast.makeText(
            this.requireContext(),
            "You clicked " + adapter.getItem(position).name + " on row number " + position,
            Toast.LENGTH_SHORT
        ).show()
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
}