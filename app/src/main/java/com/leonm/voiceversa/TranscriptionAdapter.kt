package com.leonm.voiceversa

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
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
    private val transcriptions: MutableList<Transcription>
) : RecyclerView.Adapter<TranscriptionAdapter.TranscriptionViewHolder>() {




    private val selectedItems = mutableSetOf<Int>()
    var isInSelectionMode = false
    var onSelectionModeChangeListener: ((Boolean) -> Unit)? = null
    interface OnDeleteClickListener {
        fun onDeleteClick(transcription: Transcription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TranscriptionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transcription, parent, false)
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
        private val expandButton: Button = itemView.findViewById(R.id.expand_button)
        private val textTranscriptionContent: TextView = itemView.findViewById(R.id.textTranscriptionContent)
        private val textTranscriptionDate: TextView = itemView.findViewById(R.id.textTranscriptionDate)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)

        init {
            itemView.setOnLongClickListener {
                toggleSelectionMode()
                true
            }

            textTranscriptionContent.setOnLongClickListener {
                toggleSelectionMode()
                true
            }

            checkBox.setOnClickListener {
                toggleSelection(adapterPosition)
            }

            expandButton.setOnClickListener {
                toggleExpanded()
            }

            copyButton.setOnClickListener {
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("text", transcriptions[adapterPosition].content)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(itemView.context, "Copied to Clipboard.", Toast.LENGTH_SHORT).show()
            }

            summarizeButton.setOnClickListener {
                val transcription = transcriptions[adapterPosition]
                if (transcription.summarizedContent.isNotEmpty()) {
                    textTranscriptionContent.text = transcription.summarizedContent
                }
            }

            translationButton.setOnClickListener {
                val transcription = transcriptions[adapterPosition]
                if (transcription.translatedContent.isNotEmpty()) {
                    textTranscriptionContent.text = transcription.translatedContent
                }
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

        private fun toggleExpanded() {
            val transcription = transcriptions[adapterPosition]

            with(textTranscriptionContent){
            val startHeight = height
            val endHeight: Int

            if (!transcription.expanded) {
                text = transcription.content
                maxHeight = Int.MAX_VALUE
                measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                endHeight = measuredHeight + 200
                Log.i("TAG", "measuredHeight: ${measuredHeight}")
            } else {
                maxHeight = SPECIFIED_HEIGHT
                endHeight = SPECIFIED_HEIGHT
            }

                val valueAnimator = ValueAnimator.ofInt(startHeight, endHeight).apply {
                    duration = 200
                    addUpdateListener { animation ->
                        layoutParams.height = animation.animatedValue as Int
                        requestLayout()
                    }
                }

                valueAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        requestLayout()
                    }
                })

                valueAnimator.start()

            }


            transcription.expanded = !transcription.expanded
            expandButton.animate().rotationBy(180f).setDuration(200).start()
        }

        @SuppressLint("SetTextI18n")
        fun bind(transcription: Transcription) {
            textTranscriptionDate.text = transcription.formattedDateTime
            textTranscriptionContent.apply {
                text = transcription.content
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                maxHeight = if (transcription.content.length > 100) SPECIFIED_HEIGHT else Int.MAX_VALUE
            }

            checkBox.visibility = if (isInSelectionMode) View.VISIBLE else View.GONE
            checkBox.isChecked = selectedItems.contains(adapterPosition)

            expandButton.visibility = if (transcription.content.length > 100 && !isInSelectionMode) View.VISIBLE else View.GONE

            summarizeButton.visibility = if (transcription.summarizedContent.isNotEmpty()) View.VISIBLE else View.GONE
            summarizeButton.isClickable = transcription.summarizedContent.isNotEmpty()

            translationButton.visibility = if (transcription.translatedContent.isNotEmpty()) View.VISIBLE else View.GONE
            translationButton.isClickable = transcription.translatedContent.isNotEmpty()
        }
    }

    companion object {
        private const val SPECIFIED_HEIGHT = 230
    }
}
