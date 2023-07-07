package com.station.stationdownloader.data.source.local.engine.xl

import android.content.Context
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.ITaskState
import com.station.stationdownloader.contants.ConfigureError
import com.station.stationdownloader.contants.DOWNLOAD_SPEED_LIMIT
import com.station.stationdownloader.contants.GET_MAGNET_TASK_INFO_DELAY
import com.station.stationdownloader.contants.MAGNET_TASK_TIMEOUT
import com.station.stationdownloader.contants.SPEED_LIMIT
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.contants.UPLOAD_SPEED_LIMIT
import com.station.stationdownloader.contants.tryDownloadDirectoryPath
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IConfigurationDataSource
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.StationTaskInfo
import com.station.stationdownloader.data.source.local.model.asStationTaskInfo
import com.station.stationdownloader.data.source.local.room.entities.asStationTorrentInfo
import com.station.stationdownloader.data.source.remote.FileContentHeader
import com.station.stationdownloader.data.source.remote.FileSizeApiService
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.MAGNET_PROTOCOL
import com.station.stationdownloader.utils.TaskTools
import com.xunlei.downloadlib.XLDownloadManager
import com.xunlei.downloadlib.XLTaskHelper
import com.xunlei.downloadlib.parameter.TorrentInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.internal.concurrent.Task
import java.io.File

class XLEngine internal constructor(
    private val context: Context,
    private val configurationDataSource: IConfigurationDataSource,
    private val torrentInfoRepo: ITorrentInfoRepository,
    private val fileSizeApiService: FileSizeApiService,
    private val externalScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : IEngine, DLogger {
    private var hasInit = false

    override suspend fun init() {
        if (!hasInit) {
            synchronized(this@XLEngine) {
                if (!hasInit) {
                    XLTaskHelper.init(context)
                    hasInit = true
                }
            }
        }
    }

    override suspend fun unInit() {
        if (hasInit) {
            synchronized(this@XLEngine) {
                if (hasInit) {
                    XLTaskHelper.uninit()
                    hasInit = false
                }
            }
        }
    }

    override suspend fun initUrl(originUrl: String): IResult<StationDownloadTask> =
        withContext(defaultDispatcher) {
            logger("initUrl")
            var decodeUrl = TaskTools.getUrlDecodeUrl(originUrl)

            val isMagnetHash = TaskTools.isMagnetHash(decodeUrl)
            if (isMagnetHash) decodeUrl = MAGNET_PROTOCOL + decodeUrl

            val isSupportNetWork = TaskTools.isSupportNetworkUrl(decodeUrl)
            if (isSupportNetWork) {
                /*对thunder link进行解码，用于支持用thunder link包裹磁链的任务*/
                if (decodeUrl.urlType() == DownloadUrlType.THUNDER) {
                    val realUrl = TaskTools.thunderLinkDecode(decodeUrl)
                    if (TaskTools.isSupportNetworkUrl(realUrl))
                        return@withContext initNormalUrl(originUrl, realUrl)
                    else
                        return@withContext IResult.Error(
                            Exception("${TaskExecuteError.NOT_SUPPORT_URL.name}:[originUrl]->$originUrl [RealUrl]->$realUrl"),
                            TaskExecuteError.NOT_SUPPORT_URL.ordinal
                        )
                }
                return@withContext initNormalUrl(originUrl, decodeUrl)
            }

            val isTorrent = TaskTools.isTorrentFile(decodeUrl) && XLTaskHelper.instance()
                .getTorrentInfo(decodeUrl).mInfoHash != null
            return@withContext if (!isTorrent) {
                IResult.Error(
                    Exception("${TaskExecuteError.NOT_SUPPORT_URL.name}:[Error Url]->$decodeUrl"),
                    TaskExecuteError.NOT_SUPPORT_URL.ordinal
                )
            } else {
                initTorrentUrl(decodeUrl)
            }
        }

    suspend fun getTaskSize(stationDownloadTask: StationDownloadTask): Long =
        withContext(defaultDispatcher) {
            val url = stationDownloadTask.url
            val downloadPath = stationDownloadTask.downloadPath
            val name = stationDownloadTask.name
            val urlType = stationDownloadTask.urlType
            val fileCount = stationDownloadTask.fileCount
            val selectIndexes = stationDownloadTask.selectIndexes

            when (urlType) {
                DownloadUrlType.TORRENT -> {
                    val size = stationDownloadTask.torrentInfo?.let {
                        if (it.subFileInfo.isEmpty()) {
                            return@let 0
                        }
                        var calculateSize = 0L
                        for (fileInfo in it.subFileInfo) {
                            if (fileInfo.fileIndex in selectIndexes) {
                                calculateSize += fileInfo.fileSize
                            }
                        }
                        return@let calculateSize
                    }
                    Logger.d("$size")
                    return@withContext size ?: 0
                }

                DownloadUrlType.HTTP -> {

                    Logger.d(url)
                    val d = fileSizeApiService.getHttpFileHeader(url)
                    return@withContext d.content_length

                }

                DownloadUrlType.DIRECT -> {
                    return@withContext 0
                }

                else -> {
                    return@withContext 0
                }
            }

        }

//    override suspend fun getTaskSize(
//        task: StationDownloadTask,
//        timeOut: Long
//    ): IResult<StationDownloadTask> {
//        return IResult.Success(task.copy(totalSize = getTaskSize(stationDownloadTask = task)))
//    }

    suspend fun tryDownloadToGetSize(
        url: String,
        fileName: String,
        timeOut: Long
    ): IResult<Long> = withContext(defaultDispatcher) {

        val urlType = url.urlType()
        if (urlType !in arrayOf(
                DownloadUrlType.THUNDER,
                DownloadUrlType.HTTP,
                DownloadUrlType.ED2k,
                DownloadUrlType.DIRECT
            )
        ) {
            //TODO 返回错误信息
        }
        val savePathFile = File(tryDownloadDirectoryPath, fileName)
        if (!savePathFile.exists())
            savePathFile.mkdirs()

        val taskId = XLTaskHelper.instance().addThunderTask(url, savePathFile.path, null)
        if (taskId == 0L) {
            return@withContext IResult.Error(
                Exception("${TaskExecuteError.START_TASK_FAILED.name}:Error Url is [${url}]"),
                TaskExecuteError.START_TASK_FAILED.ordinal
            )
        }
        return@withContext try {
            withTimeout(timeOut) {
                val totalSize = calculateNormalTaskSize(taskId)
                IResult.Success(totalSize)
            }

        } catch (e: TimeoutCancellationException) {
            IResult.Error(
                Exception(TaskExecuteError.GET_FILE_SIZE_TIMEOUT.name),
                TaskExecuteError.GET_FILE_SIZE_TIMEOUT.ordinal
            )
        } finally {
            XLTaskHelper.instance().deleteTask(taskId, savePathFile.path)
        }
    }

    override suspend fun getTaskSize(
        stationDownloadTask: StationDownloadTask, timeOut: Long
    ): IResult<StationDownloadTask> = withContext(defaultDispatcher) {
        val url = stationDownloadTask.url
        val downloadPath = stationDownloadTask.downloadPath
        val name = stationDownloadTask.name
        val urlType = stationDownloadTask.urlType
        val fileCount = stationDownloadTask.fileCount
        val selectIndexes = stationDownloadTask.selectIndexes
        val tmpResult = startTask(
            url = url,
            downloadPath = downloadPath,
            name = name,
            urlType = urlType,
            fileCount = fileCount,
            selectIndexes = selectIndexes.toIntArray()
        )

        if (tmpResult is IResult.Error) {
            return@withContext tmpResult
        }

        val taskId = (tmpResult as IResult.Success).data
        return@withContext try {
            if (urlType == DownloadUrlType.TORRENT) {
                withTimeout(timeOut) {
                    val totalSize = calculateTorrentTaskSize(taskId, selectIndexes)
                    IResult.Success(stationDownloadTask.copy(totalSize = totalSize))
                }
            } else {
                withTimeout(timeOut) {
                    val totalSize = calculateNormalTaskSize(taskId)
                    IResult.Success(stationDownloadTask.copy(totalSize = totalSize))
                }
            }
        } catch (e: TimeoutCancellationException) {
            IResult.Error(
                Exception(TaskExecuteError.GET_FILE_SIZE_TIMEOUT.name),
                TaskExecuteError.GET_FILE_SIZE_TIMEOUT.ordinal
            )
        } finally {
            XLTaskHelper.instance().deleteTask(taskId, stationDownloadTask.downloadPath)
        }
    }


    override suspend fun startTask(
        url: String,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount: Int,
        selectIndexes: IntArray
    ): IResult<Long> = withContext(defaultDispatcher) {
        when (urlType) {
            DownloadUrlType.THUNDER, DownloadUrlType.HTTP, DownloadUrlType.ED2k, DownloadUrlType.DIRECT -> {
                val taskId = XLTaskHelper.instance().addThunderTask(url, downloadPath, name)
                if (taskId == 0L) {
                    return@withContext IResult.Error(
                        Exception("${TaskExecuteError.START_TASK_FAILED.name}:Error Url is [${url}]"),
                        TaskExecuteError.START_TASK_FAILED.ordinal
                    )
                }
                return@withContext IResult.Success(taskId)
            }

            DownloadUrlType.TORRENT -> {
                val taskId =
                    XLTaskHelper.instance().addTorrentTask(
                        url,
                        downloadPath,
                        TaskTools.deSelectedIndexes(fileCount, selectIndexes)
                    )
                if (taskId == 0L) {
                    return@withContext IResult.Error(
                        Exception("${TaskExecuteError.START_TASK_FAILED.name}:Error Url is [${url}]"),
                        TaskExecuteError.START_TASK_FAILED.ordinal
                    )
                }
                return@withContext IResult.Success(taskId)
            }

            DownloadUrlType.UNKNOWN, DownloadUrlType.MAGNET -> {
                return@withContext IResult.Error(
                    exception = Exception("${TaskExecuteError.START_TASK_URL_TYPE_ERROR.name}:[${urlType.name}]"),
                    code = TaskExecuteError.START_TASK_URL_TYPE_ERROR.ordinal
                )
            }
        }
    }

    override suspend fun stopTask(task: StationDownloadTask) {
        if (task.taskId != 0L) XLTaskHelper.instance().stopTask(task.taskId)
    }

    override suspend fun getTaskInfo(taskId: Long): StationTaskInfo {
        return XLTaskHelper.instance().getTaskInfo(taskId).asStationTaskInfo()
    }

    override suspend fun configure(key: String, values: Array<String>): IResult<Unit> {
        when (key) {
            UPLOAD_SPEED_LIMIT, DOWNLOAD_SPEED_LIMIT, SPEED_LIMIT -> {
                if (values.size == 2) {
                    val upSpeedLimit: Long = values[0] as Long
                    val downloadSpeedLimit: Long = values[1] as Long
                    XLDownloadManager.getInstance()
                        .setSpeedLimit(upSpeedLimit, downloadSpeedLimit)
                    return IResult.Success(Unit)
                }
                return IResult.Error(
                    Exception(ConfigureError.INSUFFICIENT_NUMBER_OF_PARAMETERS.name),
                    ConfigureError.INSUFFICIENT_NUMBER_OF_PARAMETERS.ordinal
                )
            }

            else -> return IResult.Error(
                Exception(ConfigureError.NOT_SUPPORT_CONFIGURATION.name),
                ConfigureError.NOT_SUPPORT_CONFIGURATION.ordinal
            )
        }

    }

    private suspend fun autoDownloadTorrent(magnetTask: StationDownloadTask): IResult<StationDownloadTask> {
        try {
            //1.下载种子
            val taskId = XLTaskHelper.instance().addMagentTask(
                magnetTask.url, magnetTask.downloadPath, magnetTask.name
            )

            if (taskId <= 0)
                return IResult.Error(
                    Exception(TaskExecuteError.ADD_MAGNET_TASK_ERROR.name),
                    TaskExecuteError.ADD_MAGNET_TASK_ERROR.ordinal
                )

            return withTimeout(MAGNET_TASK_TIMEOUT) {
                while (XLTaskHelper.instance()
                        .getTaskInfo(taskId).mTaskStatus != ITaskState.DONE.code || XLTaskHelper.instance()
                        .getTaskInfo(taskId).mTaskStatus != ITaskState.ERROR.code || XLTaskHelper.instance()
                        .getTaskInfo(taskId).mTaskStatus != ITaskState.FAILED.code || XLTaskHelper.instance()
                        .getTaskInfo(taskId).mTaskStatus != ITaskState.UNKNOWN.code
                ) {
                    val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId)

                    //3.1获取filesize
                    if (taskInfo.mFileSize == 0L) {
                        continue
                    }

                    if (taskInfo.mFileSize == taskInfo.mDownloadSize)
                        break

                    //延时轮询
                    delay(GET_MAGNET_TASK_INFO_DELAY)
                }
                return@withTimeout initTorrentUrl(
                    File(
                        magnetTask.downloadPath,
                        magnetTask.name
                    ).path
                )
            }
        } catch (e: TimeoutCancellationException) {
            return IResult.Error(
                e, TaskExecuteError.DOWNLOAD_TORRENT_TIME_OUT.ordinal
            )
        } catch (e: Exception) {
            return IResult.Error(
                e, TaskExecuteError.ADD_MAGNET_TASK_ERROR.ordinal
            )
        }
    }

//    /**
//     * 初始化普通任务
//     */
//    private suspend fun initNormalUrlNew(url: String): IResult<NewTaskConfigModel> {
//        val taskName = XLTaskHelper.instance().getFileName(url)
//        val downloadPath =
//            File(configurationDataSource.getDownloadPath(), taskName.substringAfterLast(".")).path
//        val stationDownloadTask = StationDownloadTask(
//            url = url,
//            name = taskName,
//            urlType = TaskTools.getUrlType(url),
//            engine = configurationDataSource.getDefaultEngine(),
//            downloadPath = downloadPath,
//            fileCount = 1
//        )
//
//        val fileTaskConfig = NewTaskConfigModel.FileTaskConfig(
//            taskName = taskName,
//            downloadPath = downloadPath,
//            url = url,
//
//            )
//
//
//        if (stationDownloadTask.urlType == DownloadUrlType.MAGNET) {
//            return autoDownloadTorrent(stationDownloadTask)
//        } else {
//            return IResult.Success(stationDownloadTask)
//        }
//    }


    /**
     * 初始化普通任务
     * 情况1  realUrl 为非磁力链接
     *       -> 直接获取
     * 情况2、realUrl 为磁力链接
     *       -> 转到自动下载种子
     */
    private suspend fun initNormalUrl(
        originUrl: String,
        realUrl: String
    ): IResult<NewTaskConfigModel> {
        val taskName = XLTaskHelper.instance().getFileName(realUrl)
        val downloadPath =
            File(configurationDataSource.getDownloadPath(), taskName.substringAfterLast(".")).path
        var contextType
        if (realUrl.urlType() == DownloadUrlType.HTTP) {
            val result = getHttpFileHeader(realUrl)
            if (result is IResult.Success) {
                val fileContentHeader = result.data

            }
        }else if(realUrl.urlType()==DownloadUrlType.MAGNET){
            return autoDownloadTorrent(stationDownloadTask)
        }else{
            tryDownloadToGetSize(originUrl, taskName, 30000)
        }
        return IResult.Success(
            NewTaskConfigModel.FileTaskConfig(
                originUrl = originUrl,
                url = realUrl,
                taskName = taskName,
                downloadPath = downloadPath,
                fileType = fileContentHeader.content_type,
                fileSize = fileContentHeader.content_length,
                fileExt = taskName.ext()
            )
        )


        val stationDownloadTask = StationDownloadTask(
            url = realUrl,
            name = taskName,
            urlType = realUrl.urlType(),
            engine = configurationDataSource.getDefaultEngine(),
            downloadPath = downloadPath,
            fileCount = 1
        )

        if (stationDownloadTask.urlType == DownloadUrlType.MAGNET) {
            return autoDownloadTorrent(stationDownloadTask)
        } else {
            return IResult.Success(stationDownloadTask)
        }
    }


    private suspend fun getHttpFileHeader(httpUrl: String): IResult<FileContentHeader> {
        try {
            return IResult.Success(fileSizeApiService.getHttpFileHeader(httpUrl))
        } catch (e: Exception) {
            return IResult.Error(e, TaskExecuteError.GET_HTTP_FILE_HEADER_ERROR.ordinal)
        }
    }

    /**
     * 初始化种子任务
     */
    private suspend fun initTorrentUrl(decodeUrl: String): IResult<StationDownloadTask> {
        val torrentInfo = XLTaskHelper.instance().getTorrentInfo(decodeUrl)
            ?: return IResult.Error(
                Exception(TaskExecuteError.TORRENT_INFO_IS_NULL.name),
                TaskExecuteError.TORRENT_INFO_IS_NULL.ordinal
            )

        torrentInfoRepo.saveTorrentInfo(torrentInfo)
        val selectIndexes =
            if (torrentInfo.mFileCount == 1) listOf(0) else filterMediaFileIndexes(torrentInfo)
        if (selectIndexes.isEmpty())
            return IResult.Error(
                Exception(TaskExecuteError.SUB_TORRENT_INFO_IS_NULL.name),
                TaskExecuteError.SUB_TORRENT_INFO_IS_NULL.ordinal
            )
        val taskName = decodeUrl.substringAfterLast(File.separatorChar)
        val downloadPath =
            File(configurationDataSource.getDownloadPath(), taskName.substringAfterLast(".")).path
        val fileCount = torrentInfo.mFileCount
        val stationTorrentInfo = torrentInfo.asStationTorrentInfo()
        return IResult.Success(
            StationDownloadTask(
                url = decodeUrl,
                name = taskName,
                urlType = DownloadUrlType.TORRENT,
                engine = configurationDataSource.getDefaultEngine(),
                downloadPath = downloadPath,
                selectIndexes = selectIndexes,
                fileCount = fileCount,
                torrentInfo = stationTorrentInfo
            )
        )
    }

    private fun filterMediaFileIndexes(torrentInfo: TorrentInfo): List<Int> {
        if (torrentInfo.mSubFileInfo == null)
            return emptyList()
        var selectIndexes = mutableListOf<Int>()
        for (subInfo in torrentInfo.mSubFileInfo) {
            if (subInfo.mFileName.isMedia()) {
                selectIndexes.add(subInfo.mFileIndex)
            }
        }
        return selectIndexes
    }

    private suspend fun calculateTorrentTaskSize(taskId: Long, selectIndexes: List<Int>): Long {
        var calculateSize = 0L;
        while (calculateSize == 0L) {
            for (fileIndex in selectIndexes) {
                val taskInfo = XLTaskHelper.instance().getBtSubTaskInfo(
                    taskId, fileIndex
                ).mTaskInfo;

                if (taskInfo == null || taskInfo.mFileSize == 0L) {
                    calculateSize = 0L
                    break;
                }
                calculateSize += taskInfo.mFileSize
            }
            delay(10)
        }
        return calculateSize
    }

    private suspend fun calculateNormalTaskSize(taskId: Long): Long {
        var totalSize = 0L
        while (totalSize == 0L) {
            val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId)
            if (taskInfo != null) totalSize = taskInfo.mFileSize
            delay(10)
        }
        return totalSize
    }

    override fun DLogger.tag(): String = "XLEngine"

    private fun String.ext(): String = TaskTools.getExt(this)

    private fun String.urlType(): DownloadUrlType = TaskTools.getUrlType(this)

    private fun String.fileType() = TaskTools.isMediaFile(ext())

}