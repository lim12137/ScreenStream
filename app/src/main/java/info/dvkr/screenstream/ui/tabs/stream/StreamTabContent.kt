package info.dvkr.screenstream.ui.tabs.stream

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.dvkr.screenstream.R
import info.dvkr.screenstream.common.module.StreamingModule
import info.dvkr.screenstream.common.module.StreamingModuleManager
import org.koin.compose.koinInject

@Composable
internal fun StreamTabContent(
    boundsInWindow: Rect,
    modifier: Modifier = Modifier,
    streamingModulesManager: StreamingModuleManager = koinInject()
) {
    val context = LocalContext.current
    val activeModule = streamingModulesManager.activeModuleStateFlow.collectAsStateWithLifecycle().value

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        StreamModeBanner(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        HarmonyOsGuideCard(
            onOpenAppSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (activeModule == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.app_tab_stream_module_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            activeModule.StreamUIContent(
                windowWidthSizeClass = calculateWindowWidthSizeClass(boundsInWindow),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
            )
        }
    }
}

@Composable
private fun StreamModeBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.app_tab_stream_local_mode_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = stringResource(id = R.string.app_tab_stream_local_mode_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun HarmonyOsGuideCard(
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.app_tab_stream_harmony_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(
                            id = if (expanded) {
                                R.string.app_tab_stream_harmony_badge_recommended
                            } else {
                                R.string.app_tab_stream_harmony_badge_done
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.app_tab_stream_harmony_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (expanded) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_tab_stream_harmony_checklist_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        GuideStep(
                            index = 1,
                            title = stringResource(id = R.string.app_tab_stream_harmony_step_1_title),
                            summary = stringResource(id = R.string.app_tab_stream_harmony_step_1_summary)
                        )
                        GuideStep(
                            index = 2,
                            title = stringResource(id = R.string.app_tab_stream_harmony_step_2_title),
                            summary = stringResource(id = R.string.app_tab_stream_harmony_step_2_summary)
                        )
                        GuideStep(
                            index = 3,
                            title = stringResource(id = R.string.app_tab_stream_harmony_step_3_title),
                            summary = stringResource(id = R.string.app_tab_stream_harmony_step_3_summary)
                        )
                        GuideStep(
                            index = 4,
                            title = stringResource(id = R.string.app_tab_stream_harmony_step_4_title),
                            summary = stringResource(id = R.string.app_tab_stream_harmony_step_4_summary)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_tab_stream_harmony_recovery_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.app_tab_stream_harmony_recovery_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(onClick = onOpenAppSettings) {
                        Text(text = stringResource(id = R.string.app_tab_stream_harmony_open_app_settings))
                    }
                    OutlinedButton(onClick = { expanded = false }) {
                        Text(text = stringResource(id = R.string.app_tab_stream_harmony_acknowledge))
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideStep(
    index: Int,
    title: String,
    summary: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = summary,
            modifier = Modifier.padding(start = 32.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun calculateWindowWidthSizeClass(boundsInWindow: Rect): StreamingModule.WindowWidthSizeClass {
    if (boundsInWindow.isEmpty) return StreamingModule.WindowWidthSizeClass.COMPACT

    val widthDp = with(LocalDensity.current) { boundsInWindow.width.toDp() }
    return when {
        widthDp < 600.dp -> StreamingModule.WindowWidthSizeClass.COMPACT
        widthDp < 840.dp -> StreamingModule.WindowWidthSizeClass.MEDIUM
        else -> StreamingModule.WindowWidthSizeClass.EXPANDED
    }
}
