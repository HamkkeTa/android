package com.example.hamkketa.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamkketa.R
import com.example.hamkketa.feature.boarding.presentation.RiderStatusRoute
import com.example.hamkketa.ui.theme.HamkkeTaTheme

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(modifier = modifier.fillMaxSize()) {
        HomeScreen(state = state, onIntent = viewModel::onIntent)
        RiderStatusRoute(modifier = Modifier.align(Alignment.TopEnd).padding(24.dp))
    }
}

// A small editable greeting demonstrates state -> UI -> intent without a backend.
@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.home_greeting, state.name))
        OutlinedTextField(
            value = state.name,
            onValueChange = { onIntent(HomeIntent.NameChanged(it)) },
            label = { Text(stringResource(R.string.home_name_label)) },
            singleLine = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HamkkeTaTheme {
        HomeScreen(state = HomeState(), onIntent = {})
    }
}
