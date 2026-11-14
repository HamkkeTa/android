package com.example.hamkketa.feature.home.presentation

import androidx.lifecycle.ViewModel
import com.example.hamkketa.data.ride.LocalRideGroupRepository
import com.example.hamkketa.domain.ride.BoardingSessionRepository
import com.example.hamkketa.domain.ride.SplitRule
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class CarpoolViewModel @Inject constructor(
    private val repository: LocalRideGroupRepository,
    private val sessions: BoardingSessionRepository
) : ViewModel() {
    val data = repository.data
    val boarding = sessions.state
    private val mutableParked = MutableStateFlow(false)
    val parked = mutableParked.asStateFlow()
    fun confirmParked(value: Boolean) {
        mutableParked.value = value
    }
    private fun edit(action: () -> Unit) {
        if (parked.value && boarding.value.riders.isEmpty()) action()
    }
    fun select(id: String) = edit { repository.select(id) }
    fun addGroup(name: String) = edit { repository.addGroup(name) }
    fun addMember(name: String, phone: String) = edit { repository.addMember(name, phone) }
    fun removeMember(id: String) = edit { repository.removeMember(id) }
    fun saveRule(rule: SplitRule, exempt: Set<String>) = edit { repository.saveRule(rule, exempt) }
}
