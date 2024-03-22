package com.leonm.voiceversa

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.leonm.voiceversa.databinding.FragmentFirstBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), TranscriptionAdapter.OnDeleteClickListener {
    @Suppress("ktlint:standard:property-naming")
    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private val openAiHandler = OpenAiHandler()
    private val transcriptions = mutableListOf<Transcription>()

    private lateinit var jsonManager: JsonManager
    private lateinit var recyclerView: RecyclerView



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        jsonManager = JsonManager(requireContext())
        recyclerView = binding.recyclerView
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupRecyclerView()


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }





    private fun setupClickListeners() {
        binding.myButton.setOnClickListener {
            try {
                val selectedPositions = transcriptions.filter { it.isSelected }.map { transcriptions.indexOf(it) }


                for (position in selectedPositions.reversed()) {
                    Log.d("FirstFragment", "Deleting item at position $position")
                    transcriptions.removeAt(position)
                    recyclerView.adapter?.notifyItemRemoved(position)
                }

                transcriptions.forEach { it.isInSelectionMode = false }
                transcriptions.forEach { it.isInSelectionMode = false }

                jsonManager.saveTranscriptions(transcriptions)

            }catch (e: Exception) {
                handleError(e)
            }

        }

        binding.fab.setOnClickListener {
            lifecycleScope.launch {
                try {
                    // Launch a coroutine to perform openFileChooser
                    val fileChooserDeferred = async { openFileChooser() }

                    // Wait for openFileChooser to finish
                    fileChooserDeferred.await()

                    // Now that openFileChooser is done, proceed with performWhisperTranscription
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
        if (recyclerView.adapter == null) {
            transcriptions.clear()
            transcriptions.addAll(jsonManager.loadTranscriptions())
            transcriptions.sortByDescending { it.timestamp }
            recyclerView.adapter = TranscriptionAdapter(transcriptions, this)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDeleteClick(transcription: Transcription) {
        val position = transcriptions.indexOf(transcription)
        if (position != -1) {
            transcriptions.removeAt(position)
            recyclerView.adapter?.notifyItemRemoved(position)
            jsonManager.saveTranscriptions(transcriptions)
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun updateOutputText() {
        withContext(Dispatchers.Main) {
            recyclerView.adapter?.notifyDataSetChanged()
        }
        // Notify the adapter that data set has changed
    }

    private fun saveTranscriptionToFile(content: String) {
        transcriptions.clear()
        val transcription = Transcription(content,"","")
        transcriptions.add(transcription)

        transcriptions.addAll(jsonManager.loadTranscriptions())
        jsonManager.saveTranscriptions(transcriptions) // Replace with the actual path
    }

    @SuppressLint("NotifyDataSetChanged")
    fun reloadData() {
        transcriptions.clear()
        transcriptions.addAll(jsonManager.loadTranscriptions())
        transcriptions.sortedByDescending { it.timestamp }
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private suspend fun performWhisperTranscription(path: String?) {
        try {
            showLoading(true)
            val result = openAiHandler.whisper(openAiHandler.callOpenAI(requireContext())!!, path!!).text
            saveTranscriptionToFile(result)
            updateOutputText()
        } catch (e: Exception) {
            handleError(e)
        } finally {
            showLoading(false)
        }
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

            lifecycleScope.launch {
                performWhisperTranscription(savedFilePath)
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
        val contentResolver = requireContext().contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return "." + mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri!!))
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }


}
