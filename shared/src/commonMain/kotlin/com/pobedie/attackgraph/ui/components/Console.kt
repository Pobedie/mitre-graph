package com.pobedie.attackgraph.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun Console(
    text: String,
    freezeDisplay: Boolean,
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember(text) { mutableStateOf(text.isNotEmpty()) }

    LaunchedEffect(text, freezeDisplay) {
        if (text.isNotEmpty() && !freezeDisplay) {
            visible = true
            delay(5000)
            visible = false
            onTimeout()
        }
    }

    AnimatedVisibility(
        visible = visible || (freezeDisplay && text.isNotEmpty()),
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(Color.Transparent)
                .padding(8.dp)
        ) {
            Text(
                text = text,
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
