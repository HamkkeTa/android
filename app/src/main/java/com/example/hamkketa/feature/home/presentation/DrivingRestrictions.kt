package com.example.hamkketa.feature.home.presentation

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberDrivingRestrictions(): State<Boolean> {
    val context = LocalContext.current
    val automotive = context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    val restricted = remember { mutableStateOf(automotive) }
    DisposableEffect(context) {
        var car: Car? = null
        var manager: CarUxRestrictionsManager? = null
        if (automotive) {
            runCatching {
                car = Car.createCar(context)
                manager =
                    car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
                restricted.value =
                    manager?.currentCarUxRestrictions?.isRequiresDistractionOptimization ?: true
                manager?.registerListener {
                    restricted.value = it.isRequiresDistractionOptimization
                }
            }.onFailure { restricted.value = true }
        }
        onDispose {
            runCatching { manager?.unregisterListener() }
            runCatching { car?.disconnect() }
        }
    }
    return restricted
}
