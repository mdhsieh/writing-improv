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
import com.michaelhsieh.writingimprov.HomeFragment.Companion.COLLECTION_WRITING
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import es.dmoral.toasty.Toasty
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList


/**
 * Display clickable list of submitted and not submitted writings
 * from challenges the user has sent to other users.
 *
 */

class SentChallengesFragment:Fragment(R.layout.fragment_sent_challenges), MyWritingAdapter.ItemClickListener {

    private lateinit var adapter: MyWritingAdapter

    var db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // data to populate the RecyclerView with
        val writingItems: ArrayList<WritingItem> = ArrayList()

        val context = this.requireContext()
        // set up the RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.rv_sent_challenges)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = MyWritingAdapter(context, writingItems)
        adapter.setClickListener(this)
        recyclerView.adapter = adapter

        // add a divider between rows
        val dividerItemDecoration = DividerItemDecoration(recyclerView.context,
            (recyclerView.layoutManager as LinearLayoutManager).orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

        val progressBar = view.findViewById<ProgressBar>(R.id.pb_loading_sent_challenges)
        // Get reference to TextView
        val emptySentChallengesText = view.findViewById<TextView>(R.id.tv_sent_challenges_empty)

        // Get existing users from Firestore
        val collection = db.collection(HomeFragment.COLLECTION_USERS)

        // get current user ID
        // to get all challenges with that sender ID
        // ID is same as FirebaseUI email
        val email = getEmail()
        if (email != null) {
            collection
                .get()
                .addOnSuccessListener {

                    val otherUserIds = arrayListOf<String>()
                    // Get all user IDs which aren't user himself or herself
                    for (doc in it.documents) {
                        if (doc.id != email) {
                            otherUserIds.add(doc.id)
                        }
                    }

                    findAllChallengesWithSenderEmail(email, otherUserIds, writingItems, progressBar, emptySentChallengesText)

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
        Toasty.info(this@SentChallengesFragment.requireContext(), "You clicked " + adapter.getItem(position).name, Toast.LENGTH_LONG).show()
        // val item = adapter.getItem(position)
        // val action = ChallengesFragmentDirections.actionChallengesFragmentToWritingFragment(item.time.toInt(), item.prompt, item.url, item.thumbUrl, true, item.name, item.id)
        // findNavController().navigate(action)
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

    /**
     * Get all challenges with senderId matching current user email.
     * Then for each challenge's ID find all writing from other users with that ID
     * @param senderId Current user email
     * @param otherIds Every users' email excluding current user, as Strings in an ArrayList
     * @param writings ArrayList to append other users' writings from the challenges
     */
    private fun findAllChallengesWithSenderEmail(senderId:String, otherIds:ArrayList<String>, writings:ArrayList<WritingItem>, pBar:ProgressBar, emptyTextView: TextView) {
        val challenges = arrayListOf<ChallengeItem>()

        // Get existing users from Firestore
        val collection = db.collection(HomeFragment.COLLECTION_USERS)

        // For each user ID, get all challenge collection items which
        // have current user email as senderId
        for ((i, otherId) in otherIds.withIndex()) {
            collection
                .document(otherId)
                .collection(COLLECTION_CHALLENGES)
                .whereEqualTo("senderId", senderId)
                .get()
                .addOnSuccessListener {

                    // Convert the whole Query Snapshot to a list
                    // of objects directly
                    val items: List<ChallengeItem> =
                        it.toObjects(ChallengeItem::class.java)

                    // Set to list
                    challenges.addAll(items)
                    Toasty.info(this.requireContext(), "found challenges: " + items.size.toShort(), Toast.LENGTH_SHORT).show()

                    // if reached last collection items list, then done
                    // getting all challenges
                    if (i == otherIds.size - 1) {

                        appendAllWritingWithChallengeID(otherIds, challenges, writings, pBar, emptyTextView)

                        /*
                        // Set progress bar and text visibility
                        // Reload RecyclerView
                        adapter.notifyDataSetChanged()
                        // Hide progress bar
                        progressBar.visibility = View.GONE
                        // Show or hide no writing text
                        setEmptyTextVisibility(challengeItems.size, emptyChallengesText)
                         */
                    }


                }
                .addOnFailureListener {
                    Timber.e(it)
                    Toasty.error(this.requireContext(), R.string.error_loading_challenges, Toast.LENGTH_LONG).show()

                    // Hide progress bar
                    // progressBar.visibility = View.GONE
                    pBar.visibility = View.GONE
                }
        }
    }

    /**
     * Append to an ArrayList all writings which were submitted for
     * the challenge with given ID.
     *
     * A challenge may have more than one writing.
     * @param otherUserIds: Emails of all users excluding the current user
     * @param challengeItems: Challenges the current user has sent to other users
     * @param writingItems: ArrayList to append to
     */
    private fun appendAllWritingWithChallengeID(otherUserIds: ArrayList<String>, challengeItems:ArrayList<ChallengeItem>, writingItems:ArrayList<WritingItem>, progressBar: ProgressBar, emptySentChallengesText:TextView) {
        // Get existing users from Firestore
        val collection = db.collection(HomeFragment.COLLECTION_USERS)

        // Need indexing to know when finished looping through all challenges and writing collections
        for ((k,challenge) in challengeItems.withIndex()) {
            var isWritingFound = false
            for ((m,otherId) in otherUserIds.withIndex()) {
                collection
                    .document(otherId)
                    .collection(COLLECTION_WRITING)
                    .whereEqualTo("challengeId", challenge.id)
                    .get()
                    .addOnSuccessListener {
                        // Convert the whole Query Snapshot to a list
                        // of objects directly
                        val items: List<WritingItem> =
                            it.toObjects(WritingItem::class.java)

                        writingItems.addAll(items)

                        // if there is at least one writing collection with some writings of this challenge,
                        // set to true
                        if (items.isNotEmpty()) {
                            isWritingFound = true
                        }

                        Toasty.info(this.requireContext(), "found writings: " + writingItems.size + " with challenge ID " + challenge.id, Toast.LENGTH_SHORT).show()

                        // If reached last writing list and still no writings were found from this challenge,
                        // then append a WritingItem with an empty prompt to display to user and
                        // indicate this challenge has not been completed yet
                        if (m == otherUserIds.size - 1 && !isWritingFound) {
                            Toasty.info(this.requireContext(), "incomplete challenge: " + challenge.id, Toast.LENGTH_SHORT).show()
                            writingItems.add(
                                WritingItem(UUID.randomUUID().toString(),
                                    name = "Challenge sent to " + challenge.receiverId,
                                    prompt = challenge.prompt,
                                    time = challenge.time,
                                    url = challenge.url,
                                    thumbUrl = challenge.thumbUrl,
                                    writing = "",
                                    challengeId = challenge.id
                                )
                            )
                        }

                        // if reached last challenge ID and last writing list of that ID, then done
                        // getting all sent challenge writings to review.
                        // If there are no challenges or writings with those challenge IDs, then
                        // also done
                        if (challengeItems.size == 0 || writingItems.size == 0 ||
                            k == challengeItems.size - 1 && m == otherUserIds.size - 1) {
                            // Toasty.info(this.requireContext(), "found writings: " + writingItems.size, Toast.LENGTH_LONG).show()

                            // Set progress bar and text visibility
                            // Reload RecyclerView
                            adapter.notifyDataSetChanged()
                            // Hide progress bar
                            progressBar.visibility = View.GONE
                            // Show or hide no writing text
                            setEmptyTextVisibility(writingItems.size, emptySentChallengesText)
                        }

                    }
                    .addOnFailureListener {
                        Timber.e(it)
                        Toasty.error(this.requireContext(), R.string.error_loading_my_writing, Toast.LENGTH_LONG).show()
                        // Hide progress bar
                        // progressBar.visibility = View.GONE
                    }
            }
        }
    }
}