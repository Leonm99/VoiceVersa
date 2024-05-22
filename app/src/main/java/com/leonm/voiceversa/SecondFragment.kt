package com.leonm.voiceversa



import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.leonm.voiceversa.databinding.FragmentSecondBinding
import kotlinx.coroutines.runBlocking


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private var isKeyVisible = false
    private lateinit var buttonShowPassword: MaterialButton
    private var savedSelection = 0
    private var savedModelSelection = 0
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        sharedPreferencesManager = SharedPreferencesManager(requireContext())

        setupLanguageDropdown()
        setupModelDropdown()
        setupPasswordVisibilityToggle()
        setupApiKeyField()
        setupToggleSwitch()

        return binding.root
    }

    private fun setupLanguageDropdown() {
        val languagesAdapter = ArrayAdapter(
            requireContext(), R.layout.dropdown_item, resources.getStringArray(R.array.string_array_languages)
        )
        val autoCompleteTextView = binding.autocompleteText
        autoCompleteTextView.setAdapter(languagesAdapter)

        savedSelection = sharedPreferencesManager.loadData("LANGUAGE", "0").toInt()
        autoCompleteTextView.setText(resources.getStringArray(R.array.string_array_languages)[savedSelection], false)

        autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            sharedPreferencesManager.saveData("LANGUAGE", position)
            sharedPreferencesManager.saveData("LANGUAGE_STRING", parent.getItemAtPosition(position).toString())
            savedSelection = position
            Toast.makeText(requireContext(), "Selected: ${parent.getItemAtPosition(position)}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupModelDropdown() {
        val items = sharedPreferencesManager.loadData("MODEL_IDS", listOf(""))
        val modelsAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, items)
        val autoCompleteTextView = binding.autocompleteText2
        autoCompleteTextView.setAdapter(modelsAdapter)

        savedModelSelection = sharedPreferencesManager.loadData("SELECTED_MODEL", "0").toInt()
        autoCompleteTextView.setText(items[savedModelSelection], false)

        autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            sharedPreferencesManager.saveData("SELECTED_MODEL", position)
            sharedPreferencesManager.saveData("MODEL_STRING", parent.getItemAtPosition(position).toString())
            savedModelSelection = position
            Toast.makeText(requireContext(), "Selected: ${parent.getItemAtPosition(position)}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPasswordVisibilityToggle() {
        buttonShowPassword = binding.buttonShowPassword
        buttonShowPassword.setOnClickListener { togglePasswordVisibility() }
    }

    private fun setupApiKeyField() {
        binding.editTextTextPassword.setText(sharedPreferencesManager.loadData("API_KEY", ""))
        binding.editTextTextPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.editTextTextPassword.text.toString()
                saveApiKey(text)
            }
        }
    }

    private fun setupToggleSwitch() {
        val toggleSwitch = binding.switch1
        toggleSwitch.isChecked = sharedPreferencesManager.loadData("TOGGLE_SWITCH", false)
        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferencesManager.saveData("TOGGLE_SWITCH", isChecked)
        }
    }

    private fun saveApiKey(text: String) {
        runBlocking {
            val openAiHandler = OpenAiHandler(requireContext())
            if (!openAiHandler.checkApiKey(text)) {
                Toast.makeText(context, "API key not valid!", Toast.LENGTH_SHORT).show()
            } else {
                sharedPreferencesManager.saveData("API_KEY", text)
                Toast.makeText(context, "API key is valid!", Toast.LENGTH_SHORT).show()
                refreshModelDropdown()
            }
        }
    }

    private fun refreshModelDropdown() {
        val items = sharedPreferencesManager.loadData("MODEL_IDS", listOf(""))
        val modelsAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, items)
        val autoCompleteTextView = binding.autocompleteText2
        autoCompleteTextView.setAdapter(modelsAdapter)

        savedModelSelection = sharedPreferencesManager.loadData("SELECTED_MODEL", "0").toInt()
        autoCompleteTextView.setText(items[savedModelSelection], false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun togglePasswordVisibility() {
        isKeyVisible = !isKeyVisible
        binding.editTextTextPassword.transformationMethod = if (isKeyVisible) null else PasswordTransformationMethod.getInstance()
        binding.buttonShowPassword.setIconResource(if (isKeyVisible) R.drawable.visibility_off_fill else R.drawable.visibility_fill)
        binding.editTextTextPassword.setSelection(binding.editTextTextPassword.text.length)
    }
}
