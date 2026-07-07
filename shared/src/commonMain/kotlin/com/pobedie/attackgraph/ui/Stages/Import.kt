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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.pobedie.attackgraph.ui.theme.*
import androidx.compose.ui.unit.dp
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.connect_button
import attackgraph.shared.generated.resources.connecting_status
import attackgraph.shared.generated.resources.connection_failed
import attackgraph.shared.generated.resources.connection_success
import attackgraph.shared.generated.resources.ic_floder
import attackgraph.shared.generated.resources.import_button
import attackgraph.shared.generated.resources.import_from_file_title
import attackgraph.shared.generated.resources.llm_api_key_label
import attackgraph.shared.generated.resources.llm_model_label
import attackgraph.shared.generated.resources.llm_settings_title
import attackgraph.shared.generated.resources.llm_url_label
import attackgraph.shared.generated.resources.or_use_included_data
import attackgraph.shared.generated.resources.select_yaml_file_content_desc
import attackgraph.shared.generated.resources.select_yaml_file_dialog_title
import attackgraph.shared.generated.resources.select_yaml_file_placeholder
import attackgraph.shared.generated.resources.use_included_data_checkbox
import com.pobedie.attackgraph.ui.Language
import com.pobedie.attackgraph.ui.LlmConnectionStatus
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height


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
                .background(DialogBackground)
                .widthIn(max = 600.dp)
                .heightIn(max = 800.dp)
                .padding(bottom = 16.dp)
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
                    color = ErrorColor,
                    modifier = Modifier.padding(horizontal = 22.dp)
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
                    .padding(horizontal = 22.dp, vertical = 8.dp)
                    .align(Alignment.End),
                onClick = { viewModel.importAtlasData() },
                enabled = isImportAvailable
            ) {
                Text(stringResource(Res.string.import_button))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                text = stringResource(Res.string.llm_settings_title),
                style = MaterialTheme.typography.titleLarge
            )

            LlmSettingsField(
                label = stringResource(Res.string.llm_url_label),
                value = state.llmUrl,
                onValueChange = { viewModel.updateLlmUrl(it) },
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
            )

            LlmSettingsField(
                label = stringResource(Res.string.llm_api_key_label),
                value = state.llmApiKey,
                onValueChange = { viewModel.updateLlmApiKey(it) },
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
            )

            LlmSettingsField(
                label = stringResource(Res.string.llm_model_label),
                value = state.llmModel,
                onValueChange = { viewModel.updateLlmModel(it) },
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusText = when (state.llmConnectionStatus) {
                    LlmConnectionStatus.None -> ""
                    LlmConnectionStatus.Connecting -> stringResource(Res.string.connecting_status)
                    LlmConnectionStatus.Connected -> stringResource(Res.string.connection_success)
                    LlmConnectionStatus.Failed -> stringResource(Res.string.connection_failed)
                }
                val statusColor = when (state.llmConnectionStatus) {
                    LlmConnectionStatus.Connected -> EdgeOptimal
                    LlmConnectionStatus.Failed -> ErrorColor
                    else -> PrimaryTextColor
                }
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = { viewModel.checkLlmConnection() },
                    enabled = state.llmUrl.isNotBlank() && state.llmConnectionStatus != LlmConnectionStatus.Connecting
                ) {
                    Text(stringResource(Res.string.connect_button))
                }
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
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryButtonBackground)
            ) {
                val currentLangLabel = when (state.language) {
                    Language.English -> "ENGLISH"
                    Language.Russian -> "РУССКИЙ"
                }
                Text(
                    text = currentLangLabel,
                    color = PrimaryTextColor
                )
            }
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier.background(DropdownMenuBackground)
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
                                color = PrimaryTextColor
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
        isFileError -> OnErrorColor
        !isEnabled -> BackgroundColor.copy(alpha =  0.4f)
        else -> OnBackgroundColor
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp, max = 80.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(FileSelectionBackground)
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

@Composable
private fun LlmSettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FileSelectionBackground,
            unfocusedContainerColor = FileSelectionBackground,
            focusedTextColor = PrimaryTextColor,
            unfocusedTextColor = PrimaryTextColor,
            focusedLabelColor = TacticLabelColor,
            unfocusedLabelColor = TacticLabelColor,
            focusedBorderColor = SelectedBorderColor,
            unfocusedBorderColor = TransparentColor
        )
    )
}