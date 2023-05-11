package com.station.stationdownloader.di

import android.app.Application
import android.content.Context
import com.tencent.mmkv.MMKV
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object MMKVModule {

    @Provides
    @Singleton
    fun provideMMKV(
        @ApplicationContext context: Context
    ): MMKV {
        MMKV.initialize(context)
        return MMKV.defaultMMKV()
    }
}