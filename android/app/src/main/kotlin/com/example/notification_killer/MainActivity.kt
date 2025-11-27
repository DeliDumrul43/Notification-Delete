package com.example.notification_killer

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.content.Intent
import android.provider.Settings

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.notification_killer/actions"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "openSettings") {
                // This magic intent opens the specific "Notification Access" page
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                result.success(true)
            } else if (call.method == "clearNotifications") {
                 // Forward the call to our service helper
                 if (NotificationListener.instance != null) {
                    NotificationListener.instance?.clearAllNotifications()
                    result.success(true)
                } else {
                    result.error("UNAVAILABLE", "Listener not connected", null)
                }
            } else {
                result.notImplemented()
            }
        }
    }
}