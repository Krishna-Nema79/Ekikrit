package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class TimelineStageInfo(
    val stageKey: String,
    val title: String,
    val subtitle: String,
    val dateText: String,
    val authority: String
)

@Composable
fun ApplicationTimeline(
    application: ApplicationEntity,
    modifier: Modifier = Modifier
) {
    // 6-Stage Comprehensive Institutional Lifecycle (SIH26238 Requirement 14)
    val stages = listOf(
        TimelineStageInfo(
            stageKey = "SUBMITTED",
            title = "1. Application Submitted",
            subtitle = "Direct 1-click submission with 5 DigiLocker digital credentials",
            dateText = application.appliedDate,
            authority = "Ekikrit Unified Rail"
        ),
        TimelineStageInfo(
            stageKey = "INSTITUTE_VERIFICATION",
            title = "2. Institute Verification",
            subtitle = "AISHE & UDISE+ validation of bona fide student registration",
            dateText = if (application.currentStage != "SUBMITTED") application.lastUpdated else "In Progress",
            authority = "NIT Rourkela / Nodal Officer"
        ),
        TimelineStageInfo(
            stageKey = "STATE_VERIFICATION",
            title = "3. State Verification",
            subtitle = "State Tribal Welfare e-District & Caste validation checks",
            dateText = if (application.currentStage in listOf("STATE_VERIFICATION", "MINISTRY_REVIEW", "SANCTIONED", "DISBURSED")) application.lastUpdated else "Pending",
            authority = "District Tribal Welfare Dept, Odisha"
        ),
        TimelineStageInfo(
            stageKey = "MINISTRY_REVIEW",
            title = "4. Ministry Review",
            subtitle = "National Tribal Scholarship Division cross-system integrity ledger",
            dateText = if (application.currentStage in listOf("MINISTRY_REVIEW", "SANCTIONED", "DISBURSED")) application.lastUpdated else "Awaiting State Clearance",
            authority = "Ministry of Tribal Affairs (MoTA)"
        ),
        TimelineStageInfo(
            stageKey = "SANCTIONED",
            title = "5. Sanctioned",
            subtitle = "Official Sanction Order generated for Direct Benefit Transfer",
            dateText = if (application.currentStage in listOf("SANCTIONED", "DISBURSED")) "Sanction MoTA/2026/09" else "Pending Review Clearance",
            authority = "Financial Advisor & DDO, MoTA"
        ),
        TimelineStageInfo(
            stageKey = "DISBURSED",
            title = "6. DBT / Disbursement",
            subtitle = "Section 7 Aadhaar-linked payment credited directly to bank account",
            dateText = if (application.currentStage == "DISBURSED") "Credit confirmed via PFMS" else "Est. ~${application.estimatedDisbursementDays} days post sanction",
            authority = "Public Financial Management System (PFMS)"
        )
    )

    val currentStepIndex = when (application.currentStage) {
        "SUBMITTED" -> 0
        "INSTITUTE_VERIFICATION", "UNDER_VERIFICATION", "EXCEPTION_REVIEW" -> 1
        "STATE_VERIFICATION" -> 2
        "MINISTRY_REVIEW" -> 3
        "SANCTIONED" -> 4
        "DISBURSED" -> 5
        else -> 0
    }

    Column(modifier = modifier.fillMaxWidth()) {
        stages.forEachIndexed { index, stage ->
            val isCompleted = index < currentStepIndex || (index == currentStepIndex && application.currentStage == "DISBURSED")
            val isCurrent = index == currentStepIndex && application.currentStage != "DISBURSED"
            val isDiscrepancy = isCurrent && application.hasDiscrepancy

            Row(modifier = Modifier.fillMaxWidth()) {
                // Stepper Dot and Connecting Line
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

                // Step Content Details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (index < stages.size - 1) 16.dp else 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stage.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDiscrepancy) Color(0xFFD97706) else if (isCurrent || isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = stage.dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = stage.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Nodal: ${stage.authority}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isDiscrepancy) {
                        Surface(
                            color = Color(0xFFD97706).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Non-blocking exception auto-routed to Reviewer Desk #4 (Variance within scheme limits).",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
