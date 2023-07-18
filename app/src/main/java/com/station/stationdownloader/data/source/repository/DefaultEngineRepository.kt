package com.station.stationdownloader.data.source.repository

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.contants.SqlError
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.contants.UNKNOWN_ERROR
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.engine.asXLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.getSelectedFileIndexes
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.data.succeeded
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultEngineRepository(
    private val xlEngine: IEngine,
    private val aria2Engine: IEngine,
    private val downloadTaskRepo: IDownloadTaskRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : IEngineRepository {

    override suspend fun init(): IResult<Unit> {
        return try {
            xlEngine.init()
            aria2Engine.init()
            IResult.Success(Unit)
        } catch (e: Exception) {
            IResult.Error(exception = e)
        }

    }

    override suspend fun unInit(): IResult<Unit> {
        return try {
            xlEngine.unInit()
            IResult.Success(Unit)
        } catch (e: Exception) {
            IResult.Error(exception = e)
        }
    }

    override suspend fun initUrl(url: String): IResult<NewTaskConfigModel> {
        return xlEngine.initUrl(url)
    }

    override suspend fun startTask(newTask: NewTaskConfigModel): IResult<StationDownloadTask> =
        withContext(Dispatchers.IO) {
            val originUrl = when (newTask) {
                is NewTaskConfigModel.NormalTask -> newTask.originUrl
                is NewTaskConfigModel.TorrentTask -> newTask.torrentPath
            }
            val newSelectIndexes =
                (newTask._fileTree as TreeNode.Directory).getSelectedFileIndexes()
            var stationDownloadTask: StationDownloadTask

            val existsTask = downloadTaskRepo.getTaskByUrl(originUrl)
            if (existsTask == null) {
                downloadTaskRepo.insertTask(newTask.asXLDownloadTaskEntity())
                val entity = downloadTaskRepo.getTaskByUrl(originUrl)
                    ?: return@withContext IResult.Error(Exception(""))
                stationDownloadTask = entity.asStationDownloadTask()

                val engineResult = selectEngineStartTask(
                    stationDownloadTask.realUrl,
                    downloadPath = stationDownloadTask.downloadPath,
                    name = stationDownloadTask.name,
                    urlType = stationDownloadTask.urlType,
                    fileCount = stationDownloadTask.fileCount,
                    selectIndexes = stationDownloadTask.selectIndexes.toIntArray(),
                    downloadEngine = stationDownloadTask.engine
                )


                return@withContext when (engineResult) {
                    is IResult.Error -> return@withContext engineResult
                    is IResult.Success -> IResult.Success(stationDownloadTask.copy(taskId = engineResult.data))
                }
            }


            if (assertTaskConfigNotChange(existsTask, newTask)) {
                return@withContext IResult.Error(
                    Exception(TaskExecuteError.REPEATING_TASK_NOTHING_CHANGE.name),
                    TaskExecuteError.REPEATING_TASK_NOTHING_CHANGE.ordinal
                )
            }

            val updatedTask = existsTask.copy(
                downloadPath = newTask._downloadPath,
                engine = newTask._downloadEngine,
                selectIndexes = newSelectIndexes,
                name = newTask._name
            )
            val sqlResult = downloadTaskRepo.updateTask(updatedTask)
            if (!sqlResult.succeeded) {
                return@withContext IResult.Error(
                    Exception(SqlError.UPDATE_TASK_CONFIG_FAILED.name),
                    SqlError.UPDATE_TASK_CONFIG_FAILED.ordinal
                )
            }

            return@withContext IResult.Success(
                updatedTask.asStationDownloadTask()
            )

        }

    private fun assertTaskConfigNotChange(
        existsTask: XLDownloadTaskEntity,
        newTask: NewTaskConfigModel
    ): Boolean {
        return existsTask.engine == newTask._downloadEngine &&
                existsTask.downloadPath == newTask._downloadPath &&
                existsTask.name == newTask._name &&
                existsTask.selectIndexes == (newTask._fileTree as TreeNode.Directory).getSelectedFileIndexes()
    }

    private suspend fun selectEngineStartTask(
        url: String,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount: Int,
        selectIndexes: IntArray,
        downloadEngine: DownloadEngine
    ): IResult<Long> {

        return when (downloadEngine) {
            DownloadEngine.XL -> {
                val startTaskResult = xlEngine.startTask(
                    url, downloadPath, name, urlType, fileCount, selectIndexes
                )
                startTaskResult
            }

            DownloadEngine.ARIA2 -> {
                val startTaskResult = aria2Engine.startTask(
                    url, downloadPath, name, urlType, fileCount, selectIndexes
                )
                startTaskResult
            }
        }
    }


    override suspend fun configure(key: String, values: Array<String>): IResult<Unit> {
        val xlConfigResult = xlEngine.configure(key, values)
        val aria2ConfigResult = aria2Engine.configure(key, values)

        if (xlConfigResult is IResult.Error) return xlConfigResult.copy(exception = Exception("[xl] ${xlConfigResult.exception.message}"))

        if (aria2ConfigResult is IResult.Error) return aria2ConfigResult.copy(
            exception = Exception(
                "[aria2] ${aria2ConfigResult.exception.message}"
            )
        )

        return IResult.Success(Unit)
    }


}