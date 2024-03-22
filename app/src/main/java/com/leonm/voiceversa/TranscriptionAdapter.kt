package com.leonm.voiceversa


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale


data class Transcription(
    val content: String,
    val summarizedContent: String,
    val translatedContent: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDateTime: String = formatTimestamp(timestamp),
    var expanded: Boolean = false,
    var isSelected: Boolean = false,
    var isInSelectionMode: Boolean = false
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
    private val onDeleteClickListener: OnDeleteClickListener
) : RecyclerView.Adapter<TranscriptionAdapter.TranscriptionViewHolder>() {


    companion object {
        private const val SPECIFIED_HEIGHT = 230

        var selectedItemsCount = 0
    }



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

 

    inner class TranscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        private val summarizeButton: Button = itemView.findViewById(R.id.summarizeButton)
        private val translationButton: Button = itemView.findViewById(R.id.translationButton)
        private val textTranscriptionContent: TextView = itemView.findViewById(R.id.textTranscriptionContent)
        private val textTranscriptionDate: TextView = itemView.findViewById(R.id.textTranscriptionDate)
        private val buttonHolder: LinearLayout = itemView.findViewById(R.id.button_holder)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
       
        init {
            itemView.setOnClickListener {
                toggleExpanded()
            }

            
            textTranscriptionContent.setOnClickListener {
                toggleExpanded()
            }

            deleteButton.setOnClickListener {
                val transcription = transcriptions[bindingAdapterPosition]
                if (!transcription.isInSelectionMode) {
                    onDeleteClickListener.onDeleteClick(transcriptions[bindingAdapterPosition])
                }

            }

            deleteButton.setOnLongClickListener {
                val transcription = transcriptions[bindingAdapterPosition]
                transcription.isInSelectionMode = !transcription.isInSelectionMode

                // Update checkbox visibility for all items
                transcriptions.forEach { it.isInSelectionMode = transcription.isInSelectionMode }

                notifyDataSetChanged()
                true
            }




            checkBox.setOnClickListener {
                val transcription = transcriptions[bindingAdapterPosition]
                transcription.isSelected = !transcription.isSelected

            }
            
        }

        @SuppressLint("SetTextI18n")
        private fun toggleExpanded() {
            val transcription = transcriptions[bindingAdapterPosition]
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

            //transcriptions.forEach { it.isInSelectionMode = false }

            checkBox.visibility = View.GONE
            deleteButton.visibility = View.VISIBLE
            checkBox.visibility = if (transcription.isInSelectionMode) View.VISIBLE else View.GONE
            deleteButton.visibility = if (transcription.isInSelectionMode) View.GONE else View.VISIBLE

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

            buttonHolder.visibility = if (summaryAvailable || translationAvailable) View.VISIBLE else View.GONE
        }
    }
}