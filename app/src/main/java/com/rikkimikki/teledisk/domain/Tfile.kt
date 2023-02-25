package com.rikkimikki.teledisk.domain

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import org.drinkless.td.libcore.telegram.TdApi

data class Tfile(
    val name:String,
    val type: FileType,
    val size:Long,
    val path:String,
    val unixTimeDate:Long,
    val preview: TdApi.File? = null,
    val groupID:Long=0L,
    val fileID:Long=0L
) : TdObject() {
}