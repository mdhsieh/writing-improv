package com.michaelhsieh.writingimprov.mychallenges

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.michaelhsieh.writingimprov.ChallengeItem
import com.michaelhsieh.writingimprov.R
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import timber.log.Timber

class ChallengesAdapter internal constructor(
    context: Context?,
    data: List<ChallengeItem>
) :
    RecyclerView.Adapter<ChallengesAdapter.ViewHolder>() {
    private val mData: List<ChallengeItem>
    private val mInflater: LayoutInflater
    private var mClickListener: ItemClickListener? = null

    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = mInflater.inflate(R.layout.challenges_row, parent, false)
        return ViewHolder(view)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val writing = mData[position]
        holder.nameText.text = writing.name
        holder.promptText.text = writing.prompt
        holder.timeText.text = writing.time + " minutes"
        if (writing.completed) {
            holder.completedText.text = "Completed"
        } else {
            holder.completedText.text = "Incomplete"
        }
        loadThumbnailImage(writing.thumbUrl, holder.thumbImage)
    }

    // total number of rows
    override fun getItemCount(): Int {
        return mData.size
    }

    // stores and recycles views as they are scrolled off screen
    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var nameText: TextView
        var promptText: TextView
        var timeText: TextView
        var thumbImage:ImageView
        var completedText: TextView
        override fun onClick(view: View?) {
            if (mClickListener != null) mClickListener!!.onItemClick(view, adapterPosition)
        }

        init {
            nameText = itemView.findViewById(R.id.tv_challenge_name)
            promptText = itemView.findViewById(R.id.tv_challenge_prompt)
            timeText = itemView.findViewById(R.id.tv_challenge_time)
            thumbImage = itemView.findViewById(R.id.iv_thumb)
            completedText = itemView.findViewById(R.id.tv_challenge_is_completed)
            itemView.setOnClickListener(this)
        }
    }

    // convenience method for getting data at click position
    fun getItem(id: Int): ChallengeItem {
        return mData[id]
    }

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener?) {
        mClickListener = itemClickListener
    }

    // parent activity will implement this method to respond to click events
    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }

    // data is passed into the constructor
    init {
        mInflater = LayoutInflater.from(context)
        mData = data
    }

    /**
     * Display an image from a thumbnail URL.
     * Otherwise, show placeholder error icon.
     * @param url The url of the image
     * @param imageView The ImageView which displays the image
     */
    private fun loadThumbnailImage(url:String, imageView:ImageView) {
        Picasso.get().load(url)
            .error(R.drawable.ic_error_outline_72)
            .into(imageView, object : Callback {
                override fun onSuccess() {
                    Timber.d("finished getting image")
                }

                override fun onError(e: Exception?) {
                    // log error message
                    Timber.e(e)
                }

            })
    }
}