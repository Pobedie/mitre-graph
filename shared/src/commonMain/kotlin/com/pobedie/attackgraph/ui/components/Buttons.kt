package com.pobedie.attackgraph.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.ic_info
import attackgraph.shared.generated.resources.info_content_desc
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun StageButton(
    onClick: () -> Unit,
    buttonText: String,
    hintText: String,
    isEnabled: Boolean,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val contentColor = if (isEnabled) MaterialTheme.colorScheme.onPrimary else Color.Gray

    Button(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .width(200.dp)
            .then(
                if (isHighlighted) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    )
                } else Modifier
            ),
        onClick = onClick,
        enabled = isEnabled,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(Modifier.size(32.dp))
            Text(
                modifier = Modifier.weight(1f, true),
                text = buttonText,
                textAlign = TextAlign.Center,
                color = contentColor
            )

            // todo: Make hints not dissappear on mouse hover + allow text copy
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above,
                    spacingBetweenTooltipAndAnchor = 8.dp
                ),
                tooltip = {
                    PlainTooltip {
                        SelectionContainer {
                            Text(hintText)
                        }
                    }
                },
                state = tooltipState
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 8.dp)
                    ,
                    painter = painterResource(Res.drawable.ic_info),
                    contentDescription = stringResource(Res.string.info_content_desc),
                    tint = contentColor
                )
            }
        }

    }
}
