package com.leonm.voiceversa

import android.content.Context
import android.net.Uri
import android.text.Html
import android.util.Log
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds


class OpenAiHandler(private val context: Context) {
    private var openai: OpenAI? = null
    private val sharedPreferencesManager: SharedPreferencesManager = SharedPreferencesManager(context)
    private val client = OkHttpClient()
    private var language: String = "English"
    private val MAX_RETRIES = 3
    private val RETRY_DELAY_MS = 1000L

    init {
        initOpenAI()
    }

   fun initOpenAI() {
        val key = sharedPreferencesManager.getString("api_key_preference", "Default Value")
        openai = OpenAI(
            token = key!!,
            timeout = Timeout(socket = 120.seconds)
        )
    }

    suspend fun whisper(path: String): String {
        println("Create transcription...")
        val transcriptionRequest = TranscriptionRequest(
            audio = FileSource(path = path.toPath(), fileSystem = FileSystem.SYSTEM),
            model = ModelId("whisper-1")
        )
        val useCorrection = sharedPreferencesManager.getBoolean("switch_preference", false)
        var result = openai?.transcription(transcriptionRequest)?.text.orEmpty()

        if (useCorrection) {
            result = correctSpelling(result)
        }
        return result
    }

    suspend fun summarize(userText: String): String {
        val model = getModel()
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(model),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "You will be provided with a transcription, and your task is to summarize it in the SAME language it's written in."
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = userText
                )
            )
        )
        return openai?.chatCompletion(chatCompletionRequest)?.choices?.get(0)?.message?.content.orEmpty()
    }

    suspend fun translate(userText: String): String {
        val model = getModel()
        language = sharedPreferencesManager.getString("language_list_preference", "English")!!
        val language = language.uppercase()
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(model),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "You will be provided with a transcription, and your task is to translate it into $language."
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = userText
                )
            )
        )
        return openai?.chatCompletion(chatCompletionRequest)?.choices?.get(0)?.message?.content.orEmpty()
    }

    suspend fun correctSpelling(userText: String): String {
        val model = getModel()
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(model),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "Your task is to correct any spelling discrepancies in the transcribed text. Make the text look more presentable Add necessary punctuation such as periods, commas, capitalization, paragraphs, line breaks and use only the context provided. Use the same language the text is written in."
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = userText
                )
            )
        )
        return openai?.chatCompletion(chatCompletionRequest)?.choices?.get(0)?.message?.content.orEmpty()
    }

    fun convertAudio(inputUri: Uri, outputFormat: String): Uri? {
        return try {
            val outputPath = File(context.cacheDir, "output.$outputFormat").absolutePath
            val result = FFmpeg.execute(arrayOf("-y", "-i", inputUri.path, "-c:a", outputFormat, outputPath))
            if (result == 0) Uri.fromFile(File(outputPath)) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    suspend fun checkApiKey(apiKey: String): Boolean {
        val request = Request.Builder()
            .url("https://api.openai.com/v1/engines")
            .header("Authorization", "Bearer $apiKey")
            .build()

        repeat(MAX_RETRIES) { attempt ->
            try {
                val response: Response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val isApiKeyValid = apiKey.length == 51 && response.isSuccessful
                if (isApiKeyValid) return true
                if (response.code in 500..599) delay(RETRY_DELAY_MS) else return false
            } catch (e: IOException) {
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS) else return false
            }
        }
        return false
    }

    fun alertApiKey() {
        val textInputLayout = FrameLayout(context).apply {
            setPadding(
                context.resources.getDimensionPixelOffset(R.dimen.dp_19),
                0,
                context.resources.getDimensionPixelOffset(R.dimen.dp_19),
                0
            )
        }
        val input = EditText(context).apply {
            setHintTextColor(context.resources.getColor(R.color.md_theme_dark_onPrimary))
        }
        textInputLayout.addView(input)

        val alert = AlertDialog.Builder(context, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle("API Key")
            .setCancelable(false)
            .setView(textInputLayout)
            .setMessage("Please enter a valid OpenAI API key.")
            .setPositiveButton(Html.fromHtml("<font color='#FFFFFF'>Submit</font>", 0)) { dialog, _ ->
                var isKeyValid = false
                runBlocking { isKeyValid = checkApiKey(input.text.toString()) }
                if (isKeyValid) {
                    sharedPreferencesManager.putString("api_key_preference", input.text.toString())
                    sharedPreferencesManager.putBoolean("isApiKeyValid", true)
                    Toast.makeText(context, "API key is valid", Toast.LENGTH_LONG).show()
                    dialog.cancel()
                } else {
                    Toast.makeText(context, "API key is invalid", Toast.LENGTH_LONG).show()
                    alertApiKey()
                }
            }
            .setNegativeButton(Html.fromHtml("<font color='#FFFFFF'>Exit</font>", 0)) { dialog, _ ->
                dialog.cancel()
                exitProcess(0)
            }
            .create()

        alert.show()
    }



    private fun getModel(): String {
        return sharedPreferencesManager.getString("model_list_preference", "gpt-3.5-turbo").toString()
    }
}

