package com.rikkimikki.teledisk.data.tdLib

import com.rikkimikki.teledisk.utils.GLOBAL_PATH_TO_FILES
import org.drinkless.td.libcore.telegram.TdApi


object TelegramCredentials {
    val parameters = TdApi.TdlibParameters().apply {
        databaseDirectory = GLOBAL_PATH_TO_FILES + "td"
        useMessageDatabase = false//true
        useSecretChats = false
        apiId = 0
        apiHash = "App_hash"
        useFileDatabase = false
        systemLanguageCode = "en"
        deviceModel = "Android"
        systemVersion = "12"
        applicationVersion = "1.0"
        enableStorageOptimizer = true
        useChatInfoDatabase = true
    }
}