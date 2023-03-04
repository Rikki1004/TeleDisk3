package com.rikkimikki.teledisk.domain

enum class FiltersFromType(vararg ext:String) {
    PHOTO("ico","png","jpg","jpeg","mpeg","tiff","bmp","svg"),
    VIDEO("3g2","asf","avi","f4v","flv","h264","m4v","mkv","mov","mp4","h265","mpeg","mts","ts","webm","wmv"),
    DOCUMENTS("asp","doc","docm","docx","dot","epub","fb2","key","pdf","txt","bat","pot","potm","potx","pps","ppt","pptm","pptx","rtf","xlr","xls","xls","xlsm","xlsx"),
    MUSIC("aac","ac3","aif","aiff","amr","aud","flac","iff","m3u","m3u8","m4a","m4b","mp3","ogg","wav","xwb"),
    APPS("apk")
}
