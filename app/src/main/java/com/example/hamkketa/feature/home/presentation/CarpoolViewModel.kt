package com.example.hamkketa.feature.home.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.hamkketa.data.ride.LocalRideGroupRepository
import com.example.hamkketa.domain.ride.BoardingSessionRepository
import com.example.hamkketa.domain.ride.CostItem
import com.example.hamkketa.domain.ride.RideMember
import com.example.hamkketa.domain.ride.SplitRule
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GroupDraft(val name: String = "", val members: List<RideMember> = emptyList())

@HiltViewModel
class CarpoolViewModel @Inject constructor(
    private val repository: LocalRideGroupRepository,
    private val savedState: SavedStateHandle,
    private val sessions: BoardingSessionRepository,
    val trips: com.example.hamkketa.data.ride.TripRepository? = null
) : ViewModel() {
    val data = repository.data
    val boardingState = sessions.state
    val runningGroupId = savedState.getStateFlow<String?>("preview.runningGroup", null)
    private val gson = Gson()
    private val mutableDraft = MutableStateFlow(
        runCatching { gson.fromJson(savedState.get<String>("group.draft"), GroupDraft::class.java) }
            .getOrNull() ?: GroupDraft()
    )
    val draft = mutableDraft.asStateFlow()

    init {
        if (trips != null) {
            savedState["preview.runningGroup"] = trips.state.value.current?.group?.id
        } else if (data.value.groups.none { it.id == runningGroupId.value }) {
            savedState["preview.runningGroup"] = null
        }
        repository.activate(runningGroupId.value)
    }

    private fun updateDraft(value: GroupDraft) {
        mutableDraft.value = value
        savedState["group.draft"] = gson.toJson(value)
    }

    fun newDraft() = updateDraft(GroupDraft())
    fun draftName(name: String) = updateDraft(draft.value.copy(name = name))
    fun addDraftMember(member: RideMember) = updateDraft(
        draft.value.copy(
            members =
            draft.value.members + member
        )
    )
    fun removeDraftMember(id: String) = updateDraft(
        draft.value.copy(
            members = draft.value.members.filterNot {
                it.id ==
                    id
            }
        )
    )

    fun start(groupId: String) {
        if (runningGroupId.value == null && data.value.groups.any { it.id == groupId }) {
            if (trips != null && !trips.start(groupId)) return
            savedState["preview.runningGroup"] = groupId
            repository.activate(groupId)
            sessions.open()
        }
    }

    fun stop() {
        if (trips != null && !trips.stop()) return
        savedState["preview.runningGroup"] = null
        repository.activate(null)
        sessions.close()
    }

    fun finishInterrupted() {
        if (trips?.finishInterrupted() != true) return
        savedState["preview.runningGroup"] = null
        repository.activate(null)
        sessions.close()
    }

    fun createGroup(name: String, members: List<RideMember>): String? =
        if (runningGroupId.value == null) repository.createGroup(name, members) else null

    fun addMember(groupId: String, member: RideMember): Boolean {
        if (runningGroupId.value == groupId) return false
        if (data.value.groups.none { it.id == groupId }) return false
        repository.addMember(groupId, member)
        return true
    }

    fun removeMember(groupId: String, memberId: String) {
        if (runningGroupId.value != groupId) repository.removeMember(groupId, memberId)
    }

    fun saveRule(groupId: String, rule: SplitRule, exempt: Set<String>, costs: List<CostItem>) {
        if (runningGroupId.value != groupId) repository.saveRule(groupId, rule, exempt, costs)
    }
}
