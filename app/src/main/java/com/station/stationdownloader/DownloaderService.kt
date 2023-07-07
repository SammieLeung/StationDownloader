package com.station.stationdownloader

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gianlu.aria2lib.internal.Aria2Service
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationkitkt.DeviceUtil
import com.station.stationkitkt.RSATools
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.security.PublicKey
import javax.inject.Inject


const val ACTION_START_SERVICE = "start.service"
const val ACTION_STOP_SERVICE = "stop.service"
const val ACTION_GET_TASK_STATUS = "task.status"

const val MESSAGE_START = 1
const val MESSAGE_STOP = 2
const val MESSAGE_INIT_TASK = 3

@AndroidEntryPoint
class DownloaderService : Service() {

    private val mServiceScope: CoroutineScope = CoroutineScope(SupervisorJob())

    @Inject
    lateinit var mEngineRepo: IEngineRepository
    var mMqttClient: MqttClient? = null

    val mMqttConnectOptions: MqttConnectOptions by lazy {
        // MQTT 连接选项
        val publicKey: PublicKey = RSATools.getPublicKey(DeviceUtil.getMqttPublicKey())
        val password = RSATools.base64Bytes(
            RSATools.encrypt(
                ("station_" + DeviceUtil.getDeviceSN()).toByteArray(),
                publicKey
            )
        ).replace("\n".toRegex(), "-n")
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.userName = "station_" + DeviceUtil.getDeviceSN()
        mqttConnectOptions.password = password.toCharArray()
        mqttConnectOptions.keepAliveInterval = 15
        mqttConnectOptions.connectionTimeout = 15
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = true
        return@lazy mqttConnectOptions
    }

    private val serviceThread = HandlerThread("downloader-service")
    private val mMessenger: Messenger by lazy {
        Messenger(LocalHandler(this, mServiceScope))
    }

    override fun onCreate() {
        super.onCreate()
        serviceThread.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_START_SERVICE -> {
                    return try {
                        mMessenger.send(Message.obtain(null, MESSAGE_START))
                        if (flags == 1) START_STICKY else START_REDELIVER_INTENT
                    } catch (ex: RemoteException) {
                        startEngine()
                        if (flags == 1) START_STICKY else START_REDELIVER_INTENT
                    }
                }

                ACTION_STOP_SERVICE -> {
                    stopEngine()
                }

                ACTION_GET_TASK_STATUS -> {
                }
            }
        }

        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mMessenger.binder
    }

    fun startEngine() {
        mServiceScope.launch {
            mEngineRepo.init()
        }
    }

    fun stopEngine() {
        mServiceScope.launch {
            mEngineRepo.unInit()
        }
    }

    fun buildMqttClient(host: String = "tcp://emqx.t-firefly.com:1883"): MqttClient {
        val client = MqttClient(
            host,
            "${DeviceUtil.getDeviceSN()}:downloader",
            MemoryPersistence()
        )
        // 设置回调
        client.setCallback(MqttCallback())
        client.timeToWait = 0
        return client
    }

    fun connect() {
        if (mMqttClient == null) {
            mMqttClient = buildMqttClient()
            mMqttClient?.connect(mMqttConnectOptions)
            Logger.i("MqttClient first connected!")
        } else if (mMqttClient?.isConnected?.not() == true) {
            Logger.i("MqttClient reconnected!")
            mMqttClient?.reconnect()
        } else {
            Logger.i("MqttClient already connected!")
        }
    }

    inner class LocalHandler internal constructor(
        private val service: DownloaderService,
        private val handlerScope: CoroutineScope
    ) : Handler(
        service.serviceThread.looper
    ) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_STOP -> {
                    service.stopEngine()
                    service.stopSelf()
                }

                MESSAGE_START -> {
                    service.startEngine()
                    val intent = Intent(baseContext, DownloaderService::class.java)
                    intent.action = ACTION_GET_TASK_STATUS
                    startService(intent)
                    sendEmptyMessage(MESSAGE_INIT_TASK)
                }


                MESSAGE_INIT_TASK -> {
                    val uploadWorkRequest: OneTimeWorkRequest =
                        OneTimeWorkRequestBuilder<TestWorker>()
                            .build()
                    WorkManager.getInstance(baseContext).beginWith(uploadWorkRequest).enqueue()

                }

                else -> super.handleMessage(msg)
            }
        }
    }


    inner class MqttCallback : MqttCallbackExtended {
        override fun connectionLost(cause: Throwable?) {
            Logger.d("connectionLost $cause")
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            Logger.d("messageArrived:[$topic] ${message?.toString()}")
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            Logger.d("deliveryComplete ${token?.message.toString()}")
        }

        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            val topic = "station/dev/" + DeviceUtil.getDeviceSN()
            Logger.d("connectComplete topic[$topic] reconnect[$reconnect] serverURI:[$serverURI]")

            mMqttClient?.subscribe(topic)
        }

    }

    class TestWorker(
        context: Context,
        workerParameters: WorkerParameters
    ) : CoroutineWorker(context, workerParameters) {


        var mTestWorkerEntryPoint: TestWorkerEntryPoint
        val appContext: Context = context

        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface TestWorkerEntryPoint {
            fun getEngineRepo(): IEngineRepository
        }


        init {

            mTestWorkerEntryPoint =
                EntryPointAccessors.fromApplication(appContext, TestWorkerEntryPoint::class.java)
        }

        override suspend fun doWork(): Result {
            return Result.success()
        }

        override suspend fun getForegroundInfo(): ForegroundInfo {
            return super.getForegroundInfo()
        }

    }

    companion object {
        @JvmStatic
        fun startService(context: Context) {
            context.startService(
                Intent(context, DownloaderService::class.java).setAction(
                    ACTION_START_SERVICE
                )
            )
        }

        @JvmStatic
        fun stopService(context: Context) {
            context.startService(
                Intent(context, DownloaderService::class.java)
                    .setAction(Aria2Service.ACTION_STOP_SERVICE)
            )
        }
    }

}

