package com.rikkimikki.teledisk.presentation.login

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private val vm: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        /*vm.getReadyState().observe(this, Observer {
            if (it){
                println(it)
            }else{
                vm.sendPhone("phone")
            }
        })*/

        //requestPermissions(this,123)

        /*dataFromLocal.observe(this, Observer {
            println(it)
        })

        dataFromRemote.observe(this, Observer {
            println(it)
        })*/

        //getDataFromLocal("/storage/emulated/0/Download")


        /*vm.chathFlow.observe(this, Observer {
            Toast.makeText(this, it.chatId.toString(), Toast.LENGTH_SHORT).show()
        })*/
        /*vm.getAllChats().observe(this, Observer {
            for (i in it){
                println("chat--------- "+i.title.toString()+" "+i.id)
                helloStub.text = helloStub.text.toString()+i.title.toString()+"\n"
            }

        })*/

        /*vm.authState.observe(this, Observer { state ->
            if (state == null) return@Observer //skip

            Toast.makeText(this, state.toString(), Toast.LENGTH_LONG).show()

            if (state !is AuthState.LoggedIn) {
                *//*MaterialDialog(this).show {
                    input(hint = state.dialogHint) { _, text ->
                        when (state) {
                            AuthState.EnterPhone -> vm.sendPhone(text.toString())
                            AuthState.EnterCode -> vm.sendCode(text.toString())
                            is AuthState.EnterPassword -> vm.sendPassword(text.toString())
                        }
                    }
                    positiveButton(R.string.send)
                }*//*
            }
            if (state is AuthState.LoggedIn)
                Toast.makeText(this, "loggid", Toast.LENGTH_SHORT).show()
        })*/

        vm.error.observe(this, Observer {
            /*MaterialDialog(this).show {
                title(text = "Error!")
                message(text = it)
            }*/
        })

        /*vm.newMessage.observe(this, Observer {
            helloStub.text = it.joinToString("\n")
        })*/

    }

    /*fun requestPermissions(activity: Activity, requestCode:Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", activity.getPackageName())));
                activity.startActivityForResult(intent, requestCode);
            } catch (e:Exception) {
                val intent = Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, requestCode);
            }
        } else {
            ActivityCompat.requestPermissions(activity,
                arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE),
                requestCode)

        }
    }*/

}