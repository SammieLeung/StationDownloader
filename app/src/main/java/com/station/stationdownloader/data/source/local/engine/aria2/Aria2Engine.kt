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
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.engine.EngineStatus
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.engine.aria2.connection.client.ClientInstanceHolder
import com.station.stationdownloader.data.source.local.engine.aria2.connection.client.WebSocketClient
import com.station.stationdownloader.data.source.local.engine.aria2.connection.common.OptionsMap
import com.station.stationdownloader.data.source.local.engine.aria2.connection.profile.UserProfileManager
import com.station.stationdownloader.data.source.local.engine.aria2.connection.transport.Aria2Requests
import com.station.stationdownloader.utils.DLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * author: Sam Leung
 * date:  2023/5/9
 */
class Aria2Engine internal constructor(
    private val appContext: Context,
    private val profileManager: UserProfileManager,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : IEngine, DLogger {
    private var aria2Service: Aria2UiDispatcher = Aria2UiDispatcher(appContext)
    private var isInit = false
    private lateinit var reference: ClientInstanceHolder.Reference
    private val listener = object : Aria2Ui.Listener {
        override fun onUpdateLogs(msg: MutableList<Aria2Ui.LogMessage>) {
        }

        override fun onMessage(msg: Aria2Ui.LogMessage) {
            dispatchMessage(msg)
        }

        override fun updateUi(on: Boolean) {
            Log.e(tag(), "[Aria2][Status:$on]")
        }
    }


    override suspend fun init(): IResult<String> = withContext(defaultDispatcher) {
        try {
            if (!isInit) {
                logger("Aria2 init!!!")
                isInit = true
                profileManager.getInAppProfile()
                addAria2UiListener(listener)
                loadAria2ServiceEnv()
                aria2Service.ui.startService()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext IResult.Error(e)
        }
        return@withContext IResult.Success("${DownloadEngine.ARIA2}[${aria2Service.ui.version()}")
    }


    @Throws(BadEnvironmentException::class)
    private fun loadAria2ServiceEnv() {
        if (!aria2Service.ui.hasEnv()) {
            aria2Service.ui.loadEnv(appContext)
            aria2Service.ui.bind()
            aria2Service.ui.askForStatus()
        }
    }

    suspend fun connect() {
        reference = ClientInstanceHolder.instantiate(profileManager.getInAppProfile())
    }

    fun addAria2UiListener(listener: Aria2Ui.Listener) {
        aria2Service.listeners.add(listener)
        aria2Service.ui.askForStatus()
    }

    fun removeAria2UiListener(listener: Aria2Ui.Listener) {
        aria2Service.listeners.remove(listener)
    }

    override suspend fun unInit() {
        aria2Service.ui.stopService()
        aria2Service.listeners.remove(listener)
        isInit = false
    }

    suspend fun initUrl(url: String): IResult<NewTaskConfigModel> {
        TODO("Not yet implemented")
    }

    override suspend fun startTask(
        url: String,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount: Int,
        selectIndexes: IntArray
    ): IResult<String> {
        when (urlType) {
            DownloadUrlType.TORRENT ->
                return addTorrent(
                    url,
                    downloadPath,
                    selectIndexes,
                    false
                )

            else -> {
                return IResult.Error(
                    Exception(TaskExecuteError.NOT_SUPPORT_URL.name),
                    TaskExecuteError.NOT_SUPPORT_URL.ordinal
                )
            }
        }
    }


    private suspend fun addTorrent(
        url: String,
        downloadPath: String,
        selectIndexes: IntArray,
        isPause: Boolean
    ): IResult<String> {
        try {
            val gid = sendToWebSocketSync { continuation ->
                val base64 = base64(url)
                reference.send(requestWithResult = Aria2Requests.addTorrent(
                    base64 = base64,
                    options = OptionsMap().apply {
                        put("pause", OptionsMap.OptionValue(isPause.toString()))
                        put("select-file", OptionsMap.OptionValue(selectIndexes.map {
                            it + 1
                        }.joinToString(",")))
                        put("dir", OptionsMap.OptionValue(downloadPath))
                    }
                ),
                    onResult = object : WebSocketClient.OnResult<String> {
                        override fun onResult(result: String) {
                            continuation.resume(result)
                        }

                        override fun onException(ex: Exception) {
                            continuation.resumeWith(Result.failure(ex))
                        }

                    })
            }
            return IResult.Success(gid)
        } catch (e: Exception) {
            return IResult.Error(e)
        }
    }

    override suspend fun stopTask(taskId: String) {
        TODO("Not yet implemented")
    }


    override suspend fun configure(key: String, values: String): IResult<String> {
        return IResult.Success(Pair(key, values).toString())
    }

    suspend fun getEngineStatus(): EngineStatus {
        val data = suspendCoroutine {
            val listener = object : Aria2Ui.Listener {
                override fun onUpdateLogs(msg: MutableList<Aria2Ui.LogMessage>) {
                }

                override fun onMessage(msg: Aria2Ui.LogMessage) {
                }

                override fun updateUi(on: Boolean) {
                    it.resume(on)
                    aria2Service.listeners.remove(this)
                }
            }
            aria2Service.listeners.add(listener)
            aria2Service.ui.askForStatus()
        }
        return if (data)
            EngineStatus.ON
        else
            EngineStatus.OFF
    }

    suspend fun tellStatus(taskStatus: TaskStatus): IResult<TaskStatus> {
        try {
            val status = sendToWebSocketSync {
                reference.send(requestWithResult = Aria2Requests.tellStatus(taskStatus),
                    onResult = object : WebSocketClient.OnResult<TaskStatus> {
                        override fun onResult(result: TaskStatus) {
                            it.resume(result)
                        }

                        override fun onException(ex: Exception) {
                            it.resumeWith(Result.failure(ex))
                        }

                    })
            }
            return IResult.Success(status)
        } catch (e: Exception) {
            return IResult.Error(e)
        }
    }

    suspend fun tellActive(): List<Aria2TorrentTask> {
        try {
            val status = sendToWebSocketSync {
                reference.send(requestWithResult = Aria2Requests.tellActive(),
                    onResult = object : WebSocketClient.OnResult<List<Aria2TorrentTask>> {
                        override fun onResult(result: List<Aria2TorrentTask>) {
                            it.resume(result)
                        }

                        override fun onException(ex: Exception) {
                            it.resumeWith(Result.failure(ex))
                        }


                    })
            }
            return status
        } catch (e: Exception) {
            return emptyList()
        }
    }

    suspend fun tellWaiting(offset: Int, count: Int): List<Aria2TorrentTask> {
        try {
            val list = sendToWebSocketSync {
                reference.send(requestWithResult = Aria2Requests.tellWaiting(offset, count),
                    onResult = object : WebSocketClient.OnResult<List<Aria2TorrentTask>> {
                        override fun onResult(result: List<Aria2TorrentTask>) {
                            it.resume(result)
                        }

                        override fun onException(ex: Exception) {
                            it.resumeWith(Result.failure(ex))
                        }
                    })
            }
            return list
        } catch (e: Exception) {
            return emptyList()
        }
    }

    suspend fun tellStopped(offset: Int, count: Int): List<Aria2TorrentTask> {
        try {
            val list = sendToWebSocketSync {
                reference.send(requestWithResult = Aria2Requests.tellStopped(offset, count),
                    onResult = object : WebSocketClient.OnResult<List<Aria2TorrentTask>> {
                        override fun onResult(result: List<Aria2TorrentTask>) {
                            it.resume(result)
                        }

                        override fun onException(ex: Exception) {
                            it.resumeWith(Result.failure(ex))
                        }
                    })
            }
            return list
        } catch (e: Exception) {
            return emptyList()
        }
    }


    private suspend inline fun <R> sendToWebSocketSync(crossinline block: suspend (Continuation<R>) -> Unit): R {
        val result = suspendCoroutine { continuation ->
            val job = Job()
            val scope = CoroutineScope(job)
            scope.launch(defaultDispatcher) {
                block(continuation)
            }
        }
        return result
    }


    private fun dispatchMessage(logMessage: Aria2Ui.LogMessage) {
        when (logMessage.type) {
            Message.Type.PROCESS_STARTED -> {
                Log.d(tag(), "[PROCESS_STARTED]>>${logMessage.o}<<")
                val job = Job()
                val scope = CoroutineScope(job)
                scope.launch {
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
                logWarn(logMessage.o.toString())
            }

            Message.Type.PROCESS_ERROR -> {
                logErr(logMessage.o.toString())
            }

            Message.Type.PROCESS_INFO -> {
                logInfo(logMessage.o.toString())
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

    override fun DLogger.tag(): String {
        return "Aria2Engine"
    }


    companion object {
        const val DEBUG = true
    }
}



