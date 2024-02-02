package com.example.whispdroid

import android.content.Context
import android.content.Intent

fun Context.startFloatingService(command: String = "", path: String = "") {
    val intent = Intent(this, FloatingService::class.java)
    if (command.isNotBlank()) intent.putExtra(INTENT_COMMAND, command)
    if (path.isNotBlank()) intent.putExtra("PATH", path)
    this.startForegroundService(intent)
}






