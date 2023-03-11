package com.rikkimikki.teledisk.data.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rikkimikki.teledisk.data.local.FileBackgroundTransfer.Companion.EXTRA_STOP_ACTION
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.domain.CancelFileTransferUseCase

class CancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (EXTRA_STOP_ACTION == action) {
            context.startService(Intent(context, FileBackgroundTransfer::class.java).setAction(EXTRA_STOP_ACTION))
        }
    }
}