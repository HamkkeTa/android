package com.example.hamkketa.feature.home.presentation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {
    @Test
    fun nameInputKeepsPreviousStateImmutable() {
        val viewModel = HomeViewModel(SavedStateHandle())
        val previousState = viewModel.state.value

        viewModel.onIntent(HomeIntent.NameChanged("함께타"))

        assertEquals("함께타", viewModel.state.value.name)
        assertEquals("Android", previousState.name)
    }

    @Test
    fun nameInputCanBeRestoredInANewViewModel() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = HomeViewModel(savedStateHandle)
        viewModel.onIntent(HomeIntent.NameChanged("함께타"))
        // Simulate a new handle populated from the saved bundle.
        val restoredValues = savedStateHandle.keys().associateWith { key ->
            savedStateHandle.get<Any?>(key)
        }
        val restoredViewModel = HomeViewModel(SavedStateHandle(restoredValues))

        assertEquals("함께타", restoredViewModel.state.value.name)
    }
}
