package com.example.notification_killer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat

class NotificationListener : NotificationListenerService() {

    companion object {
        var instance: NotificationListener? = null
        // CHANGED: New ID to force Android to reset channel settings
        const val CHANNEL_ID = "sms_killer_v2" 
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotiKiller", "Service Connected! Ready to intercept.")
        instance = this
        createNotificationChannel()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    fun clearAllNotifications() {
        cancelAllNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        if (packageName == "com.example.notification_killer") return

        if (packageName != "com.samsung.android.messaging" && 
            packageName != "com.google.android.apps.messaging") {
            return 
        }

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: "New Message"
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d("NotiKiller", "Intercepted SMS from $packageName. attempting to replace...")

        // Kill original
        cancelNotification(sbn.key)

        // Show ours
        val notificationId = System.currentTimeMillis().toInt()
        showReplacementNotification(title, text, notificationId)
    }

    private fun showReplacementNotification(title: String, text: String, notificationId: Int) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val deleteIntent = Intent(this, SmsBroadcastReceiver::class.java).apply {
                action = "com.example.notification_killer.DELETE_SMS"
                putExtra("sender", title)
                putExtra("body", text)
                putExtra("notification_id", notificationId)
            }
            
            val deletePendingIntent = PendingIntent.getBroadcast(
                this, 
                notificationId,
                deleteIntent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            createNotificationChannel()

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) 
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // CHANGED: Force it to show on Lock Screen
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) 
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .addAction(android.R.drawable.ic_menu_delete, "DELETE", deletePendingIntent)

            notificationManager.notify(notificationId, builder.build())
            Log.d("NotiKiller", "Replacement posted. ID: $notificationId")

        } catch (e: Exception) {
            Log.e("NotiKiller", "FAILED to post notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // Create new channel with High Importance
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cleaned SMS v2",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows SMS with a Delete button"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                // CHANGED: Force visibility on lock screen at the channel level too
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC 
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
    }
}