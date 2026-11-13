package com.example.hamkketa.feature.home.presentation

import com.example.hamkketa.core.mvi.UiIntent
import com.example.hamkketa.core.mvi.UiState

data class HomeState(val name: String = "Android") : UiState

sealed interface HomeIntent : UiIntent {
    data class NameChanged(val name: String) : HomeIntent
}
