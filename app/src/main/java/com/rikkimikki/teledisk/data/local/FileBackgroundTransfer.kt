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

    private val transferFileDownloadUseCase = TransferFileDownloadUseCase(repository)
    private val transferFileUploadUseCase = TransferFileUploadUseCase(repository)
    private val fileOperationComplete = FileOperationCompleteUseCase(repository)
    private val sendUploadedFileUseCase = SendUploadedFileUseCase(repository)
    private var currentFileId = -1

    private lateinit var lambda : (TdApi.File) -> Unit

    private lateinit var notification:Notification



    companion object {
        private const val CHANNEL_ID = "Foreground LibTd Operations"
        private const val CHANNEL_NAME = "Foreground File Transfer"
        private const val SERVICE_ID = 1

        private lateinit var mNotificationManager :NotificationManager

        private const val EXTRA_FILE = "FILE"
        private const val EXTRA_COPY = "COPY"
        private const val EXTRA_FOLDER_DESTINATION = "FOLDER"
        private const val EXTRA_IS_DOWNLOAD = "IS_DOWNLOAD"

        private const val byteBufferSize = 1024 * 1024 * 50


        fun getIntent(context: Context, file:TdObject,folderDestination : TdObject,is_copy:Boolean) :Intent{
            val intent = Intent(context,
                FileBackgroundTransfer::class.java)
            intent.putExtra(EXTRA_FILE, file)
            intent.putExtra(EXTRA_COPY, is_copy)
            intent.putExtra(EXTRA_FOLDER_DESTINATION, folderDestination)
            intent.putExtra(EXTRA_IS_DOWNLOAD, file.placeType != PlaceType.Local && folderDestination.placeType == PlaceType.Local)
            return intent
        }
        fun getIntent(context: Context, file:TdObject) :Intent{
            val intent = Intent(context,
                FileBackgroundTransfer::class.java)
            intent.putExtra(EXTRA_FILE, file)
            return intent
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

        val scope = CoroutineScope(Dispatchers.IO)
        val(file,folder,isDownload) = checkIntent(intent)

        startObservers(isDownload)





        if (folder == null ) when{
            file.is_local()  -> {throw java.lang.Exception()}
            !file.is_local()  -> {
                lambda = {fileOperationComplete().postValue(Pair(it.local.path,true)); stopSelf()}
                scope.launch {transferFileDownloadUseCase(file).let { if (it.local.isDownloadingCompleted) lambda(it) } }
            }

        } else when{
            file.is_local() && folder.is_local() -> {
                val localFileOnTd = File(file.path)
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

                localFileOnTd.copyTo(fileDestination)

                fileOperationComplete().postValue(Pair(fileDestination.path,false))

                stopSelf()
            }
            file.is_local() && !folder.is_local() -> {
                lambda = {

                    val groupId = folder.groupID
                    val remotePath = folder.path
                    val inputFileLocal = TdApi.InputFileLocal(file.path)
                    val formattedText = TdApi.FormattedText(remotePath+"/"+file.name, arrayOf())
                    val doc = TdApi.InputMessageDocument(inputFileLocal,TdApi.InputThumbnail(), formattedText)

                    scope.launch {
                        sendUploadedFileUseCase(groupId,doc)
                        fileOperationComplete().postValue(Pair(it.local.path,false))
                        println(it)
                        stopSelf()
                    }
                }
                scope.launch {transferFileUploadUseCase(file).let { currentFileId = it.id;if (it.remote.isUploadingCompleted) lambda(it) } }

            }
            !file.is_local() && !folder.is_local() -> {

                val groupId = folder.groupID
                val remotePath = folder.path
                val inputFileLocal = TdApi.InputFileId(file.fileID)
                val formattedText = TdApi.FormattedText(remotePath+"/"+file.name, arrayOf())
                val doc = TdApi.InputMessageDocument(inputFileLocal,TdApi.InputThumbnail(), formattedText)
                scope.launch {
                    val a = sendUploadedFileUseCase(groupId,doc)
                    fileOperationComplete().postValue(Pair(formattedText.text,false))
                    stopSelf()}

            }
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
                val a = scope.launch {transferFileDownloadUseCase(file).let { if (it.local.isDownloadingCompleted) lambda(it) } }
                //scope.launch {fileTransferFileUseCase(file)}

            }
        }




        //stopSelf();
        return START_NOT_STICKY
    }



    private fun startObservers(isDownload:Boolean) {
        TelegramRepository.downloadLD.observeForever {
            if (currentFileId == it.id){
                //mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                //if ((isDownload && it.local.isDownloadingCompleted) || (!isDownload && it.remote.isUploadingCompleted)){
                if ((isDownload && it.local.isDownloadingCompleted) || (!isDownload && it.local.downloadedSize == it.remote.uploadedSize)){
                    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle(if (isDownload) "Успешно загружен" else "Успешно выгружен")
                        .setContentText("Файл")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setProgress(0,0,false)
                        .build()
                    mNotificationManager.notify(SERVICE_ID, notification)

                    currentFileId = -1
                    lambda(it)

                }
                else{
                    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Загружается")
                        .setContentText("Файл")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setProgress(it.size,if (isDownload) it.local.downloadedSize else it.remote.uploadedSize,false)
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
        val isDownload = intent.getBooleanExtra(EXTRA_IS_DOWNLOAD,false)
        currentFileId = file.fileID
        return Triple(file,folder,isDownload)
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