package com.example.whispdroid

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView

class Window(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutInflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val rootView: View = layoutInflater.inflate(R.layout.window, null)

    private val windowParams = WindowManager.LayoutParams().apply {
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

    private lateinit var textView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cardView: CardView
    private lateinit var contentContainer: FrameLayout
    private var newText: String = ""
    private var charIndex: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    private val TEXT_CHUNK_SIZE = 5
    private var continueTextAnimation: Boolean = true
    private var originalText: String = ""
    
    private fun getCurrentDisplayMetrics(): DisplayMetrics {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        return dm
    }

    private fun calculateSizeAndPosition(params: WindowManager.LayoutParams) {
        val dm = getCurrentDisplayMetrics()
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.width = dm.widthPixels
        params.height = dm.heightPixels
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
        rootView.findViewById<View>(R.id.window_close).setOnClickListener { close() }

        // Set initial properties for fade-in and slide-up
        rootView.alpha = 0f
        cardView.translationY = getCurrentDisplayMetrics().heightPixels.toFloat()

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
    }

    init {
        initWindowParams()
        initWindow()
    }

    fun open() {
        Log.d("Window", "Opening window")
        try {
            windowManager.addView(rootView, windowParams)
        } catch (e: Exception) {
            // Handle exception for production, show a warning to the user
            e.printStackTrace()
        }
    }

    fun close() {
        Log.d("Window", "Closing window")
        try {
            windowManager.removeView(rootView)

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

        handler.postDelayed(object : Runnable {
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
        }, delay)
    }

    fun stopTextAnimation() {
        continueTextAnimation = false
        textView.text = originalText
    }
}
