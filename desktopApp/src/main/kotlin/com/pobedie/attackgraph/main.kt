package com.pobedie.attackgraph

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pobedie.attackgraph.ui.MainScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AttackGraph",
    ) {
        MaterialTheme {
            MainScreen()
        }
    }
}