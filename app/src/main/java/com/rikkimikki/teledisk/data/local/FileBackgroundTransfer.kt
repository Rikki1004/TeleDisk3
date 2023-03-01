package com.rikkimikki.teledisk.data.local

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_DEFAULT
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.domain.*
import com.rikkimikki.teledisk.presentation.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File


class FileBackgroundTransfer: Service() {
    val repository = TelegramRepository

    private val getRemoteFilesUseCase = GetRemoteFilesUseCase(repository)
    private val getLocalFilesUseCase = GetLocalFilesUseCase(repository)
    private val getAllChatsUseCase = GetAllChatsUseCase(repository)
    private val fileTransferFileUseCase = TransferFileUseCase(repository)
    private val fileOperationComplete = FileOperationCompleteUseCase(repository)
    val fileScope = repository.dataFromStore
    val chatScope = repository.allChats
    private var currentFileId = -1

    private lateinit var lambda : (TdApi.File) -> Unit

    private lateinit var notification:Notification

    private val CHANNEL_ID = "Foreground LibTd Operations"
    private val CHANNEL_NAME = "Foreground File Transfer"
    private val SERVICE_ID = 1

    companion object {
        private lateinit var mNotificationManager :NotificationManager

        private const val EXTRA_FILE = "FILE"
        private const val EXTRA_FOLDER_DESTINATION = "FOLDER"
        private const val EXTRA_JUST_OPEN = "JUST_OPEN"

        private const val byteBufferSize = 1024 * 1024 * 50


        fun getIntent(context: Context, file:TdObject,folderDestination : TdObject) :Intent{
            val intent = Intent(context,
                FileBackgroundTransfer::class.java)
            intent.putExtra(EXTRA_FILE, file)
            intent.putExtra(EXTRA_FOLDER_DESTINATION, folderDestination)
            intent.putExtra(EXTRA_JUST_OPEN, false)
            return intent
        }
        fun getIntent(context: Context, file:TdObject) :Intent{
            val intent = Intent(context,
                FileBackgroundTransfer::class.java)
            intent.putExtra(EXTRA_FILE, file)
            intent.putExtra(EXTRA_JUST_OPEN, true)
            return intent
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, FileBackgroundTransfer::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        //val notificationIntent = Intent(this, MainActivity::class.java)

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
            .setPriority(PRIORITY_MAX)
            .setDefaults(FOREGROUND_SERVICE_DEFAULT)
            //.addAction()
            .build()

        startForeground(SERVICE_ID, notification)


        startObservers()


        val scope = CoroutineScope(Dispatchers.IO)
        val(file,folder,justOpen) = checkIntent(intent)


        if (folder == null ) when{
            file.is_local()  -> {throw java.lang.Exception()}
            !file.is_local()  -> {
                lambda = {fileOperationComplete().postValue(Pair(it.local.path,true)); stopSelf()}
                scope.launch {fileTransferFileUseCase(file).let { if (it.local.isDownloadingCompleted) lambda(it) } }
            }

        } else when{
            file.is_local() && folder.is_local() -> {}
            file.is_local() && !folder.is_local() -> {}
            !file.is_local() && !folder.is_local() -> {}
            !file.is_local() && folder.is_local() -> {
                lambda = { it ->
                    //val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    val localFileOnTd = File(it.local.path)
                    var fileDestination = File(folder.path+"/"+file.name)
                    var counter = 1
                    while (fileDestination.exists()){
                        fileDestination = File(folder.path+"/("+ counter++ +")"+file.name)
                    }


                    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Перемещается")
                        .setContentText(file.name)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setProgress(0,0,true)
                        .build()
                    mNotificationManager.notify(SERVICE_ID, notification)

                    //notification.contentView.setProgressBar(R.id.progress_horizontal, 10, 5, false);
                    // notify the notification manager on the update.
                    //mNotificationManager.notify(SERVICE_ID, notification);

                    //localFileOnTd.renameTo(fileDestination)
                    localFileOnTd.copyTo(fileDestination)

                    fileOperationComplete().postValue(Pair(it.local.path,false))

                    stopSelf()
                }
                scope.launch {fileTransferFileUseCase(file).let { if (it.local.isDownloadingCompleted) lambda(it) } }
                //scope.launch {fileTransferFileUseCase(file)}

            }
        }




        //stopSelf();
        return START_NOT_STICKY
    }



    private fun startObservers() {
        TelegramRepository.downloadLD.observeForever {
            if (currentFileId == it.id){
                //mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                if (it.local.isDownloadingCompleted){
                    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Успешно загружен")
                        .setContentText("Файл")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setProgress(0,0,false)
                        .build()
                    mNotificationManager.notify(SERVICE_ID, notification)

                    lambda(it)
                }
                else{
                    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Загружается")
                        .setContentText("Файл")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setProgress(it.size,it.local.downloadedSize,false)
                        .build()
                    mNotificationManager.notify(SERVICE_ID, notification)
                }

            }
        }
    }

    private fun checkIntent(intent: Intent?) : Triple<TdObject,TdObject?,Boolean>{
        if (intent == null) throw java.lang.Exception()
        val file = intent.getParcelableExtra<TdObject>(EXTRA_FILE) ?: throw java.lang.Exception()
        val folder = intent.getParcelableExtra<TdObject>(EXTRA_FOLDER_DESTINATION)
        val justOpen = intent.getBooleanExtra(EXTRA_JUST_OPEN,false)
        currentFileId = file.fileID
        return Triple(file,folder,justOpen)
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