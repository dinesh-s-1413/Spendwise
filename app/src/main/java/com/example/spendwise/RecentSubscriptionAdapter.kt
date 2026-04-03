package com.example.spendwise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecentSubscriptionAdapter(
    private val subscriptions: List<Map<String, Any>>
) : RecyclerView.Adapter<RecentSubscriptionAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvSubName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvSubCategory)
        val tvCost: TextView = itemView.findViewById(R.id.tvSubCost)
        val tvRenewal: TextView = itemView.findViewById(R.id.tvSubRenewal)
        val tvBilling: TextView = itemView.findViewById(R.id.tvSubBilling)
        val tvCategoryIcon: TextView = itemView.findViewById(R.id.tvCategoryIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sub = subscriptions[position]

        val name = sub["name"] as? String ?: ""
        val category = sub["category"] as? String ?: ""
        val cost = when (val rawCost = sub["cost"]) {
            is Double -> rawCost
            is Long -> rawCost.toDouble()
            is String -> rawCost.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val billingCycle = sub["billingCycle"] as? String ?: ""
        val renewalDate = sub["renewalDate"] as? String ?: ""

        holder.tvName.text = name
        holder.tvCategory.text = category
        val currency = CurrencyHelper.getCurrencySymbol(holder.itemView.context)
        holder.tvCost.text = "$currency%.2f".format(cost)
        holder.tvBilling.text = billingCycle
        holder.tvRenewal.text = "Renews: $renewalDate"

        holder.tvCategoryIcon.text = when {
            category.contains("Entertainment") -> "🎬"
            category.contains("Software") -> "💻"
            category.contains("Education") -> "📚"
            category.contains("Utilities") -> "⚡"
            else -> "📦"
        }
    }

    override fun getItemCount() = subscriptions.size
}