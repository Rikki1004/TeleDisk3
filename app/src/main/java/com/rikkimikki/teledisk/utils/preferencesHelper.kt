package com.rikkimikki.teledisk.utils

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.res.Configuration
import com.rikkimikki.teledisk.domain.FiltersFromType


private const val NIGHT_MODE = "NIGHT_MODE"
private const val TOOGLE = "TOOGLE"

private const val DARK_MODE = "DARK_MODE"
private const val FILTER_TYPE = "FILTER_TYPE"

fun isNightModeEnabled(context: Context): Boolean {
    val mPrefs: SharedPreferences = context.getSharedPreferences(DARK_MODE, MODE_PRIVATE)
    return mPrefs.getBoolean(NIGHT_MODE, false)
}

fun setIsNightModeEnabled(context: Context, isNightModeEnabled: Boolean) {
    val mPrefs: SharedPreferences = context.getSharedPreferences(DARK_MODE, MODE_PRIVATE)
    val editor: SharedPreferences.Editor = mPrefs.edit()
    editor.putBoolean(NIGHT_MODE, isNightModeEnabled)
    editor.apply()
}

fun setIsToogleEnabled(context: Context, isToogleEnabled: Boolean) {
    val mPrefs: SharedPreferences = context.getSharedPreferences(DARK_MODE, MODE_PRIVATE)
    val editor: SharedPreferences.Editor = mPrefs.edit()
    editor.putBoolean(TOOGLE, isToogleEnabled)
    editor.apply()
}

fun isToogleEnabled(context: Context): Boolean {
    val mPrefs: SharedPreferences = context.getSharedPreferences(DARK_MODE, MODE_PRIVATE)
    return mPrefs.getBoolean(TOOGLE, false)
}

fun isDarkMode(activity: Activity): Boolean {
    return activity.getResources()
        .getConfiguration().uiMode and Configuration.UI_MODE_NIGHT_MASK === Configuration.UI_MODE_NIGHT_YES
}


fun saveCount(context: Context,filter:FiltersFromType,count:Int){
    val mPrefs: SharedPreferences = context.getSharedPreferences(FILTER_TYPE, MODE_PRIVATE)
    val editor: SharedPreferences.Editor = mPrefs.edit()
    editor.putInt(filter.name, count)
    editor.apply()
}
fun getCount(context: Context,filter:FiltersFromType): String {
    val mPrefs: SharedPreferences = context.getSharedPreferences(FILTER_TYPE, MODE_PRIVATE)
    return mPrefs.getInt(filter.name,0).toString()
}