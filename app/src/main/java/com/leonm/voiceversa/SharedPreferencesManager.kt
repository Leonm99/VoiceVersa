package com.leonm.voiceversa

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson


class SharedPreferencesManager(context: Context): PreferenceDataStore() {

    private var masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    var sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )





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

    override fun putString(key: String, value: String?) {
        saveData(key, value)
    }

    override fun getString(key: String, defValue: String?): String? {
        return loadData(key, defValue)
    }
}
