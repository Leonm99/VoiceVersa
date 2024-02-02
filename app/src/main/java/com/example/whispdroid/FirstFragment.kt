package com.example.whispdroid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.EditText
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.whispdroid.databinding.FragmentFirstBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var localPath: String? = null
    private val whisperHandler = WhisperHandler()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStartTranscription.setOnClickListener {
            lifecycleScope.launch {
                try {
                    showLoading(true) // Show loading indicator

                    val result =
                        whisperHandler.whisper(whisperHandler.callOpenAI()!!, localPath!!).text

                    withContext(Dispatchers.Main) {
                        binding.textOutput.text = result.toString()
                    }
                } catch (e: Exception) {
                    handleError(e)
                } finally {
                    showLoading(false) // Hide loading indicator
                }
            }
        }

        binding.buttonChooseFile.setOnClickListener {
            lifecycleScope.launch {
                try {
                    openFileChooser()
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun handleError(exception: Exception) {
        // Handle exceptions here, log or display meaningful error messages
        exception.printStackTrace()
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        fileChooserLauncher.launch(intent)
    }

    private val fileChooserLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Get the selected file URI
            val data = result.data
            if (data != null) {
                val selectedFileUri = data.data

                // Save the selected file to the app's internal storage
                val savedFilePath = saveFileToInternalStorage(selectedFileUri)
                val fileText = requireView().findViewById<View>(R.id.pathTextBox) as EditText
                fileText.setText(savedFilePath)
                localPath = savedFilePath
            }
        }
    }


    private fun saveFileToInternalStorage(uri: Uri?): String? {
        return try {
            requireContext().contentResolver.openInputStream(uri!!)?.use { inputStream ->
                val fileExtension = getFileExtension(uri)
                val internalFile = File(requireContext().filesDir, "selected_file$fileExtension")
                FileOutputStream(internalFile).use { outputStream ->
                    inputStream.copyTo(outputStream, bufferSize = 4 * 1024)
                }
                internalFile.absolutePath
            }
        } catch (e: IOException) {
            handleError(e)
            null
        }
    }

    private fun getFileExtension(uri: Uri?): String {
        // Use ContentResolver to get the file extension from the URI
        val contentResolver = requireContext().contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return "." + mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri!!))
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }


}