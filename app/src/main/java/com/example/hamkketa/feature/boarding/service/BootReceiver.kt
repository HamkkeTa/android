package com.example.hamkketa.feature.boarding.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// shortcut: Pleos Vehicle SDK 2.3.2에는 시동 상태 API가 없어 헤드유닛 부팅을 시동 ON으로 간주, SDK가 시동 API를 제공하면 교체
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            BoardingSessionService.start(context)
        }
    }
}
