package com.leonm.voiceversa

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
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
        if (intent?.action == Intent.ACTION_SEND && (intent.type?.startsWith("audio/") == true || intent.type?.startsWith("video/") == true)) {
            val audioUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            if (audioUri != null) {
                val audioFile = saveToCache(audioUri)
                if (audioFile != null) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        startFloatingService("TRANSCRIBE", audioFile.absolutePath)
                    }, 500)
                }
            }
        } else if (intent?.action == Intent.ACTION_SEND && (intent.type?.startsWith("text/") == true)){

            val link = intent.extras?.getString(Intent.EXTRA_TEXT)
            println("intent data: $link")


            Handler(Looper.getMainLooper()).postDelayed({
                startFloatingService("DOWNLOAD", link!!)
            }, 500)
        }
        finish()
    }

    private fun saveToCache(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val cacheDirectory: File? = cacheDir

            if (inputStream != null && cacheDirectory != null) {
                val file = File(cacheDirectory, "shared_audio_file.${getFileExtension(uri)}")
                inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                file
            } else {
                null
            }
        } catch (e: Exception) {
            // Handle the exception in a more meaningful way, e.g. log an error message
            null
        }
    }

    private fun getFileExtension(uri: Uri): String {
        val resolver = contentResolver
        val mimeTypeMap = android.webkit.MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(resolver.getType(uri)) ?: ""
    }

    private fun startFloatingService(
        command: String = "",
        path: String = ""
    ) {


        val intent = Intent(this, FloatingService::class.java)
        if (command.isNotBlank()) {
            intent.putExtra(INTENT_COMMAND, command)

        }
        if (path.isNotBlank()) {
            intent.putExtra("PATH", path)
        }
        startForegroundService(intent)
    }
}