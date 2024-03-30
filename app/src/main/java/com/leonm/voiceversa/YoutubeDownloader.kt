package com.leonm.voiceversa


import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.concurrent.thread

class YoutubeDownloader {




    suspend fun downloadAudio(context: Context, youtubeLink: String) = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(youtubeLink)
        request.addOption("-o", File(context.cacheDir, "test.mp3").absolutePath)
        request.addOption("-f", "bestaudio/best")

        YoutubeDL.getInstance().init(context)
        YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, extraParam ->
            println("$progress% (ETA $etaInSeconds seconds) - $extraParam")
        }
    }


}