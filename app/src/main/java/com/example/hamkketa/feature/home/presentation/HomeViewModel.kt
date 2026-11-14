package com.example.hamkketa.feature.home.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.hamkketa.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle) :
    MviViewModel<HomeState, HomeIntent>(
        HomeState(name = savedStateHandle[NAME_KEY] ?: "Android")
    ) {
    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.NameChanged -> {
                savedStateHandle[NAME_KEY] = intent.name
                updateState { it.copy(name = intent.name) }
            }
        }
    }

    private companion object {
        const val NAME_KEY = "home.name"
    }
}
