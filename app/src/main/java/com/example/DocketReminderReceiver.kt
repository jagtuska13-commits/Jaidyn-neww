package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class DocketReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val description = intent.getStringExtra("description") ?: return
        val ts = intent.getLongExtra("ts", 0L)
        try {
            val channel = NotificationChannel(
                "docket",
                "Deadline reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setDescription("Reminders for upcoming legal dates captured from your matters")
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val notification = androidx.core.app.NotificationCompat.Builder(context, "docket")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Upcoming legal date")
                .setContentText(description.take(180))
                .setStyle(
                    androidx.core.app.NotificationCompat.BigTextStyle()
                        .bigText(description)
                )
                .setAutoCancel(true)
                .build()
            nm.notify(ts.toInt(), notification)
        } catch (e: Exception) {
        }
    }
}
