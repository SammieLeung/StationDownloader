package com.station.stationdownloader.di

import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.util.EngineRepoUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineUseCaseModule {
    @Provides
    @Singleton
    fun provideEngineRepoUseCase(
        engineRepo:IEngineRepository,
        @AppCoroutineScope scope: CoroutineScope
    ): EngineRepoUseCase {
        return EngineRepoUseCase(engineRepo, scope)
    }
}