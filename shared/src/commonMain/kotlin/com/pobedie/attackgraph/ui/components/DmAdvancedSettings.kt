package com.pobedie.attackgraph.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.dm_advanced_settings
import attackgraph.shared.generated.resources.dm_target_batch_size
import attackgraph.shared.generated.resources.dm_target_batch_size_hint
import attackgraph.shared.generated.resources.ic_info
import attackgraph.shared.generated.resources.info_content_desc
import com.pobedie.attackgraph.ui.theme.InfoIconColor
import com.pobedie.attackgraph.ui.theme.InfoIconInvertedColor
import com.pobedie.attackgraph.ui.theme.TooltipBackgroundColor
import com.pobedie.attackgraph.ui.theme.TooltipContentColor
import com.pobedie.attackgraph.ui.theme.TransparentColor
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun DmAdvancedSettings(
    targetBatchSize: Int,
    onTargetBatchSizeChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // todo: maybe move that to VS
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Text(
                text = stringResource(Res.string.dm_advanced_settings),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                val tooltipState = rememberTooltipState(isPersistent = true)
                val scope = rememberCoroutineScope()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.dm_target_batch_size, targetBatchSize),
                        modifier = Modifier.weight(1f)
                    )
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            positioning = TooltipAnchorPosition.Above
                        ),
                        tooltip = {
                            PlainTooltip(
                                maxWidth = 400.dp,
                                containerColor = TooltipBackgroundColor,
                                contentColor = TooltipContentColor
                            ) {
                                Text(stringResource(Res.string.dm_target_batch_size_hint))
                            }
                        },
                        state = tooltipState,
                        focusable = true,
                        onDismissRequest = { tooltipState.dismiss() }
                    ) {
                        IconButton(
                            onClick = {
                                if (tooltipState.isVisible) {
                                    tooltipState.dismiss()
                                } else {
                                    scope.launch { tooltipState.show() }
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_info),
                                contentDescription = stringResource(Res.string.info_content_desc),
                                modifier = Modifier.size(18.dp),
                                tint = InfoIconInvertedColor
                            )
                        }
                    }
                }

                Slider(
                    value = targetBatchSize.toFloat(),
                    onValueChange = { onTargetBatchSizeChange(it.roundToInt()) },
                    onValueChangeFinished = onValueChangeFinished,
                    modifier = Modifier.fillMaxWidth(),
                    valueRange = 1f..20f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        activeTickColor = TransparentColor,
                        inactiveTickColor = TransparentColor,
                        disabledActiveTickColor = TransparentColor,
                        disabledInactiveTickColor = TransparentColor
                    )
                )
            }
        }
    }
}
