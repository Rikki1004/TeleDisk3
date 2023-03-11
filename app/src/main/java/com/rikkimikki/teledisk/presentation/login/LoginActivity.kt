package com.rikkimikki.teledisk.presentation.login

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.rikkimikki.teledisk.R

class LoginActivity : AppCompatActivity() {
    private val vm: LoginViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        vm.error.observe(this) {
            println(it)
        }
    }
}