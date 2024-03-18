package com.leonm.voiceversa


import android.content.Context
import android.os.Environment
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File


class YoutubeDownloader {


    fun downloadAudio(context: Context, youtubeLink: String) {
        try {
            YoutubeDL.getInstance().init(context)
        } catch (e: YoutubeDLException) {
            Log.e("YTDL", "Failed to initialize youtubedl-android", e)
        }

        val outputDir = File(
            context.filesDir,
            "youtubedl-android"
        )
        val request = YoutubeDLRequest(youtubeLink)
        request.addOption("-o", outputDir.absolutePath + "/%(title)s.%(ext)s")
        request.addOption("-f", "bestaudio/best")
        YoutubeDL.getInstance().execute(
            request,
            { progress: Int, etaInSeconds: Int ->
                println("$progress% (ETA $etaInSeconds seconds)")
            }.toString()
        )
    }


}