package com.example.spendwise

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendwise.databinding.FragmentSubscriptionsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Calendar
import java.util.Date

class SubscriptionsFragment : Fragment() {

    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val allSubscriptions = mutableListOf<Map<String, Any>>()
    private val subscriptionList = mutableListOf<Map<String, Any>>()
    private var subscriptionListener: ValueEventListener? = null
    private var subscriptionRef: DatabaseReference? = null
    var filterStatus: String? = null
    var filterType: String? = null
    private var searchQuery = ""
    private var selectedChipFilter = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupRecyclerView()
        setupSearch()
        setupChips()
        loadSubscriptions()

        // Hide search and chips if coming from dashboard
        if (filterType != null) {
            binding.searchLayout.visibility = View.GONE
            binding.chipGroup.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        binding.rvSubscriptions.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupChips() {
        binding.chipAll.setOnClickListener {
            selectedChipFilter = "All"
            applyFilters()
        }
        binding.chipActive.setOnClickListener {
            selectedChipFilter = "Active"
            applyFilters()
        }
        binding.chipPaused.setOnClickListener {
            selectedChipFilter = "Paused"
            applyFilters()
        }
        binding.chipExpired.setOnClickListener {
            selectedChipFilter = "Expired"
            applyFilters()
        }
        binding.chipEntertainment.setOnClickListener {
            selectedChipFilter = "Entertainment"
            applyFilters()
        }
        binding.chipSoftware.setOnClickListener {
            selectedChipFilter = "Software"
            applyFilters()
        }
        binding.chipEducation.setOnClickListener {
            selectedChipFilter = "Education"
            applyFilters()
        }
        binding.chipUtilities.setOnClickListener {
            selectedChipFilter = "Utilities"
            applyFilters()
        }
        binding.chipOthers.setOnClickListener {
            selectedChipFilter = "Others"
            applyFilters()
        }
    }

    private fun applyFilters() {
        val dateFormat = java.text.SimpleDateFormat(
            "dd MMM yyyy", java.util.Locale.getDefault()
        )

        val filtered = allSubscriptions.filter { sub ->
            val name = sub["name"] as? String ?: ""
            val status = sub["status"] as? String ?: "Active"
            val category = sub["category"] as? String ?: ""
            val renewalDateStr = sub["renewalDate"] as? String ?: ""

            // Check if expired
            val isExpired = try {
                val renewalDate = dateFormat.parse(renewalDateStr)
                renewalDate != null && renewalDate.before(Date())
            } catch (e: Exception) { false }

            // Search filter
            val matchesSearch = searchQuery.isEmpty() ||
                    name.contains(searchQuery, ignoreCase = true)

            // Chip filter
            val matchesChip = when (selectedChipFilter) {
                "All" -> true
                "Active" -> !isExpired && status == "Active"
                "Paused" -> status == "Paused"
                "Expired" -> isExpired
                "Entertainment" -> category.contains("Entertainment")
                "Software" -> category.contains("Software")
                "Education" -> category.contains("Education")
                "Utilities" -> category.contains("Utilities")
                "Others" -> category.contains("Others")
                else -> true
            }

            matchesSearch && matchesChip
        }

        subscriptionList.clear()
        subscriptionList.addAll(filtered)
        updateAdapter()

        binding.tvCount.text = "${filtered.size} plans"

        if (filtered.isEmpty()) {
            binding.rvSubscriptions.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvSubscriptions.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun updateAdapter() {
        if (filterType != null) {
            binding.rvSubscriptions.adapter =
                ViewOnlySubscriptionAdapter(subscriptionList)
        } else {
            binding.rvSubscriptions.adapter = SubscriptionAdapter(
                subscriptionList,
                onEdit = { sub -> openEditDialog(sub) },
                onDelete = { sub -> confirmDelete(sub) },
                onPauseResume = { sub -> togglePauseResume(sub) },
                onRenew = { sub -> renewSubscription(sub) }
            )
        }
    }

    private fun loadSubscriptions() {
        val userId = auth.currentUser?.uid ?: return

        subscriptionRef = database.getReference("subscriptions").child(userId)
        subscriptionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                val list = mutableListOf<Map<String, Any>>()
                val today = Calendar.getInstance()
                val weekLater = Calendar.getInstance()
                weekLater.add(Calendar.DAY_OF_YEAR, 7)
                val dateFormat = java.text.SimpleDateFormat(
                    "dd MMM yyyy", java.util.Locale.getDefault()
                )

                for (subSnapshot in snapshot.children) {
                    val sub = subSnapshot.value as? Map<String, Any> ?: continue
                    val billingCycle = sub["billingCycle"] as? String ?: ""
                    val renewalDateStr = sub["renewalDate"] as? String ?: ""

                    // Apply dashboard filter if set
                    when (filterType) {
                        "dues" -> {
                            try {
                                val renewalDate = dateFormat.parse(renewalDateStr) ?: continue
                                val renewalCal = Calendar.getInstance()
                                renewalCal.time = renewalDate
                                if (renewalCal.before(today) ||
                                    renewalCal.after(weekLater)) continue
                            } catch (e: Exception) { continue }
                        }
                        "monthly" -> {
                            if (billingCycle != "Monthly") continue
                        }
                        "yearly" -> {
                            if (billingCycle != "Yearly") continue
                        }
                    }

                    list.add(sub)
                }

                allSubscriptions.clear()
                allSubscriptions.addAll(list)

                // Apply current filters
                applyFilters()

                // Update title
                val title = when (filterType) {
                    "dues" -> "Dues This Week 🗓️"
                    "all" -> "All Subscriptions 📋"
                    else -> "My Subscriptions"
                }
                binding.tvTitle.text = title
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        subscriptionRef?.addValueEventListener(subscriptionListener!!)
    }

    private fun togglePauseResume(sub: Map<String, Any>) {
        val userId = auth.currentUser?.uid ?: return
        val subId = sub["id"] as? String ?: return
        val currentStatus = sub["status"] as? String ?: "Active"
        val newStatus = if (currentStatus == "Paused") "Active" else "Paused"

        database.getReference("subscriptions")
            .child(userId)
            .child(subId)
            .child("status")
            .setValue(newStatus)
            .addOnSuccessListener {
                val msg = if (newStatus == "Paused")
                    "Subscription paused ⏸" else "Subscription resumed ▶️"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
    }

    private fun renewSubscription(sub: Map<String, Any>) {
        val billingCycle = sub["billingCycle"] as? String ?: "Monthly"
        val billingEvery = (sub["billingEvery"] as? Long)?.toInt() ?: 1

        val dateFormat = java.text.SimpleDateFormat(
            "dd MMM yyyy", java.util.Locale.getDefault()
        )

        val newRenewalDate = java.util.Calendar.getInstance()
        when (billingCycle) {
            "Monthly" -> newRenewalDate.add(java.util.Calendar.MONTH, billingEvery)
            "Yearly" -> newRenewalDate.add(java.util.Calendar.YEAR, billingEvery)
        }
        val newRenewalDateStr = dateFormat.format(newRenewalDate.time)

        val updatedSub = sub.toMutableMap()
        updatedSub["renewalDate"] = newRenewalDateStr
        updatedSub["status"] = "Active"

        val fragment = AddSubscriptionFragment.newInstance(updatedSub)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun confirmDelete(sub: Map<String, Any>) {
        val name = sub["name"] as? String ?: "this subscription"
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Subscription")
            .setMessage("Are you sure you want to delete $name?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSubscription(sub)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSubscription(sub: Map<String, Any>) {
        val userId = auth.currentUser?.uid ?: return
        val subId = sub["id"] as? String ?: return

        database.getReference("subscriptions")
            .child(userId)
            .child(subId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(),
                    "Deleted! 🗑️", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    "Failed to delete!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openEditDialog(sub: Map<String, Any>) {
        val fragment = AddSubscriptionFragment.newInstance(sub)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        subscriptionListener?.let {
            subscriptionRef?.removeEventListener(it)
        }
        _binding = null
    }
}