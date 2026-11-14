package com.example.hamkketa.feature.boarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.hamkketa.R
import com.example.hamkketa.core.mvi.UiState
import com.example.hamkketa.domain.ride.BoardingSessionRepository
import com.example.hamkketa.ui.theme.HamkkeTaTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RiderStatusState(val riderCount: Int = 0) : UiState

// Display-only widget: no intents because the driver never touches it.
@HiltViewModel
class RiderStatusViewModel @Inject constructor(sessionRepository: BoardingSessionRepository) :
    ViewModel() {
    val state: StateFlow<RiderStatusState> =
        sessionRepository.state
            .map { RiderStatusState(riderCount = it.riders.size) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RiderStatusState())
}

@Composable
fun RiderStatusRoute(
    modifier: Modifier = Modifier,
    viewModel: RiderStatusViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RiderStatusWidget(state = state, modifier = modifier)
}

@Composable
fun RiderStatusWidget(state: RiderStatusState, modifier: Modifier = Modifier) {
    val description =
        pluralStringResource(R.plurals.rider_status_description, state.riderCount, state.riderCount)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(RiderStatusWidgetDefaults.IconSpacing)
    ) {
        repeat(state.riderCount) {
            Icon(
                painter = painterResource(RiderStatusWidgetDefaults.IconRes),
                contentDescription = null,
                modifier = Modifier.size(RiderStatusWidgetDefaults.IconSize),
                tint = RiderStatusWidgetDefaults.iconColor()
            )
        }
    }
}

// Visual tokens for the widget; a redesign should only need to change this object or the icon drawable.
object RiderStatusWidgetDefaults {
    val IconRes = R.drawable.ic_rider
    val IconSize = 48.dp
    val IconSpacing = 8.dp

    @Composable
    fun iconColor(): Color = MaterialTheme.colorScheme.primary
}

@Preview(showBackground = true)
@Composable
private fun RiderStatusWidgetPreview() {
    HamkkeTaTheme {
        RiderStatusWidget(state = RiderStatusState(riderCount = 3))
    }
}
