package com.example.spendwise

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.spendwise.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.Legend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var listener: ValueEventListener? = null
    private var ref: DatabaseReference? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        loadAnalyticsData()
    }

    private fun loadAnalyticsData() {
        val userId = auth.currentUser?.uid ?: return

        ref = database.getReference("subscriptions").child(userId)
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                val categorySpending = mutableMapOf<String, Double>()
                var totalMonthly = 0.0
                var totalYearly = 0.0

                // For most/least expensive
                var mostExpensiveName = "-"
                var mostExpensiveCost = 0.0
                var leastExpensiveName = "-"
                var leastExpensiveCost = Double.MAX_VALUE
                var hasAny = false

                for (subSnapshot in snapshot.children) {
                    val sub = subSnapshot.value as? Map<String, Any> ?: continue
                    val status = sub["status"] as? String ?: "Active"
                    if (status == "Paused") continue

                    val name = sub["name"] as? String ?: "Unknown"
                    val category = sub["category"] as? String ?: "Others"
                    val billingCycle = sub["billingCycle"] as? String ?: "Monthly"
                    val cost = when (val rawCost = sub["cost"]) {
                        is Double -> rawCost
                        is Long -> rawCost.toDouble()
                        else -> 0.0
                    }

                    // Monthly cost
                    val monthlyCost = if (billingCycle == "Monthly") cost else cost / 12

                    // Clean category name
                    val cleanCategory = when {
                        category.contains("Entertainment") -> "Entertainment"
                        category.contains("Software") -> "Software"
                        category.contains("Education") -> "Education"
                        category.contains("Utilities") -> "Utilities"
                        else -> "Others"
                    }

                    categorySpending[cleanCategory] =
                        (categorySpending[cleanCategory] ?: 0.0) + monthlyCost

                    totalMonthly += monthlyCost
                    totalYearly += monthlyCost * 12

                    // Most expensive
                    if (monthlyCost > mostExpensiveCost) {
                        mostExpensiveCost = monthlyCost
                        mostExpensiveName = name
                    }

                    // Least expensive
                    if (monthlyCost < leastExpensiveCost) {
                        leastExpensiveCost = monthlyCost
                        leastExpensiveName = name
                    }

                    hasAny = true
                }

                // Update most/least cards
                if (hasAny) {
                    val currency = CurrencyHelper.getCurrencySymbol(requireContext())
                    binding.tvMostExpensiveName.text = mostExpensiveName
                    binding.tvMostExpensiveCost.text = currency + "%.2f/mo".format(mostExpensiveCost)
                    binding.tvLeastExpensiveName.text = leastExpensiveName
                    binding.tvLeastExpensiveCost.text = if (leastExpensiveCost == Double.MAX_VALUE)
                        currency + "0.00/mo"
                    else
                        currency + "%.2f/mo".format(leastExpensiveCost)
                    binding.tvTotalMonthly.text = currency + "%.2f".format(totalMonthly)
                    binding.tvTotalYearly.text = currency + "%.2f".format(totalYearly)
                } else {
                    binding.tvMostExpensiveName.text = "No data"
                    binding.tvMostExpensiveCost.text = "$0.00/mo"
                    binding.tvLeastExpensiveName.text = "No data"
                    binding.tvLeastExpensiveCost.text = "$0.00/mo"
                }

                // Update totals
                val currency = CurrencyHelper.getCurrencySymbol(requireContext())
                binding.tvTotalMonthly.text = currency + "%.2f".format(totalMonthly)
                binding.tvTotalYearly.text = currency + "%.2f".format(totalYearly)

                setupPieChart(categorySpending)
                setupBarChart(categorySpending)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref?.addValueEventListener(listener!!)
    }

    private fun setupPieChart(data: Map<String, Double>) {
        if (data.isEmpty()) return

        val entries = data.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#1B4FD8"),
                Color.parseColor("#F97316"),
                Color.parseColor("#16A34A"),
                Color.parseColor("#DC2626"),
                Color.parseColor("#9333EA")
            )
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }

        binding.pieChart.apply {
            this.data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            setHoleColor(Color.WHITE)
            centerText = "Spending\nby Category"
            setCenterTextSize(12f)
            legend.apply {
                isEnabled = true
                textSize = 12f
                form = Legend.LegendForm.CIRCLE
            }
            animateY(1000)
            invalidate()
        }
    }

    private fun setupBarChart(data: Map<String, Double>) {
        if (data.isEmpty()) return

        val entries = data.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val labels = data.keys.toList()

        val dataSet = BarDataSet(entries, "Monthly Spending ($)").apply {
            colors = listOf(
                Color.parseColor("#1B4FD8"),
                Color.parseColor("#F97316"),
                Color.parseColor("#16A34A"),
                Color.parseColor("#DC2626"),
                Color.parseColor("#9333EA")
            )
            valueTextSize = 10f
            valueTextColor = Color.BLACK
        }

        binding.barChart.apply {
            this.data = BarData(dataSet)
            description.isEnabled = false
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                textSize = 10f
            }
            axisLeft.textSize = 10f
            axisRight.isEnabled = false
            legend.textSize = 12f
            animateY(1000)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.let { ref?.removeEventListener(it) }
        _binding = null
    }
}