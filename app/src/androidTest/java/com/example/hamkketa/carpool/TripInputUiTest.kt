package com.example.hamkketa.carpool

import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hamkketa.data.ride.LocalRideGroupRepository
import com.example.hamkketa.data.ride.TripRepository
import com.example.hamkketa.feature.home.presentation.TripScreen
import com.example.hamkketa.ui.theme.HamkkeTaTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TripInputUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun manualDistanceButtonAppliesTextFieldValue() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val prefix = "trip-input-${UUID.randomUUID()}"
        val context = object : ContextWrapper(instrumentation.targetContext) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
                super.getSharedPreferences("$prefix-$name", mode)
        }
        val groups = LocalRideGroupRepository(context)
        val repository = TripRepository(context, groups)
        val sampleGroups = groups.data.value.groups
        compose.setContent {
            HamkkeTaTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TripScreen(repository, sampleGroups)
                }
            }
        }
        compose.onNodeWithText("누적 주행거리 km (예: 1000 → 1010)")
            .performScrollTo().performTextInput("1000")
        compose.onNodeWithText("누적 거리 반영").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1000.0, repository.km.value!!, 0.0) }
        instrumentation.runOnMainSync { repository.stopPolling() }
    }
}
