package com.station.stationdownloader.data.source.local.engine.xl

import android.content.Context
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.ITaskState
import com.station.stationdownloader.contants.CommonOptions
import com.station.stationdownloader.contants.Options
import com.station.stationdownloader.contants.TORRENT_DOWNLOAD_TASK_INTERVAL
import com.station.stationdownloader.contants.TORRENT_DOWNLOAD_TASK_TIMEOUT
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.contants.XLOptions
import com.station.stationdownloader.contants.tryDownloadDirectoryPath
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.MultiTaskResult
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.data.source.local.engine.MultiNewTaskConfigModel
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.remote.FileContentHeader
import com.station.stationdownloader.data.source.remote.FileSizeApiService
import com.station.stationdownloader.data.source.repository.DefaultConfigurationRepository
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.MAGNET_PROTOCOL
import com.station.stationdownloader.utils.TaskTools
import com.xunlei.downloadlib.XLDownloadManager
import com.xunlei.downloadlib.XLTaskHelper
import com.xunlei.downloadlib.parameter.TorrentInfo
import com.xunlei.downloadlib.parameter.XLTaskInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

class XLEngine internal constructor(
    private val context: Context,
    private val configRepo: DefaultConfigurationRepository,
    private val torrentInfoRepo: ITorrentInfoRepository,
    private val fileSizeApiService: FileSizeApiService,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : IEngine, DLogger {
    private var hasInit = false
    private val mutex = Mutex()

    override suspend fun init(): IResult<String> = withContext(defaultDispatcher) {
        mutex.withLock {
            if (!hasInit) {
                XLTaskHelper.init(context)
                loadOptions()
                hasInit = true
            }
        }
        return@withContext IResult.Success("${DownloadEngine.XL}[${XLTaskHelper.instance()}]")
    }

    override suspend fun unInit() = withContext(defaultDispatcher) {
        mutex.withLock {
            if (hasInit) {
                XLTaskHelper.uninit()
                hasInit = false
            }
        }
    }

    override suspend fun isInit(): Boolean {
        return hasInit
    }

    private suspend fun loadOptions() {
        val speedLimit = configRepo.getValue(XLOptions.SpeedLimit).toLong()
        XLDownloadManager.getInstance().setSpeedLimit(speedLimit, -1L)
    }

    suspend fun initUrl(originUrl: String): IResult<NewTaskConfigModel> =
        withContext(defaultDispatcher) {
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

    suspend fun initMultiUrl(urlList: List<String>): IResult<MultiNewTaskConfigModel> =
        withContext(defaultDispatcher) {
            val multiTask = MultiNewTaskConfigModel(
                downloadPath = configRepo.getValue(CommonOptions.DownloadPath),
                engine = DownloadEngine.valueOf(configRepo.getValue(CommonOptions.DefaultDownloadEngine)),
                linkCount = urlList.size
            )
            for (url in urlList) {
                val result = initUrl(url)
                if (result is IResult.Success) {
                    multiTask.addTask(result.data)
                } else {
                    multiTask.addFailedLink(url, result as IResult.Error)
                }
            }
            return@withContext IResult.Success(multiTask)
        }

    suspend fun initMultiUrlFlow(urlList: List<String>): Flow<MultiTaskResult> =
        withContext(defaultDispatcher) {
            flow {
                val multiTask = MultiNewTaskConfigModel(
                    downloadPath = configRepo.getValue(CommonOptions.DownloadPath),
                    engine = DownloadEngine.valueOf(configRepo.getValue(CommonOptions.DefaultDownloadEngine)),
                    linkCount = urlList.size
                )
                emit(MultiTaskResult.Begin(multiTask))
                for (url in urlList) {
                    emit(MultiTaskResult.InitializingTask(url))
                    val result = initUrl(url)
                    if (result is IResult.Success) {
                        multiTask.addTask(result.data)
                        emit(MultiTaskResult.Success(result.data))
                    } else {
                        multiTask.addFailedLink(url, result as IResult.Error)
                        emit(MultiTaskResult.Failed(url, result))
                    }
                }
                emit(MultiTaskResult.Finish)
            }
        }

    override suspend fun startTask(
        url: String,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount: Int,
        selectIndexes: IntArray
    ): IResult<String> = withContext(defaultDispatcher) {
        when (urlType) {
            DownloadUrlType.NORMAL, DownloadUrlType.THUNDER, DownloadUrlType.HTTP, DownloadUrlType.ED2k, DownloadUrlType.DIRECT -> {
                val taskId = XLTaskHelper.instance().addThunderTask(url, downloadPath, name)
                if (taskId == -1L) {
                    printCodeLine()
                    return@withContext IResult.Error(
                        Exception("${TaskExecuteError.START_TASK_FAILED.name}:Error Url is [${url}]"),
                        TaskExecuteError.START_TASK_FAILED.ordinal
                    )
                }
                return@withContext IResult.Success(taskId.toString())
            }

            DownloadUrlType.TORRENT -> {
                val deselectIndexes = TaskTools.deSelectedIndexes(fileCount, selectIndexes)
                val taskId = XLTaskHelper.instance().addTorrentTask(
                    url, downloadPath, deselectIndexes
                )
                logger("开始任务【$url】")
                logger("taskId【$taskId】")
                if (taskId == -1L) {
                    return@withContext IResult.Error(
                        Exception("${TaskExecuteError.START_TASK_FAILED.name}:Error Url is [${url}]"),
                        TaskExecuteError.START_TASK_FAILED.ordinal
                    )
                }
                return@withContext IResult.Success(taskId.toString())
            }

            DownloadUrlType.UNKNOWN, DownloadUrlType.MAGNET -> {
                return@withContext IResult.Error(
                    exception = Exception("${TaskExecuteError.START_TASK_URL_TYPE_ERROR.name}:[${urlType.name}]"),
                    code = TaskExecuteError.START_TASK_URL_TYPE_ERROR.ordinal
                )
            }
        }
    }

    override suspend fun stopTask(taskId: String): IResult<Boolean> {
        XLTaskHelper.instance().stopTask(taskId.toLong())
        return IResult.Success(true)
    }

    override suspend fun setOptions(key: Options, values: String): IResult<Boolean> {
        if (key is XLOptions) {
            when (key) {
                XLOptions.SpeedLimit -> {
                    val speedLimit = values.toLong()
                    XLDownloadManager.getInstance().setSpeedLimit(speedLimit, -1L)
                }
            }
            configRepo.setValue(key, values)
            return IResult.Success(true)
        }
        return IResult.Success(false)
    }


    suspend fun getTaskInfo(taskId: Long): XLTaskInfo =
        withContext(defaultDispatcher) { XLTaskHelper.instance().getTaskInfo(taskId) }

    suspend fun getTorrentInfo(torrentPath: String): IResult<TorrentInfo> =
        withContext(defaultDispatcher) {
            val torrentInfo = XLTaskHelper.instance().getTorrentInfo(torrentPath)
                ?: return@withContext IResult.Error(
                    Exception(TaskExecuteError.TORRENT_INFO_IS_NULL.name),
                    TaskExecuteError.TORRENT_INFO_IS_NULL.ordinal
                )
            return@withContext IResult.Success(torrentInfo)
        }

    private suspend fun autoDownloadTorrent(
        magnetUrl: String, downloadPath: String, torrentFileName: String
    ): IResult<NewTaskConfigModel> {
        //1.下载种子
        val taskId = XLTaskHelper.instance().addMagentTask(
            magnetUrl, downloadPath, torrentFileName
        )
        try {

            if (taskId <= 0) {
                val torrentFile = File(downloadPath, torrentFileName)
                return if (torrentFile.isFile && torrentFile.exists()) {
                    initTorrentUrl(torrentFile.path, magnetUrl)
                } else {
                    IResult.Error(
                        Exception(TaskExecuteError.ADD_MAGNET_TASK_ERROR.name),
                        TaskExecuteError.ADD_MAGNET_TASK_ERROR.ordinal
                    )
                }
            }


            Logger.w("开始下载种子【$magnetUrl】任务id:$taskId")


            return withTimeout(TORRENT_DOWNLOAD_TASK_TIMEOUT) {
                while (XLTaskHelper.instance()//FIXME ITaskState是通用任务状态，不应该和XL下载的任务状态混用
                        .getTaskInfo(taskId).mTaskStatus != ITaskState.DONE.code || XLTaskHelper.instance()
                        .getTaskInfo(taskId).mTaskStatus != ITaskState.ERROR.code || XLTaskHelper.instance()
                        .getTaskInfo(taskId).mTaskStatus != ITaskState.FAILED.code || XLTaskHelper.instance()
                        .getTaskInfo(taskId).mTaskStatus != ITaskState.UNKNOWN.code
                ) {
                    val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId)

                    //3.1获取filesize
                    if (taskInfo.mFileSize == 0L) {
                        delay(TORRENT_DOWNLOAD_TASK_INTERVAL)
                        continue
                    }

                    if (taskInfo.mFileSize == taskInfo.mDownloadSize) {
                        XLTaskHelper.instance().stopTask(taskId)
                        break
                    }
                    logger("torrentFileSize=${taskInfo.mFileSize} torrentDownloadSize=${taskInfo.mDownloadSize}")
                    //延时轮询
                    delay(TORRENT_DOWNLOAD_TASK_INTERVAL)
                }
                return@withTimeout initTorrentUrl(
                    File(
                        downloadPath, torrentFileName
                    ).path,
                    magnetUrl
                )
            }
        } catch (e: TimeoutCancellationException) {
            XLTaskHelper.instance().stopTask(taskId)
            return IResult.Error(
                e, TaskExecuteError.DOWNLOAD_TORRENT_TIME_OUT.ordinal
            )
        } catch (e: Exception) {
            e.printStackTrace()
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
        var taskName = XLTaskHelper.instance().getFileName(realUrl)
        val downloadPath =
            File(configRepo.getValue(CommonOptions.DownloadPath)).path
        val normalTask = NewTaskConfigModel.NormalTask(
            originUrl = originUrl,
            url = realUrl,
            taskName = taskName,
            downloadPath = downloadPath,
            urlType = TaskTools.getUrlType(realUrl),
            fileTree = TreeNode.Directory.createRoot()
        )

        if (realUrl.urlType() == DownloadUrlType.MAGNET) {
            return autoDownloadTorrent(
                originUrl,
                File(downloadPath, File(taskName).nameWithoutExtension).path,
                taskName
            )
        }

        if (realUrl.urlType() == DownloadUrlType.HTTP) {
            val result = getHttpFileHeader(realUrl)
            if (result is IResult.Success) {
                val fileContentHeader = result.data
                if (fileContentHeader.content_length != -1L) {
//                    if(fileContentHeader.url!=realUrl)
//                        taskName=XLTaskHelper.instance().getFileName(fileContentHeader.url)
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
                            urlType = DownloadUrlType.HTTP,
                            engine = DownloadEngine.valueOf(configRepo.getValue(CommonOptions.DefaultDownloadEngine))
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
    private suspend fun initTorrentUrl(
        torrentUrl: String,
        magnetUrl: String = ""
    ): IResult<NewTaskConfigModel> {
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
        val taskName = torrentUrl.substringAfterLast(File.separatorChar).substringBeforeLast(".")
        val downloadPath =
            File(configRepo.getValue(CommonOptions.DownloadPath)).path
        val fileCount = torrentInfo.mFileCount
        val torrentIdResult = torrentInfoRepo.saveTorrentInfo(torrentInfo, torrentUrl)

        if (torrentIdResult is IResult.Error) {
            return torrentIdResult
        }

        return IResult.Success(
            NewTaskConfigModel.TorrentTask(
                torrentId = (torrentIdResult as IResult.Success).data,
                torrentPath = torrentUrl,
                magnetUrl = magnetUrl,
                taskName = taskName,
                downloadPath = downloadPath,
                fileCount = fileCount,
                engine = DownloadEngine.valueOf(configRepo.getValue(CommonOptions.DefaultDownloadEngine)),
                fileTree = torrentInfo.createFileTree()
            )
        )
    }

    private suspend fun getHttpFileHeader(httpUrl: String): IResult<FileContentHeader> {
        return try {
            IResult.Success(fileSizeApiService.getHttpFileHeader(httpUrl))
        } catch (e: Exception) {
            IResult.Error(e, TaskExecuteError.GET_HTTP_FILE_HEADER_ERROR.ordinal)
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
        } catch (e: Exception) {
            Logger.e("${e.message}")
            IResult.Error(
                e
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
                    Logger.d(taskInfo.toString())
                    val root = TreeNode.Directory.createRoot()
                    //FIXME 处理当mFileName为null的情况
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
                            deep = currentNode.deep + 1
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
                            deep = currentNode.deep + 1
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