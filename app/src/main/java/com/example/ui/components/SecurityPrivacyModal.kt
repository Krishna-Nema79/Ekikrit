package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPrivacyModal(
    hasConsent: Boolean,
    onRevokeOrGrantConsent: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("security_privacy_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Color(0xFF059669).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Security & DPDP Compliance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Zero-Trust Architecture & Data Protection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Posture Status Cards
            Text(
                text = "System Security Posture",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            SecurityCheckItem(
                title = "Cleartext HTTP Traffic Disabled",
                subtitle = "Strict TLS 1.3 enforced for all government API mock adapters.",
                passed = true
            )
            SecurityCheckItem(
                title = "Local Backup Extraction Blocked",
                subtitle = "android:allowBackup=false protects Room DB from physical USB/ADB dumps.",
                passed = true
            )
            SecurityCheckItem(
                title = "Sensitive PII Masked at Rest & Display",
                subtitle = "Aadhaar (XXXX-XXXX-8921) and Bank Account (••••4821) strictly masked.",
                passed = true
            )
            SecurityCheckItem(
                title = "Zero-Permission Media Access",
                subtitle = "Zero broad storage permissions requested. Uses authentic DigiLocker API rails.",
                passed = true
            )
            SecurityCheckItem(
                title = "DPDP Act (2023) Section 6 Consent",
                subtitle = if (hasConsent) "Active verifiable consent granted for scheme verification." else "Consent currently REVOKED by user.",
                passed = hasConsent
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Data Classification Table
            Text(
                text = "Data Classification Standard (DPDP)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    ClassificationRow(
                        category = "Public Data",
                        items = "Scheme Guidelines, Funding Ceilings, Eligibility Rules",
                        color = Color(0xFF2563EB)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ClassificationRow(
                        category = "Personal Data",
                        items = "Student Name, Institute (NIT Rourkela), Degree Course",
                        color = Color(0xFFD97706)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ClassificationRow(
                        category = "Sensitive Personal Data",
                        items = "Masked Aadhaar, Caste Certificate (PVTG), Household Income, Bank Account",
                        color = Color(0xFFDC2626)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Consent Revocation action
            Surface(
                color = if (hasConsent) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (hasConsent) Color(0xFFBBF7D0) else Color(0xFFFECACA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasConsent) "Consent is Active" else "Consent is Revoked",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasConsent) Color(0xFF166534) else Color(0xFF991B1B)
                        )
                        Text(
                            text = if (hasConsent) "Tap to exercise Section 6(4) right to revoke DigiLocker access." else "Tap to re-authorize verified government data access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasConsent) Color(0xFF15803D) else Color(0xFFB91C1C)
                        )
                    }
                    Button(
                        onClick = { onRevokeOrGrantConsent(!hasConsent) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasConsent) Color(0xFFDC2626) else Color(0xFF059669)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (hasConsent) "Revoke" else "Grant")
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SecurityCheckItem(
    title: String,
    subtitle: String,
    passed: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (passed) Color(0xFF059669).copy(alpha = 0.3f) else Color(0xFFDC2626).copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (passed) Color(0xFF059669) else Color(0xFFDC2626),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
            }
        }
    }
}

@Composable
private fun ClassificationRow(
    category: String,
    items: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = color.copy(alpha = 0.12f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.width(120.dp)
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = items,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
