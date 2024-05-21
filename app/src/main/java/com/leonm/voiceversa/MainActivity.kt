package com.leonm.voiceversa

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import com.leonm.voiceversa.databinding.ActivityMainBinding
import kotlinx.coroutines.runBlocking
import java.security.AccessController.getContext
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(){
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var defaultToolbar: Toolbar
    private var openAiHandler = OpenAiHandler()
    private lateinit var sharedPreferencesManager : SharedPreferencesManager


    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        sharedPreferencesManager = SharedPreferencesManager(this)

        runBlocking {

            val apiKey = sharedPreferencesManager.loadData("API_KEY", "")
            val isValid = openAiHandler.checkApiKey(apiKey)

            if (!isValid) {
                sharedPreferencesManager.saveData("isApiKeyValid", false)
                alertApiKey(this@MainActivity)
            } else {

                sharedPreferencesManager.saveData("isApiKeyValid", true)
                openAiHandler.getAvailableModels(this@MainActivity)
            }



        }




        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        defaultToolbar = binding.toolbar


        setSupportActionBar(defaultToolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)








        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_YES


        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }



    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)


        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.SecondFragment -> {
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
        return navController.navigateUp(appBarConfiguration) ||
            super.onSupportNavigateUp()
    }


    @Suppress("ktlint:standard:property-naming")
    private val REQUEST_ID_MULTIPLE_PERMISSIONS = 1

    private fun checkAndRequestPermissions(): Boolean {
        val permissionReadExternalStorage: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this,
                    permission.READ_MEDIA_IMAGES,
                )
            } else {
                ContextCompat.checkSelfPermission(
                    this,
                    permission.READ_EXTERNAL_STORAGE,
                )
            }
        val permissionWriteExternalStorage: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this,
                    permission.READ_MEDIA_AUDIO,
                )
            } else {
                ContextCompat.checkSelfPermission(
                    this,
                    permission.WRITE_EXTERNAL_STORAGE,
                )
            }
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (permissionWriteExternalStorage != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listPermissionsNeeded.add(
                    permission.READ_MEDIA_AUDIO,
                )
            } else {
                listPermissionsNeeded.add(permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        if (permissionReadExternalStorage != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listPermissionsNeeded.add(
                    permission.READ_MEDIA_IMAGES,
                )
            } else {
                listPermissionsNeeded.add(permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionVideoStorage =
                ContextCompat.checkSelfPermission(
                    this,
                    permission.READ_MEDIA_VIDEO,
                )
            if (permissionVideoStorage != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission.READ_MEDIA_VIDEO)
            }
            val notificationPermission =
                ContextCompat.checkSelfPermission(
                    this,
                    permission.POST_NOTIFICATIONS,
                )
            if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission.POST_NOTIFICATIONS)
            }
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray<String>(),
                REQUEST_ID_MULTIPLE_PERMISSIONS,
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ID_MULTIPLE_PERMISSIONS -> {
                val perms: MutableMap<String, Int> = HashMap()
                // Initialize the map with both permissions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms[permission.READ_MEDIA_IMAGES] = PackageManager.PERMISSION_GRANTED
                    perms[permission.READ_MEDIA_AUDIO] = PackageManager.PERMISSION_GRANTED
                    perms[permission.READ_MEDIA_VIDEO] = PackageManager.PERMISSION_GRANTED
                    perms[permission.POST_NOTIFICATIONS] =
                        PackageManager.PERMISSION_GRANTED
                    perms[permission.SYSTEM_ALERT_WINDOW] =
                        PackageManager.PERMISSION_GRANTED
                } else {
                    perms[permission.WRITE_EXTERNAL_STORAGE] =
                        PackageManager.PERMISSION_GRANTED
                    perms[permission.READ_EXTERNAL_STORAGE] =
                        PackageManager.PERMISSION_GRANTED
                }

                // Fill with actual results from user
                if (grantResults.isNotEmpty()) {
                    var i = 0
                    while (i < permissions.size) {
                        perms[permissions[i]] = grantResults[i]
                        i++
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (perms[permission.READ_MEDIA_IMAGES] == PackageManager.PERMISSION_GRANTED &&
                            perms[permission.READ_MEDIA_AUDIO] == PackageManager.PERMISSION_GRANTED &&
                            perms[permission.READ_MEDIA_VIDEO] == PackageManager.PERMISSION_GRANTED &&
                            perms[permission.POST_NOTIFICATIONS] == PackageManager.PERMISSION_GRANTED &&
                            perms[permission.SYSTEM_ALERT_WINDOW] == PackageManager.PERMISSION_GRANTED
                        ) {
                            Toast.makeText(
                                this,
                                "Permissions Granted! :)",
                                Toast.LENGTH_LONG,
                            ).show()
                            permissionSettingScreen()
                            // else any one or both the permissions are not granted
                        } else {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    permission.READ_MEDIA_IMAGES,
                                ) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    permission.READ_MEDIA_AUDIO,
                                ) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    permission.READ_MEDIA_VIDEO,
                                ) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    permission.POST_NOTIFICATIONS,
                                )
                            ) {
                                showDialogOK { dialog, which ->
                                    when (which) {
                                        DialogInterface.BUTTON_POSITIVE -> checkAndRequestPermissions()
                                        DialogInterface.BUTTON_NEGATIVE ->
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Necessary Permissions required for this app",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                    }
                                }
                            } else {
                                permissionSettingScreen()
                            }
                        }
                    } else {
                        if (perms[permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED &&
                            perms[permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED
                        ) {
                            Toast.makeText(
                                this,
                                "Permissions Granted! :)",
                                Toast.LENGTH_SHORT,
                            ).show()
                            // else any one or both the permissions are not granted
                        } else {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    permission.WRITE_EXTERNAL_STORAGE,
                                ) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    permission.READ_EXTERNAL_STORAGE,
                                )
                            ) {
                                showDialogOK { dialog, which ->
                                    when (which) {
                                        DialogInterface.BUTTON_POSITIVE -> checkAndRequestPermissions()
                                        DialogInterface.BUTTON_NEGATIVE ->
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Necessary Permissions required for this app",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                    }
                                }
                            } else {
                                permissionSettingScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun permissionSettingScreen() {
        Toast.makeText(this, "Scroll down and grant VoiceVersa permission.", Toast.LENGTH_LONG)
            .show()
        val intent = Intent()
        intent.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        val uri = Uri.fromParts("package", packageName, null)
        intent.setData(uri)
        startActivity(intent)
        // finishAffinity();
    }

    private fun showDialogOK(okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this)
            .setMessage("Necessary Permissions required for this app")
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", okListener)
            .create()
            .show()
    }

    private fun alertApiKey(context: Context) {

            val textInputLayout = TextInputLayout(context)
            textInputLayout.setPadding(
                resources.getDimensionPixelOffset(R.dimen.dp_19),
                0,
                resources.getDimensionPixelOffset(R.dimen.dp_19),
                0
            )
            val input = EditText(context)
            input.setHintTextColor(resources.getColor(R.color.md_theme_dark_onPrimary))
            textInputLayout.hint = "API key"
            textInputLayout.addView(input)

            val alert = AlertDialog.Builder(context)
                .setTitle("Api key")
                .setCancelable(false)
                .setView(textInputLayout)
                .setMessage("Please enter a valid OpenAI API key.")
                .setPositiveButton(Html.fromHtml("<font color='#FFFFFF'>Submit</font>",0)) { dialog, _ ->
                    var isKeyValid = false
                    runBlocking {  isKeyValid = openAiHandler.checkApiKey(input.text.toString()) }
                   if (isKeyValid) {
                       sharedPreferencesManager.saveData("API_KEY", input.text.toString())
                       Toast.makeText(this, "Api key is invalid", Toast.LENGTH_LONG)
                           .show()
                       dialog.cancel()
                   }else{
                       Toast.makeText(this, "Api key is invalid", Toast.LENGTH_LONG)
                           .show()
                       alertApiKey(this)

                   }

                }
                .setNegativeButton(Html.fromHtml("<font color='#FFFFFF'>Exit</font>",0)) { dialog, _ ->
                    dialog.cancel()
                   this.finish()
                    exitProcess(0)
                }.create()

            alert.show()

        }





}
