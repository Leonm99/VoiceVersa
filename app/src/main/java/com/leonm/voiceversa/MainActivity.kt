package com.leonm.voiceversa

import android.Manifest.permission
import android.annotation.SuppressLint
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.leonm.voiceversa.databinding.ActivityMainBinding
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity(){
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var defaultToolbar: MaterialToolbar


    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()


        runBlocking {
            val isValid = SharedPreferencesManager(this@MainActivity).isValidApiKey()
            if (!isValid) {
                Toast.makeText(this@MainActivity, "Please a valid API key in settings!", Toast.LENGTH_SHORT).show()
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
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        // Inflate the menu for multi-selection toolbar


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
                if (grantResults.size > 0) {
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



}
