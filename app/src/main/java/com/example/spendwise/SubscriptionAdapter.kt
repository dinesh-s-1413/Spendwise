package com.example.spendwise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class SubscriptionAdapter(
    private val subscriptions: MutableList<Map<String, Any>>,
    private val onEdit: (Map<String, Any>) -> Unit,
    private val onDelete: (Map<String, Any>) -> Unit,
    private val onPauseResume: (Map<String, Any>) -> Unit,
    private val onRenew: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<SubscriptionAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.findViewById(R.id.subscriptionCard)
        val tvCategoryIcon: TextView = itemView.findViewById(R.id.tvCategoryIcon)
        val tvSubName: TextView = itemView.findViewById(R.id.tvSubName)
        val tvSubCategory: TextView = itemView.findViewById(R.id.tvSubCategory)
        val tvSubCost: TextView = itemView.findViewById(R.id.tvSubCost)
        val tvSubBilling: TextView = itemView.findViewById(R.id.tvSubBilling)
        val tvSubRenewal: TextView = itemView.findViewById(R.id.tvSubRenewal)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
        val btnPauseResume: MaterialButton = itemView.findViewById(R.id.btnPauseResume)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription_full, parent, false)
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
        val billingEvery = (sub["billingEvery"] as? Long)?.toInt() ?: 1
        val renewalDate = sub["renewalDate"] as? String ?: ""
        val status = sub["status"] as? String ?: "Active"

        holder.tvSubName.text = name
        holder.tvSubCategory.text = category
        val currency = CurrencyHelper.getCurrencySymbol(holder.itemView.context)
        holder.tvSubCost.text = "$currency%.2f".format(cost)
        holder.tvSubRenewal.text = "Renews: $renewalDate"

        if (billingEvery > 1) {
            holder.tvSubBilling.text = "Every $billingEvery ${billingCycle}s"
        } else {
            holder.tvSubBilling.text = billingCycle
        }

        holder.tvCategoryIcon.text = when {
            category.contains("Entertainment") -> "🎬"
            category.contains("Software") -> "💻"
            category.contains("Education") -> "📚"
            category.contains("Utilities") -> "⚡"
            else -> "📦"
        }

        // Check if expired
        val dateFormat = java.text.SimpleDateFormat(
            "dd MMM yyyy", java.util.Locale.getDefault()
        )
        val isExpired = try {
            val renewalDateParsed = dateFormat.parse(renewalDate)
            renewalDateParsed != null && renewalDateParsed.before(java.util.Date())
        } catch (e: Exception) { false }

        // Status display
        when {
            isExpired -> {
                holder.tvStatus.text = "❌ Expired"
                holder.tvStatus.setTextColor(
                    holder.itemView.context.getColor(R.color.red)
                )
                holder.card.alpha = 0.5f
                holder.btnPauseResume.text = "⏸ Pause"
                // Show Renew instead of Edit
                holder.btnEdit.text = "🔄 Renew"
            }
            status == "Paused" -> {
                holder.tvStatus.text = "⏸ Paused"
                holder.tvStatus.setTextColor(
                    holder.itemView.context.getColor(R.color.orange)
                )
                holder.card.alpha = 0.7f
                holder.btnPauseResume.text = "▶ Resume"
                holder.btnEdit.text = "✏️ Edit"
            }
            else -> {
                holder.tvStatus.text = "✅ Active"
                holder.tvStatus.setTextColor(
                    holder.itemView.context.getColor(R.color.green)
                )
                holder.card.alpha = 1.0f
                holder.btnPauseResume.text = "⏸ Pause"
                holder.btnEdit.text = "✏️ Edit"
            }
        }

        // Button clicks
        holder.btnDelete.setOnClickListener { onDelete(sub) }
        holder.btnPauseResume.setOnClickListener { onPauseResume(sub) }

        if (isExpired) {
            holder.btnEdit.setOnClickListener { onRenew(sub) }
        } else {
            holder.btnEdit.setOnClickListener { onEdit(sub) }
        }
    }

    override fun getItemCount() = subscriptions.size

    fun updateList(newList: List<Map<String, Any>>) {
        subscriptions.clear()
        subscriptions.addAll(newList)
        notifyDataSetChanged()
    }
}