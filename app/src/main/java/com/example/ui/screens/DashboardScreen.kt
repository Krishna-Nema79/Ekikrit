package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ApplicationEntity
import com.example.data.model.SchemeEntity
import com.example.data.model.StudentEntity
import com.example.ui.components.PendingActionsCard
import com.example.ui.components.UnreachedBeneficiaryBanner
import com.example.ui.util.LocalAppStrings

@Composable
fun DashboardScreen(
    student: StudentEntity?,
    applications: List<ApplicationEntity>,
    schemes: List<SchemeEntity>,
    onSelectScheme: (String) -> Unit,
    onOpenReviewDesk: () -> Unit,
    onOpenJago: () -> Unit,
    onApplyUnreached: (String) -> Unit,
    onOpenConsentDialog: () -> Unit,
    onOpenSecurityModal: () -> Unit,
    onOpenIntroTour: () -> Unit,
    onOpenLoginSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var isUnreachedBannerDismissed by remember { mutableStateOf(false) }

    val firstName = remember(student?.name) {
        student?.name?.trim()?.split("\\s+".toRegex())?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Student"
    }

    val initials = remember(student?.name) {
        val parts = student?.name?.trim()?.split("\\s+".toRegex())?.filter { it.isNotBlank() } ?: emptyList()
        when {
            parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "ST"
        }
    }

    val filteredApps = when (selectedFilter) {
        "ACTION" -> applications.filter { it.hasDiscrepancy || it.pendingActionDesc != null }
        "VERIFIED" -> applications.filter { it.currentStage in listOf("SANCTIONED", "DISBURSED") }
        "IN_PROGRESS" -> applications.filter { it.currentStage == "UNDER_VERIFICATION" || it.currentStage == "SUBMITTED" }
        else -> applications
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp)
    ) {
        // Top Compact Ministry Bar + Student Profile Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Sleek, compact Ministry Header Strip (no heavy 150dp image blocker)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFD97706),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "MINISTRY OF TRIBAL AFFAIRS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF059669),
                                shape = CircleShape
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .padding(2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.nspPfmsActive,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Top Overflow Menu for secondary actions (Switch User, Tour, Security)
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("dashboard_overflow_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Switch Student Profile") },
                                    leadingIcon = { Icon(Icons.Default.SwitchAccount, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenLoginSheet()
                                    },
                                    modifier = Modifier.testTag("switch_profile_btn")
                                )
                                DropdownMenuItem(
                                    text = { Text("App Tour & SIH Guide") },
                                    leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenIntroTour()
                                    },
                                    modifier = Modifier.testTag("open_intro_tour_btn")
                                )
                                DropdownMenuItem(
                                    text = { Text("Security & Privacy (DPDP)") },
                                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenSecurityModal()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Student Profile & Greeting Row (Optimized for modern Android phones, ample space, zero truncation)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Line 1: Small, lower-emphasis "Welcome back" label
                            Text(
                                text = strings.welcomePrefix,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Line 2: Student's first name only, prominently styled with room to breathe
                            Text(
                                text = firstName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Line 3: Full name and tribal context caption
                            Text(
                                text = "${student?.name ?: "Student"} · ${student?.category ?: "ST"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Badges row: PVTG & Aadhaar Verified & Institution
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = Color(0xFFD97706).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = strings.pvtgBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            color = Color(0xFF059669).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = strings.aadhaarVerified,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }

                        Text(
                            text = "•  ${student?.institutionName?.take(22) ?: "NIT Rourkela"}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // High-fidelity Stats Grid with clean typography
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMiniBox(
                            label = strings.statDisbursed,
                            value = "₹4,500",
                            sub = "In Bank",
                            color = Color(0xFF059669),
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniBox(
                            label = strings.statInPipeline,
                            value = "₹78,000",
                            sub = "Post-Matric",
                            color = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniBox(
                            label = strings.statDigiLocker,
                            value = "5 Docs",
                            sub = strings.zeroPaperwork,
                            color = Color(0xFFD97706),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Section: "What needs you right now" (Lead with ONE clear, calming card)
        item {
            PendingActionsCard(
                applications = applications,
                onOpenReviewDesk = onOpenReviewDesk,
                onOpenJago = onOpenJago
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Section: Unreached Beneficiary Nudge (Single dismissible banner)
        item {
            AnimatedVisibility(
                visible = !isUnreachedBannerDismissed,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    UnreachedBeneficiaryBanner(
                        onOneClickApply = { onApplyUnreached("SCH_TOPCLASS") },
                        onDismiss = { isUnreachedBannerDismissed = true }
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }

        // Section: 5 Schemes Header & Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.fiveSchemesTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.fiveSchemesSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text(strings.filterAll) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "ACTION",
                        onClick = { selectedFilter = "ACTION" },
                        label = { Text(strings.filterNeedsAction) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEF3C7),
                            selectedLabelColor = Color(0xFF92400E)
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "IN_PROGRESS",
                        onClick = { selectedFilter = "IN_PROGRESS" },
                        label = { Text(strings.filterInProgress) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "VERIFIED",
                        onClick = { selectedFilter = "VERIFIED" },
                        label = { Text(strings.filterApproved) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Collapsed Compact Scheme List View by default (Student friendly, low cognitive load)
        items(filteredApps) { app ->
            val schemeInfo = schemes.find { it.id == app.schemeId }
            CompactSchemeApplicationCard(
                application = app,
                scheme = schemeInfo,
                onClick = { onSelectScheme(app.id) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StatMiniBox(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Compact, student-friendly card for the home screen dashboard.
 * Focuses on essentials: Scheme Name, Status Dot + Friendly label, Grant Amount, and tap target.
 * Detailed progress steppers & multi-source checklists are on the dedicated SchemeDetailScreen.
 */
@Composable
fun CompactSchemeApplicationCard(
    application: ApplicationEntity,
    scheme: SchemeEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (dotColor, badgeBg, friendlyStatus) = when (application.currentStage) {
        "SUBMITTED" -> Triple(Color(0xFF2563EB), Color(0xFFEFF6FF), "Submitted • In Review")
        "UNDER_VERIFICATION" -> if (application.hasDiscrepancy) {
            Triple(Color(0xFFD97706), Color(0xFFFEF3C7), "Extra check (no action needed)")
        } else {
            Triple(Color(0xFF0284C7), Color(0xFFF0F9FF), "Checking documents")
        }
        "SANCTIONED" -> Triple(Color(0xFF059669), Color(0xFFECFDF5), "Approved • Grant Ready")
        "DISBURSED" -> Triple(Color(0xFF16A34A), Color(0xFFF0FDF4), "Deposited to Bank")
        else -> Triple(Color(0xFF9333EA), Color(0xFFFAF5FF), "Eligible to Claim")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("scheme_card_${application.schemeCode}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (application.hasDiscrepancy) Color(0xFFF59E0B).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Top line: Portal pill & Status pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = scheme?.portalOrigin ?: "NSP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = friendlyStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (application.hasDiscrepancy) Color(0xFF92400E) else dotColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scheme Name
                Text(
                    text = application.schemeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Grant amount & quick status
                Text(
                    text = "Grant: ${scheme?.maxAmount ?: "Government Aid"} • Tap to view",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
