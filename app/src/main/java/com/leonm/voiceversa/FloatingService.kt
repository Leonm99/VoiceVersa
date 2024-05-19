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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
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
        AppCompatDelegate.setDefaultNightMode(if (nightMode == Configuration.UI_MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

        window = Window(applicationContext, this, this, coroutineContext)

        val command = intent?.getStringExtra(INTENT_COMMAND)

        window?.open()
        val isValid = SharedPreferencesManager(applicationContext).loadData("isApiKeyValid", "false").toBoolean()

        if (!isValid) {
            launch(Dispatchers.Main) {
                window?.updateTextViewWithSlightlyUnevenTypingEffect("Please enter a valid OpenAI API key in the settings.")
                window?.disableButtons()
            }
        } else {

            when (command) {
                INTENT_COMMAND_TRANSCRIBE -> {

                    transcribeFile(intent.getStringExtra("PATH")!!)

                }

                INTENT_COMMAND_DOWNLOAD -> {
                    val ytdl = YoutubeDownloader()
                    clearCacheDirectory()

                    launch(Dispatchers.IO) {
                        val downloadJob = async { ytdl.downloadAudio(ContextWrapper(applicationContext), intent.getStringExtra("PATH")!!) }
                        downloadJob.await()

                        launch(Dispatchers.Main) {

                            transcribeFile(File(ContextWrapper(applicationContext).cacheDir, "test.mp3").absolutePath)
                        }

                        withContext(Dispatchers.Main) {
                            window?.disableButtons()
                            window?.updateTextViewWithSlightlyUnevenTypingEffect("Downloading file...")
                            window?.enableLoading()

                        }
                    }





                }

                INTENT_COMMAND_EXIT -> {
                    stopService()
                    return START_NOT_STICKY
                }
            }

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

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_GENERAL,
            "quicknote_general",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        channel.enableLights(false)
        channel.setShowBadge(false)
        channel.enableVibration(false)
        channel.setSound(null, null)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        try {
            manager.createNotificationChannel(channel)
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
            val transcription = Transcription(result, summarizedContent, translatedContent)
            transcriptions.add(transcription)
            summarizedContent = ""
            translatedContent = ""

            CoroutineScope(Dispatchers.IO).launch {
                transcriptions.addAll(jsonManager.loadTranscriptions())
                jsonManager.saveTranscriptions(transcriptions)
            }
            result = ""
        }
    }

    fun setSummary(value: String) {
        summarizedContent = value
    }

    fun setTranslation(value: String) {
        translatedContent = value
    }



    private fun transcribeFile(tempfile: String) {
        launch(Dispatchers.IO) {
            try {

                val audioFileUri: Uri? = tempfile.let { Uri.fromFile(File(it)) }
                val convertedUri = audioFileUri?.let {
                    OpenAiHandler().convertAudio(this@FloatingService, it, "mp3")
                }

                convertedUri?.let {
                    val openAIResult =
                        OpenAiHandler().callOpenAI(this@FloatingService) ?: return@launch
                    val whisperResult =
                        OpenAiHandler().whisper(this@FloatingService,openAIResult, it.path ?: return@launch)
                    inputLanguage = whisperResult
                    Log.d("FloatingService", "Input Language: $inputLanguage")

                    val correctedText = OpenAiHandler().correctSpelling(this@FloatingService,openAIResult,whisperResult)

                    result = correctedText
                    filePath = it.path ?: return@launch

                    window!!.enableButtons()
                    launch(Dispatchers.Main) {
                        window?.updateTextViewWithSlightlyUnevenTypingEffect(result)
                    }
                } ?: run {
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
}
