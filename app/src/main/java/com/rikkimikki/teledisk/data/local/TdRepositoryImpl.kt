package com.rikkimikki.teledisk.data.local

import androidx.lifecycle.MutableLiveData
import com.rikkimikki.teledisk.domain.*
import java.io.*

object TdRepositoryImpl{


    val dataFromLocal = MutableLiveData<List<TdObject>>()


    /*fun getDataFromLocalOld(path:String){
        val tempList = mutableListOf<TdObject>()
        File(path).listFiles()?.forEach {
            if (it.isFile)
                tempList.add(Tfile(it.name,FileType.LocalFile,it.totalSpace,it.absolutePath,it.lastModified()))
            else
                tempList.add(Tfolder(it.name,PlaceType.LocalFolder,it.absolutePath,it.lastModified(), it.totalSpace))
        }
        dataFromLocal.value = tempList
    }*/

    @Throws(IOException::class)
    fun getResourceFiles(path: String): List<String> = getResourceAsStream(path).use{
        return if(it == null) emptyList()
        else BufferedReader(InputStreamReader(it)).readLines()
    }

    private fun getResourceAsStream(resource: String): InputStream? =
        Thread.currentThread().contextClassLoader.getResourceAsStream(resource)
            ?: resource::class.java.getResourceAsStream(resource)
}