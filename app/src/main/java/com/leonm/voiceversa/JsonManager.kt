package com.leonm.voiceversa

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException

class JsonManager(private val context: Context) {
    private val gson = Gson()
    private val fileName = "Data.json"
    private val file: File
        get() = File(context.filesDir, fileName)

    fun loadTranscriptions(): List<Transcription> {
        return try {
            if (file.exists()) {
                val jsonTranscriptions = file.readText()
                gson.fromJson(jsonTranscriptions, object : TypeToken<List<Transcription>>() {}.type)
            } else {
                emptyList()
            }
        } catch (e: IOException) {
            handleError(e)
            emptyList()
        }
    }

    fun saveTranscriptions(transcriptions: List<Transcription>) {
        try {
            val jsonTranscriptions = gson.toJson(transcriptions)
            file.writeText(jsonTranscriptions)
        } catch (e: IOException) {
            handleError(e)
        }
    }

    private fun handleError(exception: Exception) {
        // Handle exceptions here, log or display meaningful error messages
        Log.e("JsonManager", "Error: ${exception.message}", exception)
    }
}
