package com.homelab.app.ui.jellyfin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homelab.app.R
import com.homelab.app.data.repository.JellyfinDashboardData
import com.homelab.app.data.repository.JellyfinSession
import com.homelab.app.ui.components.ServiceIcon
import com.homelab.app.ui.components.ServiceInstancePicker
import com.homelab.app.ui.theme.isThemeDark
import com.homelab.app.ui.theme.primaryColor
import com.homelab.app.util.ServiceType

private fun jellyfinPageBackground(isDarkTheme: Boolean, accent: Color): Brush = if (isDarkTheme) {
    Brush.verticalGradient(
        listOf(
            Color(0xFF0A0E12),
            Color(0xFF0F1318),
            accent.copy(alpha = 0.03f),
            Color(0xFF0A0D11)
        )
    )
} else {
    Brush.verticalGradient(
        listOf(
            Color(0xFFF8F9FC),
            Color(0xFFF5F7FA),
            accent.copy(alpha = 0.010f),
            Color(0xFFF7F8FB)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JellyfinDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToInstance: (String) -> Unit,
    viewModel: JellyfinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val instances by viewModel.instances.collectAsStateWithLifecycle()
    val accent = ServiceType.JELLYFIN.primaryColor
    val isDarkTheme = isThemeDark()
    val pageBrush = remember(isDarkTheme) { jellyfinPageBackground(isDarkTheme, accent) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.service_jellyfin)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh), tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBrush)
        ) {
            when (val state = uiState) {
                JellyfinUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accent)
                    }
                }
                is JellyfinUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { viewModel.refresh() }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
                is JellyfinUiState.Success -> {
                    JellyfinContent(
                        padding = padding,
                        data = state.data,
                        accent = accent,
                        instances = instances,
                        instanceId = viewModel.instanceId,
                        onSelectInstance = { instance ->
                            viewModel.setPreferredInstance(instance.id)
                            onNavigateToInstance(instance.id)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JellyfinContent(
    padding: PaddingValues,
    data: JellyfinDashboardData,
    accent: Color,
    instances: List<com.homelab.app.domain.model.ServiceInstance>,
    instanceId: String,
    onSelectInstance: (com.homelab.app.domain.model.ServiceInstance) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ServiceInstancePicker(
                instances = instances,
                selectedInstanceId = instanceId,
                onInstanceSelected = onSelectInstance
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ServiceIcon(
                            type = ServiceType.JELLYFIN,
                            size = 64.dp,
                            iconSize = 36.dp,
                            cornerRadius = 18.dp
                        )
                        Column {
                            Text(
                                text = data.serverName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "v${data.version} -- ${data.operatingSystem}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricPill(stringResource(R.string.jellyfin_sessions), data.activeSessions.toString(), accent)
                        MetricPill(stringResource(R.string.jellyfin_streams), data.activeStreams.toString(), accent)
                        if (data.totalItems > 0) {
                            MetricPill(stringResource(R.string.jellyfin_items), data.totalItems.toString(), accent)
                        }
                    }
                }
            }
        }

        if (data.sessions.isNotEmpty()) {
            item {
                Text(stringResource(R.string.jellyfin_active_sessions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(data.sessions, key = { it.id }) { session ->
                SessionCard(session = session, accent = accent)
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SessionCard(session: JellyfinSession, accent: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = session.userName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${session.client} -- ${session.deviceName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (session.nowPlayingTitle != null) {
                Text(
                    text = stringResource(R.string.jellyfin_playing, session.nowPlayingTitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
