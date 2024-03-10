package com.leonm.voiceversa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.CoroutineContext

const val INTENT_COMMAND = "com.example.whispdroid.COMMAND"
const val INTENT_COMMAND_EXIT = "EXIT"
const val INTENT_COMMAND_SHOW = "START"

private const val NOTIFICATION_CHANNEL_GENERAL = "quicknote_general"
private const val CODE_FOREGROUND_SERVICE = 1
private const val CODE_EXIT_INTENT = 2

private val transcriptions = mutableListOf<Transcription>()

class FloatingService : Service(), CoroutineScope, WindowCallback {
    private val job = Job()
    private var window: Window? = null
    private var result: String = ""
    private var filePath: String = ""
    private var inputLanguage: String = ""
    override lateinit var jsonManager: JsonManager
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        jsonManager = JsonManager(applicationContext)
        showNotification()

        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        window = Window(this, this, this, coroutineContext)

        val command = intent?.getStringExtra(INTENT_COMMAND)
        val localPath = intent?.getStringExtra("PATH")
        if (command == INTENT_COMMAND_SHOW) {
            window?.open()

            launch(Dispatchers.IO) {
                try {
                    val audioFileUri: Uri? = localPath?.let { Uri.fromFile(File(it)) }
                    val convertedUri =
                        audioFileUri?.let {
                            OpenAiHandler().convertAudio(this@FloatingService, it, "mp3")
                        }

                    if (convertedUri != null) {
                        val openAIResult = OpenAiHandler().callOpenAI(this@FloatingService) ?: return@launch
                        val whisperResult =
                            OpenAiHandler().whisper(
                                openAIResult,
                                convertedUri.path ?: return@launch,
                            )
                        inputLanguage = whisperResult.language.toString()
                        Log.d("FloatingService", "Language: $inputLanguage")

                        result = whisperResult.text
                        filePath = convertedUri.path ?: return@launch

                        window!!.enableButtons()
                        launch(Dispatchers.Main) {
                            window?.updateTextViewWithSlightlyUnevenTypingEffect(result)
                        }
                    } else {
                        // Handle the conversion failure
                    }
                } catch (e: Exception) {
                    // Handle exceptions, log, or perform any necessary cleanup
                    e.printStackTrace()
                } finally {
                    // Clear the cache directory
                    clearCacheDirectory()
                }
            }
        }

        // Exit the service if we receive the EXIT command.
        // START_NOT_STICKY is important here, we don't want
        // the service to be relaunched.
        if (command == INTENT_COMMAND_EXIT) {
            stopService()
            return START_NOT_STICKY
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun stopService() {
        Log.d("FloatingService", "stopService")
        // stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.d("FloatingService", "onRebind")
    }

    private fun showNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val exitIntent =
            Intent(this, FloatingService::class.java).apply {
                putExtra(INTENT_COMMAND, INTENT_COMMAND_EXIT)
            }

        val exitPendingIntent =
            PendingIntent.getService(
                this,
                CODE_EXIT_INTENT,
                exitIntent,
                PendingIntent.FLAG_IMMUTABLE,
            )

        try {
            with(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_GENERAL,
                    "quicknote_general",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            ) {
                enableLights(false)
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                manager.createNotificationChannel(this)
            }
        } catch (ignored: Exception) {
            // Handle the exception if needed
        }

        with(
            NotificationCompat.Builder(
                this,
                NOTIFICATION_CHANNEL_GENERAL,
            ),
        ) {
            setTicker(null)
            setContentTitle(getString(R.string.app_name))
            setContentText("VoiceVersa is running")
            setAutoCancel(false)
            setOngoing(true)
            setWhen(System.currentTimeMillis())
            setSmallIcon(R.drawable.icon3)
            priority = NotificationManager.IMPORTANCE_DEFAULT
            addAction(
                NotificationCompat.Action(
                    0,
                    "Exit",
                    exitPendingIntent,
                ),
            )
            startForeground(CODE_FOREGROUND_SERVICE, build())
        }
    }

    private fun clearCacheDirectory() {
        val cacheDirectory: File = cacheDir
        cacheDirectory.listFiles()?.forEach { file ->
            file.delete()
        }
    }

    override fun onContentButtonClicked() {
        if (result.isNotEmpty()) {
            transcriptions.clear()
            val transcription = Transcription(result)
            transcriptions.add(transcription)

            launch(Dispatchers.IO) {
                transcriptions.addAll(jsonManager.loadTranscriptions())
                jsonManager.saveTranscriptions(transcriptions)
            }
            result = ""
        }
    }
}
