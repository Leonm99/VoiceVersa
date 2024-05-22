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
class SecondFragment : Fragment(){
    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!




    private var isKeyVisible = false
    private lateinit var buttonShowPassword: MaterialButton
    private var savedSelection: Int = 0
    private var savedModelSelection: Int = 0
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        setHasOptionsMenu(true)
        _binding = FragmentSecondBinding.inflate(inflater, container, false)

        val sharedPrefs = SharedPreferencesManager(requireContext())



        val languagesAdapter = ArrayAdapter<Any?>(requireContext(), R.layout.dropdown_item, resources.getStringArray(R.array.string_array_languages))
        val autoCompleteTextView = binding.autocompleteText
        autoCompleteTextView.setAdapter(languagesAdapter)

        savedSelection = sharedPrefs.loadData("LANGUAGE", "0").toInt()
        autoCompleteTextView.setText(resources.getStringArray(R.array.string_array_languages)[savedSelection], false)


        val savedApiKey = sharedPrefs.loadData("API_KEY", "")
        binding.editTextTextPassword.setText(savedApiKey)

        autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            sharedPrefs.saveData("LANGUAGE", position)
            savedSelection = sharedPrefs.loadData("LANGUAGE", "0").toInt()

            sharedPrefs.saveData("LANGUAGE_STRING", resources.getStringArray(R.array.string_array_languages)[savedSelection])
            Toast.makeText(requireContext(), "Selected: ${parent.getItemAtPosition(position)}", Toast.LENGTH_SHORT).show()
        }

        buttonShowPassword = binding.buttonShowPassword
        buttonShowPassword.setOnClickListener {
            togglePasswordVisibility()
        }





        val items = sharedPrefs.loadData("MODEL_IDS", listOf(""))
        val modelsAdapter = ArrayAdapter<Any?>(requireContext(), R.layout.dropdown_item, items)
        val autoCompleteTextView2 = binding.autocompleteText2
        autoCompleteTextView2.setAdapter(modelsAdapter)

        savedModelSelection = sharedPrefs.loadData("SELECTED_MODEL", "0").toInt()
        autoCompleteTextView2.setText(items[savedModelSelection], false)

        autoCompleteTextView2.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            sharedPrefs.saveData("SELECTED_MODEL", position)
            savedModelSelection = sharedPrefs.loadData("SELECTED_MODEL", "0").toInt()

            sharedPrefs.saveData("MODEL_STRING", items[savedModelSelection])
            Log.d("Selected Model", items[savedModelSelection])
            Log.d("Selected Model", sharedPrefs.loadData("MODEL_STRING", ""))
            Toast.makeText(requireContext(), "Selected: ${parent.getItemAtPosition(position)}", Toast.LENGTH_SHORT).show()
        }

        val toggleSwitch = binding.switch1
        toggleSwitch.isChecked = sharedPrefs.loadData("TOGGLE_SWITCH", false)
        toggleSwitch.setOnCheckedChangeListener { _, isChecked -> sharedPrefs.saveData("TOGGLE_SWITCH", isChecked) }



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferencesManager = SharedPreferencesManager(context)
        // Save the text to SharedPreferences when the user finishes editing
        binding.editTextTextPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.editTextTextPassword.text.toString()
                sharedPreferencesManager.saveData("API_KEY", text)
                var openAiHandler = OpenAiHandler()
                runBlocking {


                    if (!openAiHandler.checkApiKey(text)){
                        Toast.makeText(context, "API key not valid!", Toast.LENGTH_SHORT).show()
                    }else{
                        sharedPreferencesManager.saveData("API_KEY", text)
                        Toast.makeText(context, "API key is valid!", Toast.LENGTH_SHORT).show()
                    }
                }


            }

            val items = sharedPreferencesManager.loadData("MODEL_IDS", listOf(""))
            val modelsAdapter = ArrayAdapter<Any?>(requireContext(), R.layout.dropdown_item, items)
            val autoCompleteTextView2 = binding.autocompleteText2
            autoCompleteTextView2.setAdapter(modelsAdapter)

            savedModelSelection = sharedPreferencesManager.loadData("SELECTED_MODEL", "0").toInt()
            autoCompleteTextView2.setText(items[savedModelSelection], false)

            val savedApiKey = sharedPreferencesManager.loadData("API_KEY", "")
            binding.editTextTextPassword.setText(savedApiKey)

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun togglePasswordVisibility() {
        isKeyVisible = !isKeyVisible
        if (isKeyVisible) {
            // Show password
            binding.editTextTextPassword.transformationMethod = null
            binding.buttonShowPassword.setIconResource(R.drawable.visibility_off_fill)
        } else {
            // Hide password
            binding.editTextTextPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.buttonShowPassword.setIconResource(R.drawable.visibility_fill)
        }
        // Move cursor to the end of text
        binding.editTextTextPassword.setSelection(binding.editTextTextPassword.text.length)
    }

}