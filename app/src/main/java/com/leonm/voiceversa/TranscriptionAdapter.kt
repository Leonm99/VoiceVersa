package com.leonm.voiceversa

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class Transcription(
    val content: String,
    val summarizedContent: String,
    val translatedContent: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDateTime: String = formatTimestamp(timestamp),
    var expanded: Boolean = false
) {
    companion object {
        private fun formatTimestamp(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault())
            return dateFormat.format(timestamp)
        }
    }
}

class TranscriptionAdapter(
    private val transcriptions: MutableList<Transcription>,
) : RecyclerView.Adapter<TranscriptionAdapter.TranscriptionViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()
    var isInSelectionMode = false
    var onSelectionModeChangeListener: ((Boolean) -> Unit)? = null

    interface OnDeleteClickListener {
        fun onDeleteClick(transcription: Transcription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TranscriptionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_transcription, parent, false)
        return TranscriptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TranscriptionViewHolder, position: Int) {
        holder.bind(transcriptions[position])
    }

    override fun getItemCount(): Int = transcriptions.size

    fun getSelectedItems(): List<Int> = selectedItems.toList()

    fun setSelectedItems(items: List<Int>) {
        selectedItems.clear()
        selectedItems.addAll(items)
    }

    fun toggleSelectionMode() {
        isInSelectionMode = !isInSelectionMode
        onSelectionModeChangeListener?.invoke(isInSelectionMode)
        notifyDataSetChanged()
    }

    inner class TranscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val summarizeButton: Button = itemView.findViewById(R.id.summarizeButton)
        private val translationButton: Button = itemView.findViewById(R.id.translationButton)
        private val copyButton: Button = itemView.findViewById(R.id.copyButton)
        private val textTranscriptionContent: TextView = itemView.findViewById(R.id.textTranscriptionContent)
        private val textTranscriptionDate: TextView = itemView.findViewById(R.id.textTranscriptionDate)
        private val buttonHolder: LinearLayout = itemView.findViewById(R.id.button_holder)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)

        init {
            itemView.setOnClickListener {
                toggleExpanded()
            }

            itemView.setOnLongClickListener {
                toggleSelectionMode()
                true // consume the long click
            }

            textTranscriptionContent.setOnClickListener {
                toggleExpanded()
            }

            textTranscriptionContent.setOnLongClickListener {
                toggleSelectionMode()
                true
            }

            checkBox.setOnClickListener {
                toggleSelection(adapterPosition)
            }
        }

        private fun toggleSelection(position: Int) {
            if (selectedItems.contains(position)) {
                selectedItems.remove(position)
            } else {
                selectedItems.add(position)
            }
            notifyDataSetChanged()
        }

        @SuppressLint("SetTextI18n")
        private fun toggleExpanded() {
            val transcription = transcriptions[adapterPosition]
            with(textTranscriptionContent) {
                if (maxHeight == SPECIFIED_HEIGHT) {
                    text = transcription.content
                    maxHeight = Int.MAX_VALUE
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                } else {
                    text = transcription.content.take(SPECIFIED_HEIGHT) + "..."
                    maxHeight = SPECIFIED_HEIGHT
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(transcription: Transcription) {
            textTranscriptionDate.text = transcription.formattedDateTime
            textTranscriptionContent.text = transcription.content.take(SPECIFIED_HEIGHT) + "..."
            textTranscriptionContent.maxHeight = SPECIFIED_HEIGHT

            checkBox.visibility = if (isInSelectionMode) View.VISIBLE else View.GONE
            checkBox.isChecked = selectedItems.contains(adapterPosition)

            val summaryAvailable = transcription.summarizedContent.isNotEmpty()
            val translationAvailable = transcription.translatedContent.isNotEmpty()

            summarizeButton.apply {
                isClickable = summaryAvailable
                visibility = if (summaryAvailable) View.VISIBLE else View.GONE
                setOnClickListener {
                    if (summaryAvailable) {
                        textTranscriptionContent.text = transcription.summarizedContent
                    }
                }
            }

            translationButton.apply {
                isClickable = translationAvailable
                visibility = if (translationAvailable) View.VISIBLE else View.GONE
                setOnClickListener {
                    if (translationAvailable) {
                        textTranscriptionContent.text = transcription.translatedContent
                    }
                }
            }

            copyButton.apply {
                setOnClickListener {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("text",  transcription.content)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied to Clipboard.", Toast.LENGTH_SHORT).show()
                }
            }


        }
    }

    companion object {
        private const val SPECIFIED_HEIGHT = 230
    }
}
