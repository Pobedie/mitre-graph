package com.pobedie.attackgraph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun AlphaValueDialog(
    alpha: Float,
    onClick: (Float) -> Unit
) {
    var alphaValue by remember { mutableFloatStateOf(alpha) }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray)
            .padding(8.dp)
        ,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Enter alpha value",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "It will be used as a multiplier for risk values"
        )
        FloatInputField(
            value = alphaValue,
            onValueChange = {alphaValue = it},
            modifier = Modifier
                .width(100.dp)
                .padding(vertical = 8.dp)
                .align(Alignment.CenterHorizontally),
        )
        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = {onClick(alphaValue)}
        ) {
            Text("Apply")
        }
    }
}