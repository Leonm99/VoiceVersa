package com.leonm.voiceversa

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isNotEmpty
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.leonm.voiceversa.databinding.FragmentFirstBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */


class FirstFragment : Fragment(), TranscriptionAdapter.OnDeleteClickListener {
    // Other code

    // Other code {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private val openAiHandler by lazy { OpenAiHandler(requireContext()) }
    private val transcriptions = mutableListOf<Transcription>()

    private lateinit var jsonManager: JsonManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var mainActivity: MainActivity
    private lateinit var tAdapter: TranscriptionAdapter
    private val transcriptionSavedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "TRANSCRIPTION_SAVED") {
                reloadData()
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        jsonManager = JsonManager(requireContext())
        recyclerView = binding.recyclerView
        mainActivity = (activity as? MainActivity)!!
        tAdapter = TranscriptionAdapter(transcriptions)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tAdapter.isInSelectionMode) {
                    tAdapter.toggleSelectionMode()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            transcriptionSavedReceiver,
            IntentFilter("TRANSCRIPTION_SAVED")
        )


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFabListeners()
        if (binding.imageView.visibility == View.VISIBLE) {
            animateArrow()
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(transcriptionSavedReceiver)
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        reloadData()
        if (tAdapter.isInSelectionMode) {
            tAdapter.toggleSelectionMode()
        }
        if (binding.imageView.visibility == View.VISIBLE) {
            animateArrow()
        }
    }

    override fun onDeleteClick(transcription: Transcription) {
        deleteTranscription(transcription)
    }



    private fun setupRecyclerView() {
        loadTranscriptions()
        recyclerView.adapter = tAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        tAdapter.onSelectionModeChangeListener = { isInSelectionMode ->
            binding.fab.setImageResource(if (isInSelectionMode) R.drawable.delete else R.drawable.add_fill)
        }
    }

    private fun setupFabListeners() {
        binding.testFab.setOnClickListener {
            addSampleTranscription()
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

    private fun addSampleTranscription() {
        var counter = 0
        while (counter != 100) {
            saveTranscriptionToFile(
                getString(R.string.lorem_ipsum),
                "DAS IST DIE SUMMARY",
                "DAS IST DIE TRANSLATION"
            )
            counter++
        }

        reloadData()
    }

    private fun deleteTranscription(transcription: Transcription) {
        val position = transcriptions.indexOf(transcription)
        if (position != -1) {
            transcriptions.removeAt(position)
            recyclerView.adapter?.notifyItemRemoved(position)
            jsonManager.saveTranscriptions(transcriptions)
            if (transcriptions.isNotEmpty()) {
                binding.infoText.visibility = View.GONE
                binding.imageView.visibility = View.GONE

            }
        }
    }

    private fun deleteMultiple() {
        try {
            val selectedItems = tAdapter.getSelectedItems().sortedDescending()
            selectedItems.forEach { position ->
                if (position in transcriptions.indices) {
                    transcriptions.removeAt(position)
                    recyclerView.adapter?.notifyItemRemoved(position)
                }
            }
            tAdapter.setSelectedItems(emptyList())
            jsonManager.saveTranscriptions(transcriptions)
            tAdapter.toggleSelectionMode()

            if (transcriptions.isNotEmpty()) {
                binding.infoText.visibility = View.GONE
                binding.imageView.visibility = View.GONE
            }else {
                binding.infoText.visibility = View.VISIBLE
                binding.imageView.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            handleError(e)
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
        loadTranscriptions()
        recyclerView.adapter?.notifyDataSetChanged()
        if (transcriptions.isNotEmpty()) {
            binding.infoText.visibility = View.GONE
            binding.imageView.visibility = View.GONE
        }else {
            binding.infoText.visibility = View.VISIBLE
            binding.imageView.visibility = View.VISIBLE
        }

    }

    private fun loadTranscriptions() {
        transcriptions.clear()
        transcriptions.addAll(jsonManager.loadTranscriptions())
        transcriptions.sortByDescending { it.timestamp }

    }

    private fun performWhisperTranscription(path: String?) {


        ServiceUtil.startFloatingService(requireContext(), "TRANSCRIBE", path!!)

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

    private fun animateArrow() {
        val animator = ObjectAnimator.ofFloat(binding.imageView, "translationY", -50f, 50f)
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.REVERSE
        animator.duration = 1000
        animator.start()
    }




}
