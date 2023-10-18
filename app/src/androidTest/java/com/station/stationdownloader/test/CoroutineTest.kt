package com.station.stationdownloader.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    public interface TestListener{
        fun onTest(data:String)
    }


    @Test
    fun testSuspendCoroutine(){
        runBlocking {
            println("start")
            val data=testSuspend()
            println("$data")
        }
    }
    suspend fun testSend(message:String,listener:TestListener){
        delay(3000)
        listener.onTest(message)
    }

    suspend fun testSuspend():String{
      val listener:TestListener
      val testMessage= suspendCoroutine<String> {
          runBlocking {    testSend("test",object : TestListener{
              override fun onTest(data: String) {
                  it.resume(data)
              }
          }) }

      }

        return testMessage
    }

}

