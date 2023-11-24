package com.station.stationdownloader.data.source.local.engine.aria2

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.gianlu.aria2lib.Aria2Ui
import com.gianlu.aria2lib.BadEnvironmentException
import com.gianlu.aria2lib.commonutils.Prefs
import com.gianlu.aria2lib.internal.Message
import com.orhanobut.logger.Logger
import com.station.stationdownloader.Aria2TorrentTask
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.TaskStatus
import com.station.stationdownloader.contants.Aria2Options
import com.station.stationdownloader.contants.BT_TRACKER_UPDATE_INTERVAL
import com.station.stationdownloader.contants.Options
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.data.source.local.engine.aria2.connection.client.ClientInstanceHolder
import com.station.stationdownloader.data.source.local.engine.aria2.connection.client.WebSocketClient
import com.station.stationdownloader.data.source.local.engine.aria2.connection.common.OptionsMap
import com.station.stationdownloader.data.source.local.engine.aria2.connection.exception.Aria2Exception
import com.station.stationdownloader.data.source.local.engine.aria2.connection.profile.UserProfileManager
import com.station.stationdownloader.data.source.local.engine.aria2.connection.transport.Aria2Request
import com.station.stationdownloader.data.source.local.engine.aria2.connection.transport.Aria2RequestWithResult
import com.station.stationdownloader.data.source.local.engine.aria2.connection.transport.Aria2Requests
import com.station.stationdownloader.data.source.remote.BtTrackerApiService
import com.station.stationdownloader.data.source.repository.DefaultConfigurationRepository
import com.station.stationdownloader.data.succeeded
import com.station.stationdownloader.utils.DLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * author: Sam Leung
 * date:  2023/5/9
 */
class Aria2Engine internal constructor(
    private val appContext: Context,
    private val profileManager: UserProfileManager,
    private val configRepo: DefaultConfigurationRepository,
    private val taskRepo: IDownloadTaskRepository,
    private val btTrackerApiService: BtTrackerApiService,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : IEngine, DLogger {
    private var aria2Service: Aria2UiDispatcher = Aria2UiDispatcher(appContext)
    private var isInit = false
    private lateinit var reference: ClientInstanceHolder.Reference
    private val aria2GidData = mutableMapOf<String, String>()
    private val connectMutex = Mutex()
    private val initMutex = Mutex()
    private val aria2UiListener = object : Aria2Ui.Listener {
        override fun onUpdateLogs(msg: MutableList<Aria2Ui.LogMessage>) {
        }

        override fun onMessage(msg: Aria2Ui.LogMessage) {
            dispatchMessage(msg)
        }

        override fun updateUi(on: Boolean) {
            Log.e(tag(), "[Aria2][Status:$on]")
        }
    }

    init {
        addAria2UiListener(aria2UiListener)
    }

    override fun DLogger.tag(): String {
        return "Aria2Engine"
    }

    override suspend fun init(): IResult<String> = withContext(defaultDispatcher) {
        try {
            initMutex.lock()
            if (!isInit) {
                loadAria2ServiceEnv()
                aria2Service.ui.startService()
                connectMutex.lock()
            } else {
                initMutex.unlock()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext IResult.Error(e)
        }
        connectMutex.withLock {
            return@withContext IResult.Success("${DownloadEngine.ARIA2}}")
        }
    }

    override suspend fun unInit() {
        initMutex.lock()
        if (isInit) {
            aria2Service.ui.stopService()
            aria2Service.listeners.remove(aria2UiListener)
            isInit = false
        } else {
            initMutex.unlock()
        }
    }

    override suspend fun isInit(): Boolean {
        return isInit
    }

    suspend fun addPauseTask(
        realUrl: String,
        downloadPath: String,
        urlType: DownloadUrlType,
        selectIndexes: IntArray
    ): IResult<String> {
        if (urlType != DownloadUrlType.TORRENT)
            return IResult.Error(
                Exception(TaskExecuteError.NOT_SUPPORT_URL.name),
                TaskExecuteError.NOT_SUPPORT_URL.ordinal
            )
        val gidResponse = addTorrent(
            realUrl,
            downloadPath,
            selectIndexes,
            true
        )
        if (gidResponse is IResult.Error) {
            return gidResponse
        }
        aria2GidData[realUrl] = (gidResponse as IResult.Success).data
        return gidResponse
    }

    override suspend fun startTask(
        realUrl: String,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount: Int,
        selectIndexes: IntArray
    ): IResult<String> {
        when (urlType) {
            DownloadUrlType.TORRENT -> {
                val gid = aria2GidData[realUrl]
                gid?.let {
                    val unpauseResponse = sendToWebSocketSync(Aria2Requests.unpause(gid))
                    if (unpauseResponse is IResult.Error) {
                        when (unpauseResponse.exception) {
                            is Aria2Exception -> {
                                if (unpauseResponse.exception.reason.contains(Regex("GID [0-9A-Fa-f]+ is not found"))) {
                                    val gidResponse = addTorrent(
                                        realUrl,
                                        downloadPath,
                                        selectIndexes,
                                        false
                                    )
                                    if (gidResponse is IResult.Error) {
                                        return gidResponse
                                    }
                                    aria2GidData[realUrl] = (gidResponse as IResult.Success).data
                                    return gidResponse
                                }
                            }
                        }
                        return unpauseResponse
                    }
                    return IResult.Success(it)
                } ?: run {
                    val gidResponse = addTorrent(
                        realUrl,
                        downloadPath,
                        selectIndexes,
                        false
                    )
                    if (gidResponse is IResult.Error) {
                        return gidResponse
                    }
                    aria2GidData[realUrl] = (gidResponse as IResult.Success).data
                    return gidResponse
                }
            }

            else -> {
                return IResult.Error(
                    Exception(TaskExecuteError.NOT_SUPPORT_URL.name),
                    TaskExecuteError.NOT_SUPPORT_URL.ordinal
                )
            }
        }
    }

    fun containGid(realUrl: String): Boolean {
        return aria2GidData.containsKey(realUrl)
    }

    fun getGid(realUrl: String): String = aria2GidData[realUrl] ?: ""

    suspend fun connect() {
        reference = ClientInstanceHolder.instantiate(profileManager.getInAppProfile())
        loadOptions()
        isInit = true
        connectMutex.unlock()
        initMutex.unlock()
    }

    override suspend fun stopTask(taskId: String): IResult<Boolean> {
        return sendToWebSocketSync(Aria2Requests.pause(taskId))
    }

    override suspend fun setOptions(key: Options, values: String): IResult<Boolean> {
        if (key is Aria2Options) {
            when (key) {
                Aria2Options.SpeedLimit -> {
                    changeGlobalOptions(OptionsMap().apply {
                        put("max-overall-download-limit", OptionsMap.OptionValue(values))
                    })
                }

                Aria2Options.BtTracker -> {
                    changeGlobalOptions(OptionsMap().apply {
                        put("bt-tracker", OptionsMap.OptionValue(values))
                    })
                }

                Aria2Options.BtTrackerLastUpdate -> {}
            }
            configRepo.setValue(key, values)
            return IResult.Success(true)
        }
        return IResult.Success(false)
    }

    @Throws(BadEnvironmentException::class)
    private fun loadAria2ServiceEnv() {
        if (!aria2Service.ui.hasEnv()) {
            aria2Service.ui.loadEnv(appContext)
            aria2Service.ui.bind()
            aria2Service.ui.askForStatus()
        }
    }

    private suspend fun loadOptions() {
        val speedLimit = configRepo.getValue(Aria2Options.SpeedLimit)
        changeGlobalOptions(OptionsMap().apply {
            put("max-overall-download-limit", OptionsMap.OptionValue(speedLimit))
        })

        var btTracker = configRepo.getValue(Aria2Options.BtTracker)

        val btTrackerLastUpdate = configRepo.getValue(Aria2Options.BtTrackerLastUpdate)
        if (btTracker.isEmpty() || btTrackerLastUpdate.isEmpty() || btTrackerLastUpdate.takeIf { (System.currentTimeMillis() - it.toLong()) > BT_TRACKER_UPDATE_INTERVAL } != null) {
            try {
                btTracker = btTrackerApiService.getBtTrackerAllList()
            } catch (e: Exception) {
                Logger.e("${e.message}")
            }
        }
        changeGlobalOptions(OptionsMap().apply {
            put("bt-tracker", OptionsMap.OptionValue(btTracker))
        })
        configRepo.setValue(Aria2Options.BtTracker, btTracker)
        configRepo.setValue(
            Aria2Options.BtTrackerLastUpdate,
            System.currentTimeMillis().toString()
        )
    }

    private suspend fun addTorrent(
        realUrl: String,
        downloadPath: String,
        selectIndexes: IntArray,
        isPause: Boolean
    ): IResult<String> {
        val base64 = base64(realUrl)
        val gidResponse = sendToWebSocketSync(Aria2Requests.addTorrent(
            base64 = base64,
            options = OptionsMap().apply {
                put("pause", OptionsMap.OptionValue(isPause.toString()))
                put("select-file", OptionsMap.OptionValue(selectIndexes.map {
                    it + 1
                }.joinToString(",")))
                put("bt-remove-unselected-file", OptionsMap.OptionValue("true"))
                put("dir", OptionsMap.OptionValue(downloadPath))
                put("seed-time", OptionsMap.OptionValue("0")) //不做种
            }
        ))
        return gidResponse
    }

    private suspend fun changeGlobalOptions(optionsMap: OptionsMap) {
        sendToWebSocketSync(Aria2Requests.changeGlobalOptions(optionsMap))
    }

    suspend fun removeTask(realUrl: String): IResult<Boolean> {
        val gid = aria2GidData[realUrl] ?: return IResult.Error(
            Exception(TaskExecuteError.ARIA2_GID_NOT_FOUND.name),
            TaskExecuteError.ARIA2_GID_NOT_FOUND.ordinal
        )

        try {
            sendToWebSocketSync(Aria2Requests.remove(gid))
            sendToWebSocketSync(Aria2Requests.removeDownloadResult(gid)) {
                aria2GidData.filter { it.value == gid }.forEach {
                    aria2GidData.remove(it.key)
                }
                true
            }
            Logger.d("RemoveTask Success $gid")
            return IResult.Success(true)
        } catch (e: Exception) {
            return IResult.Error(e)
        }
    }

    suspend fun removeDownloadResult(gid: String): IResult<Boolean> {
        return sendToWebSocketSync(Aria2Requests.removeDownloadResult(gid)) {
            Logger.d("removeDownloadResult $gid")
            aria2GidData.filter { it.value == gid }.forEach {
                aria2GidData.remove(it.key)
            }
            true
        }
    }

    suspend fun tellStatus(gid: String, url: String? = null): IResult<TaskStatus> {
        return sendToWebSocketSync<TaskStatus>(Aria2Requests.tellStatus(gid)) {
            if (url != null) {
                it.copy(url = url)
            } else {
                it
            }
        }
    }

    suspend fun getFiles(gid:String){
        sendToWebSocketSync(Aria2Requests.getFiles(gid))
    }

    private suspend fun filterTask(tasks: List<Aria2TorrentTask>): List<Aria2TorrentTask> =
        tasks.mapNotNull { aria2Task ->
            taskRepo.getTorrentTaskByHash(aria2Task.hashInfo)?.let {
                Pair(aria2Task, it)
            }
        }.map {
            val task = it.first
            val entity = it.second
            val status = task.taskStatus
            aria2GidData[entity.realUrl] = task.taskStatus.taskId.id
            task.copy(taskStatus = status.copy(url = entity.url))
        }

    suspend fun tellActive(): List<Aria2TorrentTask> {
        val listResponse = sendToWebSocketSync<List<Aria2TorrentTask>>(Aria2Requests.tellActive()) {
            filterTask(it)
        }
        if (listResponse is IResult.Success) {
            return listResponse.data
        }
        return emptyList()
    }

    suspend fun tellWaiting(offset: Int, count: Int): List<Aria2TorrentTask> {
        val listResponse =
            sendToWebSocketSync<List<Aria2TorrentTask>>(Aria2Requests.tellWaiting(offset, count)) {
                filterTask(it)
            }
        if (listResponse is IResult.Success) {
            return listResponse.data
        }
        return emptyList()
    }

    suspend fun tellStopped(offset: Int, count: Int): List<Aria2TorrentTask> {
        val listResponse =
            sendToWebSocketSync<List<Aria2TorrentTask>>(Aria2Requests.tellStopped(offset, count)) {
                filterTask(it)
            }
        if (listResponse is IResult.Success) {
            return listResponse.data
        }
        return emptyList()
    }

    suspend fun saveSession(): IResult<Boolean> {
        return try {
            sendToWebSocketSync(Aria2Requests.saveSession())
        } catch (e: Exception) {
            IResult.Error(e)
        }
    }

    private suspend fun sendToWebSocketSync(request: Aria2Request): IResult<Boolean> {
        try {
            val result = suspendCoroutine { continuation ->
                val job = Job()
                val scope = CoroutineScope(job)
                scope.launch(defaultDispatcher) {
                    reference.send(
                        request = request,
                        onSuccess = object : WebSocketClient.OnSuccess {
                            override fun onSuccess() {
                                continuation.resume(true)
                            }

                            override fun onException(ex: Exception) {
                                continuation.resumeWith(Result.failure(ex))
                            }
                        }
                    )
                }
            }
            return IResult.Success(result)
        } catch (e: Exception) {
            logErr("sendToWebSocketSync[Aria2Request] ${request.method} $e")
            return IResult.Error(e)
        }
    }

    private suspend fun <R> sendToWebSocketSync(requestWithResult: Aria2RequestWithResult<R>): IResult<R> {
        try {
            val result = suspendCoroutine { continuation ->
                val job = Job()
                val scope = CoroutineScope(job)
                scope.launch(defaultDispatcher) {
                    reference.send(
                        requestWithResult = requestWithResult,
                        onResult = object : WebSocketClient.OnResult<R> {
                            override fun onResult(result: R) {
                                continuation.resume(result)
                            }

                            override fun onException(ex: Exception) {
                                continuation.resumeWith(Result.failure(ex))
                            }
                        }
                    )
                }
            }
            return IResult.Success(result)
        } catch (e: Exception) {
            logErr("sendToWebSocketSync[Aria2RequestWithResult] ${requestWithResult.method} $e")
            return IResult.Error(e)
        }
    }

    private suspend fun sendToWebSocketSync(
        request: Aria2Request, block: () -> Boolean
    ): IResult<Boolean> {
        try {
            val result = suspendCoroutine { continuation ->
                val job = Job()
                val scope = CoroutineScope(job)
                scope.launch(defaultDispatcher) {
                    reference.send(
                        request = request,
                        onSuccess = object : WebSocketClient.OnSuccess {
                            override fun onSuccess() {
                                continuation.resume(block())
                            }

                            override fun onException(ex: Exception) {
                                continuation.resumeWith(Result.failure(ex))
                            }
                        }
                    )
                }
            }
            return IResult.Success(result)
        } catch (e: Exception) {
            logErr("sendToWebSocketSync[Aria2Request] ${request.method} $e")
            return IResult.Error(e)
        }
    }

    private suspend fun <R> sendToWebSocketSync(
        requestWithResult: Aria2RequestWithResult<R>,
        block: suspend (R) -> R
    ): IResult<R> {
        try {
            val result = suspendCoroutine { continuation ->
                val job = Job()
                val scope = CoroutineScope(job)
                scope.launch(defaultDispatcher) {
                    reference.send(
                        requestWithResult = requestWithResult,
                        onResult = object : WebSocketClient.OnResult<R> {
                            override fun onResult(result: R) {
                                scope.launch {
                                    continuation.resume(block(result))
                                }
                            }

                            override fun onException(ex: Exception) {
                                continuation.resumeWith(Result.failure(ex))
                            }
                        }
                    )
                }
            }
            return IResult.Success(result)
        } catch (e: Exception) {
            logErr("sendToWebSocketSync[Aria2RequestWithResult] ${requestWithResult.method} $e")
            return IResult.Error(e)
        }
    }

    private suspend fun <R> batch(sandBox: WebSocketClient.BatchSandBox<R>): IResult<R> {
        try {
            val result = suspendCoroutine<R> { continuation ->
                CoroutineScope(Job()).launch(defaultDispatcher) {
                    reference.batch(sandBox, object : WebSocketClient.OnResult<R> {
                        override fun onResult(result: R) {
                            continuation.resume(result)
                        }

                        override fun onException(ex: Exception) {
                            continuation.resumeWith(Result.failure(ex))
                        }
                    })
                }
            }
            return IResult.Success(result)
        } catch (e: Exception) {
            logErr("batch ${e.message}")
            return IResult.Error(e)
        }
    }

    fun addAria2UiListener(listener: Aria2Ui.Listener) {
        aria2Service.listeners.add(listener)
        aria2Service.ui.askForStatus()
    }

    fun removeAria2UiListener(listener: Aria2Ui.Listener) {
        aria2Service.listeners.remove(listener)
    }

    private fun dispatchMessage(logMessage: Aria2Ui.LogMessage) {
        when (logMessage.type) {
            Message.Type.PROCESS_STARTED -> {
                Log.d(tag(), "[PROCESS_STARTED]>>${logMessage.o}<<")
                CoroutineScope(defaultDispatcher).launch {
                    connect()
                }
            }

            Message.Type.PROCESS_TERMINATED -> {
                Log.d(tag(), "[PROCESS_TERMINATED]>>${logMessage.o}<<")
                ClientInstanceHolder.close()
            }

            Message.Type.MONITOR_FAILED,
            Message.Type.MONITOR_UPDATE -> {
                if (!DEBUG) return

                logger("dispatchMessage ${logMessage.type}")
            }

            Message.Type.PROCESS_WARN -> {
//                logWarn(logMessage.o.toString())
            }

            Message.Type.PROCESS_ERROR -> {
//                logErr(logMessage.o.toString())
            }

            Message.Type.PROCESS_INFO -> {
//                logInfo(logMessage.o.toString())
            }
        }
    }

    private fun logInfo(info: String) {
        if (!DEBUG) return
        Log.i(tag(), "[INFO]>>$info<<")
    }

    private fun logWarn(info: String) {
        if (!DEBUG) return
        Log.w(tag(), "[WARN]>>$info<<")
    }

    private fun logErr(info: String) {
        if (!DEBUG) return
        Log.e(tag(), "[ERROR]>>$info<<")
    }

    private fun base64(url: String): String {
        val uri = Uri.fromFile(File(url))
        val inputStream = appContext.contentResolver.openInputStream(uri)
        val out = ByteArrayOutputStream()

        val buffer = ByteArray(4096)
        if (inputStream != null) {
            var read: Int
            while ((inputStream.read(buffer).also { read = it }) != -1) {
                out.write(buffer, 0, read)
            }
        }
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun setNotifyListener(listener: WebSocketClient.OnNotify?) {
        reference.setNotifyListener(listener)
    }

    private inner class Aria2UiDispatcher(context: Context) : Aria2Ui.Listener {
        init {
            Prefs.init(context)
        }

        val listeners: MutableSet<Aria2Ui.Listener> = mutableSetOf()

        val ui: Aria2Ui = Aria2Ui(context, this)

        override fun onUpdateLogs(msg: MutableList<Aria2Ui.LogMessage>) {
        }

        override fun onMessage(msg: Aria2Ui.LogMessage) {
            for (listener in listeners) listener.onMessage(msg)
        }

        override fun updateUi(on: Boolean) {
            for (listener in listeners) listener.updateUi(on)
        }
    }

    companion object {
        const val DEBUG = true
    }
}



