package com.example.whispdroid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private val binding get() = _binding!!
    private var localPath: String? = null
    private val whisperHandler = WhisperHandler()
    private val transcriptions = mutableListOf<Transcription>()
    private lateinit var transcriptionAdapter: TranscriptionAdapter
    private lateinit var jsonManager: JsonManager
    lateinit var recyclerView: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        jsonManager = JsonManager(requireContext())
        recyclerView = binding.recyclerView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupRecyclerView()


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupClickListeners() {
        binding.buttonStartTranscription.setOnClickListener {
            lifecycleScope.launch {
                try {
                    showLoading(true)
                    val result = performWhisperTranscription()


                } catch (e: Exception) {
                    handleError(e)
                } finally {
                    showLoading(false)
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

    override fun onResume() {
        super.onResume()
        reloadData()
    }

    private fun setupRecyclerView() {
        // Check if the RecyclerView's adapter is already set
        if (recyclerView.adapter == null) {
            // Clear the transcriptions list to avoid duplication
            transcriptions.clear()

            // Load transcriptions from the JSON file
            transcriptions.addAll(jsonManager.loadTranscriptions())
            transcriptions.sortedByDescending { it.timestamp }
            transcriptionAdapter = TranscriptionAdapter(transcriptions)
            recyclerView.adapter = transcriptionAdapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

        }
    }

    private suspend fun performWhisperTranscription() {
        try {
            showLoading(true)
            val result = whisperHandler.whisper(whisperHandler.callOpenAI()!!, localPath!!).text
            saveTranscriptionToFile(result)
            updateOutputText()
        } catch (e: Exception) {
            handleError(e)
        } finally {
            showLoading(false)
        }
    }

    private suspend fun updateOutputText() {
        withContext(Dispatchers.Main) {

        }
        // Notify the adapter that data set has changed
        transcriptionAdapter.notifyDataSetChanged()
    }

    private fun saveTranscriptionToFile(content: String) {
        transcriptions.clear()
        val transcription = Transcription(content)
        transcriptions.add(transcription)


        transcriptions.addAll(jsonManager.loadTranscriptions())
        jsonManager.saveTranscriptions(transcriptions) // Replace with the actual path

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

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleFileSelection(result.data)
            }
        }

    private fun handleFileSelection(data: Intent?) {
        val selectedFileUri = data?.data
        if (selectedFileUri != null) {
            val savedFilePath = saveFileToInternalStorage(selectedFileUri)
            updateFilePathText(savedFilePath)
            localPath = savedFilePath
        }
    }

    private fun saveFileToInternalStorage(uri: Uri?): String? {
        return try {
            requireContext().contentResolver.openInputStream(uri!!)?.use { inputStream ->
                val fileExtension = getFileExtension(uri)
                val internalFile = File(
                    requireContext().filesDir,
                    "selected_file$fileExtension"
                )
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
        val contentResolver = requireContext().contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return "." + mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri!!))
    }

    private fun updateFilePathText(filePath: String?) {
        val fileText = requireView().findViewById<View>(R.id.pathTextBox) as EditText
        fileText.setText(filePath)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    fun reloadData() {
        transcriptions.clear()
        transcriptions.addAll(jsonManager.loadTranscriptions())
        transcriptions.sortedByDescending { it.timestamp }
        Log.d("transcriptions", "yeah we get here")
        transcriptionAdapter.notifyDataSetChanged()
    }


}


