package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AssistantViewModel
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.IndigoVibrant
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceToolsScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val lastAction by viewModel.lastActionSummary.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Device Tools & Integration",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Text(
                text = "Consent-based hardware and application integration points",
                fontSize = 13.sp,
                color = TextSecondaryDark
            )
        }

        if (!lastAction.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2A40)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanElectric.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldActive,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Last Executed Action",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanElectric
                            )
                            Text(
                                text = lastAction.orEmpty(),
                                fontSize = 14.sp,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }
            }
        }

        // Section: Camera & Media
        item {
            ToolSectionHeader(title = "HARDWARE & CAPTURE")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToolActionCard(
                    title = "Camera Capture",
                    description = "Launch camera for photos",
                    icon = Icons.Default.CameraAlt,
                    accentColor = CyanElectric,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.toolsManager.openCamera() }
                )
                ToolActionCard(
                    title = "Media Playback",
                    description = "Toggle music play/pause",
                    icon = Icons.Default.PlayArrow,
                    accentColor = IndigoVibrant,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.toolsManager.controlMedia("toggle") }
                )
            }
        }

        // Section: Media controls row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Media Key Dispatcher",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Dispatches system media keys for Spotify, YouTube Music, or active players",
                        fontSize = 12.sp,
                        color = TextMutedDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MediaButton(label = "Previous", onClick = { viewModel.toolsManager.controlMedia("previous") })
                        MediaButton(label = "Play / Pause", isPrimary = true, onClick = { viewModel.toolsManager.controlMedia("toggle") })
                        MediaButton(label = "Next", onClick = { viewModel.toolsManager.controlMedia("next") })
                    }
                }
            }
        }

        // Section: App Launchers
        item {
            ToolSectionHeader(title = "APP LAUNCHERS")
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val apps = listOf(
                    Triple("YouTube", Icons.Default.Videocam, "youtube"),
                    Triple("WhatsApp", Icons.Default.MusicNote, "whatsapp"),
                    Triple("Spotify", Icons.Default.MusicNote, "spotify"),
                    Triple("Maps", Icons.Default.Map, "maps"),
                    Triple("Chrome", Icons.Default.Language, "chrome"),
                    Triple("Calculator", Icons.Default.Calculate, "calculator")
                )

                apps.forEach { (name, icon, key) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceDark)
                            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(14.dp))
                            .clickable { viewModel.toolsManager.launchAppByName(key) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = icon, contentDescription = null, tint = CyanElectric, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name, fontSize = 13.sp, color = TextPrimaryDark)
                        }
                    }
                }
            }
        }

        // Section: System & Utilities
        item {
            ToolSectionHeader(title = "SYSTEM SETTINGS & TIMERS")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToolActionCard(
                    title = "Wi-Fi Settings",
                    description = "Network panel",
                    icon = Icons.Default.Wifi,
                    accentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.toolsManager.openSettings("wifi") }
                )
                ToolActionCard(
                    title = "Bluetooth",
                    description = "Pair devices",
                    icon = Icons.Default.Bluetooth,
                    accentColor = Color(0xFF818CF8),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.toolsManager.openSettings("bluetooth") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToolActionCard(
                    title = "5-Min Timer",
                    description = "System clock timer",
                    icon = Icons.Default.Alarm,
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.toolsManager.setAlarm(5, "Mahi Quick Timer") }
                )
                ToolActionCard(
                    title = "Web Search",
                    description = "Search Google",
                    icon = Icons.Default.Search,
                    accentColor = EmeraldActive,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.toolsManager.searchWeb("latest AI news") }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ToolSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextMutedDark,
        letterSpacing = 1.sp
    )
}

@Composable
fun ToolActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
fun MediaButton(
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isPrimary) CyanElectric else Color(0xFF1E293B))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPrimary) DeepSpace else Color.White
        )
    }
}
