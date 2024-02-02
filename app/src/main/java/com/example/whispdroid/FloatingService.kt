package com.example.whispdroid


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.CoroutineContext


const val INTENT_COMMAND = "com.example.whispdroid.COMMAND"
const val INTENT_COMMAND_EXIT = "EXIT"
const val INTENT_COMMAND_SHOW = "NOTE"

private const val NOTIFICATION_CHANNEL_GENERAL = "quicknote_general"
private const val CODE_FOREGROUND_SERVICE = 1
private const val CODE_EXIT_INTENT = 2
private const val CODE_NOTE_INTENT = 3
private val whisperHandler = WhisperHandler()
private var job: Job = Job()
private var result: String = ""


class FloatingService : Service(), CoroutineScope {
    private var window: Window? = null
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        window = Window(this)

        val command = intent?.getStringExtra(INTENT_COMMAND)
        val localPath = intent?.getStringExtra("PATH")
        launch {
            try {
                val audioFileUri: Uri? = localPath?.let { Uri.fromFile(File(it)) }
                val convertedUri = audioFileUri?.let {
                    whisperHandler.convertAudio(
                        this@FloatingService,
                        it,
                        "mp3"
                    )
                } // Replace "flac" with the desired format
                if (convertedUri != null) {
                    val openAIResult = whisperHandler.callOpenAI() ?: return@launch
                    val whisperResult =
                        whisperHandler.whisper(
                            openAIResult,
                            convertedUri.path ?: return@launch
                        ).text
                    result = whisperResult
                    window!!.open()
                    window!!.updateTextView(result)
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


        // Be sure to show the notification first for all commands.
        // Don't worry, repeated calls have no effects.
        showNotification()

        // Exit the service if we receive the EXIT command.
        // START_NOT_STICKY is important here, we don't want
        // the service to be relaunched.
        if (command == INTENT_COMMAND_EXIT) {
            stopService()
            return START_NOT_STICKY
        }


        // Show the floating window for adding a new note.
        if (command == INTENT_COMMAND_SHOW) {


            window!!.open()


            return START_STICKY
        }
        return super.onStartCommand(intent, flags, startId)

    }

    override fun onBind(intent: Intent?): IBinder? = null


    /**
     * Remove the foreground notification and stop the service.
     */
    private fun stopService() {
        stopForeground(true)
        stopSelf()
    }


    /**
     * Create and show the foreground notification.
     */
    private fun showNotification() {

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val exitIntent = Intent(this, FloatingService::class.java).apply {
            putExtra(INTENT_COMMAND, INTENT_COMMAND_EXIT)
        }

        val noteIntent = Intent(this, FloatingService::class.java).apply {
            putExtra(INTENT_COMMAND, INTENT_COMMAND_SHOW)
        }

        val exitPendingIntent = PendingIntent.getService(
            this, CODE_EXIT_INTENT, exitIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notePendingIntent = PendingIntent.getService(
            this, CODE_NOTE_INTENT, noteIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // From Android O, it's necessary to create a notification channel first.

        try {

            with(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_GENERAL,
                    "banenenschwadron",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            ) {
                enableLights(false)
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                manager.createNotificationChannel(this)
            }
        } catch (ignored: Exception) {


        }

        with(
            NotificationCompat.Builder(
                this,
                NOTIFICATION_CHANNEL_GENERAL
            )
        ) {
            setTicker(null)
            setContentTitle(getString(R.string.app_name))
            setContentText("Tap me!")
            setAutoCancel(false)
            setOngoing(true)
            setWhen(System.currentTimeMillis())
            setSmallIcon(R.drawable.ic_launcher_foreground)
            priority = NotificationManager.IMPORTANCE_DEFAULT
            setContentIntent(notePendingIntent)
            addAction(
                NotificationCompat.Action(
                    0,
                    "Exit",
                    exitPendingIntent
                )
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

}