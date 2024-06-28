package com.leonm.voiceversa

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {



    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val preferenceManager: PreferenceManager = this.preferenceManager
        preferenceManager.preferenceDataStore = SharedPreferencesManager(requireContext())

        val apiKeyPreference: EditTextPreference? = findPreference("api_key_preference")
        val languagePreference: ListPreference? = findPreference("language_list_preference")
        val modelPreference: ListPreference? = findPreference("model_list_preference")
        val switchPreference: SwitchPreference? = findPreference("switch_preference")

        apiKeyPreference?.summaryProvider =
            Preference.SummaryProvider<EditTextPreference> { preference ->
                val text = preference.text
                if (text.isNullOrEmpty()) {
                    "Not set"
                } else {
                    "Key: $text"
                }
            }

        languagePreference?.summaryProvider =
            Preference.SummaryProvider<ListPreference> { preference ->
               "Selected Language: ${preference.value}"
            }

        modelPreference?.summaryProvider =
            Preference.SummaryProvider<ListPreference> { preference ->
                "Selected Model: ${preference.value}"
            }




    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "api_key_preference") {
            Log.i(
                "SettingsFragment",
                "Preference value was updated to: " + sharedPreferences!!.getString(key, "")
            )

            GlobalScope.launch(Dispatchers.IO) {
                val openAiHandler = OpenAiHandler(requireContext())
                if (!openAiHandler.checkApiKey(key)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "API key not valid!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "API key is valid!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}