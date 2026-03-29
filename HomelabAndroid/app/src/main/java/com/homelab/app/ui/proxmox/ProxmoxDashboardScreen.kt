package com.homelab.app.ui.proxmox

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
import androidx.compose.material3.LinearProgressIndicator
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
import com.homelab.app.data.repository.ProxmoxDashboardData
import com.homelab.app.data.repository.ProxmoxNode
import com.homelab.app.data.repository.ProxmoxResource
import com.homelab.app.domain.model.ServiceInstance
import com.homelab.app.ui.components.ServiceIcon
import com.homelab.app.ui.components.ServiceInstancePicker
import com.homelab.app.ui.theme.isThemeDark
import com.homelab.app.ui.theme.primaryColor
import com.homelab.app.util.ServiceType

private fun proxmoxPageBackground(isDarkTheme: Boolean, accent: Color): Brush = if (isDarkTheme) {
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
fun ProxmoxDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToInstance: (String) -> Unit,
    viewModel: ProxmoxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val instances by viewModel.instances.collectAsStateWithLifecycle()
    val accent = ServiceType.PROXMOX.primaryColor
    val isDarkTheme = isThemeDark()
    val pageBrush = remember(isDarkTheme) { proxmoxPageBackground(isDarkTheme, accent) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.service_proxmox)) },
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
                ProxmoxUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                }
                is ProxmoxUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { viewModel.refresh() }) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
                is ProxmoxUiState.Success -> {
                    ProxmoxContent(
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
private fun ProxmoxContent(
    padding: PaddingValues,
    data: ProxmoxDashboardData,
    accent: Color,
    instances: List<ServiceInstance>,
    instanceId: String,
    onSelectInstance: (ServiceInstance) -> Unit
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
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ServiceIcon(type = ServiceType.PROXMOX, size = 64.dp, iconSize = 36.dp, cornerRadius = 18.dp)
                        Column {
                            Text(stringResource(R.string.service_proxmox), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.proxmox_node_count, data.nodes.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricPill(stringResource(R.string.proxmox_vms), "${data.runningVMs}/${data.totalVMs}", accent)
                        MetricPill(stringResource(R.string.proxmox_lxcs), "${data.runningContainers}/${data.totalContainers}", accent)
                        MetricPill(stringResource(R.string.proxmox_nodes), data.nodes.size.toString(), accent)
                    }
                }
            }
        }

        if (data.nodes.isNotEmpty()) {
            item { Text(stringResource(R.string.proxmox_nodes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(data.nodes, key = { "node-${it.node}" }) { node ->
                NodeCard(node = node, accent = accent)
            }
        }

        if (data.resources.isNotEmpty()) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.proxmox_resources), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.proxmox_running_count, data.resources.count { it.status == "running" }), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(data.resources.sortedWith(compareByDescending<ProxmoxResource> { it.status == "running" }.thenBy { it.name.lowercase() }), key = { "res-${it.id}" }) { resource ->
                ResourceCard(resource = resource, accent = accent)
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, accent: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = 0.12f)) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NodeCard(node: ProxmoxNode, accent: Color) {
    val cpuPercent = (node.cpuUsage * 100).toInt()
    val memPercent = if (node.memoryTotal > 0) ((node.memoryUsed.toDouble() / node.memoryTotal) * 100).toInt() else 0
    val onlineStr = stringResource(R.string.proxmox_online)
    val offlineStr = stringResource(R.string.proxmox_offline)
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(node.node, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Surface(shape = RoundedCornerShape(10.dp), color = if (node.status == "online") accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        if (node.status == "online") onlineStr else offlineStr,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (node.status == "online") accent else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(stringResource(R.string.proxmox_cpu_mem_uptime, cpuPercent, memPercent, formatUptime(node.uptime)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { node.cpuUsage.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = accent,
                trackColor = accent.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
private fun ResourceCard(resource: ProxmoxResource, accent: Color) {
    val isRunning = resource.status == "running"
    val typeLabel = if (resource.type == "qemu") stringResource(R.string.proxmox_vm_label) else stringResource(R.string.proxmox_lxc_label)
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(resource.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.proxmox_on_node, typeLabel, resource.node), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(10.dp), color = if (isRunning) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer) {
                Text(
                    resource.status.replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRunning) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    return if (days > 0) "${days}d ${hours}h" else "${hours}h"
}
