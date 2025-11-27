import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const NotificationKillerApp());
}

class NotificationKillerApp extends StatelessWidget {
  const NotificationKillerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NotiKiller',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        primarySwatch: Colors.red,
        scaffoldBackgroundColor: const Color(0xFF121212),
        useMaterial3: true,
      ),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  // This channel name MUST match the one in MainActivity.kt
  static const platform = MethodChannel('com.example.notification_killer/actions');
  
  String _statusMessage = "Ready to clean";
  bool _isCleaning = false;

  // Function to manually trigger a clear (optional feature)
  Future<void> _clearNotifications() async {
    setState(() {
      _isCleaning = true;
    });

    try {
      // Calls the 'clearNotifications' method in MainActivity.kt
      await platform.invokeMethod('clearNotifications');
      setState(() {
        _statusMessage = "Boom! Notifications cleared.";
      });
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = "Error: Service not running.\nDid you grant permission?";
      });
      debugPrint("Failed to clear notifications: '${e.message}'.");
    } finally {
      Future.delayed(const Duration(seconds: 2), () {
        if (mounted) {
          setState(() {
            _isCleaning = false;
            _statusMessage = "Ready to clean";
          });
        }
      });
    }
  }

  // Function to open the specific Android settings page
  Future<void> _openPermissionSettings() async {
    try {
      await platform.invokeMethod('openSettings');
    } on PlatformException catch (e) {
      debugPrint("Failed to open settings: '${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Notification Killer"),
        backgroundColor: Colors.redAccent.shade700,
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: _openPermissionSettings,
            tooltip: "Open Notification Access Settings",
          )
        ],
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Instruction Card
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.grey[900],
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.grey[800]!),
                ),
                child: const Text(
                  "STEP 1: Grant Permission\nTap the gear icon (top right) and allow 'NotiKiller' to access notifications.\n\nSTEP 2: Wait for SMS\nThe app runs in the background. When an SMS arrives, it will replace the notification with one that has a DELETE button.",
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white70, height: 1.5),
                ),
              ),
              
              const Spacer(),

              // Status Text
              Text(
                _statusMessage,
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: _statusMessage.contains("Error") ? Colors.red : Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                ),
              ),

              const SizedBox(height: 30),

              // Manual Clear Button (Just for testing)
              GestureDetector(
                onTap: _clearNotifications,
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  width: _isCleaning ? 80 : 200,
                  height: 80,
                  decoration: BoxDecoration(
                    color: _isCleaning ? Colors.green : Colors.redAccent.shade700,
                    borderRadius: BorderRadius.circular(_isCleaning ? 40 : 16),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.redAccent.withOpacity(0.4),
                        blurRadius: 20,
                        spreadRadius: 5,
                      )
                    ],
                  ),
                  child: Center(
                    child: _isCleaning
                        ? const CircularProgressIndicator(color: Colors.white)
                        : const Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.delete_sweep, color: Colors.white, size: 32),
                              SizedBox(width: 12),
                              Text(
                                "TEST CLEAR",
                                style: TextStyle(
                                  color: Colors.white,
                                  fontSize: 20,
                                  fontWeight: FontWeight.bold,
                                  letterSpacing: 1.2,
                                ),
                              ),
                            ],
                          ),
                  ),
                ),
              ),
              
              const Spacer(),
              const Text(
                "Works on Android only",
                style: TextStyle(color: Colors.grey, fontSize: 12),
              ),
            ],
          ),
        ),
      ),
    );
  }
}