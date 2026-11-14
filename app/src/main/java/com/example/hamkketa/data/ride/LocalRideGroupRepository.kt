package com.example.hamkketa.data.ride

import android.content.Context
import com.example.hamkketa.domain.ride.RideGroup
import com.example.hamkketa.domain.ride.RideGroupRepository
import com.example.hamkketa.domain.ride.RideMember
import com.example.hamkketa.domain.ride.SplitRule
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CarpoolData(
    val groups: List<RideGroup> = listOf(RideGroup("commute", "출근 카풀", emptyList())),
    val activeId: String = "commute"
) {
    val rule: SplitRule get() = groups.firstOrNull { it.id == activeId }?.rule ?: SplitRule.Equal
    val exemptIds: Set<String> get() = groups.firstOrNull { it.id == activeId }?.exemptIds.orEmpty()
}

@Singleton
class LocalRideGroupRepository @Inject constructor(@ApplicationContext context: Context) :
    RideGroupRepository {
    private val preferences = context.getSharedPreferences("carpool", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val mutableData = MutableStateFlow(
        runCatching { gson.fromJson(preferences.getString("data", null), CarpoolData::class.java) }
            .getOrNull() ?: CarpoolData()
    )
    val data = mutableData.asStateFlow()
    private val mutableGroup = MutableStateFlow(
        data.value.groups.firstOrNull {
            it.id ==
                data.value.activeId
        }
    )
    override val activeGroup: StateFlow<RideGroup?> = mutableGroup.asStateFlow()

    private fun save(value: CarpoolData) {
        preferences.edit().putString("data", gson.toJson(value)).apply()
        mutableData.value = value
        mutableGroup.value = value.groups.firstOrNull { it.id == value.activeId }
    }

    fun select(id: String) = save(data.value.copy(activeId = id))

    fun addGroup(name: String) {
        if (name.isBlank()) return
        val group = RideGroup(UUID.randomUUID().toString(), name.trim(), emptyList())
        save(data.value.copy(groups = data.value.groups + group, activeId = group.id))
    }

    fun addMember(name: String, phone: String) {
        if (name.isBlank() || phone.filter(Char::isDigit).length !in 10..11) return
        val member =
            RideMember(UUID.randomUUID().toString(), name.trim(), phone.filter(Char::isDigit))
        save(
            data.value.copy(
                groups = data.value.groups.map {
                    if (it.id == data.value.activeId) it.copy(members = it.members + member) else it
                }
            )
        )
    }

    fun removeMember(id: String) = save(
        data.value.copy(
            groups = data.value.groups.map {
                if (it.id ==
                    data.value.activeId
                ) {
                    it.copy(
                        members = it.members.filterNot { member -> member.id == id },
                        exemptIds = it.exemptIds - id
                    )
                } else {
                    it
                }
            }
        )
    )

    fun saveRule(rule: SplitRule, exempt: Set<String>) = save(
        data.value.copy(
            groups = data.value.groups.map {
                if (it.id == data.value.activeId) {
                    it.copy(
                        rule = rule,
                        exemptIds = exempt.intersect(
                            it.members.map { member ->
                                member.id
                            }.toSet()
                        )
                    )
                } else {
                    it
                }
            }
        )
    )
}
