package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.example.data.model.ScholarshipMatch

@Composable
fun UnreachedBeneficiaryBanner(
    title: String = "Did you know? You're eligible for 'Top Class ST Education'!",
    description: String = "Ministry automated cross-match found regular enrollment, but no claim has been submitted. Tap to apply with 1-click DigiLocker credentials!",
    badgeText: String = "UDISE+ & APAAR CROSS-MATCH NUDGE",
    buttonText: String = "1-Click Apply via DigiLocker (No Paperwork)",
    onOneClickApply: () -> Unit,
    match: ScholarshipMatch? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val titleText = match?.let { "You qualify for '${it.scheme.name}'!" } ?: title
    val descText = match?.let { "${it.whyMatched} Reusable documents: ${it.reusableDocuments.joinToString(", ")}." }
        ?: description
    val ctaText = match?.nextAction ?: buttonText
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("unreached_beneficiary_nudge"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F2B5C),
                            Color(0xFF1E3A8A),
                            Color(0xFF1E40AF)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = Color(0xFFF59E0B),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF451A03),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF451A03)
                            )
                        }
                    }

                    if (onDismiss != null) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = descText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE2E8F0)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOneClickApply,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color(0xFF451A03)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_unreached_scheme_btn")
                ) {
                    Text(
                        text = ctaText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
