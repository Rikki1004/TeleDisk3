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
import kotlinx.coroutines.internal.LockFreeLinkedListNode
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class FileBackgroundTransfer: Service() {
    val repository = TelegramRepository

    private val transferFileDownloadUseCase = TransferFileDownloadUseCase(repository)
    private val transferFileUploadUseCase = TransferFileUploadUseCase(repository)
    private val fileOperationComplete = FileOperationCompleteUseCase(repository)
    private val sendUploadedFileUseCase = SendUploadedFileUseCase(repository)
    private val getRemoteFilesNoLDUseCase = GetRemoteFilesNoLDUseCase(repository)
    private val getLocalFilesNoLDUseCase = GetLocalFilesNoLDUseCase(repository)
    private val tempPathsForSendUseCase = TempPathsForSendUseCase(repository)
    private var currentFileId = -1

    private val lock = Mutex()
    private var lockedObject = Any()

    private lateinit var lambda : suspend (TdApi.File) -> Unit

    private lateinit var notification:Notification

    val scope = CoroutineScope(Dispatchers.IO)


    private lateinit var files : Array<TdObject>
    private var folderDestination : TdObject? = null
    private var isDownload : Boolean = true
    private var isCopy : Boolean = true
    private var needOpen : Boolean = false

    private var filesNeedSend = mutableListOf<TdObject>()


    companion object {
        private const val CHANNEL_ID = "Foreground LibTd Operations"
        private const val CHANNEL_NAME = "Foreground File Transfer"
        private const val SERVICE_ID = 1

        private lateinit var mNotificationManager :NotificationManager

        private const val EXTRA_FILES = "FILES"
        private const val EXTRA_COPY = "COPY"
        private const val EXTRA_FOLDER_DESTINATION = "FOLDER"
        private const val EXTRA_IS_DOWNLOAD = "IS_DOWNLOAD"
        private const val EXTRA_NEED_OPEN = "NEED_OPEN"

        private const val byteBufferSize = 1024 * 1024 * 50


        fun getIntent(context: Context, files:Array<TdObject>,folderDestination : TdObject,is_copy:Boolean) :Intent{
            val intent = Intent(context,
                FileBackgroundTransfer::class.java)
            intent.putExtra(EXTRA_FILES, files)
            intent.putExtra(EXTRA_COPY, is_copy)
            intent.putExtra(EXTRA_FOLDER_DESTINATION, folderDestination)
            intent.putExtra(EXTRA_IS_DOWNLOAD, files[0].placeType != PlaceType.Local && folderDestination.placeType == PlaceType.Local)
            return intent
        }
        fun getIntent(context: Context, file:TdObject) :Intent{
            val intent = Intent(context,
                FileBackgroundTransfer::class.java)
            intent.putExtra(EXTRA_FILES, arrayOf(file))
            intent.putExtra(EXTRA_NEED_OPEN,true)
            intent.putExtra(EXTRA_IS_DOWNLOAD, true)
            intent.putExtra(EXTRA_COPY, true)
            return intent
        }
        fun getIntent(context: Context, files: Array<TdObject>) :Intent{
            val intent = Intent(context,
                FileBackgroundTransfer::class.java)
            intent.putExtra(EXTRA_FILES, files)
            intent.putExtra(EXTRA_IS_DOWNLOAD, true)
            intent.putExtra(EXTRA_COPY, true)
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


        checkIntent(intent)

        startObservers(isDownload)

        scope.launch {
            lock.tryLock()
            val a = files
            println(a)
            for (i in files)
                transfer(i,folderDestination)

            if (filesNeedSend.isNotEmpty())
                tempPathsForSendUseCase().postValue(filesNeedSend)
            //stopSelf()
        }
        return START_NOT_STICKY
    }


    private suspend fun transfer(file: TdObject,folder:TdObject?) {
        if (folder == null ) when{
            //file.is_local() -> {fileOperationComplete().postValue(Pair(file.path,true)); stopSelf()} //newer
            !file.is_local() && needOpen -> {
                lambda = {fileOperationComplete().postValue(Pair(it.local.path,true)); stopSelf()}
                transferFileDownloadUseCase(file).let {currentFileId = it.id; if (it.local.isDownloadingCompleted) lambda(it) }
            }
            !file.is_local() && !needOpen -> {

                if (file.is_file()){
                    lambda = {

                        filesNeedSend.add(TdObject("file", PlaceType.Local, FileType.File,it.local.path))
                        fileOperationComplete().postValue(Pair(it.local.path,false))
                        if(lock.isLocked) lock.unlock()

                    }
                    transferFileDownloadUseCase(file).let {currentFileId = it.id; if (it.local.isDownloadingCompleted) lambda(it) else lock.lock()}

                }else{
                    for (i in getRemoteFilesNoLDUseCase(file.groupID,file.path)){
                        transfer(i,null)
                    }
                }

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

                localFileOnTd.copyRecursively(fileDestination)

                fileOperationComplete().postValue(Pair(fileDestination.path,false))

                stopSelf()
            }
            file.is_local() && !folder.is_local() -> {
                if (file.is_file()){
                    var a = file
                    println(a)

                    lambda = {

                        val groupId = folder.groupID
                        val remotePath = folder.path
                        val inputFileLocal = TdApi.InputFileLocal(file.path)
                        val formattedText = TdApi.FormattedText(remotePath+"/"+file.name, arrayOf())
                        val doc = TdApi.InputMessageDocument(inputFileLocal,TdApi.InputThumbnail(), formattedText)

                        sendUploadedFileUseCase(groupId,doc)
                        fileOperationComplete().postValue(Pair(it.local.path,false))

                        if(lock.isLocked) lock.unlock()

                    }
                    transferFileUploadUseCase(file).let {
                        currentFileId = it.id
                        if (it.remote.isUploadingCompleted)
                            lambda(it)
                        else
                            lock.lock()
                    }

                }else{
                    for (i in getLocalFilesNoLDUseCase(file.path)){
                        transfer(i,folder.copy(path = folder.path+"/"+file.name))
                    }
                }
            }
            !file.is_local() && !folder.is_local() -> {

                if (file.is_file()){
                    val groupId = folder.groupID
                    val remotePath = folder.path
                    val inputFileId = TdApi.InputFileId(file.fileID)
                    val formattedText = TdApi.FormattedText(remotePath+"/"+file.name, arrayOf())
                    val thumbnail = TdApi.InputThumbnail(inputFileId,320,230)
                    val doc = TdApi.InputMessageDocument(inputFileId,thumbnail, formattedText)

                    sendUploadedFileUseCase(groupId,doc)
                    fileOperationComplete().postValue(Pair(formattedText.text,false))

                }else{

                    for (i in getRemoteFilesNoLDUseCase(file.groupID,file.path)){
                        transfer(i,folder.copy(path = folder.path+"/"+file.name))
                    }
                }

            }
            !file.is_local() && folder.is_local() -> {
                if (file.is_file()){

                    lambda = {

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

                        localFileOnTd.copyTo(fileDestination)

                        fileOperationComplete().postValue(Pair(it.local.path,false))

                        if(lock.isLocked) lock.unlock()

                    }
                    transferFileDownloadUseCase(file).let { currentFileId = it.id; if (it.local.isDownloadingCompleted) lambda(it) else lock.lock()}

                }else{
                    for (i in getRemoteFilesNoLDUseCase(file.groupID,file.path)){
                        transfer(i,folder.copy(path = folder.path+"/"+file.name))
                    }
                }
            }
        }
    }


    private fun startObservers(isDownload:Boolean) {
        TelegramRepository.downloadLD.observeForever {
            if (currentFileId == it.id){
                //mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                //if ((isDownload && it.local.isDownloadingCompleted) || (!isDownload && it.remote.isUploadingCompleted)){
                if ((isDownload && it.local.isDownloadingCompleted) || (!isDownload && it.local.downloadedSize == it.remote.uploadedSize && it.remote.isUploadingActive == false)){
                    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle(if (isDownload) "Успешно загружен" else "Успешно выгружен")
                        .setContentText("Файл")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setProgress(0,0,false)
                        .build()
                    mNotificationManager.notify(SERVICE_ID, notification)

                    currentFileId = -1
                    scope.launch { lambda(it)}

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

    private fun checkIntent(intent: Intent?){
        if (intent == null) throw java.lang.Exception()
        files = intent.getParcelableArrayExtra(EXTRA_FILES)?.map { it as TdObject }!!.toTypedArray()  // as Array<TdObject>?: throw java.lang.Exception()
        folderDestination = intent.getParcelableExtra<TdObject>(EXTRA_FOLDER_DESTINATION)
        isDownload = intent.getBooleanExtra(EXTRA_IS_DOWNLOAD,false)
        isCopy = intent.getBooleanExtra(EXTRA_COPY,false)
        needOpen = intent.getBooleanExtra(EXTRA_NEED_OPEN,false)
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