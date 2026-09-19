package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DocumentEntity
import com.example.data.model.SchemeEntity
import com.example.data.model.StudentEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScholarshipWizardModal(
    scheme: SchemeEntity,
    student: StudentEntity?,
    documents: List<DocumentEntity>,
    isVoiceAssistActive: Boolean = false,
    onDismiss: () -> Unit,
    onSubmitApplication: (String) -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 4

    // Form states
    var applicantName by remember { mutableStateOf(student?.name ?: "Birsa Munda Tirkey") }
    var mobileNumber by remember { mutableStateOf(student?.mobile ?: "+91 98765 43210") }
    var tribalCategory by remember { mutableStateOf(student?.category ?: "ST (PVTG - Birhor)") }
    var apaarId by remember { mutableStateOf(student?.apaarId ?: "APAAR-8839-4021-9920") }
    var institutionName by remember { mutableStateOf(student?.institutionName ?: "National Institute of Technology, Rourkela") }
    var courseName by remember { mutableStateOf(student?.course ?: "B.Tech Computer Science & Engineering") }
    var annualIncome by remember { mutableStateOf("₹${student?.annualIncome?.toInt() ?: 210000} / year") }
    var consentGranted by remember { mutableStateOf(true) }

    val stepNarration = when (currentStep) {
        1 -> "Step 1 of 4: Personal and Tribal Identity details pulled automatically from your DigiLocker profile."
        2 -> "Step 2 of 4: Academic enrollment and APAAR ID verification from National Academic Depository."
        3 -> "Step 3 of 4: Scheme selection and grant eligibility confirmation under Ministry of Tribal Affairs."
        4 -> "Step 4 of 4: Final 1-click submission. Review your verified credentials and submit without physical paper upload."
        else -> ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("scholarship_wizard_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Close & Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Surface(
                            color = Color(0xFFD97706).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "STEP $currentStep OF $totalSteps",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (currentStep) {
                                1 -> "Personal & Identity"
                                2 -> "Academic & Institute"
                                3 -> "Scheme Eligibility"
                                4 -> "Review & 1-Click Apply"
                                else -> "Application"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Close application wizard" }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar with Percentage
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { currentStep / totalSteps.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF059669),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(currentStep * 25)}% Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = scheme.code,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Voice Assist Guided Tip for Step
                if (isVoiceAssistActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stepNarration,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step Content Body (Scrollable)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (currentStep) {
                        1 -> StepPersonalDetails(
                            name = applicantName,
                            onNameChange = { applicantName = it },
                            mobile = mobileNumber,
                            onMobileChange = { mobileNumber = it },
                            category = tribalCategory,
                            income = annualIncome,
                            student = student
                        )
                        2 -> StepAcademicDetails(
                            apaarId = apaarId,
                            onApaarChange = { apaarId = it },
                            institution = institutionName,
                            onInstitutionChange = { institutionName = it },
                            course = courseName,
                            onCourseChange = { courseName = it }
                        )
                        3 -> StepSchemeDetails(
                            scheme = scheme,
                            category = tribalCategory
                        )
                        4 -> StepFinalReview(
                            scheme = scheme,
                            name = applicantName,
                            category = tribalCategory,
                            institution = institutionName,
                            documents = documents,
                            consentGranted = consentGranted,
                            onConsentToggle = { consentGranted = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action CTA Bar with Min 48dp Touch Targets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                currentStep++
                            } else {
                                onSubmitApplication(scheme.id)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(52.dp)
                            .testTag("wizard_primary_cta"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == totalSteps) Color(0xFF059669) else Color(0xFFD97706)
                        )
                    ) {
                        Text(
                            text = if (currentStep == totalSteps) "Submit Application (1-Click)" else "Continue to Step ${currentStep + 1}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentStep == totalSteps) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepPersonalDetails(
    name: String,
    onNameChange: (String) -> Unit,
    mobile: String,
    onMobileChange: (String) -> Unit,
    category: String,
    income: String,
    student: StudentEntity?
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            color = Color(0xFF059669).copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF059669))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Aadhaar e-KYC Verified via DigiLocker. Fields pre-filled for low friction.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF065F46),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Student Full Name") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Student Full Name Input: $name" },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = mobile,
            onValueChange = onMobileChange,
            label = { Text("Registered Mobile Number (OTP Verified)") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Registered Mobile Number: $mobile" },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = category,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tribal Category / PVTG Classification") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
            trailingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = "Verified Certificate", tint = Color(0xFF059669)) },
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = income,
            onValueChange = {},
            readOnly = true,
            label = { Text("Annual Family Income (Revenue Dept Validated)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
            trailingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = "Income Verified", tint = Color(0xFF059669)) },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun StepAcademicDetails(
    apaarId: String,
    onApaarChange: (String) -> Unit,
    institution: String,
    onInstitutionChange: (String) -> Unit,
    course: String,
    onCourseChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            color = Color(0xFF2563EB).copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF2563EB))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "One Nation One Student ID (APAAR/ABC) automatically synchronizes course details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1E40AF),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        OutlinedTextField(
            value = apaarId,
            onValueChange = onApaarChange,
            label = { Text("APAAR / EduLocker ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = institution,
            onValueChange = onInstitutionChange,
            label = { Text("AISHE Recognised Institution") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = course,
            onValueChange = onCourseChange,
            label = { Text("Course & Semester") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun StepSchemeDetails(
    scheme: SchemeEntity,
    category: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = scheme.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Grant Assistance: ${scheme.maxAmount}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = scheme.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            color = Color(0xFFECFDF5),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Eligibility Pre-Checked: 100% Match",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46)
                    )
                    Text(
                        text = "Tribal status '$category' & family income qualify under MoTA guidelines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF047857)
                    )
                }
            }
        }
    }
}

@Composable
fun StepFinalReview(
    scheme: SchemeEntity,
    name: String,
    category: String,
    institution: String,
    documents: List<DocumentEntity>,
    consentGranted: Boolean,
    onConsentToggle: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Review Verified Documents (Zero Paper Re-upload)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        documents.take(4).forEach { doc ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF059669))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = doc.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Issuer: ${doc.issuedBy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        color = Color(0xFF059669).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "VERIFIED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // DPDP Consent Checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = consentGranted,
                onCheckedChange = onConsentToggle,
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF059669))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "I consent to share my verified DigiLocker credentials for ${scheme.code} processing under DPDP Act 2023.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
