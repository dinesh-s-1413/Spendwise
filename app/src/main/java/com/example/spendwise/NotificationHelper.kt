package com.example.spendwise

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "spendwise_reminders"
        const val CHANNEL_NAME = "Subscription Reminders"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming subscription renewals"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showRenewalReminder(
        subscriptionName: String,
        cost: Double,
        renewalDate: String,
        notificationId: Int,
        daysLeft: Int = 2
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Different title and message based on days left
        val title = when (daysLeft) {
            0 -> "🔴 $subscriptionName renews TODAY!"
            1 -> "⚠️ $subscriptionName renews Tomorrow!"
            else -> "⏰ $subscriptionName renews in 2 days!"
        }

        val message = when (daysLeft) {
            0 -> "Your $subscriptionName subscription renews TODAY for ${"%.2f".format(cost)}. Check your balance!"
            1 -> "Your $subscriptionName subscription renews TOMORROW ($renewalDate) for ${"%.2f".format(cost)}."
            else -> "Your $subscriptionName subscription renews on $renewalDate for ${"%.2f".format(cost)}. Just a heads up!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("${"%.2f".format(cost)}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.notify(notificationId, notification)
    }
}