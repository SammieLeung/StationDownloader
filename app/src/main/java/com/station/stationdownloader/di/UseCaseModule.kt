package com.station.stationdownloader.di

import com.station.stationdownloader.data.source.repository.DefaultEngineRepository
import com.station.stationdownloader.data.usecase.EngineRepoUseCase
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
        engineRepo: DefaultEngineRepository,
        @AppCoroutineScope scope: CoroutineScope
    ): EngineRepoUseCase {
        return EngineRepoUseCase(engineRepo, scope)
    }
}