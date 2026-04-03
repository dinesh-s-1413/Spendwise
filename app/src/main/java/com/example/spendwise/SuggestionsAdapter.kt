package com.example.spendwise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SuggestionItem(
    val name: String,
    val reason: String,
    val savingAmount: Double,
    val currency: String
)

class SuggestionsAdapter(
    private val suggestions: List<SuggestionItem>
) : RecyclerView.Adapter<SuggestionsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvSuggestionName)
        val tvReason: TextView = itemView.findViewById(R.id.tvSuggestionReason)
        val tvSaving: TextView = itemView.findViewById(R.id.tvSuggestionSaving)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = suggestions[position]
        holder.tvName.text = item.name
        holder.tvReason.text = item.reason
        holder.tvSaving.text = "Save ${item.currency}%.2f/mo".format(item.savingAmount)
    }

    override fun getItemCount() = suggestions.size
}