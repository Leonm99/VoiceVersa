package com.example.whispdroid

import android.content.Context
import android.net.Uri
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
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import kotlin.time.Duration.Companion.seconds

class ChatHandler {



        private var openai: OpenAI? = null

        public fun callOpenAI(): OpenAI? {

            openai = OpenAI(
                token = "API_KEY_HERE",
                timeout = Timeout(socket = 120.seconds),
                // additional configurations...

            )
            return openai
        }

        public suspend fun summarize(openai: OpenAI, userText: String = ""): ChatCompletion {


            val chatCompletionRequest = ChatCompletionRequest(
                model = ModelId("gpt-3.5-turbo-0125"),
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        content = "Beschreibe folgenden text, gerne auch in stichpunkten."
                    ),
                    ChatMessage(
                        role = ChatRole.User,
                        content = userText
                    )
                )

            )


            return openai.chatCompletion(chatCompletionRequest)
        }


    }