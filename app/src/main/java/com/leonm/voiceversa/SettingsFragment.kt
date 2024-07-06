package com.leonm.voiceversa


import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        val preferenceManager: PreferenceManager = this.preferenceManager
        preferenceManager.preferenceDataStore = SharedPreferencesManager(requireContext())

        setPreferencesFromResource(R.xml.root_preferences, rootKey)


        val button = preferenceManager.findPreference<Preference>("toastMsg")
        button?.setOnPreferenceClickListener {
            val alert = AlertDialog.Builder(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
                .setTitle("Delete Transcriptions.")
                .setCancelable(true)
                .setMessage("Are you sure you want to delete ALL Transcriptions?")
                .setPositiveButton(Html.fromHtml("<font color='#ff0000'>YES</font>", 0)) { dialog, _ ->
                    JsonManager(requireContext()).deleteTranscriptions()
                    Toast.makeText(context, "Deleted all Transcriptions", Toast.LENGTH_LONG).show()
                    dialog.cancel()

                }
                .setNegativeButton(Html.fromHtml("<font color='#008000'>Exit</font>", 0)) { dialog, _ ->
                    dialog.cancel()
                }
                .create()

            alert.show()

            true
        }


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
                "Language for Translations: ${preference.value}"
            }

        modelPreference?.summaryProvider =
            Preference.SummaryProvider<ListPreference> { preference ->
                "Model for Summarization: ${preference.value}"
            }

        switchPreference?.summaryProvider =
            Preference.SummaryProvider<SwitchPreference> { preference ->
                "Text Correction: ${preference.isChecked}"
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