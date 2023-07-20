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
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
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

    override suspend fun initUrl(originUrl: String): IResult<NewTaskConfigModel> =
        withContext(defaultDispatcher) {
            logger("initUrl")
            var decodeUrl = TaskTools.getUrlDecodeUrl(originUrl)

            val isMagnetHash = TaskTools.isMagnetHash(decodeUrl)
            if (isMagnetHash) decodeUrl = MAGNET_PROTOCOL + decodeUrl

            val isSupportNetWork = TaskTools.isSupportNetworkUrl(decodeUrl)
            if (isSupportNetWork) {/*对thunder link进行解码，用于支持用thunder link包裹磁链的任务*/
                if (decodeUrl.urlType() == DownloadUrlType.THUNDER) {
                    val realUrl = TaskTools.thunderLinkDecode(decodeUrl)
                    if (TaskTools.isSupportNetworkUrl(realUrl))
                        return@withContext initNormalUrl(
                            originUrl,
                            realUrl
                        )
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

    override suspend fun startTask(
        url: String,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount: Int,
        selectIndexes: IntArray
    ): IResult<Long> = withContext(defaultDispatcher) {
        when (urlType) {
            DownloadUrlType.NORMAL, DownloadUrlType.THUNDER, DownloadUrlType.HTTP, DownloadUrlType.ED2k, DownloadUrlType.DIRECT -> {
                val taskId = XLTaskHelper.instance().addThunderTask(url, downloadPath, name)
                if (taskId == 0L) {
                    printCodeLine()
                    return@withContext IResult.Error(
                        Exception("${TaskExecuteError.START_TASK_FAILED.name}:Error Url is [${url}]"),
                        TaskExecuteError.START_TASK_FAILED.ordinal
                    )
                }
                return@withContext IResult.Success(taskId)
            }

            DownloadUrlType.TORRENT -> {

                val deselectIndexes=TaskTools.deSelectedIndexes(fileCount, selectIndexes)
                val taskId = XLTaskHelper.instance().addTorrentTask(
                    url, downloadPath, deselectIndexes
                )

                logger("$url")
                logger("$downloadPath")
                logger("$deselectIndexes")
                if (taskId == -1L) {
                    printCodeLine()
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

    override suspend fun configure(key: String, values: Array<String>): IResult<Unit> {
        when (key) {
            UPLOAD_SPEED_LIMIT, DOWNLOAD_SPEED_LIMIT, SPEED_LIMIT -> {
                if (values.size == 2) {
                    val upSpeedLimit: Long = values[0] as Long
                    val downloadSpeedLimit: Long = values[1] as Long
                    XLDownloadManager.getInstance().setSpeedLimit(upSpeedLimit, downloadSpeedLimit)
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

    private suspend fun autoDownloadTorrent(
        magnetUrl: String, downloadPath: String, torrentFileName: String
    ): IResult<NewTaskConfigModel> {
        try {
            //1.下载种子
            val taskId = XLTaskHelper.instance().addMagentTask(
                magnetUrl, downloadPath, torrentFileName
            )

            if (taskId <= 0) return IResult.Error(
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

                    if (taskInfo.mFileSize == taskInfo.mDownloadSize) break

                    //延时轮询
                    delay(GET_MAGNET_TASK_INFO_DELAY)
                }
                return@withTimeout initTorrentUrl(
                    File(
                        downloadPath, torrentFileName
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


    /**
     * 初始化普通任务
     * 情况1  realUrl 为非磁力链接
     *       -> 直接获取
     * 情况2、realUrl 为磁力链接
     *       -> 转到自动下载种子
     */
    private suspend fun initNormalUrl(
        originUrl: String, realUrl: String
    ): IResult<NewTaskConfigModel> {
        val taskName = XLTaskHelper.instance().getFileName(realUrl)
        val downloadPath =
            File(configurationDataSource.getDownloadPath(), taskName.substringAfterLast(".")).path
        val normalTask = NewTaskConfigModel.NormalTask(
            originUrl = originUrl,
            url = realUrl,
            taskName = taskName,
            downloadPath = downloadPath,
            urlType = TaskTools.getUrlType(realUrl),
            fileTree = TreeNode.Directory.createRoot()
        )

        if (realUrl.urlType() == DownloadUrlType.MAGNET) {
            return autoDownloadTorrent(realUrl, downloadPath, taskName)
        }

        if (realUrl.urlType() == DownloadUrlType.HTTP) {
            val result = getHttpFileHeader(realUrl)
            if (result is IResult.Success) {
                val fileContentHeader = result.data
                if (fileContentHeader.content_length != -1L) {
                    val root = TreeNode.Directory.createRoot()
                    val file = TreeNode.File(
                        fileIndex = 0,
                        fileName = taskName,
                        fileExt = taskName.ext(),
                        fileSize = fileContentHeader.content_length,
                        isChecked = true,
                        parent = root,
                        deep = 0
                    )
                    root.addChild(file)
                    return IResult.Success(
                        normalTask.copy(
                            fileTree = root,
                            urlType = DownloadUrlType.HTTP
                        )
                    )
                }
            }
        }

        val result = tryDownloadToGetTreeNode(originUrl, taskName, 30000)
        if (result is IResult.Success) {
            return IResult.Success(normalTask.copy(fileTree = result.data))
        }
        val root = TreeNode.Directory.createRoot()
        val file = TreeNode.File(
            0,
            taskName,
            taskName.ext(),
            0,
            true, root, 0
        )
        root.addChild(file)
        return IResult.Success(normalTask.copy(fileTree = root))

    }


    /**
     * 初始化种子任务
     */
    private suspend fun initTorrentUrl(torrentUrl: String): IResult<NewTaskConfigModel> {
        var torrentInfo =
            XLTaskHelper.instance().getTorrentInfo(torrentUrl) ?: return IResult.Error(
                Exception(TaskExecuteError.TORRENT_INFO_IS_NULL.name),
                TaskExecuteError.TORRENT_INFO_IS_NULL.ordinal
            )
        if (torrentInfo.mInfoHash == null) return IResult.Error(
            Exception(TaskExecuteError.TORRENT_INFO_IS_NULL.name),
            TaskExecuteError.TORRENT_INFO_IS_NULL.ordinal
        )

        if (torrentInfo.mIsMultiFiles && torrentInfo.mSubFileInfo == null) return IResult.Error(
            Exception(TaskExecuteError.SUB_TORRENT_INFO_IS_NULL.name),
            TaskExecuteError.SUB_TORRENT_INFO_IS_NULL.ordinal
        )

        val taskName = torrentUrl.substringAfterLast(File.separatorChar)
        val downloadPath =
            File(configurationDataSource.getDownloadPath(), taskName.substringAfterLast(".")).path
        val fileCount = torrentInfo.mFileCount
        var torrentId = checkTorrentHash(torrentInfo.mInfoHash)
        if (torrentId == null)
            torrentId = torrentInfoRepo.saveTorrentInfo(torrentInfo)
        return IResult.Success(
            NewTaskConfigModel.TorrentTask(
                torrentId = torrentId,
                torrentPath = torrentUrl,
                taskName = taskName,
                downloadPath = downloadPath,
                fileCount = fileCount,
                fileTree = torrentInfo.createFileTree()
            )
        )
    }

    private suspend fun getHttpFileHeader(httpUrl: String): IResult<FileContentHeader> {
        try {
            return IResult.Success(fileSizeApiService.getHttpFileHeader(httpUrl))
        } catch (e: Exception) {
            return IResult.Error(e, TaskExecuteError.GET_HTTP_FILE_HEADER_ERROR.ordinal)
        }
    }

    private suspend fun tryDownloadToGetTreeNode(
        url: String, fileName: String, timeOut: Long
    ): IResult<TreeNode> = withContext(defaultDispatcher) {

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
        if (!savePathFile.exists()) savePathFile.mkdirs()

        val taskId = XLTaskHelper.instance().addThunderTask(url, savePathFile.path, null)
        if (taskId == 0L) {
            printCodeLine()
            return@withContext IResult.Error(
                Exception("${TaskExecuteError.START_TASK_FAILED.name}:Error Url is [${url}]"),
                TaskExecuteError.START_TASK_FAILED.ordinal
            )
        }
        return@withContext try {
            withTimeout(timeOut) {
                IResult.Success(waitForFileTree(taskId))
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

    private suspend fun waitForFileTree(taskId: Long): TreeNode.Directory {
        while (true) {
            val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId)
            if (taskInfo != null) {
                if (taskInfo.mFileSize != 0L) {
                    val root = TreeNode.Directory.createRoot()
                    val file = TreeNode.File(
                        0, taskInfo.mFileName, taskInfo.mFileName.ext(),
                        taskInfo.mFileSize, true, root, 0
                    )
                    root.addChild(file)
                    return root;
                }
            }
            delay(10)
        }
    }

    private suspend fun checkTorrentHash(hash: String): Long? {
        return when (val result = torrentInfoRepo.getTorrentByHash(hash)) {
            is IResult.Error -> {
                null
            }

            is IResult.Success -> {
                result.data.firstNotNullOfOrNull {
                    it.key.id
                }
            }
        }
        return null
    }

    override fun DLogger.tag(): String = "XLEngine"

    private fun String.ext(): String = TaskTools.getExt(this)

    private fun String.urlType(): DownloadUrlType = TaskTools.getUrlType(this)

    private fun TorrentInfo.createFileTree(): TreeNode {
        val root = TreeNode.Directory.createRoot()

        for (fileInfo in mSubFileInfo) {
            val filePath =
                File(fileInfo.mSubPath, fileInfo.mFileName).path
            val pathComponents = filePath.split(File.separator)
            var currentNode: TreeNode.Directory = root
            for (idx in pathComponents.indices) {
                val comp = pathComponents[idx]
                val isFile = pathComponents.lastIndex == idx
                val existsChild: TreeNode.Directory? = currentNode.children.find {
                    it is TreeNode.Directory && it.folderName == comp
                } as TreeNode.Directory?

                if (existsChild != null) {
                    currentNode = existsChild
                } else {
                    if (isFile) {
                        val newChild = TreeNode.File(
                            fileInfo.mRealIndex,
                            comp,
                            TaskTools.getExt(comp),
                            fileInfo.mFileSize,
                            isChecked = TaskTools.isVideoFile(comp),
                            parent = currentNode,
                            deep = idx
                        )
                        currentNode.addChild(newChild)
                    } else {
                        val newChild = TreeNode.Directory(
                            comp,
                            TreeNode.FolderCheckState.NONE,
                            0,
                            0,
                            children = mutableListOf(),
                            parent = currentNode,
                            deep = idx
                        )
                        currentNode.addChild(newChild)
                        currentNode = newChild
                    }
                }
            }
        }
        return root
    }


}