package com.example.hamkketa.carpool

import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hamkketa.data.ride.LocalRideGroupRepository
import com.example.hamkketa.data.ride.TripRepository
import com.example.hamkketa.domain.ride.BoardingSessionRepository
import com.example.hamkketa.domain.ride.BoardingState
import com.example.hamkketa.domain.ride.TripCalculator
import com.example.hamkketa.domain.ride.TripSettings
import com.example.hamkketa.feature.home.presentation.CarpoolViewModel
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefix = "trip-flow-${UUID.randomUUID()}"
    private val context = object : ContextWrapper(instrumentation.targetContext) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
            super.getSharedPreferences("$prefix-$name", mode)
    }
    private val sessions = object : BoardingSessionRepository {
        override val state = MutableStateFlow(BoardingState())
        override fun open() = Unit
        override fun close() = Unit
    }

    @Test fun startDriveAlightStopAndReloadPreserveSettlement() {
        lateinit var trips: TripRepository
        lateinit var groups: LocalRideGroupRepository
        instrumentation.runOnMainSync {
            groups = LocalRideGroupRepository(context)
            trips = TripRepository(context, groups)
            trips.configure(TripSettings(10.0, 1000.0, graceMs = 1000))
            val group = groups.data.value.groups.first()
            val vm = CarpoolViewModel(groups, SavedStateHandle(), sessions, trips)
            assertFalse(trips.start(group.id))
            assertTrue(trips.acceptKm(1000.0))
            vm.start(group.id)
            assertEquals(group.id, vm.runningGroupId.value)
            assertFalse(vm.addMember(group.id, group.members.first()))
            trips.doorCycle()
            trips.signal(group.members[0].id, -50)
            trips.acceptKm(1010.0)
            trips.signal(group.members[1].id, -50)
            trips.acceptKm(1020.0)
            trips.disconnected(group.members[0].id)
            trips.acceptKm(1030.0)
            assertFalse(trips.stop())
            val recreated = CarpoolViewModel(groups, SavedStateHandle(), sessions, trips)
            assertEquals(group.id, recreated.runningGroupId.value)
            assertFalse(trips.acceptKm(1029.0))
            assertFalse(trips.stop())
            assertTrue(trips.acceptKm(1030.0))
        }
        Thread.sleep(1150)
        instrumentation.runOnMainSync {
            val vm = CarpoolViewModel(groups, SavedStateHandle(), sessions, trips)
            vm.stop()
            assertNull(vm.runningGroupId.value)
            assertNull(trips.state.value.current)
            assertFalse(trips.stop())
            assertEquals(1, trips.state.value.records.size)
            val record = trips.state.value.records.single()
            val result = TripCalculator.calculate(record)
            assertEquals(3000L, result.amounts.values.sum())
            assertEquals(20.0, result.distanceByMember.getValue(record.group.members[0].id), 0.0)
            trips.stopPolling()
            val restored = TripRepository(context, groups)
            assertEquals(record, restored.state.value.records.single())
            assertNull(restored.state.value.current)
        }
    }

    @Test fun processGapArchivesPartialTripAsInterrupted() = instrumentation.runOnMainSync {
        val groups = LocalRideGroupRepository(context)
        val trips = TripRepository(context, groups)
        trips.acceptKm(100.0)
        assertTrue(trips.start(groups.data.value.groups.first().id))
        trips.acceptKm(105.0)
        trips.stopPolling()
        val restored = TripRepository(context, groups)
        assertNull(restored.state.value.current)
        assertNotNull(restored.state.value.records.single().endedAt)
        assertTrue(restored.state.value.records.single().interrupted)
        assertEquals(105.0, restored.state.value.records.single().endKm, 0.0)
    }
}
