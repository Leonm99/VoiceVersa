package com.leonm.voiceversa

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aallam.openai.api.audio.Transcription
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import kotlin.time.Duration.Companion.seconds

class OpenAiHandler {
    private var openai: OpenAI? = null
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private val client = OkHttpClient()
    private var selectedModel: String? = null
    private var language = "English"

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
        context: Context,
        openai: OpenAI,
        userText: String = "",
    ): ChatCompletion {

        sharedPreferencesManager = SharedPreferencesManager(context)
        selectedModel = sharedPreferencesManager.loadData<String>("MODEL_STRING", "gpt-3.5-turbo")
        Log.d("selectedModel",selectedModel.toString())

        val chatCompletionRequest =

            ChatCompletionRequest(
                model = ModelId(selectedModel.toString()),
                messages =
                    listOf(
                        ChatMessage(
                            role = ChatRole.System,
                            content =
                                "You will be provided with a transcription," +
                                    "and your task is to summarize it in the SAME language its written in.",
                        ),
                        ChatMessage(
                            role = ChatRole.User,
                            content = userText,
                        ),
                    ),
            )

        return openai.chatCompletion(chatCompletionRequest)
    }

    suspend fun translate(
        context: Context,
        openai: OpenAI,
        userText: String = "",
    ): ChatCompletion {

        sharedPreferencesManager = SharedPreferencesManager(context)
        selectedModel = sharedPreferencesManager.loadData<String>("MODEL_STRING", "gpt-3.5-turbo")
        language = sharedPreferencesManager.loadData<String>("LANGUAGE_STRING", "English").uppercase()
        Log.d("selectedModel", selectedModel!!)
        Log.d("language",language)
        val chatCompletionRequest =

            ChatCompletionRequest(
                model = ModelId(selectedModel.toString()),
                messages =
                    listOf(
                        ChatMessage(
                            role = ChatRole.System,
                            content =
                                "You will be provided with a transcription, " +
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
            val outputPath = File(context.cacheDir, "output.$outputFormat").absolutePath

            val result = FFmpeg.execute(arrayOf("-y", "-i", inputUri.path, "-c:a", outputFormat, outputPath))

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

    suspend fun getAvailableModels(context : Context){
        openai = callOpenAI(context)
        val models = openai?.models()
        // Regular expression to match the model IDs
        val regex = Regex("id=ModelId\\(id=(.*?)\\)")

        // Find all matches of the regex in the response string
        val modelIds = regex.findAll(models.toString()).map { it.groupValues[1] }.toList()
        val gptModels = modelIds.filter { it.contains("gpt") }
        sharedPreferencesManager = SharedPreferencesManager(context)
        sharedPreferencesManager.saveData("MODEL_IDS",gptModels)
        Log.d("MODEL_IDS",(modelIds.toString()))
    }

    suspend fun checkApiKey(apiKey : String) : Boolean{

        val request = Request.Builder()
            .url("https://api.openai.com/v1/engines")
            .header("Authorization", "Bearer $apiKey")
            .build()

        val response: Response = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { it }
        }

        val isApiKeyValid = apiKey.length == 51 && response.isSuccessful
        return isApiKeyValid
    }

}
