package com.station.stationdownloader.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
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


}

