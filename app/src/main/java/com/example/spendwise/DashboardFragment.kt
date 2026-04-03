package com.example.spendwise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendwise.databinding.FragmentDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private var subscriptionListener: ValueEventListener? = null
    private var subscriptionRef: DatabaseReference? = null
    private var allSubscriptions = listOf<Map<String, Any>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        setGreeting()
        loadDashboardData()
        animateCards()
        setupCardClicks()
    }

    private fun setupCardClicks() {
        binding.cardActivePlans.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()
            openSubscriptionsWithFilter("all")
        }

        binding.cardDuesThisWeek.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()
            openSubscriptionsWithFilter("dues")
        }

        binding.tvSeeAll.setOnClickListener {
            openSubscriptionsWithFilter("all")
        }
    }

    private fun openSubscriptionsWithFilter(filter: String) {
        val fragment = SubscriptionsFragment()
        fragment.filterType = filter
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning! ☀️"
            hour < 17 -> "Good Afternoon! 🌤️"
            else -> "Good Evening! 🌙"
        }
        _binding?.tvGreeting?.text = greeting

        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded || _binding == null) return
                    val name = snapshot.getValue(String::class.java) ?: "User"
                    _binding?.tvUserName?.text = name
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadDashboardData() {
        val userId = auth.currentUser?.uid ?: return

        subscriptionRef = database.getReference("subscriptions").child(userId)
        subscriptionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                var activePlans = 0
                var duesThisWeek = 0
                var monthlySpending = 0.0
                var yearlySpending = 0.0

                val today = Calendar.getInstance()
                val weekLater = Calendar.getInstance()
                weekLater.add(Calendar.DAY_OF_YEAR, 7)

                val recentList = mutableListOf<Map<String, Any>>()
                val allSubsList = mutableListOf<Map<String, Any>>()

                for (subSnapshot in snapshot.children) {
                    val sub = subSnapshot.value as? Map<String, Any> ?: continue
                    val status = sub["status"] as? String ?: "Active"

                    allSubsList.add(sub)

                    if (status == "Paused") continue

                    val cost = when (val rawCost = sub["cost"]) {
                        is Double -> rawCost
                        is Long -> rawCost.toDouble()
                        is String -> rawCost.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    val billingCycle = sub["billingCycle"] as? String ?: ""
                    val renewalDateStr = sub["renewalDate"] as? String ?: ""

                    activePlans++

                    if (billingCycle == "Monthly") {
                        monthlySpending += cost
                        yearlySpending += cost * 12
                    } else {
                        yearlySpending += cost
                        monthlySpending += cost / 12
                    }

                    try {
                        val renewalDate = dateFormat.parse(renewalDateStr)
                        if (renewalDate != null) {
                            val renewalCal = Calendar.getInstance()
                            renewalCal.time = renewalDate
                            if (!renewalCal.before(today) && !renewalCal.after(weekLater)) {
                                duesThisWeek++
                            }
                        }
                    } catch (e: Exception) { }

                    recentList.add(sub)
                }

                allSubscriptions = recentList

                _binding?.tvActivePlans?.text = activePlans.toString()
                _binding?.tvDuesThisWeek?.text = duesThisWeek.toString()
                val currency = CurrencyHelper.getCurrencySymbol(requireContext())
                _binding?.tvMonthlySpending?.text = currency + "%.2f".format(monthlySpending)
                _binding?.tvYearlySpending?.text = currency + "%.2f".format(yearlySpending)

                setupRecentList(recentList.takeLast(5).reversed())
                generateSuggestions(allSubsList)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        subscriptionRef?.addValueEventListener(subscriptionListener!!)
    }

    private fun setupRecentList(list: List<Map<String, Any>>) {
        if (!isAdded || _binding == null) return
        binding.rvRecentSubscriptions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentSubscriptions.adapter = RecentSubscriptionAdapter(list)
    }

    private fun generateSuggestions(subscriptions: List<Map<String, Any>>) {
        if (!isAdded || _binding == null) return

        val suggestions = mutableListOf<SuggestionItem>()
        val currency = CurrencyHelper.getCurrencySymbol(requireContext())
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        var totalSavings = 0.0

        for (sub in subscriptions) {
            val name = sub["name"] as? String ?: continue
            val status = sub["status"] as? String ?: "Active"
            val billingCycle = sub["billingCycle"] as? String ?: "Monthly"
            val renewalDateStr = sub["renewalDate"] as? String ?: ""
            val cost = when (val rawCost = sub["cost"]) {
                is Double -> rawCost
                is Long -> rawCost.toDouble()
                else -> 0.0
            }

            val monthlyCost = if (billingCycle == "Monthly") cost else cost / 12

            val isExpired = try {
                val renewalDate = dateFormat.parse(renewalDateStr)
                renewalDate != null && renewalDate.before(java.util.Date())
            } catch (e: Exception) { false }

            if (isExpired && status != "Paused") {
                suggestions.add(SuggestionItem(
                    name = name,
                    reason = "Expired — consider renewing or pausing",
                    savingAmount = monthlyCost,
                    currency = currency
                ))
                totalSavings += monthlyCost
            }

            if (status == "Paused") {
                suggestions.add(SuggestionItem(
                    name = name,
                    reason = "Currently paused — consider cancelling",
                    savingAmount = monthlyCost,
                    currency = currency
                ))
                totalSavings += monthlyCost
            }

            if (monthlyCost > 1000 && status == "Active" && !isExpired) {
                suggestions.add(SuggestionItem(
                    name = name,
                    reason = "High cost — review if still needed",
                    savingAmount = monthlyCost,
                    currency = currency
                ))
                totalSavings += monthlyCost
            }
        }

        if (suggestions.isNotEmpty()) {
            _binding?.cardSuggestions?.visibility = View.VISIBLE
            _binding?.tvSavingsAmount?.text = "Save " + currency + "%.2f/mo".format(totalSavings)
            _binding?.rvSuggestions?.layoutManager =
                LinearLayoutManager(requireContext())
            _binding?.rvSuggestions?.adapter = SuggestionsAdapter(suggestions)
        } else {
            _binding?.cardSuggestions?.visibility = View.GONE
        }
    }

    private fun animateCards() {
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        _binding?.tvGreeting?.startAnimation(fadeIn)
        _binding?.tvUserName?.startAnimation(fadeIn)
        _binding?.cardActivePlans?.startAnimation(slideUp)
        _binding?.cardDuesThisWeek?.startAnimation(slideUp)
        _binding?.cardMonthly?.startAnimation(slideUp)
        _binding?.cardYearly?.startAnimation(slideUp)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        subscriptionListener?.let {
            subscriptionRef?.removeEventListener(it)
        }
        _binding = null
    }
}