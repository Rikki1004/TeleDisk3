package com.rikkimikki.teledisk.domain

data class TdObject (
    val name:String,
    val placeType: PlaceType,
    val fileType: FileType,
    val path:String,
    val size:Long=0,
    val unixTimeDate:Long=0,
    val previewFile: Int? = null, //String? = null,
    val groupID:Long=0L,
    val fileID:Long=0L
){
    fun is_file():Boolean{
        return fileType == FileType.File
    }
    fun is_folder():Boolean{
        return fileType == FileType.Folder
    }
}