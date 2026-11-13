package com.example.hamkketa.core.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

abstract class MviViewModel<S : UiState, I : UiIntent>(initialState: S) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<S> = mutableState.asStateFlow()

    abstract fun onIntent(intent: I)

    // The reducer must be pure: StateFlow may evaluate it more than once.
    protected fun updateState(reducer: (S) -> S) {
        mutableState.update(reducer)
    }
}
