package com.pobedie.attackgraph.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.ic_arrow_forward
import attackgraph.shared.generated.resources.next_step_content_desc
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


fun LazyListScope.StageArrow() {
    item {
        Icon(
            modifier = Modifier.height(36.dp),
            painter = painterResource(Res.drawable.ic_arrow_forward),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = stringResource(Res.string.next_step_content_desc)
        )
    }
}