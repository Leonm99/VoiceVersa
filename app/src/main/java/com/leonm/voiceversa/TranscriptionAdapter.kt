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
    val translatedContent: String,
    val summarizedContent: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDateTime: String = formatTimestamp(timestamp),
    var expanded: Boolean = false,
) {
    companion object {
        private fun formatTimestamp(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
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
        holder.bind(transcription)

        holder.deleteButton.setOnClickListener {
            onDeleteClickListener.onDeleteClick(transcription)
        }


        holder.itemView.setOnClickListener {
            transcription.expanded = !transcription.expanded
            notifyItemChanged(position)
        }

        holder.textTranscriptionContent.setOnClickListener {
            transcription.expanded = !transcription.expanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = transcriptions.size

    inner class TranscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        val summarizeButton: Button = itemView.findViewById(R.id.summarizeButton)
        val translationButton: Button = itemView.findViewById(R.id.translationButton)
        private val contentLayout: LinearLayout = itemView.findViewById(R.id.contentLayout)
        val textTranscriptionContent: TextView = itemView.findViewById(R.id.textTranscriptionContent)
        private val textTranscriptionDate: TextView = itemView.findViewById(R.id.textTranscriptionDate)
        private val buttonHolder: LinearLayout = itemView.findViewById(R.id.button_holder)

        fun bind(transcription: Transcription) {
            textTranscriptionContent.text = transcription.content
            textTranscriptionDate.text = transcription.formattedDateTime

            val maxExpandedHeight = 450
            val maxContentLength = 200
            val contentLength = transcription.content.length

            val layoutParams = contentLayout.layoutParams
            if (contentLength <= maxContentLength) {
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                if (transcription.expanded) {
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                } else {
                    layoutParams.height = maxExpandedHeight
                }
            }
            contentLayout.layoutParams = layoutParams

            var summaryAvailable = transcription.summarizedContent.isNotEmpty()
            var translationAvailable = transcription.translatedContent.isNotEmpty()
            if (summaryAvailable || translationAvailable) {
                if(summaryAvailable){
                    summarizeButton.isClickable = true
                    summarizeButton.visibility = View.VISIBLE
                }else{
                    summarizeButton.visibility = View.GONE
                }
                if (translationAvailable){
                    translationButton.isClickable = true
                    translationButton.visibility = View.VISIBLE
                }else{
                    translationButton.visibility = View.GONE
                }
            }else{
                buttonHolder.visibility = View.GONE
            }


            summarizeButton.setOnClickListener {
                if(transcription.summarizedContent.isNotEmpty()){
                    textTranscriptionContent.text = transcription.summarizedContent
                }
            }
            translationButton.setOnClickListener {
                if(transcription.translatedContent.isNotEmpty()){
                    textTranscriptionContent.text = transcription.translatedContent
                }
            }
        }
    }
}
