package com.rikkimikki.teledisk.data.local

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_DEFAULT
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.domain.baseClasses.FileType
import com.rikkimikki.teledisk.domain.baseClasses.PlaceType
import com.rikkimikki.teledisk.domain.baseClasses.TdObject
import com.rikkimikki.teledisk.domain.useCases.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File


class FileBackgroundTransfer: Service() {
    val repository = TelegramRepository
    private val transferFileDownloadUseCase = TransferFileDownloadUseCase(repository)
    private val transferFileUploadUseCase = TransferFileUploadUseCase(repository)
    private val fileOperationComplete = FileOperationCompleteUseCase(repository)
    private val sendUploadedFileUseCase = SendUploadedFileUseCase(repository)
    private val getRemoteFilesNoLDUseCase = GetRemoteFilesNoLDUseCase(repository)
    private val getLocalFilesNoLDUseCase = GetLocalFilesNoLDUseCase(repository)
    private val tempPathsForSendUseCase = TempPathsForSendUseCase(repository)
    private val deleteFileUseCase = DeleteFileUseCase(repository)
    private val deleteFolderUseCase = DeleteFolderUseCase(repository)
    private val cancelFileTransferUseCase = CancelFileTransferUseCase(repository)
    private val coroutineExceptionHandler = CoroutineExceptionHandler{ _, throwable ->
        throwable.printStackTrace()
    }
    private val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
    private val lock = Mutex(true)
    private var folderDestination : TdObject? = null
    private var isDownload : Boolean = true
    private var isCopy : Boolean = true
    private var needOpen : Boolean = false
    private var filesNeedSend = mutableListOf<TdObject>()
    private var currentFileId = NO_ID
    private var serviceExist = false
    private lateinit var files : Array<TdObject>
    private lateinit var lambda : suspend (TdApi.File) -> Unit
    private lateinit var notification:NotificationCompat.Builder
    private lateinit var mNotificationManager :NotificationManager

    companion object {
        private const val CHANNEL_ID = "Foreground LibTd Operations"
        private const val CHANNEL_NAME = "Foreground File Transfer"
        private const val SERVICE_ID = 1
        private const val NO_ID = -1

        private const val EXTRA_FILES = "FILES"
        private const val EXTRA_COPY = "COPY"
        private const val EXTRA_FOLDER_DESTINATION = "FOLDER"
        private const val EXTRA_IS_DOWNLOAD = "IS_DOWNLOAD"
        private const val EXTRA_NEED_OPEN = "NEED_OPEN"

        const val EXTRA_STOP_ACTION = "STOP_ACTION"

        fun getIntent(context: Context, files:Array<TdObject>, folderDestination : TdObject, is_copy:Boolean) :Intent{
            val intent = Intent(context,
                FileBackgroundTransfer::class.java)
            intent.putExtra(EXTRA_FILES, files)
            intent.putExtra(EXTRA_COPY, is_copy)
            intent.putExtra(EXTRA_FOLDER_DESTINATION, folderDestination)
            intent.putExtra(EXTRA_IS_DOWNLOAD, files[0].placeType != PlaceType.Local && folderDestination.placeType == PlaceType.Local)
            return intent
        }
        fun getIntent(context: Context, file: TdObject) :Intent{//Open remote file
            val intent = Intent(context,
                FileBackgroundTransfer::class.java)
            intent.putExtra(EXTRA_FILES, arrayOf(file))
            intent.putExtra(EXTRA_NEED_OPEN,true)
            intent.putExtra(EXTRA_IS_DOWNLOAD, true)
            intent.putExtra(EXTRA_COPY, true)
            return intent
        }
        fun getIntent(context: Context, files: Array<TdObject>) :Intent{//Share remote files
            val intent = Intent(context,
                FileBackgroundTransfer::class.java)
            intent.putExtra(EXTRA_FILES, files)
            intent.putExtra(EXTRA_IS_DOWNLOAD, true)
            intent.putExtra(EXTRA_COPY, true)
            return intent
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action != null && intent.action.equals(EXTRA_STOP_ACTION)) {
            scope.launch {
                cancelFileTransferUseCase(currentFileId,isDownload)
                currentFileId = NO_ID
                notification.setContentTitle(getString(R.string.is_canceled))
                notification.setProgress(0,0,true)
                notification.setAutoCancel(true)
                mNotificationManager.notify(SERVICE_ID, notification.build())
                stopForeground(false)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        if (serviceExist){
            Toast.makeText(this, getString(R.string.need_wait_previous_download), Toast.LENGTH_SHORT).show()
            return START_NOT_STICKY
        }
        serviceExist = true

        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.is_processing))
            .setSmallIcon(R.drawable.ic_launcher_main_foreground)
            .addAction(R.drawable.ic_cancel_black_18dp, getString(R.string.cancel_button_text), getPendingIntent())
            .setOnlyAlertOnce(true)
            .setPriority(PRIORITY_MAX)
            .setDefaults(FOREGROUND_SERVICE_DEFAULT)
        startForeground(SERVICE_ID, notification.build())
        checkIntent(intent)
        startObservers(isDownload)
        scope.launch {
            //lock.tryLock()
            for (i in files){
                if (i.size>0 || !i.is_file()){
                    notification.setContentText(i.name)
                    transfer(i,folderDestination)
                }
            }
            if(!needOpen) fileOperationComplete().postValue(Pair(getString(R.string.all_transfers_are_completed),false))

            if (filesNeedSend.isNotEmpty())
                tempPathsForSendUseCase().postValue(filesNeedSend)
            stopSelf()
        }
        return START_NOT_STICKY
    }


    private suspend fun transfer(file: TdObject, folder: TdObject?) {
        if (folder == null ) when{
            //file.is_local() -> {fileOperationComplete().postValue(Pair(file.path,true)); stopSelf()} //newer
            !file.is_local() && needOpen -> {
                lambda = {fileOperationComplete().postValue(Pair(it.local.path,true)); stopSelf()}
                transferFileDownloadUseCase(file).let {currentFileId = it.id; if (it.local.isDownloadingCompleted) lambda(it) }
            }
            !file.is_local() && !needOpen -> {
                if (file.is_file()){
                    lambda = {
                        filesNeedSend.add(TdObject(file.name, PlaceType.Local, FileType.File,it.local.path))
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
                val fileDestination = checkName(folder.path, file.name)

                setNotification(file.name)

                localFileOnTd.copyRecursively(fileDestination)
                if (!isCopy) localFileOnTd.deleteRecursively()
            }
            file.is_local() && !folder.is_local() -> {
                if (file.is_file()){
                    lambda = {
                        val groupId = folder.groupID
                        val remotePath = folder.path
                        val inputFileLocal = TdApi.InputFileLocal(file.path)
                        val formattedText = TdApi.FormattedText(remotePath+(if (remotePath == "/") "" else "/")+file.name, arrayOf())
                        val doc = TdApi.InputMessageDocument(inputFileLocal,TdApi.InputThumbnail(), formattedText)

                        sendUploadedFileUseCase(groupId,doc)
                        if (!isCopy) File(file.path).deleteRecursively()
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
                    if (!isCopy) deleteFileUseCase(file)
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
                        val fileDestination = checkName(folder.path,file.name)

                        setNotification(file.name)
                        if(localFileOnTd.name != "FOLDER")
                            localFileOnTd.copyTo(fileDestination)
                        localFileOnTd.delete()
                        if (!isCopy) deleteFileUseCase(file)
                        if(lock.isLocked) lock.unlock()
                    }
                    transferFileDownloadUseCase(file).let { currentFileId = it.id; if (it.local.isDownloadingCompleted) lambda(it) else lock.lock()}
                }else{
                    for (i in getRemoteFilesNoLDUseCase(file.groupID,file.path)){
                        transfer(i,folder.copy(path = folder.path+"/"+file.name))
                    }
                    if (!isCopy) deleteFolderUseCase(file)
                }
            }
        }
    }

    private fun setNotification(name:String,hasMoving:Boolean = false){
        notification.setContentTitle(if (hasMoving) getString(R.string.is_moving) else getString(R.string.is_processing))
        notification.setProgress(0,0,true)
        notification.setContentText(name)
        mNotificationManager.notify(SERVICE_ID, notification.build())
    }

    private fun checkName(path: String,name:String):File{
        var fileDestination = File("$path/$name")
        var counter = 1
        while (fileDestination.exists()){
            fileDestination = File(path+"/("+ counter++ +")"+name)
        }
        return fileDestination
    }

    private fun startObservers(isDownload:Boolean) {
        TelegramRepository.downloadLD.observeForever {
            if (currentFileId == it.id){
                if ((isDownload && it.local.isDownloadingCompleted) ||
                    (!isDownload && it.local.downloadedSize == it.remote.uploadedSize && !it.remote.isUploadingActive)){

                    notification.setContentTitle(if (isDownload) getString(R.string.upload_success) else getString(
                        R.string.download_success))
                    notification.setProgress(0,0,false)
                    mNotificationManager.notify(SERVICE_ID, notification.build())

                    currentFileId = NO_ID
                    scope.launch { lambda(it)}
                }
                else{
                    notification.setContentTitle(if (isDownload) getString(R.string.is_downloading) else getString(
                                            R.string.is_uploading))
                    notification.setProgress(it.size,if (isDownload) it.local.downloadedSize else it.remote.uploadedSize,false)
                    mNotificationManager.notify(SERVICE_ID, notification.build())
                }
            }
        }
    }

    private fun checkIntent(intent: Intent?){
        if (intent == null) throw java.lang.Exception()
        files = intent.getParcelableArrayExtra(EXTRA_FILES)?.map { it as TdObject }!!.toTypedArray()  // as Array<TdObject>?: throw java.lang.Exception()
        folderDestination = intent.getParcelableExtra(EXTRA_FOLDER_DESTINATION)
        isDownload = intent.getBooleanExtra(EXTRA_IS_DOWNLOAD,false)
        isCopy = intent.getBooleanExtra(EXTRA_COPY,false)
        needOpen = intent.getBooleanExtra(EXTRA_NEED_OPEN,false)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun getPendingIntent(): PendingIntent {
        val yesReceive = Intent(this, CancelReceiver::class.java)
        yesReceive.action = EXTRA_STOP_ACTION
        return PendingIntent.getBroadcast(
            this,
            0,
            yesReceive,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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