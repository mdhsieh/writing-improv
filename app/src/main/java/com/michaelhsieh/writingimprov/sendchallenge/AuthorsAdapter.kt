package com.michaelhsieh.writingimprov.sendchallenge

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.michaelhsieh.writingimprov.AuthorItem
import com.michaelhsieh.writingimprov.R

class AuthorsAdapter internal constructor(
    context: Context?,
    data: List<AuthorItem>
) :
    RecyclerView.Adapter<AuthorsAdapter.ViewHolder>() {
    private val mData: List<AuthorItem>
    private val mInflater: LayoutInflater
    private var mClickListener: ItemClickListener? = null

    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = mInflater.inflate(R.layout.author_row, parent, false)
        return ViewHolder(view)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val writing = mData[position]
        holder.nameText.text = writing.name
    }

    // total number of rows
    override fun getItemCount(): Int {
        return mData.size
    }

    // stores and recycles views as they are scrolled off screen
    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var nameText: TextView
        override fun onClick(view: View?) {
            if (mClickListener != null) mClickListener!!.onItemClick(view, adapterPosition)
        }

        init {
            nameText = itemView.findViewById(R.id.tv_author_name)
            itemView.setOnClickListener(this)
        }
    }

    // convenience method for getting data at click position
    fun getItem(id: Int): AuthorItem {
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
}