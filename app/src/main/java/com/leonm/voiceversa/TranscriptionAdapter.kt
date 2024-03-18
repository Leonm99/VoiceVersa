package com.leonm.voiceversa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
    interface OnDeleteClickListener {
        fun onDeleteClick(transcription: Transcription)
    }



    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): TranscriptionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_transcription, parent, false)
        return TranscriptionViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: TranscriptionViewHolder,
        position: Int
    ) {
        val transcription = transcriptions[position]
        val textTranscriptionContent = holder.textTranscriptionContent
        holder.bind(transcription)


        holder.deleteButton.setOnClickListener {
            onDeleteClickListener.onDeleteClick(transcription)
        }


        holder.itemView.setOnClickListener {

            if (textTranscriptionContent.maxHeight == holder.specifiedHeight) {
                textTranscriptionContent.maxHeight = Int.MAX_VALUE
                val params: ViewGroup.LayoutParams = textTranscriptionContent.layoutParams
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                textTranscriptionContent.layoutParams = params
            } else {
                textTranscriptionContent.maxHeight = holder.specifiedHeight

            }
           // notifyItemChanged(position)
        }

        holder.textTranscriptionContent.setOnClickListener {

            if (textTranscriptionContent.maxHeight == holder.specifiedHeight) {
                textTranscriptionContent.maxHeight = Int.MAX_VALUE
                val params: ViewGroup.LayoutParams = textTranscriptionContent.layoutParams
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                textTranscriptionContent.layoutParams = params
            } else {
                textTranscriptionContent.maxHeight = holder.specifiedHeight

            }
        }
    }

    override fun getItemCount(): Int = transcriptions.size

    inner class TranscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        private val summarizeButton: Button = itemView.findViewById(R.id.summarizeButton)
        private val translationButton: Button = itemView.findViewById(R.id.translationButton)
        private val contentLayout = itemView.findViewById<LinearLayout>(R.id.contentLayout)

        val textTranscriptionContent: TextView = itemView.findViewById(R.id.textTranscriptionContent)
        private val textTranscriptionDate: TextView = itemView.findViewById(R.id.textTranscriptionDate)
        private val buttonHolder: LinearLayout = itemView.findViewById(R.id.button_holder)

        val specifiedHeight = 200
        // specify the maximum height

        var measuredHeight: Int = textTranscriptionContent.text.length

        fun bind(transcription: Transcription) {
            textTranscriptionContent.text = transcription.content
            textTranscriptionDate.text = transcription.formattedDateTime

         measuredHeight = textTranscriptionContent.text.length

         if (measuredHeight > specifiedHeight) {
                textTranscriptionContent.maxHeight = specifiedHeight
            }




            val summaryAvailable = transcription.summarizedContent.isNotEmpty()
            val translationAvailable = transcription.translatedContent.isNotEmpty()

            if (summaryAvailable || translationAvailable) {
                summarizeButton.isClickable = summaryAvailable
                summarizeButton.visibility = if (summaryAvailable) View.VISIBLE else View.GONE

                translationButton.isClickable = translationAvailable
                translationButton.visibility = if (translationAvailable) View.VISIBLE else View.GONE
            } else {
                buttonHolder.visibility = View.GONE
            }

            summarizeButton.setOnClickListener {
                if (summaryAvailable) {
                    textTranscriptionContent.text = transcription.summarizedContent
                }
            }

            translationButton.setOnClickListener {
                if (translationAvailable) {
                    textTranscriptionContent.text = transcription.translatedContent
                }
            }
        }

        private fun calculateCardViewHeight(contentLength: Int, maxContentLength: Int, maxExpandedHeight: Int, isExpanded: Boolean): Int {
            return if (contentLength < maxContentLength || isExpanded) {
                ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                maxExpandedHeight
            }
        }
    }
}
