package com.rikkimikki.teledisk.presentation.main

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.rikkimikki.teledisk.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*supportFragmentManager.beginTransaction()
            .replace(R.id.main_view_container,MainFragment.newInstance())
            .commit()*/

    }
    companion object {
        fun getInstance(context:Context): Intent {
            return Intent(context,MainActivity::class.java)
        }
    }
}