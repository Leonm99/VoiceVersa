package com.leonm.voiceversa

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class SharedPreferencesManager(val context: Context?) {
    val sharedPreferences: SharedPreferences = context!!.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val gson = Gson()

    fun <T> saveData(key: String, data: T) {
        val json = gson.toJson(data)
        sharedPreferences.edit().putString(key, json).apply()
    }

    inline fun <reified T> loadData(key: String, defaultValue: T): T {
        val json = this.sharedPreferences.getString(key, null)
        return if (json != null) {
            gson.fromJson(json, object : TypeToken<T>() {}.type)
        } else {
            defaultValue
        }
    }



    fun isValidApiKey(): Boolean {
        val apiKey = loadData("API_KEY", "")

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.openai.com/v1/engines")
            .header("Authorization", "Bearer $apiKey")
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            println("Response: ${response.body?.string()}")
            saveData("isApiKeyValid", true)
            response.isSuccessful
        } catch (e: Exception) {
            println(e.message)
            saveData("isApiKeyValid", false)
            false
        }
    }





}