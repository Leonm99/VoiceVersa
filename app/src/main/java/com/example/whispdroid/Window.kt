package com.example.whispdroid


import android.content.Context
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast


class Window(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val rootView = layoutInflater.inflate(R.layout.window, null)

    private val windowParams = WindowManager.LayoutParams(
        0,
        0,
        0,
        0,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    )

    private lateinit var textView: TextView
    private lateinit var progressBar: ProgressBar

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
        // Using kotlin extension for views caused error, so good old findViewById is used
        textView = rootView.findViewById(R.id.result_text)
        progressBar = rootView.findViewById(R.id.loading)
        rootView.findViewById<View>(R.id.window_close).setOnClickListener { close() }
        rootView.findViewById<View>(R.id.content_button).setOnClickListener {
            Toast.makeText(context, "Adding notes to be implemented.", Toast.LENGTH_SHORT).show()
            // Example: Update TextView text

        }
    }


    init {
        initWindowParams()
        initWindow()
    }


    fun open() {
        try {
            windowManager.addView(rootView, windowParams)
        } catch (e: Exception) {
            // Ignore exception for now, but in production, you should have some
            // warning for the user here.
        }
    }


    fun close() {
        try {
            windowManager.removeView(rootView)
        } catch (e: Exception) {
            // Ignore exception for now, but in production, you should have some
            // warning for the user here.
        }
    }

    public fun updateTextView(newText: String) {
        textView.text = newText
        progressBar.visibility = View.GONE
    }

}