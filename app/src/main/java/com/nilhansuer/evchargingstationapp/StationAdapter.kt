package com.nilhansuer.evchargingstationapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StationAdapter(private val activityList: List<String>) :
    RecyclerView.Adapter<StationAdapter.StationViewHolder>() {

    inner class StationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textActivity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_layout, parent, false)
        return StationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        holder.textView.text = activityList[position]
    }

    override fun getItemCount() = activityList.size
}
