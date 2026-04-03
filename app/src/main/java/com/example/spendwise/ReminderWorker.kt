package com.example.spendwise

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: return Result.success()

            val database = FirebaseDatabase.getInstance()
            val snapshot = database.getReference("subscriptions")
                .child(userId)
                .get()
                .await()

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val notificationHelper = NotificationHelper(applicationContext)

            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            var notificationId = 1000

            for (subSnapshot in snapshot.children) {
                val name = subSnapshot.child("name")
                    .getValue(String::class.java) ?: continue
                val status = subSnapshot.child("status")
                    .getValue(String::class.java) ?: "Active"
                val renewalDateStr = subSnapshot.child("renewalDate")
                    .getValue(String::class.java) ?: continue
                val cost = when (val rawCost = subSnapshot.child("cost").value) {
                    is Double -> rawCost
                    is Long -> rawCost.toDouble()
                    else -> 0.0
                }

                // Skip paused subscriptions ⏸
                if (status == "Paused") continue

                try {
                    val renewalDate = dateFormat.parse(renewalDateStr) ?: continue
                    val renewalCal = Calendar.getInstance()
                    renewalCal.time = renewalDate
                    renewalCal.set(Calendar.HOUR_OF_DAY, 0)
                    renewalCal.set(Calendar.MINUTE, 0)
                    renewalCal.set(Calendar.SECOND, 0)
                    renewalCal.set(Calendar.MILLISECOND, 0)

                    // Calculate days difference
                    val diffMs = renewalCal.timeInMillis - today.timeInMillis
                    val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

                    when (diffDays) {
                        2 -> {
                            notificationHelper.showRenewalReminder(
                                subscriptionName = name,
                                cost = cost,
                                renewalDate = renewalDateStr,
                                notificationId = notificationId++,
                                daysLeft = 2
                            )
                        }
                        1 -> {
                            notificationHelper.showRenewalReminder(
                                subscriptionName = name,
                                cost = cost,
                                renewalDate = renewalDateStr,
                                notificationId = notificationId++,
                                daysLeft = 1
                            )
                        }
                        0 -> {
                            notificationHelper.showRenewalReminder(
                                subscriptionName = name,
                                cost = cost,
                                renewalDate = renewalDateStr,
                                notificationId = notificationId++,
                                daysLeft = 0
                            )
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}