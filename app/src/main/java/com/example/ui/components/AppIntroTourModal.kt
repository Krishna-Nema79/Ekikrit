package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class IntroStep(
    val stepNumber: Int,
    val tag: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val keyPoints: List<String>,
    val badgeColor: Color
)

@Composable
fun AppIntroTourModal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        IntroStep(
            stepNumber = 1,
            tag = "SIH26238 • PROBLEM STATEMENT BRIEF",
            title = "Welcome to Ekikrit (एकिकृत)",
            description = "A unified, mobile-first scholarship portal for Scheduled Tribe (ST) students across India, breaking portal silos and streamlining higher education grants.",
            icon = Icons.Default.AccountBalance,
            keyPoints = listOf(
                "Consolidates 5 central schemes previously fragmented across NSP, Canara Bank SFMP, and the NOS standalone portal.",
                "Designed under Ministry of Tribal Affairs (MoTA) guidelines with end-to-end lifecycle visibility.",
                "Built for PVTG and tribal students with localized multilingual support."
            ),
            badgeColor = Color(0xFFD97706)
        ),
        IntroStep(
            stepNumber = 2,
            tag = "SCHEMES CONSOLIDATION",
            title = "All 5 Tribal Schemes in One View",
            description = "Track applications across all five Ministry scholarship schemes simultaneously with zero confusion.",
            icon = Icons.Default.School,
            keyPoints = listOf(
                "Post-Matric Scholarship (PMS-ST) — Higher education tuition & maintenance grants.",
                "Pre-Matric Scholarship — Secondary school completion assistance.",
                "National Fellowship (NFST) — Full financial aid for M.Phil / Ph.D scholars.",
                "National Overseas Scholarship (NOS) — Prestigious global master's & doctoral support.",
                "Top Class Education for ST Students — 100% funding at elite premier institutes (IITs, NITs, IIMs)."
            ),
            badgeColor = Color(0xFF2563EB)
        ),
        IntroStep(
            stepNumber = 3,
            tag = "DIGILOCKER INTEGRATION",
            title = "Zero-Reupload Document Wallet",
            description = "Pull your authentic government credentials once; automatically reuse them across every scholarship scheme without uploading physical paper.",
            icon = Icons.Default.FolderShared,
            keyPoints = listOf(
                "Digital ST Caste Certificate (with PVTG Birhor classification) pulled directly via DigiLocker.",
                "Income Certificate, Class 10/12 Marksheets, and Domicile credentials verified digitally.",
                "SHA-256 digital signature validity checked against National Root Authority.",
                "Section 6 DPDP Act, 2023 compliant consent: purpose-bound and easily revocable."
            ),
            badgeColor = Color(0xFF059669)
        ),
        IntroStep(
            stepNumber = 4,
            tag = "VERIFICATION & EXCEPTION ROUTING",
            title = "Multi-Source Auto Verification",
            description = "Automated cross-checks against UIDAI, AISHE, UDISE+, APAAR, UGC-NTA, and e-District with non-blocking exception workflows.",
            icon = Icons.Default.Shield,
            keyPoints = listOf(
                "UIDAI demographic match auto-clears identity instantly.",
                "Non-blocking Routing: If income or spelling variances occur (e.g. +11.9%), the application is NOT blocked or rejected.",
                "Exceptions are routed to the District Tribal Welfare Officer's Reviewer Desk for statutory clearance.",
                "Demo Switcher: Judges can jump to the Reviewer Desk and approve exceptions live in front of the audience!"
            ),
            badgeColor = Color(0xFF7C3AED)
        ),
        IntroStep(
            stepNumber = 5,
            tag = "ASSISTANCE & DIRECT BENEFIT TRANSFER",
            title = "JAGO Assistant & PFMS-DBT Rail",
            description = "Stay informed in your mother tongue and monitor scholarship payments directly credited to your Aadhaar-linked bank account.",
            icon = Icons.Default.SmartToy,
            keyPoints = listOf(
                "JAGO Multilingual Bot: Converses fluently in English, हिन्दी (Hindi), ଓଡ଼ିଆ (Odia), and गोंडी (Gondi).",
                "App-State Aware: Knows your exact pending items, income flag causes, and eligibility.",
                "Consolidated DBT Tracking: Live payment ledger and estimated disbursement countdowns.",
                "Unreached-Beneficiary Matcher: Proactively alerts eligible students who haven't yet claimed their grants."
            ),
            badgeColor = Color(0xFFEA580C)
        )
    )

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
                .testTag("app_intro_tour_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Bar with step counter & skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = step.badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = step.tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = step.badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (currentStepIndex == steps.size - 1) "Close" else "Skip",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "IntroContentTransition"
                ) { current ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(current.badgeColor, current.badgeColor.copy(alpha = 0.7f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = current.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = current.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Step ${current.stepNumber} of ${steps.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = current.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                current.keyPoints.forEach { point ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = current.badgeColor,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = point,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Pagination & Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pager Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        steps.indices.forEach { index ->
                            val isCurrent = index == currentStepIndex
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 20.dp else 8.dp, 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) step.badgeColor else Color(0xFFCBD5E1)
                                    )
                            )
                        }
                    }

                    // Navigation Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentStepIndex > 0) {
                            OutlinedButton(
                                onClick = { currentStepIndex-- },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", modifier = Modifier.size(16.dp))
                            }
                        }

                        Button(
                            onClick = {
                                if (currentStepIndex < steps.size - 1) {
                                    currentStepIndex++
                                } else {
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = step.badgeColor),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("intro_next_btn")
                        ) {
                            Text(
                                text = if (currentStepIndex < steps.size - 1) "Next" else "Explore Portal",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (currentStepIndex < steps.size - 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
