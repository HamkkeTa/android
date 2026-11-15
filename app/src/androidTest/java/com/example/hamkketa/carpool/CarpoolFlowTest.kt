package com.example.hamkketa.carpool

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hamkketa.data.ride.LocalRideGroupRepository
import com.example.hamkketa.domain.ride.BoardingSessionRepository
import com.example.hamkketa.domain.ride.BoardingState
import com.example.hamkketa.domain.ride.CostItem
import com.example.hamkketa.domain.ride.RideMember
import com.example.hamkketa.domain.ride.SessionStatus
import com.example.hamkketa.domain.ride.SplitRule
import com.example.hamkketa.feature.home.presentation.CarpoolViewModel
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CarpoolFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val preferences = instrumentation.targetContext.getSharedPreferences(
        "carpool-test-${UUID.randomUUID()}",
        Context.MODE_PRIVATE
    )
    private val context = object : ContextWrapper(instrumentation.targetContext) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = preferences
    }
    private val session = object : BoardingSessionRepository {
        override val state = MutableStateFlow(BoardingState())
        override fun open() {
            state.value = BoardingState(status = SessionStatus.Open)
        }
        override fun close() {
            state.value = BoardingState()
        }
    }

    @After fun cleanUp() {
        preferences.edit().clear().commit()
    }

    @Test fun runningGroupBlocksEditsUntilStopped() = instrumentation.runOnMainSync {
        val repository = LocalRideGroupRepository(context)
        val vm = CarpoolViewModel(repository, SavedStateHandle(), session)
        val group = vm.data.value.groups.first()
        assertNull(vm.runningGroupId.value)
        assertNull(repository.activeGroup.value)
        vm.start(group.id)
        assertEquals(group.id, repository.activeGroup.value?.id)
        assertFalse(vm.addMember(group.id, RideMember("new", "새 동승자")))
        vm.removeMember(group.id, group.members.first().id)
        vm.saveRule(
            group.id,
            SplitRule.Passengers,
            setOf(group.members.first().id),
            emptyList()
        )
        assertEquals(group, vm.data.value.groups.first())
        vm.stop()
        assertNull(repository.activeGroup.value)
        assertTrue(vm.addMember(group.id, RideMember("new", "새 동승자")))
    }

    @Test fun draftRegistrationAndNewGroupDefaultsSurviveStorage() = instrumentation.runOnMainSync {
        val repository = LocalRideGroupRepository(context)
        val handle = SavedStateHandle()
        val vm = CarpoolViewModel(repository, handle, session)
        vm.draftName("주말 카풀")
        vm.addDraftMember(RideMember("new", "반지민", "01012345678", "iPhone 16", true, true))
        val restored = CarpoolViewModel(repository, handle, session)
        assertEquals("주말 카풀", restored.draft.value.name)
        assertEquals(1, restored.draft.value.members.size)
        val id = restored.createGroup(restored.draft.value.name, restored.draft.value.members)
        val group = LocalRideGroupRepository(context).data.value.groups.first { it.id == id }
        assertEquals(SplitRule.Equal, group.rule)
        assertTrue(group.exemptIds.isEmpty())
        assertEquals(3, group.costs.size)
        assertTrue(group.costs.all { it.included })
        assertEquals("iPhone 16", group.members.single().deviceName)
    }

    @Test fun ruleCostsAndExemptionsAreSavedPerGroup() = instrumentation.runOnMainSync {
        val repository = LocalRideGroupRepository(context)
        val vm = CarpoolViewModel(repository, SavedStateHandle(), session)
        val first = vm.data.value.groups[0]
        val other = vm.data.value.groups[1]
        vm.saveRule(
            first.id,
            SplitRule.Passengers,
            setOf(first.members.first().id),
            listOf(CostItem("charge", "충전비", false))
        )
        val loaded = LocalRideGroupRepository(context).data.value.groups
        assertEquals(
            listOf(CostItem("charge", "충전비", false)),
            loaded.first {
                it.id == first.id
            }.costs
        )
        assertEquals(other, loaded.first { it.id == other.id })
    }

    @Test fun legacyMembersAndCostsCanBeLoaded() {
        preferences.edit().putString(
            "data",
            """{"groups":[{"id":"old","name":"이전 그룹","members":[{"id":"a","name":"이전 동승자"}],"rule":"Equal","exemptIds":[]}],"activeId":"old"}"""
        ).commit()
        val group = LocalRideGroupRepository(context).data.value.groups.single()
        assertEquals("", group.members.single().deviceName)
        assertEquals(3, group.costs.size)
    }
}
