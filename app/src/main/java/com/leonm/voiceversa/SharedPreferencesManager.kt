package com.leonm.voiceversa

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

    fun checkApiKeyValidity() {
        val apiKey = loadData("API_KEY", "") // Retrieve API key from SharedPreferences
        if (apiKey.isNotEmpty()) {
            checkApiKey(apiKey) { isValid ->
                // Cache the validation result
                cacheApiKeyValidity(isValid)
                if (isValid) {
                    // API key is valid, continue with app initialization

                } else {
                    // API key is invalid, show error message or take appropriate action

                   // Toast.makeText(context, "Api Key INVALID!", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            // API key is not available in SharedPreferences, prompt user to enter it
           // Toast.makeText(context, "Enter API Key in settings!", Toast.LENGTH_LONG).show()
        }
    }

    private fun cacheApiKeyValidity(isValid: Boolean) {
        saveData("isApiKeyValid", isValid)
    }


    private fun isValidApiKey(apiKey: String): Boolean {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.openai.com/v1/engines")
            .header("Authorization", "Bearer $apiKey")
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            println("Response: ${response.body?.string()}")
            response.isSuccessful
        } catch (e: Exception) {
            println(e.message)
            false
        }
    }

    private fun checkApiKey(apiKey: String, callback: (Boolean) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            val isValid = isValidApiKey(apiKey)
            callback(isValid)
        }
    }



}