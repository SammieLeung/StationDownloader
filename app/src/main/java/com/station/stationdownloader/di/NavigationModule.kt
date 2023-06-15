package com.station.stationdownloader.di

import com.station.stationdownloader.navgator.AppNavigator
import com.station.stationdownloader.navgator.AppNavigatorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@InstallIn(ActivityComponent::class)
@Module
abstract class NavigationModule {

    @Binds
    abstract fun bindNavigator(impl: AppNavigatorImpl): AppNavigator
}