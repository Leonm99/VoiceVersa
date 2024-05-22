package com.leonm.voiceversa

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
        if (intent?.action == Intent.ACTION_SEND) {
            when {
                intent.type?.startsWith("audio/") == true || intent.type?.startsWith("video/") == true -> {
                    val uri = getUriFromIntent(intent)
                    uri?.let {
                        val audioFile = saveToCache(it)
                        audioFile?.let { file ->
                            startFloatingService("TRANSCRIBE", file.absolutePath)
                        }
                    }
                }
                intent.type?.startsWith("text/") == true -> {
                    val link = intent.extras?.getString(Intent.EXTRA_TEXT)
                    link?.let {
                        startFloatingService("DOWNLOAD", it)
                    }
                }
            }
        }
        finish()
    }

    private fun getUriFromIntent(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun saveToCache(uri: Uri): File? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val file = File(cacheDir, "shared_audio_file.${getFileExtension(uri)}")
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                file
            }
        } catch (e: Exception) {
            // Log error message for better debugging
            Log.e("ReceiveIntentActivity", "Failed to save file to cache", e)
            null
        }
    }

    private fun getFileExtension(uri: Uri): String {
        val mimeType = contentResolver.getType(uri)
        return android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
    }

    private fun startFloatingService(command: String, path: String) {
        val intent = Intent(this, FloatingService::class.java).apply {
            putExtra(INTENT_COMMAND, command)
            putExtra("PATH", path)
        }
        startForegroundService(intent)
    }

    companion object {
        const val INTENT_COMMAND = "INTENT_COMMAND"
    }
}
