package com.example.hamkketa.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// Declare once per file; inject the resulting singleton via StorageModule.
internal val Context.settingsDataStore by preferencesDataStore(name = "hamkketa_settings")
