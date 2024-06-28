package com.leonm.voiceversa

import android.Manifest.permission
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.leonm.voiceversa.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var defaultToolbar: Toolbar
    private  lateinit var openAiHandler: OpenAiHandler
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openAiHandler = OpenAiHandler(this@MainActivity)
        checkAndRequestPermissions()

        sharedPreferencesManager = SharedPreferencesManager(this)

        lifecycleScope.launch {
            val apiKey = sharedPreferencesManager.loadData("api_key_preference", "")
            val isValid = openAiHandler.checkApiKey(apiKey)

            if (!isValid) {
                sharedPreferencesManager.saveData("isApiKeyValid", false)
                openAiHandler.alertApiKey()
            } else {
                sharedPreferencesManager.saveData("isApiKeyValid", true)
                openAiHandler.getAvailableModels()
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        defaultToolbar = binding.toolbar
        setSupportActionBar(defaultToolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        configureNightMode()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.SettingsFragment -> {
                NavigationUI.onNavDestinationSelected(
                    item,
                    findNavController(R.id.nav_host_fragment_content_main),
                )
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun configureNightMode() {
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    private val permissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                Toast.makeText(this, "Permissions Granted! :)", Toast.LENGTH_SHORT).show()
                showOverlayPermissionDialog()
            } else {
                showDialogOK { _, which ->
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        checkAndRequestPermissions()
                    } else {
                        Toast.makeText(this, "Necessary Permissions required for this app", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionRequestLauncher.launch(permissions.toTypedArray())
        } else {
            showOverlayPermissionDialog()
        }
    }

    private fun showOverlayPermissionDialog() {
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Draw Over Other Apps Permission")
                .setMessage("To continue, please scroll down and find VoiceVersa in the list, then enable the 'Draw over other apps' permission.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, REQUEST_CODE_DRAW_OVERLAY)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(this, "Draw over other apps permission is required for this app to function properly.", Toast.LENGTH_LONG).show()
                }
                .show()
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DRAW_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Draw over other apps permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Draw over other apps permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDialogOK(okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this)
            .setMessage("Necessary Permissions required for this app")
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", okListener)
            .create()
            .show()
    }

    companion object {
        private const val REQUEST_CODE_DRAW_OVERLAY = 1234
    }
}
