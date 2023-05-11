package com.station.stationdownloader.test

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.station.stationdownloader.data.datasource.local.room.AppDatabase
import com.station.stationdownloader.data.datasource.local.room.dao.DownloadTaskDao
import com.station.stationdownloader.data.datasource.local.room.entities.DownloadTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DownloadTaskEntityReadWriteTest {
    private lateinit var downloadTaskDao: DownloadTaskDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).build()
        downloadTaskDao = db.downloadTaskDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeUserAndReadInList() {
        runBlocking {
            val scope= CoroutineScope(Job())
            scope.launch {
                downloadTaskDao.observeTasksStream().collect{
                   println("observe:")
                    it.forEach {
                        println("$it")
                    }
                }
            }
            withContext(Dispatchers.IO){
                val downloadTaskEntity= DownloadTaskEntity(0,"http://test","myName", selectIndexes = listOf(1,2,3), fileList = listOf("fiel.anme","dfet/mna"))
                val id=downloadTaskDao.insertTask(downloadTaskEntity)
                println("id:$id")
                delay(20)
                val id2=downloadTaskDao.insertTask(downloadTaskEntity.copy(url="metalink:xxxxx",name="testName2"))
                println("id2=$id2")
                delay(20)
                val res=downloadTaskDao.deleteTask(downloadTaskEntity.copy(id=id))
                println("res:$res")

            }
        }

        Thread.sleep(2000)

    }
}