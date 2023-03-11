package com.rikkimikki.teledisk.utils

import com.rikkimikki.teledisk.domain.baseClasses.TdObject

fun findIndex(obj: TdObject, list: List<TdObject>):Int?{
    var index : Int? = null
    for (i in list.indices){
        if (with(list[i]){
                name == obj.name &&
                        placeType == obj.placeType &&
                        fileType == obj.fileType &&
                        path == obj.path &&
                        size == obj.size &&
                        unixTimeDate == obj.unixTimeDate &&
                        fileID == obj.fileID &&
                        groupID == obj.groupID
            }
        )
            index = i
    }
    return index
}