package com.rikkimikki.teledisk.domain

import android.graphics.Bitmap

data class Tfolder(
    val name:String,
    val type: FolderType,
    val path:String,
    val unixTimeDate:Long=0,
    val size:Long=0,
    val tdObject:List<TdObject> = listOf<TdObject>()
): TdObject() {
}