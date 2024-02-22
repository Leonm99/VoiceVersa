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
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDateTime: String = formatTimestamp(timestamp),
    var expanded: Boolean = false // Track expansion state
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
    private val onDeleteClickListener: OnDeleteClickListener,
) : RecyclerView.Adapter<TranscriptionAdapter.TranscriptionViewHolder>() {
    interface OnDeleteClickListener {
        fun onDeleteClick(transcription: Transcription)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): TranscriptionViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transcription, parent, false)
        return TranscriptionViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: TranscriptionViewHolder,
        position: Int,
    ) {
        val transcription = transcriptions[position]
        holder.bind(transcription)

        // Set the click listener for the delete button
        holder.deleteButton.setOnClickListener {
            onDeleteClickListener.onDeleteClick(transcription)
        }

        // Set click listener to toggle expansion state
        holder.itemView.setOnClickListener {
            transcription.expanded = !transcription.expanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = transcriptions.size

    inner class TranscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        private val contentLayout: LinearLayout = itemView.findViewById(R.id.contentLayout)
        private val textTranscriptionContent: TextView = itemView.findViewById(R.id.textTranscriptionContent)
        private val textTranscriptionDate: TextView = itemView.findViewById(R.id.textTranscriptionDate)

        fun bind(transcription: Transcription) {
            textTranscriptionContent.text = transcription.content
            textTranscriptionDate.text = transcription.formattedDateTime

            // Adjust height based on expansion state and content length
            val maxExpandedHeight = 450
            val maxContentLength = 200
            val contentLength = transcription.content.length

            val layoutParams = contentLayout.layoutParams
            if (contentLength <= maxContentLength) {
                // If content length is smaller than or equal to max length, match content height
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                // Otherwise, set maximum height when not expanded
                if (transcription.expanded) {
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                } else {
                    layoutParams.height = maxExpandedHeight
                }
            }
            contentLayout.layoutParams = layoutParams
        }
    }
}