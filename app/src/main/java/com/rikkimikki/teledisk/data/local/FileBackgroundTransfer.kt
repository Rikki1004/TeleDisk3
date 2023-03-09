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
    private val scope = CoroutineScope(Dispatchers.IO)
    private val lock = Mutex()
    private var folderDestination : TdObject? = null
    private var isDownload : Boolean = true
    private var isCopy : Boolean = true
    private var needOpen : Boolean = false
    private var filesNeedSend = mutableListOf<TdObject>()
    private var currentFileId = NO_ID
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

        fun getIntent(context: Context, files:Array<TdObject>,folderDestination : TdObject,is_copy:Boolean) :Intent{
            val intent = Intent(context,
                FileBackgroundTransfer::class.java)
            intent.putExtra(EXTRA_FILES, files)
            intent.putExtra(EXTRA_COPY, is_copy)
            intent.putExtra(EXTRA_FOLDER_DESTINATION, folderDestination)
            intent.putExtra(EXTRA_IS_DOWNLOAD, files[0].placeType != PlaceType.Local && folderDestination.placeType == PlaceType.Local)
            return intent
        }
        fun getIntent(context: Context, file:TdObject) :Intent{//Open remote file
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
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.is_processing))
            .setSmallIcon(R.drawable.ic_launcher_main_foreground)
            .setContentIntent(getPendingIntent())
            .setOnlyAlertOnce(true)
            .setPriority(PRIORITY_MAX)
            .setDefaults(FOREGROUND_SERVICE_DEFAULT)

        startForeground(SERVICE_ID, notification.build())
        checkIntent(intent)
        startObservers(isDownload)

        scope.launch {
            lock.tryLock()
            for (i in files)
                transfer(i,folderDestination)

            if (filesNeedSend.isNotEmpty())
                tempPathsForSendUseCase().postValue(filesNeedSend)
            stopSelf()
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
                        filesNeedSend.add(TdObject(file.name, PlaceType.Local, FileType.File,it.local.path))
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
                val fileDestination = checkName(folder.path, file.name)

                setNotification(file.name)

                localFileOnTd.copyRecursively(fileDestination)
                fileOperationComplete().postValue(Pair(fileDestination.path,false))
            }
            file.is_local() && !folder.is_local() -> {
                if (file.is_file()){
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
                        val fileDestination = checkName(folder.path,file.name)

                        setNotification(file.name)
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

    private fun getPendingIntent(): PendingIntent{
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE)
            }
        return pendingIntent
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