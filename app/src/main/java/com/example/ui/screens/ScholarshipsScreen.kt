package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.example.data.model.DocumentEntity
import com.example.data.model.SchemeEntity
import com.example.data.model.StudentEntity
import com.example.domain.EligibilityEngine
import com.example.domain.EligibilityResult
import com.example.domain.EligibilityStatus
import com.example.ui.components.ScholarshipWizardModal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScholarshipsScreen(
    student: StudentEntity?,
    schemes: List<SchemeEntity>,
    applications: List<ApplicationEntity>,
    documents: List<DocumentEntity>,
    onSelectApplication: (String) -> Unit,
    onApplyScheme: (String, Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedSchemeForWizard by remember { mutableStateOf<SchemeEntity?>(null) }

    val filteredSchemes = remember(schemes, applications, selectedFilter, student, documents) {
        when (selectedFilter) {
            "APPLIED" -> schemes.filter { sc -> applications.any { it.schemeId == sc.id } }
            "ELIGIBLE" -> {
                if (student == null) schemes
                else schemes.filter { sc ->
                    val result = EligibilityEngine.evaluate(student, sc, documents, applications)
                    result.status == EligibilityStatus.ELIGIBLE
                }
            }
            "NEEDS_ATTENTION" -> schemes.filter { sc -> applications.any { it.schemeId == sc.id && it.hasDiscrepancy } }
            else -> schemes
        }
    }

    selectedSchemeForWizard?.let { scheme ->
        ScholarshipWizardModal(
            scheme = scheme,
            student = student,
            documents = documents,
            isVoiceAssistActive = false,
            onDismiss = { selectedSchemeForWizard = null },
            onSubmitApplication = { schemeId ->
                onApplyScheme(schemeId, student?.annualIncome)
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
        // Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schemes_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color(0xFFD97706).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Ministry of Tribal Affairs (MoTA)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "5 National Tribal Scholarships",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Unified 1-click eligibility matching across all central and state tribal schemes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reusable credential banner
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Single-Wallet Rule: Attached DigiLocker documents are shared automatically without repetitive uploads.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Filter Chips Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf(
                    "ALL" to "All Schemes (5)",
                    "ELIGIBLE" to "Eligible for Me",
                    "APPLIED" to "Applied (${applications.size})",
                    "NEEDS_ATTENTION" to "Needs Attention"
                )

                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Schemes List
        items(filteredSchemes, key = { it.id }) { scheme ->
            val existingApp = applications.find { it.schemeId == scheme.id }
            val eligibility = if (student != null) {
                EligibilityEngine.evaluate(student, scheme, documents, applications)
            } else null

            SchemeOpportunityCard(
                scheme = scheme,
                existingApplication = existingApp,
                eligibility = eligibility,
                onViewApplication = { existingApp?.let { onSelectApplication(it.id) } },
                onApply = { selectedSchemeForWizard = scheme }
            )
        }
    }
}

@Composable
fun SchemeOpportunityCard(
    scheme: SchemeEntity,
    existingApplication: ApplicationEntity?,
    eligibility: EligibilityResult?,
    onViewApplication: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scheme_card_${scheme.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.2.dp,
            when {
                existingApplication?.hasDiscrepancy == true -> Color(0xFFD97706)
                existingApplication != null -> Color(0xFF059669).copy(alpha = 0.6f)
                eligibility?.status == EligibilityStatus.ELIGIBLE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Scheme Code & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = scheme.code,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (existingApplication != null) {
                    val badgeColor = if (existingApplication.hasDiscrepancy) Color(0xFFD97706) else Color(0xFF059669)
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (existingApplication.hasDiscrepancy) "Reviewing Exception" else existingApplication.currentStage.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (eligibility != null) {
                    val (statusText, color) = when (eligibility.status) {
                        EligibilityStatus.ELIGIBLE -> "Eligible (100% Match)" to Color(0xFF059669)
                        EligibilityStatus.NEEDS_REVIEW -> "${eligibility.matchPercentage}% Match" to Color(0xFFD97706)
                        EligibilityStatus.NOT_ELIGIBLE -> "Not Eligible" to Color(0xFF64748B)
                    }
                    Surface(
                        color = color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = scheme.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = scheme.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Key Highlights: Grant Amount & Deadline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Grant / Benefit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(scheme.maxAmount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Portal Deadline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(scheme.deadline, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            // Eligibility Recommendation Note
            if (eligibility != null && existingApplication == null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (eligibility.status == EligibilityStatus.ELIGIBLE) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (eligibility.status == EligibilityStatus.ELIGIBLE) Color(0xFF059669) else Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = eligibility.summaryRecommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            if (existingApplication != null) {
                Button(
                    onClick = onViewApplication,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Track 6-Stage Lifecycle (${existingApplication.currentStage.replace("_", " ")})")
                }
            } else {
                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_scheme_btn_${scheme.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (eligibility?.status == EligibilityStatus.ELIGIBLE) MaterialTheme.colorScheme.primary else Color(0xFF475569)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply in 1 Click (Pre-attached DigiLocker)")
                }
            }
        }
    }
}
