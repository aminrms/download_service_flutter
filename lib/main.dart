import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_application_1/download_service.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: DownloadScreen(),
    );
  }
}

class DownloadScreen extends StatefulWidget {
  @override
  _DownloadScreenState createState() => _DownloadScreenState();
}

class _DownloadScreenState extends State<DownloadScreen> {
  static const platform =
      MethodChannel('com.example.flutter_application_1/download');
  final _checkNotificationPermission = DownloadService().requestNotificationPermission;
  @override
  void initState() {
    super.initState();
    _checkNotificationPermission(context);
  }

  Future<void> _startDownload() async {
    try {
      await platform.invokeMethod('startDownloadService');
    } on PlatformException catch (e) {
      print("Failed to start download service: '${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Download Demo'),
      ),
      body: Center(
        child: ElevatedButton(
          onPressed: _startDownload,
          child: Text('Start Download'),
        ),
      ),
    );
  }
}
