package com.rikkimikki.teledisk.utils


import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

fun covertTimestampToTime(timestamp:Long?) : String{
    if (timestamp == null) return ""
    val stamp = Timestamp(timestamp)
    val date = Date(stamp.time)
    val patern = "dd.MM.yyyy, HH:mm"
    val sdf = SimpleDateFormat(patern, Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()
    return sdf.format(date)
}