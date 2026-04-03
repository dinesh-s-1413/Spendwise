package com.example.spendwise

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.spendwise.databinding.FragmentAddSubscriptionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddSubscriptionFragment : Fragment() {

    private lateinit var binding: FragmentAddSubscriptionBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private var selectedStartDate: Calendar? = null
    private var editingSubscriptionId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddSubscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupCategoryDropdown()
        setupBillingCycleDropdown()
        setupDatePickers()

        binding.btnSave.setOnClickListener {
            saveSubscription()
        }

        // Check if we have edit data from Bundle
        arguments?.let { args ->
            val id = args.getString("id")
            if (id != null) {
                editingSubscriptionId = id
                binding.tvFormTitle.text = "Edit Subscription"
                binding.btnSave.text = "Update Subscription"

                // Pre-fill all fields
                binding.etName.setText(args.getString("name", ""))
                binding.actvCategory.setText(args.getString("category", ""), false)
                binding.etCost.setText(args.getDouble("cost", 0.0).toString())
                binding.actvBillingCycle.setText(args.getString("billingCycle", ""), false)
                binding.etBillingEvery.setText(args.getInt("billingEvery", 1).toString())
                binding.etStartDate.setText(args.getString("startDate", ""))
                binding.etRenewalDate.setText(args.getString("renewalDate", ""))

                // Set selected start date for calendar
                try {
                    val startDateStr = args.getString("startDate", "")
                    if (startDateStr.isNotEmpty()) {
                        val date = dateFormat.parse(startDateStr)
                        if (date != null) {
                            selectedStartDate = Calendar.getInstance()
                            selectedStartDate?.time = date
                        }
                    }
                } catch (e: Exception) { }
            }
        }
    }

    private fun setupCategoryDropdown() {
        val categories = listOf(
            "🎬 Entertainment",
            "💻 Software",
            "📚 Education",
            "⚡ Utilities",
            "📦 Others"
        )
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.actvCategory.setAdapter(adapter)
    }

    private fun setupBillingCycleDropdown() {
        val cycles = listOf("Monthly", "Yearly")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            cycles
        )
        binding.actvBillingCycle.setAdapter(adapter)

        binding.actvBillingCycle.setOnItemClickListener { _, _, _, _ ->
            calculateRenewalDate()
        }

        binding.etBillingEvery.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateRenewalDate()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener {
            val calendar = selectedStartDate ?: Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val selected = Calendar.getInstance()
                    selected.set(year, month, day)
                    selectedStartDate = selected
                    binding.etStartDate.setText(dateFormat.format(selected.time))
                    calculateRenewalDate()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etRenewalDate.setOnClickListener {
            val calendar = selectedStartDate ?: Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val selected = Calendar.getInstance()
                    selected.set(year, month, day)
                    binding.etRenewalDate.setText(dateFormat.format(selected.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun calculateRenewalDate() {
        val startDate = selectedStartDate ?: return
        val billingCycle = binding.actvBillingCycle.text.toString()
        if (billingCycle.isEmpty()) return

        val everyStr = binding.etBillingEvery.text.toString().trim()
        val every = everyStr.toIntOrNull() ?: 1

        val renewalDate = startDate.clone() as Calendar

        when (billingCycle) {
            "Monthly" -> renewalDate.add(Calendar.MONTH, every)
            "Yearly" -> renewalDate.add(Calendar.YEAR, every)
        }

        binding.etRenewalDate.setText(dateFormat.format(renewalDate.time))
    }

    private fun saveSubscription() {
        val name = binding.etName.text.toString().trim()
        val category = binding.actvCategory.text.toString().trim()
        val costStr = binding.etCost.text.toString().trim()
        val billingCycle = binding.actvBillingCycle.text.toString().trim()
        val startDate = binding.etStartDate.text.toString().trim()
        val renewalDate = binding.etRenewalDate.text.toString().trim()

        if (name.isEmpty()) {
            binding.nameLayout.error = "Name is required"
            return
        }
        if (category.isEmpty()) {
            binding.categoryLayout.error = "Category is required"
            return
        }
        if (costStr.isEmpty()) {
            binding.costLayout.error = "Cost is required"
            return
        }
        if (billingCycle.isEmpty()) {
            binding.billingCycleLayout.error = "Billing cycle is required"
            return
        }

        val billingEvery = binding.etBillingEvery.text.toString().trim().toIntOrNull() ?: 1
        if (billingEvery < 1) {
            binding.billingEveryLayout.error = "Must be at least 1"
            return
        }
        binding.billingEveryLayout.error = null

        if (startDate.isEmpty()) {
            binding.startDateLayout.error = "Start date is required"
            return
        }
        if (renewalDate.isEmpty()) {
            binding.renewalDateLayout.error = "Renewal date is required"
            return
        }

        binding.nameLayout.error = null
        binding.categoryLayout.error = null
        binding.costLayout.error = null
        binding.billingCycleLayout.error = null
        binding.startDateLayout.error = null
        binding.renewalDateLayout.error = null

        val cost = costStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val userId = auth.currentUser?.uid ?: return
        val isEditing = editingSubscriptionId != null

        binding.btnSave.isEnabled = false
        binding.btnSave.text = if (isEditing) "Updating..." else "Saving..."

        val subscriptionId = editingSubscriptionId
            ?: database.getReference("subscriptions").child(userId).push().key
            ?: return

        val subscription = mapOf(
            "id" to subscriptionId,
            "name" to name,
            "category" to category,
            "cost" to cost,
            "billingCycle" to billingCycle,
            "billingEvery" to billingEvery,
            "startDate" to startDate,
            "renewalDate" to renewalDate,
            "userId" to userId,
            "createdAt" to System.currentTimeMillis()
        )

        database.getReference("subscriptions")
            .child(userId)
            .child(subscriptionId)
            .setValue(subscription)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    if (isEditing) "Updated! ✅" else "Saved! ✅",
                    Toast.LENGTH_SHORT
                ).show()
                clearForm()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Save Subscription"
                binding.tvFormTitle.text = "Add Subscription"

                if (isEditing) {
                    parentFragmentManager.popBackStack()
                    requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                        R.id.bottomNavigation
                    ).selectedItemId = R.id.nav_subscriptions
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed!", Toast.LENGTH_SHORT).show()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = if (isEditing) "Update Subscription" else "Save Subscription"
            }
    }

    private fun clearForm() {
        binding.etName.text?.clear()
        binding.actvCategory.text?.clear()
        binding.etCost.text?.clear()
        binding.actvBillingCycle.text?.clear()
        binding.etStartDate.text?.clear()
        binding.etRenewalDate.text?.clear()
        binding.etBillingEvery.setText("1")
        selectedStartDate = null
        editingSubscriptionId = null
    }

    companion object {
        fun newInstance(sub: Map<String, Any>): AddSubscriptionFragment {
            val fragment = AddSubscriptionFragment()
            val bundle = Bundle()

            bundle.putString("id", sub["id"] as? String ?: "")
            bundle.putString("name", sub["name"] as? String ?: "")
            bundle.putString("category", sub["category"] as? String ?: "")

            val cost = when (val rawCost = sub["cost"]) {
                is Double -> rawCost
                is Long -> rawCost.toDouble()
                else -> 0.0
            }
            bundle.putDouble("cost", cost)
            bundle.putString("billingCycle", sub["billingCycle"] as? String ?: "")
            val billingEvery = when (val raw = sub["billingEvery"]) {
                is Long -> raw.toInt()
                is Int -> raw
                else -> 1
            }
            bundle.putInt("billingEvery", billingEvery)
            bundle.putString("startDate", sub["startDate"] as? String ?: "")
            bundle.putString("renewalDate", sub["renewalDate"] as? String ?: "")

            fragment.arguments = bundle
            return fragment
        }
    }
}