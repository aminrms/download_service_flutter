import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

class DownloadService {
  static const platform = MethodChannel('com.example.flutter_application_1/download');

  Future<void> requestNotificationPermission(BuildContext context) async {
    var status = await Permission.notification.status;

    if (status.isDenied) {
      // Show dialog explaining why the permission is needed
      bool? retry = await showDialog<bool>(
        context: context,
        builder: (BuildContext context) => AlertDialog(
          title: Text('Notification Permission'),
          content: Text('This app requires notification permission to notify you about the download status.'),
          actions: [
            TextButton(
              child: Text('Deny'),
              onPressed: () {
                Navigator.of(context).pop(false);
              },
            ),
            TextButton(
              child: Text('Allow'),
              onPressed: () {
                Navigator.of(context).pop(true);
              },
            ),
          ],
        ),
      );

      if (retry == true) {
        // Try requesting the permission again
        if (await Permission.notification.request().isGranted) {
          print("Notification permission granted");
        } else {
          print("Notification permission denied");
        }
      }
    } else if (status.isPermanentlyDenied) {
      // User has permanently denied the permission, open app settings
      await openAppSettings();
    }
  }

  Future<void> startDownload(String url, BuildContext context) async {
    try {
      // Request notification permission first
      await requestNotificationPermission(context);

      // Now, start the download via the method channel
      await platform.invokeMethod('startDownload', {'url': url});
    } on PlatformException catch (e) {
      print("Failed to start download: '${e.message}'.");
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("Failed to start download: ${e.message}")),
      );
    }
  }
}
