package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ApplicationEntity
import com.example.data.model.DocumentEntity
import com.example.data.model.SchemeEntity
import com.example.data.model.StudentEntity
import com.example.ui.components.DashboardSkeletonLoader
import com.example.ui.components.OfflineErrorStateCard
import com.example.ui.components.PendingActionsCard
import com.example.ui.components.ScholarshipWizardModal
import com.example.ui.components.VoiceAssistBanner
import com.example.ui.util.LocalAppStrings

@Composable
fun DashboardScreen(
    student: StudentEntity?,
    applications: List<ApplicationEntity>,
    schemes: List<SchemeEntity>,
    documents: List<DocumentEntity> = emptyList(),
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
    var isVoiceAssistActive by remember { mutableStateOf(false) }
    var selectedSchemeForWizard by remember { mutableStateOf<SchemeEntity?>(null) }
    var isOfflineSimulation by remember { mutableStateOf(false) }

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

    val eligibleSchemes = remember(schemes, applications) {
        schemes.filter { scheme ->
            applications.none { it.schemeId == scheme.id }
        }
    }

    val latestAppForTracking = remember(applications) {
        applications.firstOrNull { it.currentStage != "DISBURSED" } ?: applications.firstOrNull()
    }

    val spokenText = remember(firstName, eligibleSchemes, applications) {
        "Johar $firstName! You have ${applications.size} active scholarship applications. ${eligibleSchemes.size} additional tribal schemes are open for 1-click application without paper re-upload."
    }

    // Modal Wizard for 1-Click Application
    selectedSchemeForWizard?.let { scheme ->
        ScholarshipWizardModal(
            scheme = scheme,
            student = student,
            documents = documents,
            isVoiceAssistActive = isVoiceAssistActive,
            onDismiss = { selectedSchemeForWizard = null },
            onSubmitApplication = { schemeId ->
                onApplyUnreached(schemeId)
                selectedSchemeForWizard = null
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Voice Assist UI Mode Toggle
        item {
            VoiceAssistBanner(
                isVoiceAssistEnabled = isVoiceAssistActive,
                onToggleVoiceAssist = { isVoiceAssistActive = it },
                spokenNarration = spokenText,
                onPlayNarration = {
                    // Simulates immediate voice feedback
                }
            )
        }

        // Student Profile & High-Contrast Identity Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Ministry Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFD97706),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "MINISTRY OF TRIBAL AFFAIRS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Overflow Options Menu
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("dashboard_overflow_menu_btn")
                                    .semantics { contentDescription = "Options and Profile switcher menu" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Switch Student Profile") },
                                    leadingIcon = { Icon(Icons.Default.SwitchAccount, contentDescription = null, tint = Color(0xFFD97706)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenLoginSheet()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("App Tour & SIH Guide") },
                                    leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFF2563EB)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenIntroTour()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Security & Privacy (DPDP)") },
                                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF059669)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenSecurityModal()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Student Name & Large High-Contrast Avatar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF1E3A8A),
                            shape = CircleShape,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome back,",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = firstName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${student?.name ?: "Student"} · ${student?.category ?: "ST"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Verification Badges Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Text(
                                text = student?.pvtgCommunity?.takeIf { it.isNotBlank() } ?: "ST Beneficiary",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Aadhaar e-KYC Verified",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // High-Contrast Quick Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMiniBox(
                            label = "Disbursed",
                            value = "₹4,500",
                            sub = "DBT Credited",
                            color = Color(0xFF059669),
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniBox(
                            label = "In Pipeline",
                            value = "₹78,000",
                            sub = "Post-Matric ST",
                            color = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniBox(
                            label = "Wallet",
                            value = "5 Docs",
                            sub = "DigiLocker Synced",
                            color = Color(0xFFD97706),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Pending Actions / Human Review Notices (if any)
        item {
            PendingActionsCard(
                applications = applications,
                onOpenReviewDesk = onOpenReviewDesk,
                onOpenJago = onOpenJago
            )
        }

        // ==========================================
        // 1. CARD: "Eligible Scholarships" (Distinct Card)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_eligible_scholarships"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, Color(0xFF059669).copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF059669),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Eligible Scholarships",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Pre-qualified schemes for your tribal category",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF059669).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${eligibleSchemes.size.coerceAtLeast(1)} Available",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    if (eligibleSchemes.isEmpty()) {
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "You are currently enrolled in all applicable tribal scholarship schemes!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                    } else {
                        eligibleSchemes.forEach { scheme ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = scheme.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Grant: ${scheme.maxAmount}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFFD97706).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "100% MATCH",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD97706),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = scheme.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Prominent Touch-friendly CTA Button (Min 48dp)
                                    Button(
                                        onClick = { selectedSchemeForWizard = scheme },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("apply_wizard_btn_${scheme.code}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                    ) {
                                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Apply via Wizard (1-Click)", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. CARD: "My Applications" (Distinct Card)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_my_applications"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF2563EB),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Assignment, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "My Applications",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Active and approved submissions (${applications.size})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    applications.forEach { app ->
                        val schemeInfo = schemes.find { it.id == app.schemeId }
                        CompactSchemeApplicationCard(
                            application = app,
                            scheme = schemeInfo,
                            onClick = { onSelectScheme(app.id) }
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. CARD: "Track Status" (Distinct Card with Timeline)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_track_status"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, Color(0xFFD97706).copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFD97706),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Timeline, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Track Status",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Real-time lifecycle & DBT sanction progress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        latestAppForTracking?.let { app ->
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = app.schemeCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // 4-Stage Human-Friendly Timeline Tracker
                    val activeStage = latestAppForTracking?.currentStage ?: "UNDER_VERIFICATION"
                    val stageLevel = when (activeStage) {
                        "SUBMITTED" -> 1
                        "UNDER_VERIFICATION" -> 2
                        "SANCTIONED" -> 3
                        "DISBURSED" -> 4
                        else -> 2
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TrackingMilestoneRow(
                            stepNumber = 1,
                            title = "DigiLocker e-KYC Verified",
                            subtitle = "Aadhaar, Caste & Income auto-verified against state databases",
                            isCompleted = stageLevel >= 1,
                            isActive = stageLevel == 1
                        )
                        TrackingMilestoneRow(
                            stepNumber = 2,
                            title = "Institution & APAAR Verification",
                            subtitle = "NIT Rourkela nodal officer approved attendance & Bonafide",
                            isCompleted = stageLevel >= 2,
                            isActive = stageLevel == 2
                        )
                        TrackingMilestoneRow(
                            stepNumber = 3,
                            title = "District Tribal Welfare Officer",
                            subtitle = "State MoTA sanction order generated with digital signature",
                            isCompleted = stageLevel >= 3,
                            isActive = stageLevel == 3
                        )
                        TrackingMilestoneRow(
                            stepNumber = 4,
                            title = "DBT Bank Credit (PFMS Rail)",
                            subtitle = "Funds transferred directly to Aadhaar-seeded bank account",
                            isCompleted = stageLevel >= 4,
                            isActive = stageLevel == 4
                        )
                    }

                    // Direct Action Button to view detailed audit trail
                    Button(
                        onClick = {
                            latestAppForTracking?.let { onSelectScheme(it.id) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Complete Verification Checklist", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingMilestoneRow(
    stepNumber: Int,
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Step $stepNumber: $title. Status: ${if (isCompleted) "Completed" else if (isActive) "In Progress" else "Pending"}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = when {
                isCompleted -> Color(0xFF059669)
                isActive -> Color(0xFFD97706)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            shape = CircleShape,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
                color = if (isCompleted) Color(0xFF065F46) else if (isActive) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            Triple(Color(0xFFD97706), Color(0xFFFEF3C7), "Under Review (No action needed)")
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
