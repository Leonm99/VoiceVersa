package com.leonm.voiceversa


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
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
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext


interface WindowCallback {
    fun onContentButtonClicked()

    var jsonManager: JsonManager
}

class Window(
    private val context: Context,
    private val windowCallback: WindowCallback,
    private val floatingService: FloatingService,
    override val coroutineContext: CoroutineContext
) : CoroutineScope {

    private lateinit var textView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cardView: CardView
    private lateinit var contentButton: MaterialButton
    private lateinit var summarizeButton: MaterialButton
    private lateinit var translationButton: MaterialButton
    private lateinit var copyButton: MaterialButton
    private lateinit var divider: View
    private lateinit var divider2: View
    private lateinit var headerText: TextView
    private lateinit var loadingText: TextView
    private var newText: String = ""
    private var charIndex: Int = 0
    private var metrics = Pair(0, 0)
    private var continueTextAnimation = true
    private var originalText = ""
    private var isWindowOpen = false
    private var translatedText = ""
    private var summary = ""
    private var transcription: String = ""
    private var isLoading = true
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutInflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    var wrapper = FrameLayout(context)
    var rootView = layoutInflater.inflate(R.layout.window, wrapper)

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
        windowParams.width = metrics.first
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        windowParams.x = 0
        windowParams.y = 0
    }

    private fun initWindow() {
        wrapper = object : FrameLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                return when (event.keyCode) {
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_APP_SWITCH -> {
                        close()
                        true
                    }
                    else -> super.dispatchKeyEvent(event)
                }
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
        copyButton = rootView.findViewById(R.id.copy_button)
        headerText = rootView.findViewById(R.id.header_text)
        divider = rootView.findViewById(R.id.divider)
        divider2 = rootView.findViewById(R.id.divider2)
        loadingText = rootView.findViewById(R.id.loading_text)



        rootView.isFocusableInTouchMode = true
        rootView.requestFocus()
        rootView.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                close()
                true
            } else {
                false
            }
        }

        disableButtons()

        // Set initial properties for fade-in and slide-up
        rootView.alpha = 0f
        cardView.translationY = metrics.second.toFloat()

        // Set the touch listeners
        textView.setOnClickListener { stopTextAnimation() }
        progressBar.setOnClickListener { stopTextAnimation() }

        contentButton.setOnClickListener { onContentButtonClicked() }
        summarizeButton.setOnClickListener { summarizeButtonClicked() }
        translationButton.setOnClickListener { translationButtonClicked() }
        copyButton.setOnClickListener { copyButtonClicked() }




    }


    private fun onContentButtonClicked() {
        if ("\"" + transcription + "\"" != textView.text.toString()) {
            contentButton.icon = ContextCompat.getDrawable(context, R.drawable.save)
            toggleLoading()
            headerText.text = "Transcription:"
            launch(Dispatchers.Main) {
                updateTextViewWithSlightlyUnevenTypingEffect(transcription)
            }

        }else{
            contentButton.isClickable = false
            windowCallback.onContentButtonClicked()
            if (originalText.isNotEmpty()) {
                showToast("Transcription saved!")
                close()
        }

        }
    }

    private fun summarizeButtonClicked() {
        contentButton.icon = ContextCompat.getDrawable(context, R.drawable.reply)
        if (summary.isNotEmpty()) {
            toggleLoading()
            headerText.text = "Summary:"
            launch(Dispatchers.Main) {
                updateTextViewWithSlightlyUnevenTypingEffect(summary)
            }

        }else{
            summarizeButton.isClickable = false
            stopTextAnimation()
            toggleLoading()
            headerText.text = "Summary:"
            launch(Dispatchers.IO) {
                OpenAiHandler(context).initOpenAI()
                summary = OpenAiHandler(context).summarize(transcription)

                floatingService.setSummary(summary)
                launch(Dispatchers.Main) {
                    updateTextViewWithSlightlyUnevenTypingEffect(summary)
                }
            }

        }

    }

    private fun translationButtonClicked() {
        contentButton.icon = ContextCompat.getDrawable(context, R.drawable.reply)
        if (translatedText.isNotEmpty()) {
            toggleLoading()
            headerText.text = "Translation:"
            launch(Dispatchers.Main) {
                updateTextViewWithSlightlyUnevenTypingEffect(translatedText)
            }

        }else{
            contentButton.icon = ContextCompat.getDrawable(context, R.drawable.reply)
            translationButton.isClickable = false
            stopTextAnimation()
            toggleLoading()
            headerText.text = "Translation:"
            launch(Dispatchers.IO) {
                val openAiHandler = OpenAiHandler(context)
                openAiHandler.initOpenAI() ?: return@launch
                translatedText = openAiHandler.translate(transcription)

                floatingService.setTranslation(translatedText)
                launch(Dispatchers.Main) {
                    updateTextViewWithSlightlyUnevenTypingEffect(translatedText)
                }
            }

        }

    }

    private fun copyButtonClicked() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text", textView.text.toString())
        clipboard.setPrimaryClip(clip)
        showToast("Copied to clipboard!")
    }




    fun open() {
            if (!isWindowOpen) {
                isWindowOpen = true
                rootView.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(500)
                    .start()

                cardView.animate()
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(500)
                    .start()

                windowManager.addView(rootView, windowParams)
            }

    }

    private fun close() {
        if (isWindowOpen) {
            isWindowOpen = false
            rootView.animate()
                .alpha(0f)
                .translationY(metrics.second.toFloat())
                .setDuration(500)
                .setStartDelay(500)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        windowManager.removeView(rootView)
                        floatingService.stopService()
                    }
                })
                .start()
        }
    }

    fun updateTextViewWithSlightlyUnevenTypingEffect(newText: String) {
        if (transcription.isEmpty()){
            transcription = newText

        }

        toggleLoading()
        originalText = "\"" + newText + "\""
        this.newText = ""
        charIndex = 0
        continueTextAnimation = true
        displayTextWithSlightlyUnevenTiming(originalText)
    }


    private fun displayTextWithSlightlyUnevenTiming(text: String) {
        val delay = 5L
        val unevenFactor = 0.5
        disableButtons()
        GlobalScope.launch {

            while (charIndex <= text.length && continueTextAnimation) {
                newText = text.substring(0, charIndex)
                withContext(Dispatchers.Main) {
                    textView.text = newText
                }
                charIndex++
                val nextDelay =
                    (delay * (1 + (Math.random() - 0.5) * 2 * unevenFactor)).toLong()
                delay(nextDelay)
            }

        }
        enableButtons()
    }

    private fun stopTextAnimation() {
        continueTextAnimation = false
        textView.text = originalText
    }

    private fun getCurrentDisplayMetrics() {
        val screenWidth: Int
        val screenHeight: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
        }
        metrics = Pair(screenWidth, screenHeight)
    }



    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }



    fun enableButtons() {
        contentButton.isClickable = true
        summarizeButton.isClickable = true
        translationButton.isClickable = true
        copyButton.isClickable = true

    }

    fun disableButtons() {
        contentButton.isClickable = false
        summarizeButton.isClickable = false
        translationButton.isClickable = false
        copyButton.isClickable = false
    }

    private fun toggleLoading() {
        if (!isLoading){
            disableButtons()
            progressBar.visibility = View.VISIBLE
            loadingText.visibility = View.VISIBLE
            divider.visibility = View.GONE
            divider2.visibility = View.GONE
            headerText.visibility = View.GONE
            textView.visibility = View.GONE
            isLoading = !isLoading
        }else{
            enableButtons()
            progressBar.visibility = View.GONE
            loadingText.visibility = View.GONE
            divider.visibility = View.VISIBLE
            divider2.visibility = View.VISIBLE
            headerText.visibility = View.VISIBLE
            textView.visibility = View.VISIBLE
            isLoading = !isLoading
        }


    }



}
