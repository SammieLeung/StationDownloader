package com.station.stationdownloader.test

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.ITorrentInfoDataSource
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.TorrentInfoLocalDataSource
import com.station.stationdownloader.data.source.local.engine.xl.XLEngine
import com.station.stationdownloader.data.source.local.room.AppDatabase
import com.station.stationdownloader.data.source.local.room.dao.TorrentFileInfoDao
import com.station.stationdownloader.data.source.local.room.dao.TorrentInfoDao
import com.station.stationdownloader.data.source.repository.DefaultTorrentInfoRepository
import com.station.stationdownloader.di.LocalConfigurationDataSource
import com.xunlei.downloadlib.XLTaskHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DataBaseTest {
    private lateinit var torrentDataSource: ITorrentInfoDataSource
    private lateinit var torrentDataRepo: ITorrentInfoRepository
    private lateinit var torrentFileDao: TorrentFileInfoDao
    private lateinit var torrentDao: TorrentInfoDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        Logger.addLogAdapter(AndroidLogAdapter())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        torrentDao = db.getTorrentInfoDao()
        torrentFileDao=db.getTorrentFileInfoDao()
        torrentDataSource = TorrentInfoLocalDataSource(torrentDao, torrentFileDao)
        torrentDataRepo = DefaultTorrentInfoRepository(torrentDataSource, CoroutineScope(Job()))
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testTorrent() {
        runBlocking {
            val torrentUrl = "sdcard/Station/tvset2.torrent"
            var torrentInfo =
                XLTaskHelper.instance().getTorrentInfo(torrentUrl)

            val taskName = torrentUrl.substringAfterLast(File.separatorChar)
            val downloadPath =
                File("sdcard/Station/Download", taskName.substringAfterLast(".")).path
            val fileCount = torrentInfo.mFileCount


            Logger.d(torrentInfo.mInfoHash)

            val result = torrentDataRepo.getTorrentByHash("123",)
            if (result is IResult.Success) {
                Logger.e("${result.data}")
            }

            val res2=torrentDataRepo.getTorrentByHash(torrentInfo.mInfoHash,)
            if (res2 is IResult.Success) {
                res2.data.forEach { t, u ->
                    Logger.d(t)
                    u.forEach {
                        Logger.e(it.toString())
                    }
                }
            }

        }

    }
}