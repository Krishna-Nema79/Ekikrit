package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ApplicationEntity

@Composable
fun ApplicationTimeline(
    application: ApplicationEntity,
    modifier: Modifier = Modifier
) {
    val stages = listOf(
        Triple("Applied", "Application submitted via ${application.schemeCode} portal", application.appliedDate),
        Triple("Verified", "Multi-source API verification (UIDAI, DigiLocker, AISHE)", if (application.currentStage != "SUBMITTED") application.lastUpdated else "Pending"),
        Triple("Sanctioned", "Sanction Order issued by Ministry of Tribal Affairs", if (application.currentStage in listOf("SANCTIONED", "DISBURSED")) "Sanction MoTA/2026/09" else "Awaiting verification clearance"),
        Triple("Disbursed", "Direct Benefit Transfer (DBT) via Aadhaar-linked rail", if (application.currentStage == "DISBURSED") "Credit confirmed via PFMS" else "Estimated ~${application.estimatedDisbursementDays} days post sanction")
    )

    val currentStepIndex = when (application.currentStage) {
        "SUBMITTED" -> 0
        "UNDER_VERIFICATION" -> 1
        "EXCEPTION_REVIEW" -> 1
        "SANCTIONED" -> 2
        "DISBURSED" -> 3
        else -> -1
    }

    Column(modifier = modifier.fillMaxWidth()) {
        stages.forEachIndexed { index, (title, description, dateText) ->
            val isCompleted = index < currentStepIndex || (index == currentStepIndex && application.currentStage == "DISBURSED")
            val isCurrent = index == currentStepIndex && application.currentStage != "DISBURSED"
            val isDiscrepancy = isCurrent && application.hasDiscrepancy

            Row(modifier = Modifier.fillMaxWidth()) {
                // Stepper Column with Dot and Connecting Line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp)
                ) {
                    val dotColor = when {
                        isDiscrepancy -> Color(0xFFD97706) // Amber for exception review
                        isCompleted -> Color(0xFF059669) // Emerald
                        isCurrent -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant // Inactive gray
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isDiscrepancy -> Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Discrepancy",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            isCompleted -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            isCurrent -> Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = "In Progress",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            else -> Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (index < stages.size - 1) {
                        val lineColor = if (index < currentStepIndex) Color(0xFF059669) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(56.dp)
                                .background(lineColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Step content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (index < stages.size - 1) 20.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isCurrent || isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isDiscrepancy) {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = CircleShape,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "Exception Reviewing",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isCompleted) {
                            Text(
                                text = "Verified",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
