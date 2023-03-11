package com.rikkimikki.teledisk.presentation.main

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.*
import com.rikkimikki.teledisk.BuildConfig
import com.rikkimikki.teledisk.R
import com.rikkimikki.teledisk.data.local.FileBackgroundTransfer
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository
import com.rikkimikki.teledisk.data.tdLib.TelegramRepository.downloadLD
import com.rikkimikki.teledisk.domain.*
import com.rikkimikki.teledisk.utils.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File


class ListFileViewModel(application: Application):AndroidViewModel(application) {
    var currentLocalPath = "/"
    var currentRemotePath = "/"

    val repository = TelegramRepository

    private val getRemoteFilesUseCase = GetRemoteFilesUseCase(repository)
    private val getLocalFilesUseCase = GetLocalFilesUseCase(repository)
    private val getAllChatsUseCase = GetAllChatsUseCase(repository)
    private val fileOperationComplete = FileOperationCompleteUseCase(repository)
    private val createFolderUseCase = CreateFolderUseCase(repository)
    private val renameFileUseCase = RenameFileUseCase(repository)
    private val renameFolderUseCase = RenameFolderUseCase(repository)
    private val deleteFileUseCase = DeleteFileUseCase(repository)
    private val deleteFolderUseCase = DeleteFolderUseCase(repository)
    private val tempPathsForSendUseCase = TempPathsForSendUseCase(repository)
    private val getAllFilteredLocalFilesUseCase = GetAllFilteredLocalFilesUseCase(repository)
    private val getAllFilteredRemoteFilesUseCase = GetAllFilteredRemoteFilesUseCase(repository)
    private val createGroupUseCase = CreateGroupUseCase(repository)
    val fileScope = repository.dataFromStore
    val chatScope = repository.allChats


    //private val context = getApplication<Application>().applicationContext
    //var context: Context? = getApplication<Application>().getApplicationContext()


    var needLaunchIntent = SingleLiveData<Intent>()
    var needPressBackButton = SingleLiveData<Unit>()
    var needCancelSelect = SingleLiveData<Unit>()
    var needHideSelect = SingleLiveData<Unit>()
    var is_copy_mode = false

    val selectedItems = mutableListOf<TdObject>()
    lateinit var currentDirectory : TdObject
    var currentGroup : Long = NO_GROUP
    set(value) {
        sharedpreferences.edit().putLong(PREF_GROUP_IG,value).apply()
        field = value
        Toast.makeText(getApplication(), "Группа выбрана", Toast.LENGTH_SHORT).show()
    }
    get() {
        if (field != NO_GROUP)
            return field
        if (sharedpreferences.contains(PREF_GROUP_IG))
            return sharedpreferences.getLong(PREF_GROUP_IG,NO_GROUP)
        else
            //Toast.makeText(getApplication(), "Группа не выбрана", Toast.LENGTH_SHORT).show()
            return NO_GROUP
    }

    private lateinit var sharedpreferences: SharedPreferences

    private val contentResolver by lazy {
        application.contentResolver
    }

    private val externalCacheDirs by lazy {
        application.externalCacheDirs
    }
    /*private val application by lazy {
        application
    }*/

    companion object{
        const val PREF_GROUP_IG = "group"
        const val APP_PREFERENCES = "pref"
        const val NO_GROUP = -1L
    }
init {
    sharedpreferences =
        application.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)

    //repository.reload()
    tempPathsForSendUseCase().observeForever {
        shareItems(it)
    }
}
    val isRemoteDownloadComplete = SingleLiveData<String>()

    fun refresh(){
        changeDirectory(currentDirectory)
    }
    fun refreshFileScope(){
        fileScope.value = listOf()
    }
    fun refreshSelectedItems(){
        selectedItems.clear()
    }

    fun copyFile(){
        if (selectedItems.size > 1000){
            Toast.makeText(getApplication(), getApplication<Application>().getString(R.string.limit_1000), Toast.LENGTH_SHORT).show()
            return
        }
        val startIntent = FileBackgroundTransfer.getIntent(
            getApplication(),
            selectedItems.toTypedArray(),
            currentDirectory,
            true
            )
        transferFiles(startIntent)
        refreshSelectedItems()
    }
    fun moveFile() {
        if (selectedItems.size > 1000){
            Toast.makeText(getApplication(), getApplication<Application>().getString(R.string.limit_1000), Toast.LENGTH_SHORT).show()
            return
        }
        val startIntent = FileBackgroundTransfer.getIntent(
            getApplication(),
            selectedItems.toTypedArray(),
            currentDirectory,
            false
        )
        transferFiles(startIntent)
        refreshSelectedItems()
    }

    fun setLocalPath(path: String) {
        TelegramRepository.currentLocalFolderPath = path
    }

    fun getRemoteFiles(path:String){
        currentDirectory = TdObject("currentDir",PlaceType.TeleDisk,FileType.Folder,path, groupID = currentGroup)
        viewModelScope.launch { getRemoteFilesUseCase(currentGroup,path) }
    }

    fun getLocalFiles(path:String){

        currentDirectory = TdObject("currentDir",PlaceType.Local,FileType.Folder,path)
        viewModelScope.launch { getLocalFilesUseCase(path) }
    }
    fun clickArrow(startPath: String){
        if (currentDirectory.is_local() && currentDirectory.path != startPath && currentDirectory.name.isNotBlank())
            getLocalFiles(currentDirectory.path.substringBeforeLast("/"))
        else if (!currentDirectory.is_local() && currentDirectory.path != startPath && currentDirectory.name.isNotBlank()){
            var tempPath = currentDirectory.path.substringBeforeLast("/")
            if (tempPath.isBlank()) tempPath = "/"
            getRemoteFiles(tempPath)
        }

        else
            needPressBackButton.value = Unit
    }

    fun getLocalFilesFiltered(filter:FiltersFromType,path:String){
        currentDirectory = TdObject("",PlaceType.Local,FileType.Folder,path)
        viewModelScope.launch { getAllFilteredLocalFilesUseCase(filter) }
    }
    fun getRemoteFilesFiltered(filter:FiltersFromType,path:String){
        currentDirectory = TdObject("",PlaceType.TeleDisk,FileType.Folder,path, groupID = currentGroup)
        viewModelScope.launch { getAllFilteredRemoteFilesUseCase(currentGroup,filter) }
    }


    fun createFolder(name:String){
        viewModelScope.launch { createFolderUseCase(currentDirectory, name); refresh() }
    }

    fun renameItem(newName: String) {
        val file = selectedItems[0]
        viewModelScope.launch{
            if (file.is_file())
                renameFileUseCase(file,newName)
            else
                renameFolderUseCase(file,newName)
            refreshSelectedItems()
            needCancelSelect.value = Unit
            refresh()
        }

    }
    fun getRenamedItemName() : Pair<String,Boolean>{
        val item = selectedItems[0]
        return item.name to item.is_file()
    }

    fun deleteItem() {
        viewModelScope.launch{
            for (i in selectedItems.toList()){
                if (i.is_file())
                    deleteFileUseCase(i)
                else
                    deleteFolderUseCase(i)
            }
            refreshSelectedItems()
            needCancelSelect.value = Unit
            refresh()
        }
    }

    fun getChats() : LiveData<List<Pair<Long,String>>> {
        viewModelScope.launch { getAllChatsUseCase() }
        return repository.allChats.map { it.map { it.id to it.title.substring(1,it.title.length-1) } }
    }

    fun changeDirectory(directory:TdObject) {
        if (directory.is_folder() && directory.placeType == PlaceType.TeleDisk)
            getRemoteFiles(directory.path)
        if (directory.is_folder() && directory.placeType == PlaceType.Local)
            getLocalFiles(directory.path)
    }


    fun getStorages():List<PlaceItem>{

        val placeItems = mutableListOf<PlaceItem>()


        for(i in externalCacheDirs){
            val stat = StatFs(i.path)
            if (i.absolutePath.startsWith(GLOBAL_MAIN_STORAGE_PATH)){
                placeItems.add(PlaceItem(
                    "Память устройства",
                    i.path.substringBefore(GLOBAL_CACHE_DIRS_PATH_OFFSET),
                    stat.totalBytes,
                    stat.availableBytes,
                    ScopeType.Local,
                    true
                ))
            }else{
                placeItems.add(PlaceItem(
                    i.path.substringBefore(GLOBAL_CACHE_DIRS_PATH_OFFSET).let { it.substring(0,it.length-1) },
                    i.path.substringBefore(GLOBAL_CACHE_DIRS_PATH_OFFSET),
                    stat.totalBytes,
                    stat.availableBytes,
                    ScopeType.Sd
                ))
            }
        }

        placeItems.add(1,PlaceItem(
            "Teledisk",
            "/",
            0,
            Long.MAX_VALUE,
            ScopeType.TeleDisk
        ))

        placeItems.add(PlaceItem(
            "VkDisk (В разработке)",
            "/",
            0,
            Long.MAX_VALUE,
            ScopeType.VkMsg
        ))
        return placeItems
    }
    fun getNeedOpenLD(): LiveData<Pair<String, Boolean>> {
        return fileOperationComplete()
    }


    fun openLocalFile(path:String){
        val uri = FileProvider.getUriForFile(getApplication(),
            BuildConfig.APPLICATION_ID + ".provider", File(path)
        )
        val intent = Intent(Intent.ACTION_VIEW)
        val type = contentResolver.getType(uri)
        intent.setDataAndType(uri,type)
        intent.flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        //return intent
        needLaunchIntent.value = intent
    }

    fun shareItems(listSelected: List<TdObject> = selectedItems){
        if (listSelected.isEmpty())
            return
        if (listSelected.size > 1000){
            Toast.makeText(getApplication(), getApplication<Application>().getString(R.string.limit_1000), Toast.LENGTH_SHORT).show()
            return
        }
        if (!listSelected[0].is_local()){
            val startIntent = FileBackgroundTransfer.getIntent(
                getApplication(),
                listSelected.toTypedArray()
            )
            transferFiles(startIntent)
            return
        }

        val urisList = arrayListOf<Uri>()

        for (i in listSelected){
            if (i.is_file()){
                urisList.add(FileProvider.getUriForFile(getApplication(),
                    BuildConfig.APPLICATION_ID + ".provider", File(i.path)
                ))
            } else{
                File(i.path).walk().filter{ it.isFile }.forEach {
                    urisList.add(FileProvider.getUriForFile(getApplication(),
                        BuildConfig.APPLICATION_ID + ".provider", it
                    ))
                }
            }
        }

        if (urisList.size == 1){
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "*/*"//contentResolver.getType(urisList[0])
            intent.putExtra(Intent.EXTRA_STREAM, urisList[0])
            needLaunchIntent.value = intent
        }
        if (urisList.size > 1){
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "*/*"
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisList);
            needLaunchIntent.value = intent
        }
        refreshSelectedItems()
        needCancelSelect.value = Unit
        refresh()
    }


    fun getInfo():FileInfo {
        if (selectedItems.size == 1){
            val item = selectedItems[0]
            return FileInfo(
                item.name,
                covertTimestampToTime(item.unixTimeDate),
                humanReadableByteCountSI(item.size),
                item.path,
                true
            )
        } else {
            val filesCount =selectedItems.filter { it.is_file() }.size
            val foldersCount=selectedItems.filter { !it.is_file() }.size
            val totalSize= selectedItems.sumOf { it.size }
            return FileInfo(
                size =  humanReadableByteCountSI(totalSize),
                contains = "%s folders, %s files".format(foldersCount, filesCount),
                single = false
            )
        }
    }

    private fun transferFiles(intent: Intent) {
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun prepareToCopy() {
        is_copy_mode = true
        needHideSelect.value = Unit
    }
    fun cancelCopy() {
        is_copy_mode = false
        needCancelSelect.value = Unit
    }

    fun createGroup(text: String) {
        val groupName = "|$text|"
        viewModelScope.launch { createGroupUseCase(groupName) }
    }
}