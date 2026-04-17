package info.dvkr.screenstream.ui.tabs.stream

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
    val activeModule = streamingModulesManager.activeModuleStateFlow.collectAsStateWithLifecycle().value

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        StreamModeBanner(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
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
private fun calculateWindowWidthSizeClass(boundsInWindow: Rect): StreamingModule.WindowWidthSizeClass {
    if (boundsInWindow.isEmpty) return StreamingModule.WindowWidthSizeClass.COMPACT

    val widthDp = with(LocalDensity.current) { boundsInWindow.width.toDp() }
    return when {
        widthDp < 600.dp -> StreamingModule.WindowWidthSizeClass.COMPACT
        widthDp < 840.dp -> StreamingModule.WindowWidthSizeClass.MEDIUM
        else -> StreamingModule.WindowWidthSizeClass.EXPANDED
    }
}
