package com.leonm.voiceversa



import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.leonm.voiceversa.databinding.FragmentSecondBinding


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment(){
    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!




    private var isPasswordVisible = false
    private lateinit var buttonShowPassword: ImageButton
    private var savedSelection: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        setHasOptionsMenu(true)
        _binding = FragmentSecondBinding.inflate(inflater, container, false)

        val sharedPreferencesManager = SharedPreferencesManager(context)

        buttonShowPassword = binding.buttonShowPassword
        val aa: ArrayAdapter<*> = ArrayAdapter<Any?>(this.requireContext(), R.layout.dropdown_item, resources.getStringArray(R.array.string_array_languages))

        val autoCompleteTextView = binding.autocompleteText

        autoCompleteTextView.setAdapter(aa)

        // Set the saved selection, if any
        savedSelection = sharedPreferencesManager.loadData<String>("LANGUAGE", "-1")
        if (savedSelection!!.toInt() != -1) {

            autoCompleteTextView.setText(resources.getStringArray(R.array.string_array_languages)[savedSelection!!.toInt()], false)
        }

        val savedText = sharedPreferencesManager.loadData("API_KEY", "")
        binding.editTextTextPassword.setText(savedText)

        autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            // Save the selected item to SharedPreferences
            sharedPreferencesManager.saveData("LANGUAGE", position)
            savedSelection = sharedPreferencesManager.loadData("LANGUAGE", "-1")
            sharedPreferencesManager.saveData("LANGUAGE_STRING", resources.getStringArray(R.array.string_array_languages)[savedSelection!!.toInt()])
            Toast.makeText(requireContext(), "Selected: ${parent.getItemAtPosition(position)}", Toast.LENGTH_SHORT).show()
        }

        buttonShowPassword.setOnClickListener {
            togglePasswordVisibility()
        }



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
                if (!SharedPreferencesManager(context).isValidApiKey()){
                    Toast.makeText(context, "API key not valid!", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(context, "API key is valid!", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            // Show password
            binding.editTextTextPassword.transformationMethod = null
        } else {
            // Hide password
            binding.editTextTextPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        }
        // Move cursor to the end of text
        binding.editTextTextPassword.setSelection(binding.editTextTextPassword.text.length)
    }

}