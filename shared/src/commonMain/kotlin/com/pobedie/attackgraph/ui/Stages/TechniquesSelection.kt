package com.pobedie.attackgraph.ui.Stages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.clear_selections_button
import attackgraph.shared.generated.resources.ic_info
import attackgraph.shared.generated.resources.id_description_format
import attackgraph.shared.generated.resources.select_target_button
import attackgraph.shared.generated.resources.select_techniques_title
import attackgraph.shared.generated.resources.start_building_vectors_button
import attackgraph.shared.generated.resources.tactic_description_content_desc
import attackgraph.shared.generated.resources.technique_description_content_desc
import com.pobedie.attackgraph.core.entity.Tactic
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

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .padding(top = 30.dp)
                .padding(horizontal = 30.dp)
                .fillMaxWidth()
        ){
            Text(
                text = stringResource(Res.string.select_techniques_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        val scrollState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        LazyRow(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                // by default to scroll horizontaly you need to use Shift+MouseWheel which is a bad UX in this case
                .onPointerEvent(PointerEventType.Scroll) {
                    val delta = it.changes.first().scrollDelta
                    coroutineScope.launch {
                        scrollState.scrollBy(delta.y * 40f)
                    }
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
                    targetTechnique = state.targetTechnique,
                    onTechniqueClick = {
                        viewModel.selectTechnique(it)
                        if (state.isTargetSelectionInProgress){
                            viewModel.selectTargetTechnique(it)
                        }
                    }
                )
            }

        }

        Row(
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Button(
                onClick = { viewModel.startTargetTechniqueSelection() },
                enabled = !state.isTargetSelectionInProgress
            ) {
                Text(stringResource(Res.string.select_target_button))
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(
                visible = state.isAttackVectorMappingStageAvailable,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row {
                    Button(
                        onClick = { viewModel.clearTechniqueSelectoins() },
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
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.TacticColumn(
    tactic: Tactic,
    isTargetSelectionInProgress: Boolean,
    selectedTechniques: List<String>,
    targetTechnique: String?,
    onTechniqueClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val columnColor = if (isTargetSelectionInProgress) Color(255, 171, 140, 255) else Color.LightGray

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
            .widthIn(min = 100.dp, max = 150.dp)
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
                ) {
                    SelectionContainer {
                        Text(stringResource(Res.string.id_description_format, tactic.id, tactic.description))
                    }
                }
            },
            state = tacticTooltipState,
            onDismissRequest = {tacticShowTooltip = false}
        ) {
            Text(
                modifier = Modifier
                    .padding(8.dp)
                    .height(50.dp)
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
                            onClick = {tacticShowTooltip = true}
                        )
                    ,
                    painter = painterResource(Res.drawable.ic_info),
                    tint = Color.DarkGray.copy(alpha = 0.8f),
                    contentDescription = stringResource(Res.string.tactic_description_content_desc)
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f, false),
            thickness = 3.dp,
            color = MaterialTheme.colorScheme.onBackground
        )
        tactic.techniques.forEachIndexed { index, technique ->
            val techniqueTooltipState = rememberTooltipState(isPersistent = true)
            var techniqueShowTooltip by remember{ mutableStateOf(false) }
            var isTechniqueInfoIconVisible by remember{ mutableStateOf(false) }
            val isTarget = technique.id == targetTechnique

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
                        if (isTarget)
                            Modifier.background(Color(255, 103, 76))
                        else if (selectedTechniques.contains(technique.id))
                            Modifier.background(Color(170, 218, 255, 255))
                        else
                            Modifier
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
                        ) {
                            SelectionContainer {
                                Text(stringResource(Res.string.id_description_format, technique.id, technique.description))
                            }
                        }
                    },
                    state = techniqueTooltipState,
                    onDismissRequest = {techniqueShowTooltip = false}
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                            .alpha(if (isTechniqueInfoIconVisible) 1f else 0f)
                            .onClick(
                                onClick = {techniqueShowTooltip = true}
                            )
                        ,
                        painter = painterResource(Res.drawable.ic_info),
                        tint = Color.DarkGray.copy(alpha = 0.8f),
                        contentDescription = stringResource(Res.string.technique_description_content_desc)
                    )

                }
            }
            if (index != tactic.techniques.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f, false),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

    }
}