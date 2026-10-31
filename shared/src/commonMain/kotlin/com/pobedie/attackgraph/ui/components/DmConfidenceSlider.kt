package com.pobedie.attackgraph.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.dm_confidence_threshold
import com.pobedie.attackgraph.ui.theme.TransparentColor
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun DmConfidenceSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(stringResource(Res.string.dm_confidence_threshold, (value * 100).roundToInt()))
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..1f,
            steps = 99,
            colors = SliderDefaults.colors(
                activeTickColor = TransparentColor,
                inactiveTickColor = TransparentColor,
                disabledActiveTickColor = TransparentColor,
                disabledInactiveTickColor = TransparentColor
            )
        )
    }
}
