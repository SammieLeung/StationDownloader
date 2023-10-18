package com.station.stationdownloader.di

import android.content.Context
import com.station.stationdownloader.data.source.local.engine.aria2.connection.profile.UserProfileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
object ProfileManagerModule {

    @Provides
    fun provideUserProfileManager(
        @ApplicationContext appContext: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): UserProfileManager {
        return UserProfileManager(appContext,ioDispatcher)
    }
}