package com.pobedie.attackgraph.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AttackGraphColors(
    val hostContainerBackground: Color,
    val hostContainerBorder: Color,
    val edgeOptimal: Color,
    val edgeProbable: Color,
    val edgeDefault: Color,
    val deselectHint: Color,
    val nodeBorderTarget: Color,
    val mitigationIrrelevant: Color,
    val edgeLabelEnabled: Color,
    val infoIconColor: Color,
    val mainBackground: Color,
    val consoleTextColor: Color,
    val dialogBackground: Color,
    val dropdownMenuBackground: Color,
    val secondaryButtonBackground: Color,
    val disabledInputField: Color,
    val inputFieldBackground: Color,
    val stageBarBackground: Color,
    val disabledContentColor: Color,
    val primaryTextColor: Color,
    val selectedBorderColor: Color,
    val transparentColor: Color,
    val nodeTextColor: Color,
    val hostZoneBackground: Color,
    val deleteIconColor: Color,
    val selectionInProgressBackground: Color,
    val infoIconSecondaryColor: Color,
    val infoIconSecondaryDarkColor: Color,
    val selectedTechniqueBackground: Color,
    val rootTechniqueBackground: Color,
    val hostItemSelectionBackground: Color,
    val techniqueInHostBackground: Color,
    val secondaryTextColor: Color,
    val labelColor: Color,
    val disabledLabelColor: Color,
    val focusedLabelColor: Color,
    val unfocusedLabelColor: Color,
    val statusSuccess: Color,
    val statusFail: Color,
    val nodeBorderRoot: Color,
    val errorColor: Color,
    val onErrorColor: Color,
    val errorContainerColor: Color,
    val onErrorContainerColor: Color,
    val onTertiaryColor: Color,
    val secondaryContainerColor: Color,
    val onPrimaryColor: Color,
    val surfaceColor: Color,
    val onBackgroundColor: Color,
    val backgroundColor: Color,
    val onSecondaryContainerColor: Color,

    // Firewall mapping specific
    val firewallHostBackground: Color,
    val firewallHostBorder: Color,
    val firewallHostHeaderBorder: Color,
    val firewallHostHeaderText: Color,
    val firewallTechniqueText: Color,
    val firewallTechniqueSelectedText: Color,
    val firewallTechniqueSelectedBackground: Color
)

val DarkAppColors = AttackGraphColors(
    hostContainerBackground = Color(0x96323232),
    hostContainerBorder = Color(0x66FFFFFF),
    edgeOptimal = Color(0xFF81C709),
    edgeProbable = Color(0xFFC9AE1D),
    edgeDefault = Color(0xFFA8A8A8),
    deselectHint = Color(0x80FFFFFF),
    nodeBorderTarget = Color(0xFFFF674C),
    mitigationIrrelevant = Color(0xFFD3D3D3),
    edgeLabelEnabled = Color(0xA0D0D0D0),
    infoIconColor = Color(0x99FFFFFF),
    mainBackground = Color(0xFF141414),
    consoleTextColor = Color(0xFFD3D3D3),
    dialogBackground = Color(0xFFD3D3D3),
    dropdownMenuBackground = Color(0xFF808080),
    secondaryButtonBackground = Color(0xFF444444),
    inputFieldBackground = Color(0xFF767676),
    disabledInputField = Color(0xFF212121),
    stageBarBackground = Color(0xFF444444),
    disabledContentColor = Color(0xFF808080),
    primaryTextColor = Color(0xFFFFFFFF),
    selectedBorderColor = Color(0xFFFFFFFF),
    transparentColor = Color(0x00000000),
    nodeTextColor = Color(0xFFFFFFFF),
    hostZoneBackground = Color(0xFF232323),
    deleteIconColor = Color(0xFFFF6464),
    selectionInProgressBackground = Color(0xFFFFAB8C),
    infoIconSecondaryColor = Color(0xCC444444),
    infoIconSecondaryDarkColor = Color(0xCC444444),
    selectedTechniqueBackground = Color(0xFFAADAFF),
    rootTechniqueBackground = Color(0xFFC8E6C9),
    hostItemSelectionBackground = Color(0xFF323232),
    techniqueInHostBackground = Color(0xFF414141),
    secondaryTextColor = Color(0xFFD3D3D3),
    labelColor = Color(0xFF000000),
    disabledLabelColor = Color(0xFF808080),
    focusedLabelColor = Color(0xFF000000),
    unfocusedLabelColor = Color(0xFF363636),
    statusSuccess = Color(0xFF527612),
    statusFail = Color(0xFF862B0E),
    nodeBorderRoot = Color(0xFF81C709),
    errorColor = Color(0xFFF2B8B5),
    onErrorColor = Color(0xFF601410),
    errorContainerColor = Color(0xFF8C1D18),
    onErrorContainerColor = Color(0xFFF9DEDC),
    onTertiaryColor = Color(0xFF492532),
    secondaryContainerColor = Color(0xFF4A4458),
    onPrimaryColor = Color(0xFFFFFFFF),
    surfaceColor = Color(0xFF1C1B1F),
    onBackgroundColor = Color(0xFFE6E1E5),
    backgroundColor = Color(0xFF141414),
    onSecondaryContainerColor = Color(0xFFDEE4F8),

    firewallHostBackground = Color.White,
    firewallHostBorder = Color(0xFFCCCCCC),
    firewallHostHeaderBorder = Color(0xFFDDDDDD),
    firewallHostHeaderText = Color.Black,
    firewallTechniqueText = Color(0xFF444444),
    firewallTechniqueSelectedText = Color(0xFF1A3D63),
    firewallTechniqueSelectedBackground = Color(0x1A1A3D63)
)

val LightAppColors = AttackGraphColors(
    hostContainerBackground = Color(0x96C8C8C8),
    hostContainerBorder = Color(0x66000000),
    edgeOptimal = Color(0xFF4A7200),
    edgeProbable = Color(0xFFA58D00),
    edgeDefault = Color(0xFF2F2F2F),
    deselectHint = Color(0x80000000),
    nodeBorderTarget = Color(0xFFFF674C),
    mitigationIrrelevant = Color(0xFF444444),
    edgeLabelEnabled = Color(0xA0323232),
    infoIconColor = Color(0x99FFFFFF),
    mainBackground = Color(0xFFCFCDCA),
    consoleTextColor = Color(0xFF000000),
    dialogBackground = Color(0xFFFFFFFF),
    dropdownMenuBackground = Color(0xFFFFFFFF),
    secondaryButtonBackground = Color(0xFFFFFFFF),
    inputFieldBackground = Color(0xFFE0E0E0),
    disabledInputField = Color(0xFF878787),
    stageBarBackground = Color(0xFFD3D3D3),
    disabledContentColor = Color(0xFF808080),
    primaryTextColor = Color(0xFF000000),
    selectedBorderColor = Color(0xFFEFF9FF),
    transparentColor = Color(0x00000000),
    nodeTextColor = Color(0xFFFFFFFF),
    hostZoneBackground = Color(0xFFD3D3D3),
    deleteIconColor = Color(0xFFFF6464),
    selectionInProgressBackground = Color(0xFFFFAB8C),
    infoIconSecondaryColor = Color(0xCCB3B3B3),
    infoIconSecondaryDarkColor = Color(0xCC282828),
    selectedTechniqueBackground = Color(0xFFAADAFF),
    rootTechniqueBackground = Color(0xFFA5D6A7),
    hostItemSelectionBackground = Color(0xFFFFFFFF),
    techniqueInHostBackground = Color(0xFFF5F5F5),
    secondaryTextColor = Color(0xFF444444),
    labelColor = Color(0xFF000000),
    disabledLabelColor = Color(0xFF808080),
    focusedLabelColor = Color(0xFF000000),
    unfocusedLabelColor = Color(0xFF535353),
    statusSuccess = Color(0xFF527612),
    statusFail = Color(0xFF862B0E),
    nodeBorderRoot = Color(0xFF689F38),
    errorColor = Color(0xFFB3261E),
    onErrorColor = Color(0xFFFFFFFF),
    errorContainerColor = Color(0xFFF9DEDC),
    onErrorContainerColor = Color(0xFF410E0B),
    onTertiaryColor = Color(0xFFFFFFFF),
    secondaryContainerColor = Color(0xFFB2BAC9),
    onPrimaryColor = Color(0xFFFFFFFF),
    surfaceColor = Color(0xFFFFFFFF),
    onBackgroundColor = Color(0xFF000000),
    backgroundColor = Color(0xFFCFCDCA),
    onSecondaryContainerColor = Color(0xFF1D192B),

    firewallHostBackground = Color.White,
    firewallHostBorder = Color(0xFFCCCCCC),
    firewallHostHeaderBorder = Color(0xFFDDDDDD),
    firewallHostHeaderText = Color.Black,
    firewallTechniqueText = Color(0xFF444444),
    firewallTechniqueSelectedText = Color(0xFF1A3D63),
    firewallTechniqueSelectedBackground = Color(0x1A1A3D63)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

@Composable
fun AttackGraphTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF1A3D63),
            secondary = Color(0xFFCCC2DC),
            tertiary = Color(0xFFEFB8C8),
            background = appColors.backgroundColor,
            surface = appColors.surfaceColor,
            error = appColors.errorColor,
            onPrimary = appColors.onPrimaryColor,
            onSecondary = Color(0xFF332D41),
            onTertiary = appColors.onTertiaryColor,
            onBackground = appColors.onBackgroundColor,
            onSurface = appColors.onBackgroundColor,
            onError = appColors.onErrorColor,
            errorContainer = appColors.errorContainerColor,
            onErrorContainer = appColors.onErrorContainerColor,
            secondaryContainer = appColors.secondaryContainerColor,
            onSecondaryContainer = appColors.onSecondaryContainerColor
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF626D80),
            secondary = Color(0xFF625B71),
            tertiary = Color(0xFF7D5260),
            background = appColors.backgroundColor,
            surface = appColors.surfaceColor,
            error = appColors.errorColor,
            onPrimary = appColors.onPrimaryColor,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black,
            onError = appColors.onErrorColor,
            errorContainer = appColors.errorContainerColor,
            onErrorContainer = appColors.onErrorContainerColor,
            secondaryContainer = appColors.secondaryContainerColor,
            onSecondaryContainer = appColors.onSecondaryContainerColor
        )
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

val HostContainerBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.hostContainerBackground
val HostContainerBorder: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.hostContainerBorder
val EdgeOptimal: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.edgeOptimal
val EdgeProbable: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.edgeProbable
val EdgeDefault: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.edgeDefault
val DeselectHint: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.deselectHint
val NodeBorderTarget: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.nodeBorderTarget
val NodeBorderRoot: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.nodeBorderRoot
val MitigationRelevantBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.edgeOptimal
val MitigationRelevantIcon: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.infoIconColor
val MitigationIrrelevant: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.mitigationIrrelevant
val EdgeLabelEnabled: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.edgeLabelEnabled
val InfoIconColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.infoIconColor
val MainBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.mainBackground
val ConsoleTextColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.consoleTextColor
val DialogBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.dialogBackground
val DropdownMenuBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.dropdownMenuBackground
val SecondaryButtonBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.secondaryButtonBackground
val InputFieldBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.inputFieldBackground
val InputFieldDisabledText: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.disabledInputField
val StageBarBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.stageBarBackground
val DisabledContentColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.disabledContentColor
val PrimaryTextColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryTextColor
val SelectedBorderColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.selectedBorderColor
val TransparentColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.transparentColor

val NodeTextColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.nodeTextColor

val HostZoneBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.hostZoneBackground
val DeleteIconColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.deleteIconColor
val SelectionInProgressBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.selectionInProgressBackground
val InfoIconSecondaryColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.infoIconSecondaryColor
val InfoIconSecondaryDarkColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.infoIconSecondaryDarkColor
val SelectedTechniqueBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.selectedTechniqueBackground
val RootTechniqueBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.rootTechniqueBackground
val HostItemSelectionBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.hostItemSelectionBackground
val TechniqueInHostBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.techniqueInHostBackground
val SecondaryTextColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.secondaryTextColor

val LabelColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.labelColor
val DisabledLabelColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.disabledLabelColor
val FocusedLabelColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.focusedLabelColor
val UnfocusedLabelColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.unfocusedLabelColor

val TooltipBackgroundColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceColor
val TooltipContentColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onBackgroundColor

val StatusSuccess: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.statusSuccess
val StatusFail: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.statusFail

// Theme colors
val ErrorColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.errorColor
val OnErrorColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onErrorColor
val ErrorContainerColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.errorContainerColor
val OnErrorContainerColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onErrorContainerColor
val OnTertiaryColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onTertiaryColor
val SecondaryContainerColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.secondaryContainerColor
val OnPrimaryColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onPrimaryColor
val SurfaceColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceColor
val OnBackgroundColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onBackgroundColor
val BackgroundColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.backgroundColor
val OnSecondaryContainerColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onSecondaryContainerColor

// Firewall mapping specific
val FirewallHostBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.firewallHostBackground
val FirewallHostBorder: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.firewallHostBorder
val FirewallHostHeaderBorder: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.firewallHostHeaderBorder
val FirewallHostHeaderText: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.firewallHostHeaderText
val FirewallTechniqueText: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.firewallTechniqueText
val FirewallTechniqueSelectedText: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.firewallTechniqueSelectedText
val FirewallTechniqueSelectedBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.firewallTechniqueSelectedBackground
