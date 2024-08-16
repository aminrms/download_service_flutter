package com.example.flutter_application_1

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class DownloadService : Service() {

    private val channelId = "DownloadServiceChannel"
    private val notificationId = 1
    private var job: Job? = null
    private val downloadUrl = "http://212.183.159.230/5MB.zip"  // Hard-coded URL

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(notificationId, createNotification("Download started", ""))
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                downloadFile(downloadUrl)
                // Hide the progress notification and show the completion notification
                cancelProgressNotification()
                showCompletionNotification("Download complete")
            } catch (e: Exception) {
                showFialedToDownloadNotification()
                Log.e("DownloadService", "Download failed with exception", e)
                // Reschedule using WorkManager if an exception occurs
                scheduleWorkManagerDownload(downloadUrl)
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Download Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private suspend fun downloadFile(url: String) {
        withContext(Dispatchers.IO) {
            val file = File(getExternalFilesDir(null), "downloadedFile")
            val urlConnection = URL(url).openConnection()
            val inputStream = urlConnection.getInputStream()
            val outputStream = FileOutputStream(file)

            val buffer = ByteArray(1024)
            var bytesRead: Int
            var totalBytesRead = 0
            val fileSize = urlConnection.contentLength

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                updateProgress(totalBytesRead, fileSize)
            }

            outputStream.close()
            inputStream.close()

            // Download complete, cancel the progress notification
            cancelProgressNotification()
            // Show completion notification
            showCompletionNotification("Download complete")
        }
    }

    private fun updateProgress(bytesRead: Int, totalBytes: Int) {
        val progress = (bytesRead * 100 / totalBytes)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Downloading...")
            .setContentText("$progress% completed")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progress, false)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }

    private fun cancelProgressNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(notificationId)  // Cancel the notification with the progress bar
    }

    private fun showCompletionNotification(content: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Download Service")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 1000, 500))  // Vibration pattern
            // Attach the pending intent to open the app
            .build()
    
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }

    private fun showFialedToDownloadNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Download Failed")
            .setContentText("Failed to download the file")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 1000, 500))  // Vibration pattern
            .setContentIntent(pendingIntent)  // Attach the pending intent to open the app
            .build()
    
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }

    private fun scheduleWorkManagerDownload(url: String) {
        val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("download_url" to url))
            .build()

        WorkManager.getInstance(this).enqueue(downloadWorkRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()

        scheduleWorkManagerDownload(downloadUrl)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
