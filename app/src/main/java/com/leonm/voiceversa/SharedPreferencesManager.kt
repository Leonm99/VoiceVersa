package com.leonm.voiceversa

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class SharedPreferencesManager(context: Context) {
     val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    }
     val gson: Gson by lazy { Gson() }

    fun <T> saveData(key: String, data: T) {
        val json = gson.toJson(data)
        sharedPreferences.edit().putString(key, json).apply()
    }

    inline fun <reified T> loadData(key: String, defaultValue: T): T {
        val json = sharedPreferences.getString(key, null)
        return json?.let {
            gson.fromJson(it, T::class.java)
        } ?: defaultValue
    }
}
