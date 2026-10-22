package com.pobedie.attackgraph.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.dm_error_code
import attackgraph.shared.generated.resources.dm_error_no_status
import attackgraph.shared.generated.resources.dm_error_title
import attackgraph.shared.generated.resources.dm_error_cancel
import attackgraph.shared.generated.resources.dm_error_retry
import com.pobedie.attackgraph.ui.DmRequestError
import org.jetbrains.compose.resources.stringResource

@Composable
fun DmErrorDialog(error: DmRequestError, onRetry: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dm_error_title)) },
        text = {
            SelectionContainer {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    val code = listOfNotNull(error.statusCode?.toString(), error.errorCode).joinToString(" / ")
                    Text(if (code.isNotBlank()) {
                        stringResource(Res.string.dm_error_code, code)
                    } else {
                        stringResource(Res.string.dm_error_no_status)
                    })
                    Text(error.message)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry) { Text(stringResource(Res.string.dm_error_retry)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dm_error_cancel)) }
        }
    )
}
