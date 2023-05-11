package com.station.stationdownloader.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChannelTest {
    @Test
    fun testChannelSend() {
        runBlocking {

            val testChannel = Channel<TestIntent>(1)
            launch {
                testChannel.consumeAsFlow().collect {
                    when(it){
                        is TestIntent.LoopIntent ->
                        {
                            for(i in 1 .. it.time){
                                println("${it.data}[$i]")
                            }
                        }
                        is TestIntent.PrintIntent -> {
                            println("${it.msg}")
                        }
                    }
                }
            }
            launch {
                testChannel.send(TestIntent.LoopIntent("testData",5))

            }
            launch {
                testChannel.send(TestIntent.PrintIntent("testMessage"))
            }

        }


    }
}


sealed class TestIntent {
    data class PrintIntent(val msg: String) : TestIntent()
    data class LoopIntent(val data: String, val time: Int) : TestIntent()
}