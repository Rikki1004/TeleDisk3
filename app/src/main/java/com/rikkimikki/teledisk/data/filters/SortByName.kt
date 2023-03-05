package com.rikkimikki.teledisk.data.filters

import com.rikkimikki.teledisk.domain.TdObject

class SortByName(){}
/*
class SortByName : Comparator<TdObject> {
    override fun compare(p0: TdObject, p1: TdObject): Int {
        return if (p0.size > p1.size) 1 else if (p0.size === p1.size) 0 else -1
        // **or** the previous return statement can be simplified to:
        return p0.size - p1.size
    }
}*/
