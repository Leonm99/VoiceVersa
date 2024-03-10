package com.leonm.voiceversa



import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
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

    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private lateinit var editText: EditText

    private var isPasswordVisible = false
    private lateinit var buttonShowPassword: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        setHasOptionsMenu(true)
        _binding = FragmentSecondBinding.inflate(inflater, container, false)

        sharedPreferencesManager = SharedPreferencesManager(this.requireContext())
        editText = binding.editTextTextPassword
        buttonShowPassword = binding.buttonShowPassword
        val aa: ArrayAdapter<*> = ArrayAdapter<Any?>(this.requireContext(), R.layout.dropdown_item, resources.getStringArray(R.array.string_array_languages))

        val autoCompleteTextView = binding.autocompleteText

        autoCompleteTextView.setAdapter(aa)

        // Set the saved selection, if any
        val savedSelection = sharedPreferencesManager.loadData<String>("LanguagePos", "-1")
        if (savedSelection.toInt() != -1) {
            autoCompleteTextView.setText(resources.getStringArray(R.array.string_array_languages)[savedSelection.toInt()], false)
        }

        val savedText = sharedPreferencesManager.loadData("ApiKey", "")
        editText.setText(savedText)

        autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            // Save the selected item to SharedPreferences
            sharedPreferencesManager.saveData("LanguagePos", position)
            Toast.makeText(requireContext(), "Selected: ${parent.getItemAtPosition(position)}", Toast.LENGTH_SHORT).show()
        }

        buttonShowPassword.setOnClickListener {
            togglePasswordVisibility()
        }



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Save the text to SharedPreferences when the user finishes editing
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = editText.text.toString()
                sharedPreferencesManager.saveData("ApiKey", text)
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
            editText.transformationMethod = null
        } else {
            // Hide password
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
        }
        // Move cursor to the end of text
        editText.setSelection(editText.text.length)
    }

}