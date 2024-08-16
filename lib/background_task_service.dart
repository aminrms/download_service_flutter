import 'package:flutter/services.dart';

class BackgroundTaskService {
  static const MethodChannel _channel = MethodChannel('example.flutter_application_1/background');

  static Future<String?> startBackgroundTask() async {
    try {
      final String? result = await _channel.invokeMethod('startBackgroundTask');
      return result;
    } on PlatformException catch (e) {
      print("Failed to start background task: '${e.message}'.");
      return null;
    }
  }
}
