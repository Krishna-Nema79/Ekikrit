package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun VoiceAssistBanner(
    isVoiceAssistEnabled: Boolean,
    onToggleVoiceAssist: (Boolean) -> Unit,
    spokenNarration: String = "Welcome! You have 1 verified scholarship ready for DBT disbursement and 1 eligible scheme waiting for 1-click application.",
    onPlayNarration: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_assist_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isVoiceAssistEnabled) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.5.dp,
            if (isVoiceAssistEnabled) Color(0xFFD97706) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics {
                        contentDescription = "Voice Assist and Audio Accessibility Mode toggle"
                    }
                ) {
                    Surface(
                        color = if (isVoiceAssistEnabled) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isVoiceAssistEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = null,
                                tint = if (isVoiceAssistEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Voice Assist Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isVoiceAssistEnabled) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isVoiceAssistEnabled) "Spoken guidance active (Tribal & Plain Language)" else "Tap switch to enable audio assistance",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isVoiceAssistEnabled) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isVoiceAssistEnabled,
                    onCheckedChange = onToggleVoiceAssist,
                    modifier = Modifier
                        .testTag("voice_assist_switch")
                        .semantics { role = Role.Switch }
                )
            }

            AnimatedVisibility(
                visible = isVoiceAssistEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFFBEB))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                isPlaying = !isPlaying
                                onPlayNarration()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlaying) Color(0xFF059669) else Color(0xFFD97706)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .semantics {
                                    contentDescription = if (isPlaying) "Pause audio narration" else "Listen to screen instructions aloud"
                                }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPlaying) "Playing Audio..." else "Listen Aloud",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        if (isPlaying) {
                            VoiceWaveform(modifier = Modifier.weight(1f))
                        } else {
                            Text(
                                text = "Speaks screen elements & steps clearly",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF78350F),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = "🔊 \"$spokenNarration\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceWaveform(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val heights = (0..4).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 6f,
            targetValue = 24f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 350 + (index * 80), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave_$index"
        )
    }

    Row(
        modifier = modifier.height(30.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { heightAnim ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(heightAnim.value.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF059669))
            )
        }
    }
}
