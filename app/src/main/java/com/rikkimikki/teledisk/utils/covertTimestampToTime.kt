package com.rikkimikki.teledisk.utils


import java.sql.Timestamp
import java.text.CharacterIterator
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
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

fun humanReadableByteCountSI(bytesCount: Long): String {
    var bytes = bytesCount
    if (-1000 < bytes && bytes < 1000) {
        return "$bytes B"
    }
    val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
    while (bytes <= -999950 || bytes >= 999950) {
        bytes /= 1000
        ci.next()
    }
    return String.format("%.1f %cB", bytes / 1000.0, ci.current())
}