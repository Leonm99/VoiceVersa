package com.example.whispdroid

import android.Manifest.permission
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import com.example.whispdroid.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkAndRequestPermissions()) {

            // Do your desire work here

        } else {
            // Call Again :
            checkAndRequestPermissions()
        }
        // ... rest of body of onCreateView() ...

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(
            item,
            findNavController(R.id.nav_host_fragment_content_main)
        )
                || super.onOptionsItemSelected(item)

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()

        Log.d("MainActivity", "onResume")
        // Find the FirstFragment by tag
        val firstFragment =
            supportFragmentManager.findFragmentByTag("FirstFragment") as? FirstFragment
        // Check if the fragment is not null and is added


        if (firstFragment != null) {
            Log.d("MainActivity", "reload")
            firstFragment.reloadData()
        }

    }

    val REQUEST_ID_MULTIPLE_PERMISSIONS = 1


    private fun checkAndRequestPermissions(): Boolean {
        val permissionReadExternalStorage: Int
        permissionReadExternalStorage =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(
                this,
                permission.READ_MEDIA_IMAGES
            ) else ContextCompat.checkSelfPermission(
                this,
                permission.READ_EXTERNAL_STORAGE
            )
        val permissionWriteExtarnalStorage: Int
        permissionWriteExtarnalStorage =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(
                this,
                permission.READ_MEDIA_AUDIO
            ) else ContextCompat.checkSelfPermission(
                this,
                permission.WRITE_EXTERNAL_STORAGE
            )
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (permissionWriteExtarnalStorage != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listPermissionsNeeded.add(
                permission.READ_MEDIA_AUDIO
            ) else listPermissionsNeeded.add(permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissionReadExternalStorage != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listPermissionsNeeded.add(
                permission.READ_MEDIA_IMAGES
            ) else listPermissionsNeeded.add(permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionVideoStorage = ContextCompat.checkSelfPermission(
                this,
                permission.READ_MEDIA_VIDEO
            )
            if (permissionVideoStorage != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission.READ_MEDIA_VIDEO)
            }
            val notificationPermission = ContextCompat.checkSelfPermission(
                this,
                permission.POST_NOTIFICATIONS
            )
            if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission.POST_NOTIFICATIONS)
            }


        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray<String>(),
                REQUEST_ID_MULTIPLE_PERMISSIONS
            )
            return false
        }
        return true
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
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
                                Toast.LENGTH_LONG
                            ).show()
                            permissionSettingScreen()
                            //else any one or both the permissions are not granted
                        } else {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    permission.READ_MEDIA_IMAGES
                                )
                                || ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    permission.READ_MEDIA_AUDIO
                                )
                                || ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    permission.READ_MEDIA_VIDEO
                                )
                                || ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    permission.POST_NOTIFICATIONS
                                )
                            ) {
                                showDialogOK(
                                    "Necessary Permissions required for this app"
                                ) { dialog, which ->
                                    when (which) {
                                        DialogInterface.BUTTON_POSITIVE -> checkAndRequestPermissions()
                                        DialogInterface.BUTTON_NEGATIVE ->                                                     // proceed with logic by disabling the related features or quit the app.
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Necessary Permissions required for this app",
                                                Toast.LENGTH_LONG
                                            ).show()
                                    }
                                }
                            } else {
                                permissionSettingScreen()
                            }
                        }
                    } else {
                        if (perms[permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED
                            && perms[permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED
                        ) {
                            Toast.makeText(
                                this,
                                "Permissions Granted! :)",
                                Toast.LENGTH_LONG
                            ).show()
                            //else any one or both the permissions are not granted
                        } else {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    permission.WRITE_EXTERNAL_STORAGE
                                )
                                || ActivityCompat.shouldShowRequestPermissionRationale(
                                    this,
                                    permission.READ_EXTERNAL_STORAGE
                                )
                            ) {
                                showDialogOK(
                                    "Necessary Permissions required for this app"
                                ) { dialog, which ->
                                    when (which) {
                                        DialogInterface.BUTTON_POSITIVE -> checkAndRequestPermissions()
                                        DialogInterface.BUTTON_NEGATIVE ->                                                     // proceed with logic by disabling the related features or quit the app.
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Necessary Permissions required for this app",
                                                Toast.LENGTH_LONG
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
        Toast.makeText(this, "Scroll down and grant WhispDroid permission.", Toast.LENGTH_LONG)
            .show()
        val intent = Intent()
        intent.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        val uri = Uri.fromParts("package", packageName, null)
        intent.setData(uri)
        startActivity(intent)
        // finishAffinity();

    }

    private fun showDialogOK(message: String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", okListener)
            .create()
            .show()
    }

}