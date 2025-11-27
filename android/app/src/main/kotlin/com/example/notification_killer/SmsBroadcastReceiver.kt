package com.example.notification_killer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.notification_killer.DELETE_SMS") {
            // The text we got from the notification
            val notificationBody = intent.getStringExtra("body")?.trim() ?: ""
            val notificationId = intent.getIntExtra("notification_id", 0)

            Log.d("NotiKiller", "Searching for message: '$notificationBody'")

            if (notificationBody.isNotEmpty()) {
                deleteSms(context, notificationBody)
            } else {
                Log.e("NotiKiller", "Notification body was empty, cannot delete.")
            }

            // Dismiss the notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(notificationId)
        }
    }

    private fun deleteSms(context: Context, targetBody: String) {
        try {
            // CHANGED: Query "content://sms" instead of "inbox" to search everywhere
            val smsUri = Uri.parse("content://sms")
            val cursor = context.contentResolver.query(
                smsUri,
                arrayOf("_id", "body", "address", "date"),
                null,
                null,
                "date DESC LIMIT 50" // CHANGED: Increased limit from 10 to 50
            )

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow("_id"))
                    val dbBody = cursor.getString(cursor.getColumnIndexOrThrow("body"))?.trim() ?: ""
                    
                    // Log the first 20 chars to see what we are scanning without spamming logs
                    val shortBody = if (dbBody.length > 20) dbBody.substring(0, 20) + "..." else dbBody
                    Log.d("NotiKiller", "Scanning ID $id: '$shortBody'")

                    // RELAXED MATCHING LOGIC:
                    if (dbBody.equals(targetBody, ignoreCase = true) || 
                        dbBody.contains(targetBody, ignoreCase = true) || 
                        targetBody.contains(dbBody, ignoreCase = true)) {
                        
                        Log.d("NotiKiller", "MATCH FOUND! ID: $id. Attempting delete...")
                        
                        val rowsDeleted = context.contentResolver.delete(
                            Uri.parse("content://sms/$id"),
                            null,
                            null
                        )

                        if (rowsDeleted > 0) {
                            Toast.makeText(context, "Message Deleted Successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Match found, but delete failed. (App is not Default SMS App?)", Toast.LENGTH_LONG).show()
                        }
                        cursor.close()
                        return
                    }
                } while (cursor.moveToNext())
                cursor.close()
            }
            
            Log.d("NotiKiller", "Scanned last 50 messages, no match found.")
            Log.w("NotiKiller", "WARNING: If this is an RCS (Chat) message, it cannot be deleted via SMS database.")
            Toast.makeText(context, "Could not find message. Is it an RCS/Chat message?", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e("NotiKiller", "CRASH during delete", e)
            Toast.makeText(context, "Error: App must be Default SMS App to delete", Toast.LENGTH_LONG).show()
        }
    }
}