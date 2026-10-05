package com.cloudlink.app.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppThemeType(val displayName: String) {
    DARK("Modern Dark"),
    AMOLED("AMOLED Dark"),
    LIGHT("Classic Light"),
    CYBERPUNK("Cyberpunk Neon"),
    NORD("Nordic Frost"),
    DRACULA("Dracula"),
    CATPPUCCIN("Catppuccin Latte/Mocha"),
    TOKYO_NIGHT("Tokyo Night"),
    GRUVBOX("Gruvbox Retro")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorBottomSheet(
    currentTheme: AppThemeType,
    onThemeSelected: (AppThemeType) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.paddingMedium)
        ) {
            Text(
                text = "Choose a theme",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Colors update immediately across the application.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Dimensions.paddingMedium))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AppThemeType.entries) { theme ->
                    val preview = remember(theme) { ThemeSelector.getColorScheme(theme) }
                    val selected = theme == currentTheme
                    Surface(
                        onClick = { onThemeSelected(theme) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf(preview.primary, preview.secondary, preview.surface).forEach { color ->
                                    Surface(
                                        modifier = Modifier.size(18.dp),
                                        shape = CircleShape,
                                        color = color,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = preview.outlineVariant
                                        )
                                    ) {}
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(selected = selected, onClick = null)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private data class CloudPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surfaceLowest: Color,
    val surfaceLow: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val surfaceBright: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val isLight: Boolean = false
)

private fun CloudPalette.toColorScheme(): ColorScheme {
    val base = if (isLight) lightColorScheme() else darkColorScheme()
    val error = if (isLight) Color(0xFFBA1A1A) else Color(0xFFFF6B6B)
    val onError = if (isLight) Color.White else Color(0xFF3B0710)
    val errorContainer = if (isLight) Color(0xFFFFDAD6) else Color(0xFF5B2028)
    val onErrorContainer = if (isLight) Color(0xFF410002) else Color(0xFFFFDAD9)
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = primary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = surfaceHigh,
        onSecondaryContainer = onSurface,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = surfaceHigh,
        onTertiaryContainer = onSurface,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceHigh,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        inverseSurface = onSurface,
        inverseOnSurface = background,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = Color.Black,
        surfaceBright = surfaceBright,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = surfaceHighest,
        surfaceContainerLow = surfaceLow,
        surfaceContainerLowest = surfaceLowest,
        surfaceDim = background
    )
}

object ThemeSelector {
    private val palettes = mapOf(
        AppThemeType.DARK to CloudPalette(
            primary = Color(0xFF67A4FF), onPrimary = Color(0xFF071A34),
            primaryContainer = Color(0xFF173A64), onPrimaryContainer = Color(0xFFD7E8FF),
            secondary = Color(0xFF5CC8F5), onSecondary = Color(0xFF04202D),
            tertiary = Color(0xFFAFA1FF), onTertiary = Color(0xFF1C1544),
            background = Color(0xFF090F1B), onBackground = Color(0xFFE7EDF6),
            surfaceLowest = Color(0xFF070B13), surfaceLow = Color(0xFF0D1422),
            surface = Color(0xFF121B2A), surfaceHigh = Color(0xFF192536),
            surfaceHighest = Color(0xFF223044), surfaceBright = Color(0xFF2B3A4F),
            onSurface = Color(0xFFE7EDF6), onSurfaceVariant = Color(0xFFAAB7C9),
            outline = Color(0xFF6F7F94), outlineVariant = Color(0xFF344256)
        ),
        AppThemeType.AMOLED to CloudPalette(
            primary = Color(0xFF56D8FF), onPrimary = Color(0xFF001F2A),
            primaryContainer = Color(0xFF073845), onPrimaryContainer = Color(0xFFB9EDFF),
            secondary = Color(0xFF52E0A4), onSecondary = Color(0xFF002C1E),
            tertiary = Color(0xFFFFB86B), onTertiary = Color(0xFF341900),
            background = Color.Black, onBackground = Color(0xFFF3F5F7),
            surfaceLowest = Color.Black, surfaceLow = Color(0xFF050505),
            surface = Color(0xFF0A0A0A), surfaceHigh = Color(0xFF121212),
            surfaceHighest = Color(0xFF1A1A1A), surfaceBright = Color(0xFF242424),
            onSurface = Color(0xFFF3F5F7), onSurfaceVariant = Color(0xFFB8BEC6),
            outline = Color(0xFF72777D), outlineVariant = Color(0xFF303438)
        ),
        AppThemeType.LIGHT to CloudPalette(
            primary = Color(0xFF1559C5), onPrimary = Color.White,
            primaryContainer = Color(0xFFD8E5FF), onPrimaryContainer = Color(0xFF061B40),
            secondary = Color(0xFF006C8C), onSecondary = Color.White,
            tertiary = Color(0xFF6547A5), onTertiary = Color.White,
            background = Color(0xFFF6F8FC), onBackground = Color(0xFF18202C),
            surfaceLowest = Color.White, surfaceLow = Color(0xFFF1F4F9),
            surface = Color(0xFFEBEFF5), surfaceHigh = Color(0xFFE3E8F0),
            surfaceHighest = Color(0xFFDCE2EB), surfaceBright = Color.White,
            onSurface = Color(0xFF18202C), onSurfaceVariant = Color(0xFF526071),
            outline = Color(0xFF738095), outlineVariant = Color(0xFFC2CBD8), isLight = true
        ),
        AppThemeType.CYBERPUNK to CloudPalette(
            primary = Color(0xFFFF5AF1), onPrimary = Color(0xFF31002E),
            primaryContainer = Color(0xFF572052), onPrimaryContainer = Color(0xFFFFD7F7),
            secondary = Color(0xFF4DE8F2), onSecondary = Color(0xFF002024),
            tertiary = Color(0xFFFFE566), onTertiary = Color(0xFF292300),
            background = Color(0xFF100818), onBackground = Color(0xFFF9E7FF),
            surfaceLowest = Color(0xFF0A050F), surfaceLow = Color(0xFF160C20),
            surface = Color(0xFF1D1129), surfaceHigh = Color(0xFF281735),
            surfaceHighest = Color(0xFF342043), surfaceBright = Color(0xFF432A54),
            onSurface = Color(0xFFF9E7FF), onSurfaceVariant = Color(0xFFCDB6D5),
            outline = Color(0xFF8F7997), outlineVariant = Color(0xFF493751)
        ),
        AppThemeType.NORD to CloudPalette(
            primary = Color(0xFF88C0D0), onPrimary = Color(0xFF17252B),
            primaryContainer = Color(0xFF355D68), onPrimaryContainer = Color(0xFFD9F2F8),
            secondary = Color(0xFF8FBCBB), onSecondary = Color(0xFF152624),
            tertiary = Color(0xFFEBCB8B), onTertiary = Color(0xFF2A210F),
            background = Color(0xFF242A35), onBackground = Color(0xFFECEFF4),
            surfaceLowest = Color(0xFF20252F), surfaceLow = Color(0xFF2A303C),
            surface = Color(0xFF303744), surfaceHigh = Color(0xFF3B4352),
            surfaceHighest = Color(0xFF465063), surfaceBright = Color(0xFF535E72),
            onSurface = Color(0xFFECEFF4), onSurfaceVariant = Color(0xFFBEC7D5),
            outline = Color(0xFF7C8799), outlineVariant = Color(0xFF4A5363)
        ),
        AppThemeType.DRACULA to CloudPalette(
            primary = Color(0xFFBD93F9), onPrimary = Color(0xFF241337),
            primaryContainer = Color(0xFF4B3569), onPrimaryContainer = Color(0xFFEBDFFF),
            secondary = Color(0xFF50FA7B), onSecondary = Color(0xFF07290F),
            tertiary = Color(0xFFFF79C6), onTertiary = Color(0xFF351126),
            background = Color(0xFF20212B), onBackground = Color(0xFFF8F8F2),
            surfaceLowest = Color(0xFF1B1C25), surfaceLow = Color(0xFF282A36),
            surface = Color(0xFF30323F), surfaceHigh = Color(0xFF3A3D4B),
            surfaceHighest = Color(0xFF454958), surfaceBright = Color(0xFF515667),
            onSurface = Color(0xFFF8F8F2), onSurfaceVariant = Color(0xFFC5C5CC),
            outline = Color(0xFF858795), outlineVariant = Color(0xFF4D5060)
        ),
        AppThemeType.CATPPUCCIN to CloudPalette(
            primary = Color(0xFFCBA6F7), onPrimary = Color(0xFF28153E),
            primaryContainer = Color(0xFF49335F), onPrimaryContainer = Color(0xFFEEDFFF),
            secondary = Color(0xFF89B4FA), onSecondary = Color(0xFF102340),
            tertiary = Color(0xFFF9E2AF), onTertiary = Color(0xFF2C240F),
            background = Color(0xFF181825), onBackground = Color(0xFFCDD6F4),
            surfaceLowest = Color(0xFF11111B), surfaceLow = Color(0xFF1E1E2E),
            surface = Color(0xFF262637), surfaceHigh = Color(0xFF313244),
            surfaceHighest = Color(0xFF3B3D52), surfaceBright = Color(0xFF494C64),
            onSurface = Color(0xFFCDD6F4), onSurfaceVariant = Color(0xFFA6ADC8),
            outline = Color(0xFF7F849C), outlineVariant = Color(0xFF45475A)
        ),
        AppThemeType.TOKYO_NIGHT to CloudPalette(
            primary = Color(0xFF7AA2F7), onPrimary = Color(0xFF102246),
            primaryContainer = Color(0xFF2D477D), onPrimaryContainer = Color(0xFFDCE7FF),
            secondary = Color(0xFF73DACA), onSecondary = Color(0xFF092C29),
            tertiary = Color(0xFFBB9AF7), onTertiary = Color(0xFF24133F),
            background = Color(0xFF15161F), onBackground = Color(0xFFC0CAF5),
            surfaceLowest = Color(0xFF101117), surfaceLow = Color(0xFF1A1B26),
            surface = Color(0xFF202231), surfaceHigh = Color(0xFF292D42),
            surfaceHighest = Color(0xFF343A55), surfaceBright = Color(0xFF414866),
            onSurface = Color(0xFFC0CAF5), onSurfaceVariant = Color(0xFF9AA5CE),
            outline = Color(0xFF737DA3), outlineVariant = Color(0xFF3B4261)
        ),
        AppThemeType.GRUVBOX to CloudPalette(
            primary = Color(0xFFFABD2F), onPrimary = Color(0xFF2A2000),
            primaryContainer = Color(0xFF55440E), onPrimaryContainer = Color(0xFFFFE8A3),
            secondary = Color(0xFFB8BB26), onSecondary = Color(0xFF252600),
            tertiary = Color(0xFFFE8019), onTertiary = Color(0xFF321400),
            background = Color(0xFF1D2021), onBackground = Color(0xFFEBDBB2),
            surfaceLowest = Color(0xFF181A1B), surfaceLow = Color(0xFF282828),
            surface = Color(0xFF32302F), surfaceHigh = Color(0xFF3C3836),
            surfaceHighest = Color(0xFF504945), surfaceBright = Color(0xFF665C54),
            onSurface = Color(0xFFEBDBB2), onSurfaceVariant = Color(0xFFD5C4A1),
            outline = Color(0xFF928374), outlineVariant = Color(0xFF504945)
        )
    )

    private val colorSchemes = palettes.mapValues { (_, palette) -> palette.toColorScheme() }

    fun getColorScheme(type: AppThemeType): ColorScheme = colorSchemes.getValue(type)

    fun getSemanticColors(type: AppThemeType): CloudLinkSemanticColors {
        val isLight = palettes.getValue(type).isLight
        return if (isLight) {
            CloudLinkSemanticColors(
                success = Color(0xFF08783F), onSuccess = Color.White,
                successContainer = Color(0xFFC2F2D3), onSuccessContainer = Color(0xFF062713),
                warning = Color(0xFF8B5800), onWarning = Color.White,
                warningContainer = Color(0xFFFFDEA3), onWarningContainer = Color(0xFF2D1B00),
                info = Color(0xFF1559C5), onInfo = Color.White,
                infoContainer = Color(0xFFD8E5FF), onInfoContainer = Color(0xFF061B40)
            )
        } else {
            CloudLinkSemanticColors(
                success = Color(0xFF47D99A), onSuccess = Color(0xFF032C1D),
                successContainer = Color(0xFF123C2D), onSuccessContainer = Color(0xFFC1F4DA),
                warning = Color(0xFFFFC857), onWarning = Color(0xFF302000),
                warningContainer = Color(0xFF49350C), onWarningContainer = Color(0xFFFFE6B0),
                info = Color(0xFF78B3FF), onInfo = Color(0xFF061B37),
                infoContainer = Color(0xFF173A64), onInfoContainer = Color(0xFFD7E8FF)
            )
        }
    }
}
