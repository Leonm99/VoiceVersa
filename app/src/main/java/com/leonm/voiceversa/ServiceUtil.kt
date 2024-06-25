package com.leonm.voiceversa

import android.content.Context
import android.content.Intent
import android.util.Log

object ServiceUtil {
    fun startFloatingService(context: Context, command: String, path: String) {
        Log.d("ServiceUtil", "Starting service with command: $command and path: $path")
        val intent = Intent(context, FloatingService::class.java).apply {
            putExtra(INTENT_COMMAND, command)
            putExtra("PATH", path)
        }
        context.startForegroundService(intent)
    }
}