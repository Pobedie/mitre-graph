package com.pobedie.attackgraph.ui.Stages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.ic_floder
import attackgraph.shared.generated.resources.import_button
import attackgraph.shared.generated.resources.import_from_file_title
import attackgraph.shared.generated.resources.or_use_included_data
import attackgraph.shared.generated.resources.select_yaml_file_content_desc
import attackgraph.shared.generated.resources.select_yaml_file_dialog_title
import attackgraph.shared.generated.resources.select_yaml_file_placeholder
import attackgraph.shared.generated.resources.use_included_data_checkbox
import com.pobedie.attackgraph.ui.Language
import com.pobedie.attackgraph.ui.ViewModel
import com.pobedie.attackgraph.ui.ViewState
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize


@Composable
fun ImportStage(
    viewModel: ViewModel,
    state: ViewState
){
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray)
                .widthIn(max = 600.dp)
                .heightIn(max = 800.dp)
            ,
            horizontalAlignment = Alignment.Start
        ) {

            Text(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                text = stringResource(Res.string.import_from_file_title),
                style = MaterialTheme.typography.titleLarge
            )
            FileSelectionField(
                modifier = Modifier.padding(horizontal = 22.dp),
                filePath = state.filePath,
                onClick = {
                    viewModel.selectFile(
                        path = openFilePicker(),
                        useDefault = false
                    )
                },
                isFileError = state.fileError != null,
                isEnabled = !state.isProvidedAtlasDateSelected
            )

            AnimatedVisibility(
                visible = state.fileError != null
            ) {
                Text(
                    text = state.fileError.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .padding( horizontal = 16.dp),
                text = stringResource(Res.string.or_use_included_data),
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                modifier = Modifier.padding(horizontal = 8.dp,),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.isProvidedAtlasDateSelected,
                    onCheckedChange = { viewModel.selectFile(useDefault = !state.isProvidedAtlasDateSelected) },
                )
                Text(
                    text = stringResource(Res.string.use_included_data_checkbox),
                )
            }
            val isImportAvailable = (state.filePath.isNotBlank() || state.isProvidedAtlasDateSelected)
            Button(
                modifier = Modifier
                    .padding(horizontal = 22.dp, vertical = 16.dp)
                    .align(Alignment.End),
                onClick = { viewModel.importAtlasData() },
                enabled = isImportAvailable
            ) {
                Text(stringResource(Res.string.import_button))
            }

        }

        // Language selector
        var isMenuExpanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Button(
                onClick = { isMenuExpanded = true },
                shape = RoundedCornerShape(8.dp),
                interactionSource = MutableInteractionSource(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                val currentLangLabel = when (state.language) {
                    Language.English -> "ENGLISH"
                    Language.Russian -> "РУССКИЙ"
                }
                Text(
                    text = currentLangLabel,
                    color = Color.White
                )
            }
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier.background(Color.Gray)
            ) {
                Language.entries.forEach { lang ->
                    DropdownMenuItem(
                        text = {
                            val label = when (lang) {
                                Language.English -> "ENGLISH"
                                Language.Russian -> "РУССКИЙ"
                            }
                            Text(
                                text = label,
                                color = Color.White
                            )
                        },
                        onClick = {
                            viewModel.changeLanguage(lang)
                            isMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileSelectionField(
    filePath: String,
    onClick: () -> Unit,
    isFileError: Boolean,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
){
    val contentColor = when {
        isFileError -> MaterialTheme.colorScheme.onError
        !isEnabled -> MaterialTheme.colorScheme.background.copy(alpha =  0.4f)
        else -> MaterialTheme.colorScheme.background
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp, max = 80.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.DarkGray)
            .clickable(
                enabled = true,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = filePath.takeUnless{it.isBlank()} ?: stringResource(Res.string.select_yaml_file_placeholder),
            color = contentColor
        )
        Icon(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(24.dp),
            painter = painterResource(Res.drawable.ic_floder),
            tint = contentColor,
            contentDescription = stringResource(Res.string.select_yaml_file_content_desc)
        )

    }
}

fun openFilePicker(
    title: String? = null
): String? {
    val resolvedTitle = title ?: runBlocking { getString(Res.string.select_yaml_file_dialog_title) }
    val window = Frame(resolvedTitle)
    val dialog = FileDialog(window, resolvedTitle, FileDialog.LOAD)
    window.setSize(800, 600)
    window.setLocationRelativeTo(null)
    val allowedExtensions = listOf(".yaml")

    if (allowedExtensions.isNotEmpty()) {
        dialog.setFilenameFilter { _, name ->
            allowedExtensions.any { name.lowercase().endsWith(it) }
        }
    }

    dialog.isVisible = true

    return if (dialog.file != null) {
        File(dialog.directory, dialog.file).absolutePath
    } else {
        null // User cancelled
    }
}