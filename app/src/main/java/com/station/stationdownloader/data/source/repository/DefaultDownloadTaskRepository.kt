package com.station.stationdownloader.data.source.repository

import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
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

    override suspend fun saveTask(newTask: NewTaskConfigModel): IResult<XLDownloadTaskEntity> =
        withContext(ioDispatcher) {
            val originUrl = when (newTask) {
                is NewTaskConfigModel.NormalTask -> newTask.originUrl
                is NewTaskConfigModel.TorrentTask -> newTask.torrentPath
            }

            val rootDir = newTask._fileTree as TreeNode.Directory
            val newSelectIndexes =
                rootDir.getSelectedFileIndexes()

            if (newSelectIndexes.isEmpty())
                return@withContext IResult.Error(
                    Exception(TaskExecuteError.SELECT_AT_LEAST_ONE_FILE.name),
                    TaskExecuteError.SELECT_AT_LEAST_ONE_FILE.ordinal
                )

            val existsTask = getTaskByUrl(originUrl)
            if (existsTask == null) {
                val insertResult = insertTask(newTask.asXLDownloadTaskEntity())
                if (insertResult is IResult.Error)
                    return@withContext insertResult
                val newTaskResult = getTaskByUrl(originUrl)
                    ?: return@withContext IResult.Error(
                        Exception(TaskExecuteError.TASK_INSERT_ERROR.name),
                        TaskExecuteError.TASK_INSERT_ERROR.ordinal
                    )
                return@withContext IResult.Success(newTaskResult)
            }

            if (assertTaskConfigNotChange(existsTask, newTask)) {
                return@withContext IResult.Error(
                    Exception(TaskExecuteError.REPEATING_TASK_NOTHING_CHANGED.name),
                    TaskExecuteError.REPEATING_TASK_NOTHING_CHANGED.ordinal
                )
            }

            val updatedTask = existsTask.copy(
                downloadPath = File(newTask._downloadPath,newTask._name).path,
                engine = newTask._downloadEngine,
                selectIndexes = newSelectIndexes,
                name = newTask._name
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

    override suspend fun deleteTask(url: String): IResult<Int> {
        return localDataSource.deleteTask(url)
    }


    private fun assertTaskConfigNotChange(
        existsTask: XLDownloadTaskEntity,
        newTask: NewTaskConfigModel
    ): Boolean {
        return existsTask.engine == newTask._downloadEngine &&
                existsTask.downloadPath == File(newTask._downloadPath,newTask._name).path &&
                existsTask.name == newTask._name &&
                existsTask.selectIndexes == (newTask._fileTree as TreeNode.Directory).getSelectedFileIndexes()
    }
}