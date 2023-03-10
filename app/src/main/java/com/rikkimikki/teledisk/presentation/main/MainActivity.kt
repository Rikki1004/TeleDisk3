package com.rikkimikki.teledisk.presentation.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentContainerView
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.ActivityMainBinding
import com.rikkimikki.teledisk.utils.isNightModeEnabled

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        if (isNightModeEnabled(applicationContext)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!checkPermission())
            requestPermissions(this)

    }



    private fun requestPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.data = Uri.parse(String.format("package:%s", activity.packageName));
                activity.startActivity(intent);
            } catch (e:Exception) {
                val intent = Intent();
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;
                activity.startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                0)
        }
    }



    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val result: Int =
                ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            result == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        fun getInstance(context:Context): Intent {
            return Intent(context,MainActivity::class.java)
        }
    }
}