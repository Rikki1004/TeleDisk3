package com.rikkimikki.teledisk.data.local

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.domain.GetAllChatsUseCase
import com.rikkimikki.teledisk.domain.GetLocalFilesUseCase
import com.rikkimikki.teledisk.domain.GetRemoteFilesUseCase
import com.rikkimikki.teledisk.presentation.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FileBackgroundTransfer : Service() {
    val repository = TelegramRepository

    private val getRemoteFilesUseCase = GetRemoteFilesUseCase(repository)
    private val getLocalFilesUseCase = GetLocalFilesUseCase(repository)
    private val getAllChatsUseCase = GetAllChatsUseCase(repository)
    val fileScope = repository.dataFromStore
    val chatScope = repository.allChats
    private var currentFileId = -1

    private lateinit var notification:Notification

    private val CHANNEL_ID = "Foreground LibTd Operations"
    private val CHANNEL_NAME = "Foreground File Transfer"
    private val SERVICE_ID = 1

    companion object {
        const val EXTRA_FILE_ID = "FILE_ID"
        const val EXTRA_FILE_Path = "FILE_PATH"

        fun startService(context: Context, id:Int) {
            val startIntent = Intent(context,
                FileBackgroundTransfer::class.java)
            startIntent.putExtra(EXTRA_FILE_ID, id)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun getIntent(context: Context, id:Int) :Intent{
            val startIntent = Intent(context,
                FileBackgroundTransfer::class.java)
            startIntent.putExtra(EXTRA_FILE_ID, id)
            return startIntent
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, FileBackgroundTransfer::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        TelegramRepository.downloadLD.observeForever {
            if (currentFileId == it.id){
                val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Загружается")
                    .setContentText("Файл")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setProgress(it.size,it.local.downloadedSize,false)
                    .build()

                mNotificationManager.notify(SERVICE_ID, notification)
            }
        }

        //do heavy work on a background thread
        val id = intent!!.getIntExtra(EXTRA_FILE_ID,0)
        currentFileId = id

        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            //TelegramRepository.getAllChats()
            TelegramRepository.loadFile(id)

        }

        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE)
            }
        notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Загружается")
            .setContentText("Файл")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            //.addAction()
            .build()

        startForeground(SERVICE_ID, notification)
        //stopSelf();
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT)

            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }
}