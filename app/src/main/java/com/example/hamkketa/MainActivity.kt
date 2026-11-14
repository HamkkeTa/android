package com.example.hamkketa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.hamkketa.data.ble.BlePermissions
import com.example.hamkketa.feature.boarding.service.BoardingSessionService
import com.example.hamkketa.navigation.HamkkeTaNavHost
import com.example.hamkketa.ui.theme.HamkkeTaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // The session starts only when permissions are granted; a denial leaves the widget empty.
    private val blePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            BoardingSessionService.start(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BlePermissions.granted(this)) {
            BoardingSessionService.start(this)
        } else {
            blePermissionLauncher.launch(BlePermissions.required)
        }
        enableEdgeToEdge()
        setContent {
            HamkkeTaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HamkkeTaNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
