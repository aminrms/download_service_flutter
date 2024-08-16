package com.example.flutter_application_1

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString("download_url") ?: return Result.failure()

        return try {
            downloadFile(url)
            Result.success()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Download failed", e)
            Result.retry()
        }
    }

    private suspend fun downloadFile(url: String) {
        withContext(Dispatchers.IO) {
            val file = File(applicationContext.getExternalFilesDir(null), "downloadedFile")
            val urlConnection = URL(url).openConnection()
            val inputStream = urlConnection.getInputStream()
            val outputStream = FileOutputStream(file)

            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.close()
            inputStream.close()
        }
    }
}
