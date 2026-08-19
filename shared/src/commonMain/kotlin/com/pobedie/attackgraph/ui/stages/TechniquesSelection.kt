package com.pobedie.attackgraph.ui.stages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.pobedie.attackgraph.ui.theme.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.achieve_label
import attackgraph.shared.generated.resources.automatic_label
import attackgraph.shared.generated.resources.add_host_content_desc
import attackgraph.shared.generated.resources.cancel_selection_button
import attackgraph.shared.generated.resources.clear_selections_button
import attackgraph.shared.generated.resources.delete_host_content_desc
import attackgraph.shared.generated.resources.delete_technique_from_host_content_desc
import attackgraph.shared.generated.resources.description_format
import attackgraph.shared.generated.resources.description_maturity_format
import attackgraph.shared.generated.resources.highest_severity_label
import attackgraph.shared.generated.resources.hosts_title
import attackgraph.shared.generated.resources.ic_info
import attackgraph.shared.generated.resources.ic_shield
import attackgraph.shared.generated.resources.manual_label
import attackgraph.shared.generated.resources.maturity_demonstrated
import attackgraph.shared.generated.resources.maturity_feasible
import attackgraph.shared.generated.resources.maturity_format
import attackgraph.shared.generated.resources.maturity_realized
import attackgraph.shared.generated.resources.maturity_unknown
import attackgraph.shared.generated.resources.mitigation_full_description_format
import attackgraph.shared.generated.resources.next_host_content_desc
import attackgraph.shared.generated.resources.previous_host_content_desc
import attackgraph.shared.generated.resources.select_techniques_title
import attackgraph.shared.generated.resources.set_as_irrelevant
import attackgraph.shared.generated.resources.set_as_relevant
import attackgraph.shared.generated.resources.severity_score_format
import attackgraph.shared.generated.resources.show_mitigation_info_content_desc
import attackgraph.shared.generated.resources.start_building_vectors_button
import attackgraph.shared.generated.resources.starting_point_label
import attackgraph.shared.generated.resources.tactic_description_content_desc
import attackgraph.shared.generated.resources.target_technique_label
import attackgraph.shared.generated.resources.technique_description_content_desc
import com.pobedie.attackgraph.core.entity.Tactic
import com.pobedie.attackgraph.core.entity.Technique
import com.pobedie.attackgraph.core.entity.Mitigation
import com.pobedie.attackgraph.core.entity.TechniqueMaturity
import com.pobedie.attackgraph.ui.TargetGoal
import com.pobedie.attackgraph.ui.RootNodeGoal
import com.pobedie.attackgraph.ui.ViewModel
import com.pobedie.attackgraph.ui.ViewState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TechniqueSelection(
    viewModel: ViewModel,
    state: ViewState
) {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 30.dp)
                    .padding(horizontal = 30.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.select_techniques_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryTextColor
                )
            }

            val scrollState = rememberLazyListState()
            val verticalScrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
            Box(modifier = Modifier.weight(1f)) {
                LazyRow(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        // by default to scroll horizontaly you need to use Shift+MouseWheel which is a bad UX in this case
                        .onPointerEvent(PointerEventType.Scroll) {
                            val delta = it.changes.first().scrollDelta
                            coroutineScope.launch {
                                scrollState.scrollBy(delta.y * 40f)
                            }
                            it.changes.first().consume()
                        },
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 30.dp)
                ) {
                    items(
                        items = state.tactics,
                        key = { tactic -> tactic.id }
                    ) { tactic ->
                        TacticColumn(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            tactic = tactic,
                            selectedTechniques = state.selectedTechniquesId,
                            isTargetSelectionInProgress = state.isTargetSelectionInProgress,
                            isRootSelectionInProgress = state.isRootSelectionInProgress,
                            targetTechniques = state.targetTechniques,
                            rootTechniques = state.rootTechniques,
                            currentHostId = state.hosts[state.currentHostIndex].id,
                            onTechniqueClick = {
                                if (state.isRootSelectionInProgress) {
                                    viewModel.selectRootTechnique(it)
                                } else if (state.isTargetSelectionInProgress) {
                                    viewModel.selectTargetTechnique(it)
                                } else {
                                    viewModel.selectTechnique(it)
                                }
                            }
                        )
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().padding(start = 2.dp),
                    adapter = rememberScrollbarAdapter(verticalScrollState),
                    style = ScrollbarStyle(
                        minimalHeight = 16.dp,
                        thickness = 8.dp,
                        shape = RoundedCornerShape(4.dp),
                        hoverDurationMillis = 300,
                        unhoverColor = PrimaryTextColor.copy(alpha = 0.3f),
                        hoverColor = PrimaryTextColor.copy(alpha = 0.7f)
                    )
                )
            }

            FlowRow(
                modifier = Modifier
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalArrangement = Arrangement.Bottom,
                itemVerticalAlignment = Alignment.Bottom
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.starting_point_label),
                        color = PrimaryTextColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.width(8.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DialogBackground),
                    ) {
                        SelectionOption(
                            text = stringResource(Res.string.automatic_label),
                            isSelected = state.rootNodeGoal == RootNodeGoal.Automatic,
                            onClick = { viewModel.setRootNodeGoal(RootNodeGoal.Automatic) }
                        )
                        SelectionOption(
                            text = stringResource(Res.string.manual_label),
                            isSelected = state.rootNodeGoal == RootNodeGoal.Manual,
                            onClick = { viewModel.setRootNodeGoal(RootNodeGoal.Manual) }
                        )
                    }
                }
                Spacer(Modifier.width(24.dp))
                Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.achieve_label),
                        color = PrimaryTextColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.width(8.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DialogBackground),
                    ) {
                        SelectionOption(
                            text = stringResource(Res.string.highest_severity_label),
                            isSelected = state.targetGoal == TargetGoal.HighestSeverity,
                            onClick = { viewModel.setTargetGoal(TargetGoal.HighestSeverity) }
                        )
                        SelectionOption(
                            text = stringResource(Res.string.target_technique_label),
                            isSelected = state.targetGoal == TargetGoal.Specific,
                            onClick = { viewModel.setTargetGoal(TargetGoal.Specific) }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = state.isRootSelectionInProgress || state.isTargetSelectionInProgress,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(16.dp))
                        Button(
                            onClick = { viewModel.cancelSelectionMode() },
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text(stringResource(Res.string.cancel_selection_button))
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                AnimatedVisibility(
                    visible = state.isAttackVectorMappingStageAvailable,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { viewModel.clearTechniqueSelections() },
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text(stringResource(Res.string.clear_selections_button))
                        }
                        Spacer(Modifier.width(20.dp))
                        Button(
                            onClick = { viewModel.switchToAttackVectorBuildingStage() },
                        ) {
                            Text(stringResource(Res.string.start_building_vectors_button))
                        }
                    }
                }
            }
        }

        // Host zone
        Column(
            modifier = Modifier
                .widthIn(min = 150.dp, max = 400.dp)
                .background(HostZoneBackground)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.hosts_title),
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryTextColor
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { viewModel.previousHost() },
                    enabled = state.currentHostIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(Res.string.previous_host_content_desc),
                        tint = if (state.currentHostIndex > 0) PrimaryTextColor else DisabledContentColor
                    )
                }

                Text(
                    text = "${state.currentHostIndex + 1} / ${state.hosts.size}",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = PrimaryTextColor
                )

                if (state.currentHostIndex == state.hosts.size - 1) {
                    IconButton(onClick = { viewModel.addHost() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(Res.string.add_host_content_desc),
                            tint = PrimaryTextColor
                        )
                    }
                } else {
                    IconButton(onClick = { viewModel.nextHost() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(Res.string.next_host_content_desc),
                            tint = PrimaryTextColor
                        )
                    }
                }

                if (state.hosts.size > 1) {
                    IconButton(onClick = {
                        state.hosts.getOrNull(state.currentHostIndex)?.let {
                            viewModel.deleteHost(it.id)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.delete_host_content_desc),
                            tint = DeleteIconColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val currentHost = state.hosts.getOrNull(state.currentHostIndex)
            if (currentHost != null) {
                HostItem(
                    name = currentHost.name,
                    techniques = currentHost.techniques,
                    mitigations = state.mitigations,
                    onNameChange = { viewModel.updateHostName(currentHost.id, it) },
                    onSeverityScoreSet = { techId, score ->
                        viewModel.updateTechniqueSeverityScore(currentHost.id, techId, score)
                    },
                    onTechniqueDelete = { techId ->
                        viewModel.removeTechniqueFromHost(currentHost.id, techId)
                    },
                    onToggleMitigationRelevance = {
                        viewModel.toggleMitigationRelevance(it)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun TacticColumn(
    tactic: Tactic,
    isTargetSelectionInProgress: Boolean,
    isRootSelectionInProgress: Boolean,
    selectedTechniques: List<String>,
    targetTechniques: List<Pair<String, String>>,
    rootTechniques: List<Pair<String, String>>,
    currentHostId: String,
    onTechniqueClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val columnColor = if (isTargetSelectionInProgress) {
        SelectionInProgressBackground
    } else if (isRootSelectionInProgress) {
        StatusSuccess.copy(alpha = 0.2f)
    } else {
        DialogBackground
    }

    val tacticTooltipState = rememberTooltipState(isPersistent = true)
    var tacticShowTooltip by remember{ mutableStateOf(false) }
    var isTacticInfoIconVisible by remember{ mutableStateOf(false) }

    LaunchedEffect(tacticShowTooltip) {
        if (tacticShowTooltip) {
            tacticTooltipState.show()
        } else {
            tacticTooltipState.dismiss()
        }
    }

    Column(
        modifier = modifier
            .widthIn(min = 100.dp, max = 200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(columnColor)
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 2.dp
            ),
            enableUserInput = false,
            tooltip = {
                PlainTooltip(
                    maxWidth = 400.dp,
                    contentColor = TooltipContentColor,
                    containerColor = TooltipBackgroundColor
                ) {
                    SelectionContainer {
                        Text(stringResource(Res.string.description_format, tactic.id, tactic.description))
                    }
                }
            },
            state = tacticTooltipState,
            onDismissRequest = { tacticShowTooltip = false }
        ) {
            Text(
                modifier = Modifier
                    .padding(8.dp)
                    .height(30.dp)
                    .onPointerEvent(PointerEventType.Enter) {
                        isTacticInfoIconVisible = true
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        isTacticInfoIconVisible = false
                    },
                text = tactic.name,
                style = MaterialTheme.typography.titleSmallEmphasized,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .onPointerEvent(PointerEventType.Enter) {
                        isTacticInfoIconVisible = true
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        isTacticInfoIconVisible = false
                    },
                contentAlignment = Alignment.TopEnd
            ) {
                Icon(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(16.dp)
                        .fillMaxWidth()
                        .alpha(if (isTacticInfoIconVisible) 1f else 0f)
                        .onClick(
                            onClick = { tacticShowTooltip = true }
                        ),
                    painter = painterResource(Res.drawable.ic_info),
                    tint = InfoIconSecondaryDarkColor,
                    contentDescription = stringResource(Res.string.tactic_description_content_desc)
                )
            }
        }
        HorizontalDivider(
            thickness = 3.dp,
            color = BackgroundColor
        )

        tactic.techniques.forEachIndexed { index, technique ->
            val techniqueTooltipState = rememberTooltipState(isPersistent = true)
            var techniqueShowTooltip by remember { mutableStateOf(false) }
            var isTechniqueInfoIconVisible by remember { mutableStateOf(false) }

            LaunchedEffect(techniqueShowTooltip) {
                if (techniqueShowTooltip) {
                    techniqueTooltipState.show()
                } else {
                    techniqueTooltipState.dismiss()
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .onPointerEvent(PointerEventType.Enter) {
                        isTechniqueInfoIconVisible = true
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        isTechniqueInfoIconVisible = false
                    }
                    .then(
                        run {
                            val isTargetForCurrent = targetTechniques.any { it.first == technique.id && it.second == currentHostId }
                            val isRootForCurrent = rootTechniques.any { it.first == technique.id && it.second == currentHostId }
                            val isTargetAnywhere = targetTechniques.any { it.first == technique.id }
                            val isRootAnywhere = rootTechniques.any { it.first == technique.id }

                            when {
                                isTargetForCurrent -> Modifier.background(NodeBorderTarget)
                                isTargetAnywhere -> Modifier.background(NodeBorderTarget.copy(alpha = 0.5f))
                                isRootForCurrent -> Modifier.background(RootTechniqueBackground)
                                isRootAnywhere -> Modifier.background(RootTechniqueBackground.copy(alpha = 0.5f))
                                selectedTechniques.contains(technique.id) -> Modifier.background(SelectedTechniqueBackground)
                                else -> Modifier
                            }
                        }
                    ),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    modifier = Modifier
                        .clickable(
                            onClick = { onTechniqueClick(technique.id) }
                        )
                        .fillMaxWidth()
                        .padding(2.dp)
                        .widthIn(max = 150.dp),
                    text = technique.name
                )

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        positioning = TooltipAnchorPosition.Above,
                        spacingBetweenTooltipAndAnchor = 0.dp
                    ),
                    enableUserInput = false,
                    tooltip = {
                        PlainTooltip(
                            maxWidth = 400.dp,
                            contentColor = TooltipContentColor,
                            containerColor = TooltipBackgroundColor
                        ) {
                            SelectionContainer {
                                val maturityString = stringResource(
                                    when (technique.maturity) {
                                        TechniqueMaturity.Demonstrated -> Res.string.maturity_demonstrated
                                        TechniqueMaturity.Feasible -> Res.string.maturity_feasible
                                        TechniqueMaturity.Realized -> Res.string.maturity_realized
                                        TechniqueMaturity.Unknown -> Res.string.maturity_unknown
                                    }
                                )
                                Text(
                                    stringResource(
                                        Res.string.description_maturity_format,
                                        technique.id,
                                        maturityString,
                                        technique.description
                                    )
                                )
                            }
                        }
                    },
                    state = techniqueTooltipState,
                    onDismissRequest = { techniqueShowTooltip = false }
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                            .alpha(if (isTechniqueInfoIconVisible) 1f else 0f)
                            .onClick(
                                onClick = { techniqueShowTooltip = true }
                            ),
                        painter = painterResource(Res.drawable.ic_info),
                        tint = InfoIconSecondaryDarkColor,
                        contentDescription = stringResource(Res.string.technique_description_content_desc)
                    )

                }
            }
            if (index != tactic.techniques.size - 1) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = BackgroundColor
                )
            }
        }
    }
}

/**
* @param onSeverityScoreSet returns Technique id and its severity score
*/
@Composable
private fun HostItem(
    name: String,
    techniques: List<Technique>,
    mitigations: List<Mitigation>,
    onNameChange: (String) -> Unit,
    onSeverityScoreSet: (String, Int) -> Unit,
    onTechniqueDelete: (String) -> Unit,
    onToggleMitigationRelevance: (String) -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HostItemSelectionBackground)
    ) {
        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = PrimaryTextColor,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(12.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(techniques) { _technique ->
                TechniqueInHost(
                    technique = _technique,
                    mitigations = mitigations.filter { it.targetTechnique == _technique.id },
                    onSeverityScoreSet = { onSeverityScoreSet(_technique.id, it) },
                    onDelete = { onTechniqueDelete(_technique.id) },
                    onToggleMitigationRelevance = onToggleMitigationRelevance
                )
            }
        }
    }
}

@Composable
private fun LazyItemScope.TechniqueInHost(
    technique: Technique,
    mitigations: List<Mitigation>,
    onSeverityScoreSet: (Int) -> Unit,
    onDelete: () -> Unit,
    onToggleMitigationRelevance: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TechniqueInHostBackground)
            .padding(12.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = technique.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryTextColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.delete_technique_from_host_content_desc),
                    tint = DeleteIconColor
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val maturityString = stringResource(
            when (technique.maturity) {
                TechniqueMaturity.Demonstrated -> Res.string.maturity_demonstrated
                TechniqueMaturity.Feasible -> Res.string.maturity_feasible
                TechniqueMaturity.Realized -> Res.string.maturity_realized
                TechniqueMaturity.Unknown -> Res.string.maturity_unknown
            }
        )
        Text(
            text = stringResource(Res.string.maturity_format, maturityString),
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryTextColor
        )
        Text(
            text = stringResource(Res.string.severity_score_format, technique.severityScore),
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryTextColor
        )

        Spacer(Modifier.height(4.dp))

        Slider(
            value = technique.severityScore.toFloat(),
            onValueChange = { onSeverityScoreSet(it.roundToInt()) },
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.fillMaxWidth()
        )

        if (mitigations.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                mitigations.forEach { mitigation ->
                    val mitigationTooltipState = rememberTooltipState(isPersistent = true)
                    var mitigationShowTooltip by remember { mutableStateOf("") }
                    LaunchedEffect(mitigationShowTooltip) {
                        if (mitigationShowTooltip == mitigation.id) {
                            mitigationTooltipState.show()
                        } else {
                            mitigationTooltipState.dismiss()
                        }
                    }

                    val backgroundColor = if (mitigation.isRelevant) {
                        MitigationRelevantBackground
                    } else {
                        MitigationIrrelevant
                    }
                    val iconColor = if (mitigation.isRelevant) {
                        MitigationRelevantIcon
                    } else {
                        OnTertiaryColor
                    }

                    TooltipBox(
                        modifier = Modifier
                            .padding(end = 4.dp, bottom = 4.dp)
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(backgroundColor)
                            .clickable(
                                onClick = {
                                    mitigationShowTooltip = mitigation.id
                                }
                            ),
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            positioning = TooltipAnchorPosition.Above,
                            spacingBetweenTooltipAndAnchor = 0.dp
                        ),
                        enableUserInput = false,
                        tooltip = {
                            PlainTooltip(
                                maxWidth = 400.dp,
                                contentColor = TooltipContentColor,
                                containerColor = TooltipBackgroundColor
                            ) {
                                val mitigationDescription = stringResource(
                                    Res.string.mitigation_full_description_format,
                                    mitigation.id,
                                    mitigation.relationshipDescription,
                                    mitigation.mitigationDescription
                                )
                                SelectionContainer {
                                    Column {
                                        Text(mitigationDescription)
                                        Button(
                                            onClick = { onToggleMitigationRelevance(mitigation.id) },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (mitigation.isRelevant)
                                                    stringResource(Res.string.set_as_irrelevant)
                                                else
                                                    stringResource(Res.string.set_as_relevant)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        state = mitigationTooltipState,
                        onDismissRequest = { mitigationShowTooltip = "" }
                    ) {
                        Icon(
                            modifier = Modifier
                                .scale(0.8f),
                            painter = painterResource(Res.drawable.ic_shield),
                            tint = iconColor,
                            contentDescription = stringResource(Res.string.show_mitigation_info_content_desc)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else LabelColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
