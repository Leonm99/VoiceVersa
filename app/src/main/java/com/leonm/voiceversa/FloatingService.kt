package com.leonm.voiceversa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext

const val INTENT_COMMAND = "com.example.voiceversa.COMMAND"
const val INTENT_COMMAND_EXIT = "EXIT"
const val INTENT_COMMAND_TRANSCRIBE = "TRANSCRIBE"
const val INTENT_COMMAND_DOWNLOAD = "DOWNLOAD"

private const val NOTIFICATION_CHANNEL_GENERAL = "voiceversa_general"
private const val CODE_FOREGROUND_SERVICE = 1
private const val CODE_EXIT_INTENT = 2

private val transcriptions = mutableListOf<Transcription>()
private var summarizedContent: String = ""
private var translatedContent: String = ""

class FloatingService : Service(), CoroutineScope, WindowCallback{


    private val job = Job()
    private var window: Window? = null
    private var result: String = ""
    private var filePath: String = ""
    private var inputLanguage: String = ""
    override lateinit var jsonManager: JsonManager
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        jsonManager = JsonManager(applicationContext)
        showNotification()
        configureNightMode()

        window = Window(applicationContext, this, this, coroutineContext)
        window?.open()

        val command = intent?.getStringExtra(INTENT_COMMAND)

        val isApiKeyValid = SharedPreferencesManager(applicationContext)
            .getBoolean("isApiKeyValid", false)

        if (!isApiKeyValid) {
            window?.apply {
                disableButtons()
                updateTextViewWithSlightlyUnevenTypingEffect("Please enter a valid OpenAI API key in the settings.")
            }
        } else {
            handleCommand(command, intent)
        }

        return super.onStartCommand(intent, flags, START_NOT_STICKY)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.d("FloatingService", "onRebind")
    }

    override fun onContentButtonClicked() {
        if (result.isNotEmpty()) {
            transcriptions.clear()
            transcriptions.add(Transcription(result, summarizedContent, translatedContent))
            summarizedContent = ""
            translatedContent = ""

            launch(Dispatchers.IO) {
                transcriptions.addAll(jsonManager.loadTranscriptions())
                jsonManager.saveTranscriptions(transcriptions)
                sendTranscriptionSavedBroadcast()
            }
            result = ""
        }
    }

    private fun handleCommand(command: String?, intent: Intent?) {
        Log.d("FloatingService", "Received command: $command")

        when (command) {
            INTENT_COMMAND_TRANSCRIBE -> {
                val path = intent?.getStringExtra("PATH") ?: ""
                transcribeFile(path)
            }
            INTENT_COMMAND_DOWNLOAD -> {
                downloadAndTranscribeFile(intent?.getStringExtra("PATH") ?: "")
            }
            INTENT_COMMAND_EXIT -> {
                stopService()
            }
        }
    }

    private fun configureNightMode() {
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        AppCompatDelegate.setDefaultNightMode(
            if (nightMode == Configuration.UI_MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun showNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val exitIntent = Intent(this, FloatingService::class.java).apply {
            putExtra(INTENT_COMMAND, INTENT_COMMAND_EXIT)
        }
        val exitPendingIntent = PendingIntent.getService(
            this, CODE_EXIT_INTENT, exitIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_GENERAL, "voiceversa_general", NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannel(channel)

        with(NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_GENERAL)) {
            setContentTitle(getString(R.string.app_name))
            setContentText("VoiceVersa is running")
            setAutoCancel(false)
            setOngoing(true)
            setSmallIcon(R.drawable.icon3)
            priority = NotificationManager.IMPORTANCE_DEFAULT
            addAction(NotificationCompat.Action(0, "Exit", exitPendingIntent))
            startForeground(CODE_FOREGROUND_SERVICE, build())
        }
    }

    private fun downloadAndTranscribeFile(path: String) {
        clearCacheDirectory()
        val ytdl = YoutubeDownloader()

        window!!.loadingText.text = "Downloading file..."

        launch(Dispatchers.IO) {
            val downloadJob = async { ytdl.downloadAudio(ContextWrapper(applicationContext), path) }
            downloadJob.await()

            withContext(Dispatchers.Main) {
                transcribeFile(File(ContextWrapper(applicationContext).cacheDir, "test.mp3").absolutePath)
                window?.apply {
                    disableButtons()
                    updateTextViewWithSlightlyUnevenTypingEffect("Downloading file...")

                }
            }
        }
    }

    private fun transcribeFile(tempfile: String) {
        Log.d("FloatingService", "Starting transcription for file: $tempfile")

        window!!.loadingText.text = "Transcribing file..."
        launch(Dispatchers.IO) {
            try {
                val audioFile = File(tempfile)
                if (!audioFile.exists()) {
                    Log.e("FloatingService", "File does not exist: $tempfile")
                    return@launch
                }
                val audioFileUri = Uri.fromFile(audioFile)
                val convertedUri = OpenAiHandler(this@FloatingService).convertAudio(audioFileUri, "mp3")

                if (convertedUri == null) {
                    Log.e("FloatingService", "Failed to convert audio file.")
                    return@launch
                }

                OpenAiHandler(this@FloatingService).initOpenAI()
                val whisperResult = OpenAiHandler(this@FloatingService).whisper(convertedUri.path ?: return@launch)

                inputLanguage = whisperResult
                result = whisperResult
                filePath = convertedUri.path ?: return@launch

                window?.enableButtons()
                withContext(Dispatchers.Main) {
                    window?.updateTextViewWithSlightlyUnevenTypingEffect(result)
                }
            } catch (e: Exception) {
                Log.e("FloatingService", "Error during transcription", e)

            } finally {
                clearCacheDirectory()
            }
        }
    }

    private fun clearCacheDirectory() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun setSummary(value: String) {
        summarizedContent = value
    }

    fun setTranslation(value: String) {
        translatedContent = value
    }

    fun stopService() {
        Log.d("FloatingService", "stopService")
        stopForeground(true)
        stopSelf()
    }

    private fun sendTranscriptionSavedBroadcast() {
        val intent = Intent("TRANSCRIPTION_SAVED")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

}
