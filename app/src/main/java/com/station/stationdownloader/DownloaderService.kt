package com.station.stationdownloader

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import com.gianlu.aria2lib.internal.Aria2Service
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.datasource.IEngineRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


const val ACTION_START_SERVICE = "start"
const val ACTION_STOP_SERVICE = "stop"

const val MESSAGE_STATUS = 2
const val MESSAGE_STOP = 3
const val MESSAGE_START = 4

@AndroidEntryPoint
class DownloaderService : Service() {

    private var isXLEngineRunning = false
    private var isAira2EngineRunning = false
    @Inject
    lateinit var mEngineRepo: IEngineRepository
    private val serviceThread = HandlerThread("aria2-service")
    private val mMessenger: Messenger by lazy {
        Messenger(LocalHandler(this))
    }

    override fun onCreate() {
        super.onCreate()
        serviceThread.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.action == ACTION_START_SERVICE) {
                try {
                    mMessenger.send(Message.obtain(null, MESSAGE_START))
                    return if (flags == 1) START_STICKY else START_REDELIVER_INTENT
                } catch (ex: RemoteException) {
//                    try {
                    start()
                    return if (flags == 1) START_STICKY else START_REDELIVER_INTENT
//                    } catch (exx: IOException) {
//                        Log.e(Aria2Service.TAG, "Still failed to start service.", exx)
//                    } catch (exx: BadEnvironmentException) {
//                        Log.e(Aria2Service.TAG, "Still failed to start service.", exx)
//                    }
                }
            } else if (intent.action == ACTION_STOP_SERVICE) {
                stop()
            }
        }

        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mMessenger.getBinder()
    }

    fun start() {
        mEngineRepo.init()
        Logger.i("engineRepo init")
    }

    fun stop() {
        mEngineRepo.unInit()
    }


    inner class LocalHandler internal constructor(private val service: DownloaderService) : Handler(
        service.serviceThread.looper
    ) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_STATUS -> {}
                MESSAGE_STOP -> {
                    service.stop()
                    service.stopSelf()
                }

                MESSAGE_START -> {
                    service.start()
                }

                else -> super.handleMessage(msg)
            }
        }
    }

    companion object {
        @JvmStatic
        fun startService(context: Context) {
            Handler(Looper.getMainLooper()).post {
                context.startService(
                    Intent(context, DownloaderService::class.java).setAction(
                        ACTION_START_SERVICE
                    )
                )
            }
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