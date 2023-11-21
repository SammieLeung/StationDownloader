package com.station.stationdownloader.test

import androidx.test.core.app.ActivityScenario.launch
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.internal.notify
import okhttp3.internal.notifyAll
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ThreadLocalRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@RunWith(AndroidJUnit4::class)
class CoroutineTest {
    @Test
    fun testRunBlocking() {
        runBlocking {
            launch {
                for (i in 1..1000) {
                    println("from launch 1 [$i]")
                }
            }
            launch {
                for (i in 1..1000) {
                    println("from launch 2 [$i]")
                }
            }
        }
    }

    @Test
    fun testRunBlocking2() {
        val cur = System.currentTimeMillis()
        runBlocking {
            for (i in 1..100000) {
                val a = i + 2
            }
        }
        println("${System.currentTimeMillis() - cur} ms ")

    }


    @Test
    fun testCoroutineScope() {

        val job_1 = Job()
        val job_2 = Job()
        val scope_1 = CoroutineScope(job_1)
        val scope_2 = CoroutineScope(job_2)

        runBlocking {
            coroutineScope { }
            scope_1.launch {
                for (i in 1..1000) {
                    println("from launch 1 [$i]")
                }
            }

            scope_2.launch {
                for (i in 1..1000) {
                    println("from launch 2 [$i]")
                }
            }
        }
        Thread.sleep(2000)
    }

    @Test
    fun testCoroutineScope2() {
        val job = Job()
        val scope = CoroutineScope(job)

        runBlocking {
            scope.launch {
                for (i in 1..1000) {
                    println("from launch 1 [$i]")
                }
            }

            scope.launch {
                for (i in 1..1000) {
                    println("from launch 2 [$i]")
                }
            }
        }
        Thread.sleep(2000)
    }

    @Test
    fun testCoroutineScope3() {
        val job = Job()
        val scope = CoroutineScope(job)

        runBlocking {
            scope.launch {
                launch {
                    for (i in 1..1000) {
                        println("from launch 1 [$i]")
                    }
                }
                launch {
                    for (i in 1..1000) {
                        println("from launch 2 [$i]")
                    }
                }

            }

        }
        Thread.sleep(2000)
    }

    @Test
    fun testCoroutineSync() {
        runBlocking {
            launch(Dispatchers.Default) {
                init("launch1")
            }
            launch(Dispatchers.Default) {
                init("launch2")
            }
            delay(5000)
        }
    }

    var init = false
    var lock = Any()
    suspend fun init(name: String) {
        if (!init) {
            println("${Thread.currentThread().name} not init now $name")
            delay(ThreadLocalRandom.current().nextInt(1000, 1010).toLong())
            synchronized(lock) {
                if (!init) {
                    Thread.sleep(1000)
                    println("${Thread.currentThread().name} I get lock $name")
                    init = true
                } else {
                    println("${Thread.currentThread().name} I don't get lock $name")
                }
            }
        }
    }

    public interface TestListener {
        fun onTest(data: String)
    }


    @Test
    fun testSuspendCoroutine() {
        runBlocking {
            println("start")
            val data = testSuspend()
            println("$data")
        }
    }

    suspend fun testSend(message: String, listener: TestListener) {
        delay(3000)
        listener.onTest(message)
    }

    suspend fun testSuspend(): String {
        val listener: TestListener
        val testMessage = suspendCoroutine<String> {
            runBlocking {
                testSend("test", object : TestListener {
                    override fun onTest(data: String) {
                        it.resume(data)
                    }
                })
            }

        }

        return testMessage
    }

    @Test
    fun testMutex() {
        runBlocking {
            sendStartTask()
            delay(1000)
            sendStopTask()
            delay(8000)
        }
    }

    var startJob: Job? = null
    var stopJob: Job? = null
    val mutex: Mutex = Mutex()
    val scope = CoroutineScope(Job())
    suspend fun sendStartTask() {
        startJob?.cancel()
        stopJob?.cancel()
        println("sendStartTask")
        startJob = startTask(mutex, scope)
    }

    suspend fun sendStopTask() {
        stopJob?.cancel()
        startJob?.cancel()
        println("sendStopTask")
        stopJob = stopTask(mutex)
    }


    suspend fun startTask(mutex: Mutex, extScope: CoroutineScope): Job {
        return GlobalScope.launch {
            try {
                println("start 1 $this")
                stopJob?.cancel()
                stopJob = stopTask(mutex)
                stopJob?.join()
                println("start 1.1 $this")
                launch {
                    println("start test join ${this@launch}")
                    delay(1000)
                    println("start test join over ${this@launch}")
                }.join()
                delay(3000)
                println("start 1.2 END $this")
            } finally {
                println("start 1.3 $this")
            }
        }

    }

    fun stopTask(mutex: Mutex): Job {
        return GlobalScope.launch {
            println("stop 2 $this")
            println("stop 2.1 $this")
            delay(1000)
            println("stop 2.1.1 $this")
            delay(1000)
            println("stop 2.1.2 $this")
            delay(1000)
            println("stop 2.1.3 $this")
            delay(1000)
            println("stop 2.2 END $this")
        }

    }

}

