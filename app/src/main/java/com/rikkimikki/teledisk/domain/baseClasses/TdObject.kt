package com.rikkimikki.teledisk.domain.baseClasses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TdObject (
    val name:String,
    val placeType: PlaceType,
    val fileType: FileType,
    val path:String,
    val size:Long=0,
    val unixTimeDate:Long=0,
    val previewFile: Int? = null, //String? = null,
    val groupID:Long=0L,
    val fileID:Int=-1,
    val messageID:Long=-1L,
    var isChecked:Boolean = false
): Parcelable{

    fun equals2(other: Any?): Boolean {
        if (other !is TdObject)
            return false
        return (
                name == other.name &&
                placeType == other.placeType &&
                fileType == other.fileType &&
                path == other.path &&
                size == other.size &&
                unixTimeDate == other.unixTimeDate &&
                fileID == other.fileID &&
                groupID == other.groupID
                )
    }

    fun is_file():Boolean{
        return fileType == FileType.File
    }
    fun is_folder():Boolean{
        return fileType == FileType.Folder
    }
    fun is_local():Boolean{
        return placeType == PlaceType.Local
    }
    fun getFilePath():String{
        return path.substringBeforeLast("/")+"/"
    }
}