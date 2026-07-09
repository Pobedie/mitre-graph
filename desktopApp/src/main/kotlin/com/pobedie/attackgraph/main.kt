package com.pobedie.attackgraph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pobedie.attackgraph.core.MainRepository
import com.pobedie.attackgraph.database.kotlin.DriverFactory
import com.pobedie.attackgraph.database.kotlin.createAtlasDatabase
import com.pobedie.attackgraph.database.kotlin.createSettingsDatabase
import com.pobedie.attackgraph.ui.MainScreen
import com.pobedie.attackgraph.ui.ViewModel
import com.pobedie.attackgraph.ui.theme.AttackGraphTheme

fun main() = application {
    val driverFactory = remember { DriverFactory() }
    val database = remember { createAtlasDatabase(driverFactory) }
    val settingsDatabase = remember { createSettingsDatabase(driverFactory) }
    val repository = remember { MainRepository(database, settingsDatabase) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "AttackGraph",
    ) {
        val scope = rememberCoroutineScope()
        val mainViewModel = remember { ViewModel(scope, repository) }
        val state by mainViewModel.state.collectAsStateWithLifecycle()
        AttackGraphTheme(darkTheme = state.isDarkMode) {
            MainScreen(mainViewModel)
        }
    }
}