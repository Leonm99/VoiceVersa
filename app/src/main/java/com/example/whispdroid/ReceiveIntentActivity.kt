package com.example.whispdroid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ReceiveIntentActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true) {
            // An audio file was received
            val audioUri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)

            if (audioUri != null) {
                // Save the audio file to app's cache directory
                val audioFile = saveToCache(audioUri)
                if (audioFile != null) {
                    // Start your FloatingService with a delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        startFloatingService("NOTE", audioFile.absolutePath)
                    }, 500)
                }
            }
        }

        // Finish the activity
        finish()
    }

    private fun saveToCache(uri: Uri): File? {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val cacheDir: File? = cacheDir

            if (inputStream != null && cacheDir != null) {
                val file = File(cacheDir, "shared_audio_file.${getFileExtension(uri)}")
                val outputStream = FileOutputStream(file)
                val buffer = ByteArray(1024)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()
                return file
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun getFileExtension(uri: Uri): String {
        val contentResolver = contentResolver
        val mimeTypeMap = android.webkit.MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ?: ""
    }


}

