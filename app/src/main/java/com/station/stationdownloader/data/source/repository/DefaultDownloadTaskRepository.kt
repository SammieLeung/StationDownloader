package com.station.stationdownloader.data.source.repository

import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskDataSource
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.engine.asXLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.getSelectedFileIndexes
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.data.succeeded
import com.station.stationdownloader.utils.TaskTools
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * author: Sam Leung
 * date:  2023/5/15
 */
class DefaultDownloadTaskRepository(
    private val localDataSource: IDownloadTaskDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : IDownloadTaskRepository {
    override suspend fun getTasks(): List<XLDownloadTaskEntity> {
        return when (val result = localDataSource.getTasks()) {
            is IResult.Success -> {
                result.data
            }

            is IResult.Error -> {
                //TODO 处理错误
                emptyList()
            }
        }

    }

    override suspend fun getTaskByUrl(url: String): XLDownloadTaskEntity? {
        return localDataSource.getTaskByUrl(url)
    }

    override suspend fun getTaskByUrl(
        url: String,
        engine: DownloadEngine,
        downloadPath: String
    ): IResult<XLDownloadTaskEntity> {
        return when (engine) {
            DownloadEngine.XL -> localDataSource.getTaskByUrl(url, downloadPath)
            DownloadEngine.ARIA2 -> IResult.Error(
                Exception(TaskExecuteError.NOT_SUPPORT_YET.name),
                TaskExecuteError.NOT_SUPPORT_YET.ordinal
            )
        }

    }

    override fun getTasksStream(): Flow<IResult<List<XLDownloadTaskEntity>>> {
        return localDataSource.getTasksStream()
    }

    override suspend fun insertTask(task: XLDownloadTaskEntity): IResult<Long> {
        return localDataSource.insertTask(task)
    }

    override suspend fun updateTask(task: XLDownloadTaskEntity): IResult<Int> {
        return localDataSource.updateTask(task)
    }


   override suspend fun saveTask(
       torrentId: Long,
       originUrl: String,
       realUrl: String,
       taskName: String,
       urlType: DownloadUrlType,
       engine: DownloadEngine,
       totalSize: Long,
       downloadPath: String,
       selectIndexes: List<Int>,
       fileList: List<String>,
       fileCount: Int
    ): IResult<XLDownloadTaskEntity> = withContext(ioDispatcher)
    {
        val existsTask = getTaskByUrl(originUrl)
        if (existsTask == null) {
            val insertResult = insertTask(
                XLDownloadTaskEntity(
                    id = 0,
                    torrentId = torrentId,
                    url = originUrl,
                    realUrl = realUrl,
                    name = taskName,
                    urlType = urlType,
                    engine = engine,
                    totalSize = totalSize,
                    downloadPath = downloadPath,
                    selectIndexes = selectIndexes,
                    fileList = fileList,
                    fileCount = fileCount
                )
            )
            if (insertResult is IResult.Error)
                return@withContext insertResult
            val newTaskResult = getTaskByUrl(originUrl)
                ?: return@withContext IResult.Error(
                    Exception(TaskExecuteError.TASK_INSERT_ERROR.name),
                    TaskExecuteError.TASK_INSERT_ERROR.ordinal
                )
            return@withContext IResult.Success(newTaskResult)
        }

        if (assertTaskConfigNotChange(existsTask, engine, downloadPath, taskName, selectIndexes)) {
            return@withContext IResult.Error(
                Exception(originUrl),
                TaskExecuteError.REPEATING_TASK_NOTHING_CHANGED.ordinal
            )
        }


        val updatedTask = existsTask.copy(
            downloadPath = File(downloadPath, taskName).path,
            engine = engine,
            selectIndexes = selectIndexes,
            name = taskName
        )
        val sqlResult = updateTask(updatedTask)
        if (!sqlResult.succeeded) {
            return@withContext IResult.Error(
                Exception(TaskExecuteError.UPDATE_TASK_CONFIG_FAILED.name),
                TaskExecuteError.UPDATE_TASK_CONFIG_FAILED.ordinal
            )
        }
        return@withContext IResult.Success(
            updatedTask
        )
    }


    override suspend fun saveTask(newTask: NewTaskConfigModel): IResult<XLDownloadTaskEntity> =
        withContext(ioDispatcher) {
            val rootDir = newTask._fileTree as TreeNode.Directory
            val fileSize = rootDir.totalCheckedFileSize
            val fileCount = rootDir.totalFileCount
            val newSelectIndexes =
                rootDir.getSelectedFileIndexes()

            if (newSelectIndexes.isEmpty())
                return@withContext IResult.Error(
                    Exception(TaskExecuteError.SELECT_AT_LEAST_ONE_FILE.name),
                    TaskExecuteError.SELECT_AT_LEAST_ONE_FILE.ordinal
                )


            return@withContext when (newTask) {
                is NewTaskConfigModel.NormalTask -> {
                    val (originUrl, realUrl, taskName, urlType, downloadPath, engine) = newTask
                    val realDownloadPath = File(downloadPath, taskName).path
                    saveTask(
                        torrentId = -1,
                        originUrl = originUrl,
                        realUrl = realUrl,
                        taskName = taskName,
                        urlType = urlType,
                        engine = engine,
                        totalSize = fileSize,
                        downloadPath = realDownloadPath,
                        selectIndexes = newSelectIndexes,
                        fileList = emptyList(),
                        fileCount = fileCount
                    )
                }

                is NewTaskConfigModel.TorrentTask -> {
                    val (torrentId, torrentPath, taskName, downloadPath, _,engine,_) = newTask
                    val realDownloadPath = File(downloadPath, taskName).path
                    saveTask(
                        torrentId = torrentId,
                        originUrl = torrentPath,
                        realUrl = torrentPath,
                        taskName = taskName,
                        urlType = DownloadUrlType.TORRENT,
                        engine = engine,
                        totalSize = fileSize,
                        downloadPath = realDownloadPath,
                        selectIndexes = newSelectIndexes,
                        fileList = emptyList(),
                        fileCount = fileCount
                    )
                }
            }
        }

    override suspend fun deleteTask(url: String, isDeleteFile: Boolean): IResult<Int> {
        val xlEntity = getTaskByUrl(url)
            ?: return IResult.Error(
                Exception(TaskExecuteError.DELETE_TASK_FAILED.name),
                TaskExecuteError.DELETE_TASK_FAILED.ordinal
            )
        withContext(Dispatchers.IO) {
            if (isDeleteFile) {
                val fileDirectory = File(xlEntity.downloadPath)
                TaskTools.deleteFolder(fileDirectory)
            }
        }

        return localDataSource.deleteTask(url)

    }


    private fun assertTaskConfigNotChange(
        existsTask: XLDownloadTaskEntity,
        newTask: NewTaskConfigModel
    ): Boolean {
        return existsTask.engine == newTask._downloadEngine &&
                existsTask.downloadPath == File(newTask._downloadPath, newTask._name).path &&
                existsTask.name == newTask._name &&
                existsTask.selectIndexes == (newTask._fileTree as TreeNode.Directory).getSelectedFileIndexes()
    }

    private fun assertTaskConfigNotChange(
        existsTask: XLDownloadTaskEntity,
        engine: DownloadEngine,
        downloadPath: String,
        taskName: String,
        selectedFileIndexes: List<Int>
    ): Boolean {
        return existsTask.engine == engine &&
                existsTask.downloadPath == File(downloadPath, taskName).path &&
                existsTask.name == taskName &&
                existsTask.selectIndexes == selectedFileIndexes
    }

}