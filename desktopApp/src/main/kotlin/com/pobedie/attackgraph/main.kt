package com.pobedie.attackgraph

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pobedie.attackgraph.core.MainRepository
import com.pobedie.attackgraph.database.kotlin.DriverFactory
import com.pobedie.attackgraph.database.kotlin.createAtlasDatabase
import com.pobedie.attackgraph.database.kotlin.createSettingsDatabase
import com.pobedie.attackgraph.ui.MainScreen
import com.pobedie.attackgraph.ui.ViewModel

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AttackGraph",
    ) {
        val scope = rememberCoroutineScope()
        val driverFactory = DriverFactory()
        val database = createAtlasDatabase(driverFactory)
        val settingsDatabase = createSettingsDatabase(driverFactory)
        val repository = MainRepository(database, settingsDatabase)
        val mainViewModel = ViewModel(scope,repository)
        MaterialTheme {
            MainScreen(mainViewModel)
        }
    }
}