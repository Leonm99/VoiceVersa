package com.leonm.voiceversa

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button

import android.widget.FrameLayout
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
    private lateinit var contentButton: Button
    private lateinit var summarizeButton: Button
    private lateinit var translationButton: Button

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
    var wrapper: FrameLayout = FrameLayout(context)
    var rootView: View = layoutInflater.inflate(R.layout.window, wrapper)

    private val windowParams =
        WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = (
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS


            )
            format = PixelFormat.TRANSLUCENT
        }

    init {
        initWindowParams()
        initWindow()
    }

    private fun initWindowParams() {
        getCurrentDisplayMetrics()
        windowParams.gravity = Gravity.BOTTOM
        windowParams.width = metrix.first
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        windowParams.x = 0
        windowParams.y = 0
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initWindow() {
        Log.d("Window", "Initializing window")



        wrapper = object : FrameLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                return if (event.keyCode == KeyEvent.KEYCODE_BACK or KeyEvent.KEYCODE_HOME or KeyEvent.KEYCODE_APP_SWITCH) {
                    close()
                    true
                } else super.dispatchKeyEvent(event)
            }


        }

        rootView = layoutInflater.inflate(R.layout.window, wrapper)

        textView = rootView.findViewById(R.id.result_text)
        progressBar = rootView.findViewById(R.id.loading)
        cardView = rootView.findViewById(R.id.cardView)

        rootView.findViewById<View>(R.id.window_close).setOnClickListener { close() }
        contentButton = rootView.findViewById(R.id.content_button)
        summarizeButton = rootView.findViewById(R.id.summarize_content)
        translationButton = rootView.findViewById(R.id.translate_content)

        rootView.isFocusableInTouchMode = true

        rootView.requestFocus()
        rootView.setOnKeyListener(
            View.OnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    close()
                    return@OnKeyListener true
                }
                false
            },
        )
        disableButtons()

        // Set initial properties for fade-in and slide-up
        rootView.alpha = 0f
        cardView.translationY = metrix.second.toFloat()

        // Set the touch listeners
        textView.setOnClickListener { stopTextAnimation() }
        progressBar.setOnClickListener { stopTextAnimation() }



        contentButton.setOnClickListener {
            contentButtonClicked()
        }

        summarizeButton.setOnClickListener {
            summarizeButtonClicked()
        }

        translationButton.setOnClickListener {
            translationButtonClicked()
        }
    }

    private fun contentButtonClicked() {
        contentButton.isClickable = false
        callback.onContentButtonClicked()
        if (originalText.isNotEmpty()) {
            showToast("Transcription saved!")
            close()
        }
    }


    private fun summarizeButtonClicked() {
        summarizeButton.isClickable = false
        stopTextAnimation()
        progressBar.visibility = View.VISIBLE
        launch(Dispatchers.IO) {
            val openAI = OpenAiHandler().callOpenAI() ?: return@launch
            val chatCompletion = OpenAiHandler().summarize(openAI, originalText)
            val summary = chatCompletion.choices[0].message.content
            val result = summary.toString()
            Log.d("Summary", result)

            launch(Dispatchers.Main) {
                updateTextViewWithSlightlyUnevenTypingEffect(result)
            }
        }
    }

    private fun translationButtonClicked() {
        translationButton.isClickable = false
        stopTextAnimation()
        progressBar.visibility = View.VISIBLE
        launch(Dispatchers.IO) {
            val openAI = OpenAiHandler().callOpenAI() ?: return@launch
            val chatCompletion = OpenAiHandler().translate(openAI, originalText)
            val translatedText = chatCompletion.choices[0].message.content
            val result = translatedText.toString()
            Log.d("Translation", result)

            launch(Dispatchers.Main) {
                updateTextViewWithSlightlyUnevenTypingEffect(result)
            }
        }
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
            handleException(e)
        }
    }

    private fun close() {
        Log.d("Window", "Closing window")
        try {
            if (isOpen) {
                isOpen = false
                // Create an AnimatorListener to listen for animation completion
                val animatorListener =
                    object : AnimatorListenerAdapter() {
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
                    .setListener(animatorListener)
                    .start()

                cardView.animate()
                    .translationY(metrix.second.toFloat())
                    .setDuration(500)
                    .setStartDelay(500)
                    .start()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    fun updateTextViewWithSlightlyUnevenTypingEffect(newText: String) {
        progressBar.visibility = View.GONE
        originalText = newText
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
                        textView.setText(newText)

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

    private fun stopTextAnimation() {
        continueTextAnimation = false
        textView.setText(originalText)
    }

    private fun getCurrentDisplayMetrics() {
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
    }

    private fun disableButtons() {
        contentButton.isClickable = false
        summarizeButton.isClickable = false
        translationButton.isClickable = false
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun handleException(e: Exception) {
        // Handle exception for production, show a warning to the user
        e.printStackTrace()
    }

    fun enableButtons() {
        contentButton.isClickable = true
        summarizeButton.isClickable = true
        translationButton.isClickable = true
    }


}


