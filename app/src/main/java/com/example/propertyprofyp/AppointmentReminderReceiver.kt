package com.example.propertyprofyp

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AppointmentReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Extract information from the Intent
        val bookingId = intent.getStringExtra("bookingId") ?: "Unknown Booking"
        val message = "You have an appointment in 3 days. Check it out!"

        // Create a notification channel (required for Android 8.0 and higher)
        createNotificationChannel(context)

        // Create a notification
        val builder = NotificationCompat.Builder(context, YOUR_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.bell) // Replace with your notification icon
            .setContentTitle("Appointment Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)


        try {
            with(NotificationManagerCompat.from(context)) {
                notify(
                    bookingId.hashCode(),
                    builder.build()
                ) // Use a unique ID for each notification
            }
        } catch (e: SecurityException) {
            // Handle the SecurityException here
            e.printStackTrace() // You can replace this with your own handling logic
        }
//        with(NotificationManagerCompat.from(context)) {
//            notify(bookingId.hashCode(), builder.build()) // Use a unique ID for each notification
//        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                YOUR_NOTIFICATION_CHANNEL_ID,
                "Appointment Reminder Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for appointment reminders"
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val YOUR_NOTIFICATION_CHANNEL_ID = "appointment_reminder_channel"
    }
}

