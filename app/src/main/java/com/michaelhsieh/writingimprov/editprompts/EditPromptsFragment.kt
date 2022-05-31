package com.michaelhsieh.writingimprov.editprompts

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.michaelhsieh.writingimprov.PromptItem
import com.michaelhsieh.writingimprov.R
import com.michaelhsieh.writingimprov.WritingItem
import com.michaelhsieh.writingimprov.home.HomeFragment
import com.michaelhsieh.writingimprov.mywriting.MyWritingAdapter
import es.dmoral.toasty.Toasty
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

class EditPromptsFragment : Fragment(R.layout.fragment_edit_prompts),
EditPromptsAdapter.ItemClickListener {

    private lateinit var adapter: EditPromptsAdapter

    // Show progress bar while getting prompts
    private lateinit var progressBar: ProgressBar

    private lateinit var editText: EditText
    private lateinit var addPromptButton: Button

    val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // data to populate the RecyclerView with
        val promptItems: ArrayList<PromptItem> = ArrayList()

        val context = this.requireContext()
        // set up the RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.rv_my_prompts)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = EditPromptsAdapter(context, promptItems)
        adapter.setClickListener(this)
        recyclerView.adapter = adapter

        // add a divider between rows
        val dividerItemDecoration = DividerItemDecoration(recyclerView.context,
            (recyclerView.layoutManager as LinearLayoutManager).orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

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
                        val promptToDelete = adapter.getItem(viewHolder.adapterPosition)
                        if (adapter.itemCount <= 1) {
                            Toasty.error(this@EditPromptsFragment.requireContext(),
                                R.string.error_delete_all_prompts
                            ).show()
                            // Make swiped out view animate back to original position
                            adapter.notifyItemChanged(viewHolder.adapterPosition)
                        } else {
                            deletePrompt(promptToDelete, viewHolder, promptItems, email)
                        }
                    } else {
                        Timber.d("Email is null")
                        Toasty.error(this@EditPromptsFragment.requireContext(),
                            R.string.error_email_deleted_writing
                        ).show()
                    }
                }

            }

        progressBar = view.findViewById(R.id.pb_loading_prompts)
        progressBar.visibility = View.VISIBLE

        getUserPrompts(promptItems, progressBar)

        // swipe to delete
        // swipe to delete
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Add user's new custom prompt to Firestore and display
        editText = view.findViewById(R.id.et_new_prompt)
        addPromptButton = view.findViewById(R.id.btn_add_prompt)
        addPromptButton.setOnClickListener {
            if (editText.text.isEmpty()) {
                // @EditPromptsFragment
                Toasty.error(this.requireContext(), getString(R.string.error_create_prompt_empty), Toast.LENGTH_LONG).show()
            } else {
                val newPromptItem = PromptItem(
                    id = UUID.randomUUID().toString(),
                    prompt = editText.text.toString(),
                    timestamp = System.currentTimeMillis() / 1000
                )
                // Add to RecyclerView
                promptItems.add(newPromptItem)
                adapter.notifyItemChanged(promptItems.size - 1)
                // Automatically scroll to show user where new prompt is
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPosition(promptItems.size - 1)
                // Add to user's custom prompts collection
                val email = getEmail()
                if (email != null) {
                    db.collection(HomeFragment.COLLECTION_USERS)
                        .document(email)
                        .collection(HomeFragment.COLLECTION_PROMPTS)
                        .add(newPromptItem)
                        .addOnSuccessListener {
                            Timber.d("Successfully added prompt: %s", newPromptItem.prompt)
                        }
                        .addOnFailureListener {
                            // Toast to let user know prompt wasn't saved
                            Toasty.error(this.requireContext(), getString(R.string.error_create_prompt_empty), Toast.LENGTH_LONG).show()
                        }
                }

            }
        }
    }

    private fun deletePrompt(promptToDelete: PromptItem, viewHolder:RecyclerView.ViewHolder,
                             promptItems:ArrayList<PromptItem>, email:String) {
        promptItems.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)

        // Start delete from Firestore
        val collection = db.collection(HomeFragment.COLLECTION_USERS)
            .document(email)
            .collection(HomeFragment.COLLECTION_PROMPTS)

        collection.whereEqualTo("id", promptToDelete.id)
            .get()
            .addOnSuccessListener {
                for (doc in it.documents) {
                    // Get Firestore auto-generated document ID which is different from prompt ID field
                    // and use that to delete document instead
                    val docRefId = doc.id
                    collection.document(docRefId).delete()
                        .addOnSuccessListener {
                            Toasty.info(
                                this@EditPromptsFragment.requireContext(),
                                getString(R.string.deleted_prompt, promptToDelete.prompt),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Timber.d(it)
                            Toasty.info(
                                this@EditPromptsFragment.requireContext(),
                                getString(R.string.error_deleted_prompt, promptToDelete.prompt),
                                Toast.LENGTH_SHORT
                            ).show()
                            // Make swiped out view animate back to original position
                            adapter.notifyItemChanged(viewHolder.adapterPosition)
                        }
                }
            }
            .addOnFailureListener {
                Timber.d("Error getting docs where equal to id")
                Timber.d(it)

                Toasty.info(
                    this@EditPromptsFragment.requireContext(),
                    getString(R.string.error_deleted_prompt, promptToDelete.prompt),
                    Toast.LENGTH_SHORT
                ).show()
                // Make swiped out view animate back to original position
                adapter.notifyItemChanged(viewHolder.adapterPosition)
            }
    }

    /** Generates a random prompt from user's custom prompts collection, or
     * if not available, generates from a newly created set of default prompts. */
    private fun getUserPrompts(promptData: ArrayList<PromptItem>, pBar: ProgressBar) {
        val email = getEmail()
        if (email != null) {
            db.collection(HomeFragment.COLLECTION_USERS)
                .document(email)
                .collection(HomeFragment.COLLECTION_PROMPTS)
                .get()
                .addOnCompleteListener(OnCompleteListener<QuerySnapshot?> { task ->
                    if (task.isSuccessful) {
                        if (task.result?.size()!! > 0) {
                            for (document in task.result!!) {
                                Timber.d("Prompts already exist. Get from Firestore")
                                getPromptsFromFirestore(email, promptData, progressBar)
                            }
                        } else {
                            Timber.d("No prompts exist. Create a new prompts collection")
                            createDefaultPrompts(email, promptData, progressBar)
                        }
                    } else {
                        Timber.d("Error getting practice prompts: %s", task.exception)
                        Toasty.error(this.requireContext(), getString(R.string.error_loading_prompts), Toast.LENGTH_LONG).show()

                        progressBar.visibility = View.GONE
                    }
                })
        }
    }

    /**
     * Creates a new collection in Firestore containing default prompts from String resources.
     * Then generates a random prompt from this collection.
     */
    private fun createDefaultPrompts(userId:String, promptData: ArrayList<PromptItem>, pBar: ProgressBar) {
        val promptArray = arrayOf(
            getString(R.string.prompt_feel),
            getString(R.string.prompt_story),
            getString(R.string.prompt_mystery),
            getString(R.string.prompt_action),
            getString(R.string.prompt_thriller),
            getString(R.string.prompt_comedy)
        )

        val collection = db.collection(HomeFragment.COLLECTION_USERS)
            .document(userId)
            .collection(HomeFragment.COLLECTION_PROMPTS)

        var counter = 0
        for (text in promptArray) {
            val promptItem = PromptItem(
                id = UUID.randomUUID().toString(),
                prompt = text,
                timestamp = System.currentTimeMillis() / 1000
            )
            collection.add(promptItem).addOnCompleteListener {
                if (counter == promptArray.size) {
                    Timber.d("Done adding default prompts")
                    getPromptsFromFirestore(userId, promptData, pBar)
                }
                counter += 1
            }
        }

    }

    /** Gets prompt collection from Firestore. */
    private fun getPromptsFromFirestore(userId: String, promptData: ArrayList<PromptItem>, pBar: ProgressBar) {
        val collection = db.collection(HomeFragment.COLLECTION_USERS)
            .document(userId)
            .collection(HomeFragment.COLLECTION_PROMPTS)

        collection
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener {
                // Convert the whole Query Snapshot to a list
                // of objects directly
                val items: List<PromptItem> =
                    it.toObjects(PromptItem::class.java)

                Timber.d("Got %s items", items.size)
                // update RecyclerView
                // reset prompts in case called previously
                promptData.clear()
                promptData.addAll(items)
                adapter.notifyDataSetChanged()
                // done loading and hide progress bar
                pBar.visibility = View.GONE
            }.addOnFailureListener {
                Timber.e(it)
                Toasty.error(this.requireContext(), R.string.error_loading_prompts, Toast.LENGTH_LONG).show()

                pBar.visibility = View.GONE
            }
    }

    override fun onItemClick(view: View?, position: Int) {
        val item = adapter.getItem(position)
        Timber.d("Clicked %s at position %s", item.prompt, position)
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