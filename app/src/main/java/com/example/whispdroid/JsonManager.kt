package com.example.whispdroid

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException

class JsonManager(private val context: Context) {

    private val gson = Gson()

    fun loadTranscriptions(): List<Transcription> {
        try {
            val file = File(context.filesDir, "transcriptions.json")
            if (file.exists()) {
                val jsonTranscriptions = file.readText()
                return gson.fromJson(jsonTranscriptions, object : TypeToken<List<Transcription>>() {}.type)
            }
        } catch (e: IOException) {
            handleError(e)
        }
        return emptyList()
    }

    fun saveTranscriptions(transcriptions: List<Transcription>) {
        try {
            val jsonTranscriptions = gson.toJson(transcriptions)
            val file = File(context.filesDir, "transcriptions.json")
            file.writeText(jsonTranscriptions)
        } catch (e: IOException) {
            handleError(e)
        }
    }

    private fun handleError(exception: Exception) {
        // Handle exceptions here, log or display meaningful error messages
        exception.printStackTrace()
    }
}