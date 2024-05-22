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
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private val openAiHandler by lazy { OpenAiHandler(requireContext()) }
    private val transcriptions = mutableListOf<Transcription>()

    private lateinit var jsonManager: JsonManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var mainActivity: MainActivity
    private lateinit var tAdapter: TranscriptionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        jsonManager = JsonManager(requireContext())
        recyclerView = binding.recyclerView
        mainActivity = (activity as? MainActivity)!!
        tAdapter = TranscriptionAdapter(transcriptions, this, mainActivity)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tAdapter.isInSelectionMode) {
                    tAdapter.toggleSelectionMode()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFabListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        reloadData()
        if (tAdapter.isInSelectionMode) {
            tAdapter.toggleSelectionMode()
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

    private fun setupRecyclerView() {
        transcriptions.clear()
        transcriptions.addAll(jsonManager.loadTranscriptions())
        transcriptions.sortByDescending { it.timestamp }
        recyclerView.adapter = tAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        tAdapter.onSelectionModeChangeListener = { isInSelectionMode ->
            binding.fab.setImageResource(if (isInSelectionMode) R.drawable.delete else R.drawable.add_fill)
        }
    }

    private fun setupFabListeners() {
        binding.testFab.setOnClickListener {
            saveTranscriptionToFile(
                getString(R.string.lorem_ipsum),
                "DAS IST DIE SUMMARY",
                "DAS IST DIE TRANSLATION"
            )
            reloadData()
        }

        binding.fab.setOnClickListener {
            if (tAdapter.isInSelectionMode) {
                deleteMultiple()
                Toast.makeText(requireContext(), "Items deleted!", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    try {
                        val fileChooserDeferred = async { openFileChooser() }
                        fileChooserDeferred.await()
                    } catch (e: Exception) {
                        handleError(e)
                    }
                }
            }
        }
    }

    private fun deleteMultiple() {
        try {
            val selectedItems = tAdapter.getSelectedItems().sortedDescending()
            selectedItems.forEach { position ->
                if (position in transcriptions.indices) {
                    transcriptions.removeAt(position)
                }
            }
            tAdapter.setSelectedItems(emptyList())
            recyclerView.adapter?.notifyDataSetChanged()
            jsonManager.saveTranscriptions(transcriptions)
            tAdapter.toggleSelectionMode()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    private suspend fun updateOutputText() {
        withContext(Dispatchers.Main) {
            recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun saveTranscriptionToFile(content: String, summary: String, translation: String) {
        val transcription = Transcription(content, summary, translation)
        transcriptions.clear()
        transcriptions.add(transcription)
        transcriptions.addAll(jsonManager.loadTranscriptions())
        jsonManager.saveTranscriptions(transcriptions)
    }

    private fun reloadData() {
        transcriptions.clear()
        transcriptions.addAll(jsonManager.loadTranscriptions())
        transcriptions.sortByDescending { it.timestamp }
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private suspend fun performWhisperTranscription(path: String?) {
        try {
            showLoading(true)
            openAiHandler.initOpenAI()
            val result = openAiHandler.whisper(path!!)
            saveTranscriptionToFile(result, "", "")
            updateOutputText()
        } catch (e: Exception) {
            handleError(e)
        } finally {
            showLoading(false)
        }
    }

    private fun handleError(exception: Exception) {
        exception.printStackTrace()
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "audio/*" }
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
        selectedFileUri?.let {
            val savedFilePath = saveFileToInternalStorage(it)
            lifecycleScope.launch {
                performWhisperTranscription(savedFilePath)
            }
        }
    }

    private fun saveFileToInternalStorage(uri: Uri): String? {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileExtension = getFileExtension(uri)
                val internalFile = File(requireContext().filesDir, "selected_file$fileExtension")
                FileOutputStream(internalFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                internalFile.absolutePath
            }
        } catch (e: IOException) {
            handleError(e)
            null
        }
    }

    private fun getFileExtension(uri: Uri): String {
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return ".${mimeTypeMap.getExtensionFromMimeType(requireContext().contentResolver.getType(uri))}"
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
