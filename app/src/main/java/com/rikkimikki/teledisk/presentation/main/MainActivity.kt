package com.rikkimikki.teledisk.presentation.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.material.navigation.NavigationView
import com.rikkimikki.teledisk.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView*/

        //drawerLayout.openDrawer(R.id.drawer_layout)

        /*appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )*/
        /*navView.menu.add("MAINMenu");
        for (i in 1..10){
            navView.menu.addSubMenu("Menu"+i);
            //navView.menu.add(Menu.NONE, R.id.nav_home, Menu.NONE,"Menu"+i);
        }
        navView.showContextMenu()*/

        /*val disks = arrayOf("disk1","disk2","disk3","disk4")

        val menu = navView.menu
        val submenu: Menu = menu.addSubMenu("Удаленные диски")

        for (i in disks)
            submenu.add("Super Item"+i)


        navView.invalidate()

        navView.setNavigationItemSelectedListener {
            Toast.makeText(this, "rsedc", Toast.LENGTH_SHORT).show()
            return@setNavigationItemSelectedListener true
        }*/
    }


    companion object {
        fun getInstance(context:Context): Intent {
            return Intent(context,MainActivity::class.java)
        }
    }
}