package com.example.hamkketa.feature.boarding.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.hamkketa.R
import com.example.hamkketa.data.ble.BlePermissions
import com.example.hamkketa.domain.ride.BoardingSessionRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Keeps the BLE boarding session alive while the app UI is not in front.
@AndroidEntryPoint
class BoardingSessionService : Service() {
    @Inject lateinit var sessionRepository: BoardingSessionRepository

    override fun onCreate() {
        super.onCreate()
        val channel =
            NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.boarding_channel_name))
                .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_rider)
                .setContentTitle(getString(R.string.boarding_notification_title))
                .setOngoing(true)
                .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
        sessionRepository.open()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        sessionRepository.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val CHANNEL_ID = "boarding_session"
        private const val NOTIFICATION_ID = 1

        // A connectedDevice foreground service throws without BLE permissions, so check first.
        fun start(context: Context) {
            if (BlePermissions.granted(context)) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, BoardingSessionService::class.java)
                )
            }
        }
    }
}
