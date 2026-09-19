package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ApplicationEntity
import com.example.data.model.DocumentEntity
import com.example.data.model.SchemeEntity
import com.example.data.model.VerificationRecordEntity
import com.example.ui.components.ApplicationTimeline
import com.example.ui.components.VerificationChecklist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeDetailScreen(
    application: ApplicationEntity?,
    scheme: SchemeEntity?,
    documents: List<DocumentEntity>,
    verificationRecords: List<VerificationRecordEntity>,
    onBack: () -> Unit,
    onTriggerVerification: (String) -> Unit,
    onOpenReviewDesk: () -> Unit,
    onPullDocument: (String, String, String, String) -> Unit,
    isSimulating: Boolean,
    modifier: Modifier = Modifier
) {
    var showPullDocSheet by remember { mutableStateOf(false) }

    if (application == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Scheme application not found.")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = application.schemeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "App ID: ${application.id} • ${scheme?.portalOrigin ?: "NSP"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("scheme_detail_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Live Status Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (application.hasDiscrepancy) Color(0xFFF59E0B).copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (application.hasDiscrepancy) Icons.Default.WarningAmber else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (application.hasDiscrepancy) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val stageNameFormatted = when (application.currentStage) {
                            "SUBMITTED" -> "Application Submitted"
                            "INSTITUTE_VERIFICATION", "UNDER_VERIFICATION" -> "Institute Verification"
                            "STATE_VERIFICATION" -> "State Verification"
                            "MINISTRY_REVIEW" -> "Ministry Review"
                            "SANCTIONED" -> "Sanctioned"
                            "DISBURSED" -> "Payment Disbursed"
                            else -> application.currentStage
                        }
                        Text(
                            text = if (application.hasDiscrepancy) "Exception Routing Active" else "Current Stage: $stageNameFormatted",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (application.hasDiscrepancy) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = application.statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (application.hasDiscrepancy) {
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onOpenReviewDesk,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFD97706),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jump_to_reviewer_desk_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resolve Mismatch in Reviewer Queue (Live Demo)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stepper Timeline
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Application Lifecycle Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Submission → Verification → Sanction → Disbursement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    ApplicationTimeline(application = application)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-source Verification Checklist
            VerificationChecklist(
                records = verificationRecords,
                onTriggerReverification = { onTriggerVerification(application.id) },
                isSimulating = isSimulating
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Section 4.4: DigiLocker Single-Wallet Integration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Linked DigiLocker Documents",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Zero re-upload architecture across all 5 schemes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showPullDocSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Pull Document",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    documents.take(3).forEach { doc ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = doc.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        Text(text = "${doc.source} • ${doc.docNumberMasked}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Surface(
                                    color = Color(0xFF059669).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = doc.verificationStatus,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF059669),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showPullDocSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pull Additional Certificate from DigiLocker")
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showPullDocSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPullDocSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Pull Document from DigiLocker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select an authentic digital credential from national DigiLocker registry to attach to ${application.schemeCode}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                val availableMockDocs = listOf(
                    Triple("ST Caste Certificate (Updated)", "ST/OD/2026/10294", "Tehsildar Bonai"),
                    Triple("Institute Bona-fide Certificate", "NITR/ACAD/2026/81", "NIT Rourkela Registrar"),
                    Triple("Family Ration / Food Security Card", "RC/OD/SNG/8271", "Food & Supplies Dept Odisha")
                )

                availableMockDocs.forEach { (title, number, issuer) ->
                    OutlinedCard(
                        onClick = {
                            onPullDocument("Other", title, number, issuer)
                            showPullDocSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(text = "$issuer • $number", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            }
                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
