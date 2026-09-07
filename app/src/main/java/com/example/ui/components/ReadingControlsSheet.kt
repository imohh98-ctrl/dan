package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReadingSettings
import com.example.ui.theme.ReaderCreamBg
import com.example.ui.theme.ReaderDarkBg
import com.example.ui.theme.ReaderParchmentBg
import com.example.ui.theme.ReaderSepiaBg
import com.example.ui.theme.ReaderWhiteBg
import com.example.ui.viewmodel.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingControlsSheet(
    settings: ReadingSettings,
    isPdfOrScanned: Boolean,
    currentLanguage: AppLanguage,
    onFontSizeChange: (Float) -> Unit,
    onThemeChange: (ReaderThemeMode) -> Unit,
    onAutoScrollToggle: () -> Unit,
    onAutoScrollSpeedChange: (Float) -> Unit,
    onDistractionFreeToggle: () -> Unit,
    onZoomScaleChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isArabic = currentLanguage == AppLanguage.ARABIC

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("reading_controls_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(id = R.string.reader_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Reading Color Theme / Background palette
            Text(
                text = stringResource(id = R.string.reader_theme),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemePaletteOption(
                    name = stringResource(id = R.string.reader_theme_parchment),
                    bgColor = ReaderParchmentBg,
                    isSelected = settings.themeMode == ReaderThemeMode.PARCHMENT,
                    onClick = { onThemeChange(ReaderThemeMode.PARCHMENT) },
                    testTag = "theme_parchment"
                )
                ThemePaletteOption(
                    name = stringResource(id = R.string.reader_theme_cream),
                    bgColor = ReaderCreamBg,
                    isSelected = settings.themeMode == ReaderThemeMode.CREAM,
                    onClick = { onThemeChange(ReaderThemeMode.CREAM) },
                    testTag = "theme_cream"
                )
                ThemePaletteOption(
                    name = stringResource(id = R.string.reader_theme_sepia),
                    bgColor = ReaderSepiaBg,
                    isSelected = settings.themeMode == ReaderThemeMode.SEPIA,
                    onClick = { onThemeChange(ReaderThemeMode.SEPIA) },
                    testTag = "theme_sepia"
                )
                ThemePaletteOption(
                    name = stringResource(id = R.string.reader_theme_dark),
                    bgColor = ReaderDarkBg,
                    isSelected = settings.themeMode == ReaderThemeMode.DARK,
                    onClick = { onThemeChange(ReaderThemeMode.DARK) },
                    testTag = "theme_dark"
                )
                ThemePaletteOption(
                    name = stringResource(id = R.string.reader_theme_white),
                    bgColor = ReaderWhiteBg,
                    isSelected = settings.themeMode == ReaderThemeMode.WHITE,
                    onClick = { onThemeChange(ReaderThemeMode.WHITE) },
                    testTag = "theme_white"
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))

            // 2. Font Size Adjustment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.reader_font_size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${settings.fontSizeSp.toInt()} sp",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = { onFontSizeChange(settings.fontSizeSp - 2f) },
                        modifier = Modifier.size(40.dp).testTag("font_decrease_btn"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = stringResource(id = R.string.reader_font_smaller))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    FilledIconButton(
                        onClick = { onFontSizeChange(settings.fontSizeSp + 2f) },
                        modifier = Modifier.size(40.dp).testTag("font_increase_btn"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(id = R.string.reader_font_larger))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))

            // 3. Auto Scroll controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.reader_auto_scroll),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (settings.isAutoScrolling) {
                            stringResource(id = R.string.auto_scroll_enabled, settings.autoScrollSpeed)
                        } else {
                            stringResource(id = R.string.auto_scroll_paused)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (settings.isAutoScrolling) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = settings.isAutoScrolling,
                    onCheckedChange = { onAutoScrollToggle() },
                    modifier = Modifier.testTag("auto_scroll_switch")
                )
            }

            if (settings.isAutoScrolling) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                        FilterChip(
                            selected = settings.autoScrollSpeed == speed,
                            onClick = { onAutoScrollSpeedChange(speed) },
                            label = { Text("${speed}x") },
                            modifier = Modifier.testTag("speed_${speed}x")
                        )
                    }
                }
            }

            // 4. Enhanced Zoom Magnification for Arabic PDF & documents
            if (isPdfOrScanned) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.reader_pdf_zoom),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(settings.contentZoomScale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { onZoomScaleChange(settings.contentZoomScale - 0.2f) },
                            modifier = Modifier.testTag("zoom_out_btn")
                        ) {
                            Icon(imageVector = Icons.Default.ZoomOut, contentDescription = stringResource(id = R.string.reader_zoom_out))
                        }
                        IconButton(
                            onClick = { onZoomScaleChange(settings.contentZoomScale + 0.2f) },
                            modifier = Modifier.testTag("zoom_in_btn")
                        ) {
                            Icon(imageVector = Icons.Default.ZoomIn, contentDescription = stringResource(id = R.string.reader_zoom_in))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePaletteOption(
    name: String,
    bgColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 10.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
