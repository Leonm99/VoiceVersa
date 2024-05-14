package com.leonm.voiceversa

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class SharedPreferencesManager(val context: Context?) {
    val sharedPreferences: SharedPreferences = context!!.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val openAiHandler = OpenAiHandler()


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


}