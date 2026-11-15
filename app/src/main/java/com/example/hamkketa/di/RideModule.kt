package com.example.hamkketa.di

import com.example.hamkketa.data.ble.BleBoardingSessionRepository
import com.example.hamkketa.data.ride.LocalRideGroupRepository
import com.example.hamkketa.domain.ride.BoardingSessionRepository
import com.example.hamkketa.domain.ride.RideGroupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RideModule {
    @Binds
    abstract fun bindRideGroupRepository(impl: LocalRideGroupRepository): RideGroupRepository

    @Binds
    abstract fun bindBoardingSessionRepository(
        impl: BleBoardingSessionRepository
    ): BoardingSessionRepository
}
