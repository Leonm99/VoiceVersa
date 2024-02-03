package com.example.whispdroid

import android.content.Context
import android.net.Uri
import com.aallam.openai.api.audio.Transcription
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.arthenica.mobileffmpeg.FFmpeg
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import kotlin.time.Duration.Companion.seconds

class WhisperHandler {

    private var openai: OpenAI? = null

    public fun callOpenAI(): OpenAI? {

        openai = OpenAI(
            token = "API_KEY_HERE",
            timeout = Timeout(socket = 120.seconds),
            // additional configurations...
        )
        return openai
    }

    public suspend fun whisper(openAI: OpenAI, path: String = ""): Transcription {


        println("\n>️ Create transcription...")
        val transcriptionRequest = TranscriptionRequest(
            audio = FileSource(path = path.toPath(), fileSystem = FileSystem.SYSTEM),
            model = ModelId("whisper-1"),
        )

        return openAI.transcription(transcriptionRequest)
    }


    suspend fun convertAudio(context: Context, inputUri: Uri, outputFormat: String): Uri? {
        return try {
            val outputPath = context.cacheDir.absolutePath + "/output.${outputFormat}"

            // FFmpeg command for audio conversion
            val command = arrayOf(
                "-y", // This option makes FFmpeg overwrite the output file
                "-i", inputUri.path,
                "-c:a", outputFormat,
                outputPath
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