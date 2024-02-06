package com.leonm.voiceversa

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

interface WindowCallback {
    fun onContentButtonClicked()

    var jsonManager: JsonManager
}

class Window(
    private val context: Context,
    private val callback: WindowCallback,
    private val floatingService: FloatingService,

    override val coroutineContext: CoroutineContext,
) : CoroutineScope {

    private lateinit var textView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cardView: CardView
    private lateinit var contentContainer: FrameLayout
    private lateinit var contentButton: Button
    private lateinit var summarizeButton: Button
    private var newText: String = ""
    private var charIndex: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    private var metrix = Pair(0, 0)

    private var continueTextAnimation: Boolean = true
    private var originalText: String = ""

    private var isOpen: Boolean = false


    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutInflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val rootView: View = layoutInflater.inflate(R.layout.window, null)

    private val windowParams =
        WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = (
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            )
            format = PixelFormat.TRANSLUCENT
        }



    private fun getCurrentDisplayMetrics(): Any {
        val width: Int
        val height: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
        } else {
            val dm = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(dm)
            width = dm.widthPixels
            height = dm.heightPixels
        }
        metrix = Pair(width, height)
        return metrix
    }

    private fun calculateSizeAndPosition(params: WindowManager.LayoutParams) {
        val dm = getCurrentDisplayMetrics()
        params.gravity = Gravity.TOP or Gravity.START
        params.width = metrix.first
        params.height = metrix.second
        params.x = 0
        params.y = 0
    }

    private fun initWindowParams() {
        calculateSizeAndPosition(windowParams)
    }

    private fun initWindow() {
        Log.d("Window", "Initializing window")
        textView = rootView.findViewById(R.id.result_text)
        progressBar = rootView.findViewById(R.id.loading)
        cardView = rootView.findViewById(R.id.cardView)
        contentContainer = rootView.findViewById(R.id.contentContainer)
        rootView.findViewById<View>(R.id.window_close).setOnClickListener { close()  }
        contentButton = rootView.findViewById(R.id.content_button)
        summarizeButton = rootView.findViewById(R.id.summarize_content)

        // Set initial properties for fade-in and slide-up
        rootView.alpha = 0f
        cardView.translationY = metrix.second.toFloat()


        // Set the touch listeners
        textView.setOnClickListener {
            stopTextAnimation()
        }

        contentContainer.setOnClickListener {
            stopTextAnimation()
        }

        progressBar.setOnClickListener {
            stopTextAnimation()
        }

        contentButton.setOnClickListener {
            // Handle the content button click
            contentButton.isActivated = false
            callback.onContentButtonClicked()
            if (newText.isNotEmpty()) {
                Toast.makeText(context, "Transcription saved!", Toast.LENGTH_LONG).show()
                close()

            }
        }

        summarizeButton.setOnClickListener {
            // Handle the summarize button click
            summarizeButton.isActivated = false

            launch(Dispatchers.IO) {
                val openAI = OpenAiHandler().callOpenAI() ?: return@launch
                val chatCompletion = OpenAiHandler().summarize(openAI, newText)
                val summary = chatCompletion.choices[0].message.content
                val result = summary.toString()
                Log.d("Summary", result)

                launch(Dispatchers.Main) {
                    updateTextViewWithSlightlyUnevenTypingEffect(result)
                }
            }
            Toast.makeText(context, "Transcription saved!", Toast.LENGTH_LONG).show()
        }
    }

    init {

        initWindowParams()
        initWindow()
    }

    fun open() {
        Log.d("Window", "Opening window")
        try {
            if (!isOpen) {
                isOpen = true
                // Apply the fade-in and slide-up animation
                rootView.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(500)
                    .withEndAction {
                        Log.d("Window", "Animation complete")
                    }.start()

                cardView.animate()
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(500)
                    .start()

                windowManager.addView(rootView, windowParams)
            }
        } catch (e: Exception) {
            // Handle exception for production, show a warning to the user
            e.printStackTrace()
        }
    }

    private fun close() {
        Log.d("Window", "Closing window")
        try {
            if (isOpen) {
                isOpen = false

                // Create an AnimatorListener to listen for animation completion
                val animatorListener = object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        Log.d("Window", "Animation complete")
                        windowManager.removeView(rootView)
                        floatingService.stopService()
                    }
                }

                // Apply the fade-in and slide-up animation
                rootView.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .setStartDelay(500)
                    .setListener(animatorListener) // Set the AnimatorListener
                    .start()

                cardView.animate()
                    .translationY(metrix.second.toFloat())
                    .setDuration(500)
                    .setStartDelay(500)
                    .start()
            }
        } catch (e: Exception) {
            // Handle exception for production, show a warning to the user
            e.printStackTrace()
        }
    }


    fun updateTextViewWithSlightlyUnevenTypingEffect(newText: String) {
        progressBar.visibility = View.GONE
        this.originalText = newText
        this.newText = ""
        charIndex = 0
        continueTextAnimation = true
        displayTextWithSlightlyUnevenTiming(originalText)
    }

    private fun displayTextWithSlightlyUnevenTiming(text: String) {
        val delay = 15L
        val unevenFactor = 0.5

        handler.postDelayed(
            object : Runnable {
                override fun run() {
                    if (charIndex <= text.length && continueTextAnimation) {
                        newText = text.substring(0, charIndex)
                        textView.text = newText

                        charIndex++
                        val nextDelay =
                            (delay * (1 + (Math.random() - 0.5) * 2 * unevenFactor)).toLong()
                        handler.postDelayed(this, nextDelay)
                    }
                }
            },
            delay,
        )
    }

    fun stopTextAnimation() {
        continueTextAnimation = false
        textView.text = originalText
    }
}
