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


    @Suppress("ktlint:standard:property-naming")
    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private val openAiHandler by lazy { OpenAiHandler() }
    private val transcriptions = mutableListOf<Transcription>()

    private lateinit var jsonManager: JsonManager
    private lateinit var recyclerView: RecyclerView


    private lateinit var mainActivity: MainActivity
    private lateinit var tAdapter: TranscriptionAdapter






    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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

                }
            }
        })

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)





        if (recyclerView.adapter == null) {
            transcriptions.clear()
            transcriptions.addAll(jsonManager.loadTranscriptions())
            transcriptions.sortByDescending { it.timestamp }
            recyclerView.adapter = tAdapter

            recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }


        tAdapter.onSelectionModeChangeListener = { isInSelectionMode ->
            if (isInSelectionMode) {
                // Change FAB icon to indicate multiselect mode
                binding.fab.setImageResource(R.drawable.delete)
            } else {
                // Change FAB icon back to the original icon
                binding.fab.setImageResource(R.drawable.add_fill)
            }
        }


        binding.testFab.setOnClickListener {
            saveTranscriptionToFile(resources.getString(R.string.lorem_ipsum),"DAS IST DIE SUMMARY","DAS IST DIE TRANSLATION")
            reloadData()
        }

        binding.fab.setOnClickListener {
            if (tAdapter.isInSelectionMode) {

                deleteMultiple()
                // Handle multiselect mode action here
                // For example, you can show a Toast message indicating multiselect mode is active
                Toast.makeText(requireContext(), "Items deleted!", Toast.LENGTH_SHORT).show()
            } else {
                // Handle regular FAB action here
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



    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




    override fun onResume() {
        super.onResume()


        reloadData()

        if(tAdapter.isInSelectionMode){
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




    private fun deleteMultiple() {
        try {
            val selectedItems = tAdapter.getSelectedItems().sortedDescending()
            Log.d("FirstFragment", "Selected items: $selectedItems")

            selectedItems.forEach { position ->
                if (position >= 0 && position < transcriptions.size) {
                    Log.d("FirstFragment", "Deleting item at position $position")
                    transcriptions.removeAt(position)
                }
            }

            tAdapter.setSelectedItems(emptyList()) // Clear selected items
            recyclerView.adapter?.notifyDataSetChanged()
            jsonManager.saveTranscriptions(transcriptions)
            tAdapter.toggleSelectionMode()
        } catch (e: Exception) {
            handleError(e)
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private suspend fun updateOutputText() {
        withContext(Dispatchers.Main) {
            recyclerView.adapter?.notifyDataSetChanged()
        }
        // Notify the adapter that data set has changed
    }

    private fun saveTranscriptionToFile(content: String,summary: String, translation: String) {
        transcriptions.clear()
        val transcription = Transcription(content,summary,translation)
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
            val result = openAiHandler.whisper(requireContext(), openAiHandler.callOpenAI(requireContext())!!, path!!)
            saveTranscriptionToFile(result,"","")
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
