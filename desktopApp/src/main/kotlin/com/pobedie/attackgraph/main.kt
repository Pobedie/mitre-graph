package com.pobedie.attackgraph

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pobedie.attackgraph.core.MainRepository
import com.pobedie.attackgraph.database.kotlin.DriverFactory
import com.pobedie.attackgraph.database.kotlin.createDatabase
import com.pobedie.attackgraph.ui.MainScreen
import com.pobedie.attackgraph.ui.ViewModel
import kotlinx.coroutines.CoroutineScope

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AttackGraph",
    ) {
        val scope = rememberCoroutineScope()
        val driverFactory = DriverFactory()
        val database = createDatabase(driverFactory)
        val repository = MainRepository(database)
        val mainViewModel = ViewModel(scope,repository)
        MaterialTheme {
            MainScreen(mainViewModel)
        }
    }
}