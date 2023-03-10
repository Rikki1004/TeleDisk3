package com.rikkimikki.teledisk.utils


import com.rikkimikki.teledisk.domain.TdObject
import java.sql.Timestamp
import java.text.CharacterIterator
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.*


const val GLOBAL_MAIN_STORAGE_PATH = "/storage/emulated/0"
const val GLOBAL_CACHE_DIRS_PATH_OFFSET = "/Android/data"
const val GLOBAL_REMOTE_STORAGE_PATH = "/"
fun covertTimestampToTime(timestamp:Long?) : String{
    if (timestamp == null || timestamp == 0L) return ""
    val stamp = Timestamp(timestamp)
    val date = Date(stamp.time)
    val patern = "dd.MM.yyyy, HH:mm"
    val sdf = SimpleDateFormat(patern, Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()
    return sdf.format(date)
}

fun humanReadableByteCountSI(bytesCount: Long): String {
    if (bytesCount == Long.MAX_VALUE)
        return "∞"
    if (bytesCount == 0L)
        return ""
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
