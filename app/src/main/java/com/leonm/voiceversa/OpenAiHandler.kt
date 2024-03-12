package com.leonm.voiceversa

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.aallam.openai.api.audio.Transcription
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.wait
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class OpenAiHandler {
    private var openai: OpenAI? = null
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
     fun callOpenAI(context: Context): OpenAI? {

        sharedPreferencesManager = SharedPreferencesManager(context)
        val key = sharedPreferencesManager.loadData<String>("API_KEY", "Default Value")

                 openai =
                 OpenAI(
                     token = key,
                     timeout = Timeout(socket = 120.seconds),
                     // additional configurations...
                 )




             //Toast.makeText(context, "Api Key INVALID!", Toast.LENGTH_LONG).show()

         return openai
    }

     suspend fun whisper(
        openAI: OpenAI,
        path: String = "",
    ): Transcription {

        println("\n>️ Create transcription...")
        val transcriptionRequest =
            TranscriptionRequest(
                audio = FileSource(path = path.toPath(), fileSystem = FileSystem.SYSTEM),
                model = ModelId("whisper-1"),
            )

        return openAI.transcription(transcriptionRequest)
    }

    suspend fun summarize(
        openai: OpenAI,
        userText: String = "",
    ): ChatCompletion {
        val chatCompletionRequest =

            ChatCompletionRequest(
                model = ModelId("gpt-3.5-turbo-0125"),
                messages =
                    listOf(
                        ChatMessage(
                            role = ChatRole.System,
                            content =
                                "You will be provided with a transcription of a voice message," +
                                    "and your task is to summarize it in the language its written in.",
                        ),
                        ChatMessage(
                            role = ChatRole.User,
                            content = userText,
                        ),
                    ),
            )

        return openai.chatCompletion(chatCompletionRequest)
    }

    suspend fun translate(context: Context,
        openai: OpenAI,
        userText: String = "",
    ): ChatCompletion {

        sharedPreferencesManager = SharedPreferencesManager(context)
        val language = sharedPreferencesManager.loadData<String>("LANGUAGE_STRING", "English")
        val chatCompletionRequest =

            ChatCompletionRequest(
                model = ModelId("gpt-3.5-turbo-0125"),
                messages =
                    listOf(
                        ChatMessage(
                            role = ChatRole.System,
                            content =
                                "You will be provided with a transcription of a voice message, " +
                                    "and your task is to translate it into $language.",
                        ),
                        ChatMessage(
                            role = ChatRole.User,
                            content = userText,
                        ),
                    ),
            )

        return openai.chatCompletion(chatCompletionRequest)
    }

    fun convertAudio(
        context: Context,
        inputUri: Uri,
        outputFormat: String,
    ): Uri? {
        return try {
            val outputPath = context.cacheDir.absolutePath + "/output.$outputFormat"

            // FFmpeg command for audio conversion // This option makes FFmpeg overwrite the output file
            val command =
                arrayOf(
                    "-y",
                    "-i",
                    inputUri.path,
                    "-c:a",
                    outputFormat,
                    outputPath,
                )

            val result = FFmpeg.execute(command)

            if (result == 0) {
                Uri.fromFile(File(outputPath))
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }




}
