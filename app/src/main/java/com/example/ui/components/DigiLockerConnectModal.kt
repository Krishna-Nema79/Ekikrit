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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

enum class DigiLockerStep {
    LOGIN,
    OTP,
    CONSENT,
    FETCHING,
    SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigiLockerConnectModal(
    onDismiss: () -> Unit,
    onSuccess: (mobileOrAadhaar: String) -> Unit,
    studentName: String = "Birsa Munda Tirkey",
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(DigiLockerStep.LOGIN) }
    var identifier by remember { mutableStateOf("+91 98765 43210") }
    var pin by remember { mutableStateOf("202601") }
    var otp by remember { mutableStateOf("") }
    var otpTimer by remember { mutableIntStateOf(30) }
    var fetchingProgress by remember { mutableFloatStateOf(0.1f) }
    var fetchingStatusText by remember { mutableStateOf("Contacting National Root Authority...") }

    // Stepper logic for simulated live fetching
    LaunchedEffect(step) {
        if (step == DigiLockerStep.FETCHING) {
            fetchingProgress = 0.2f
            fetchingStatusText = "Connecting to DigiLocker MeriPehchaan Gateway..."
            delay(600)
            fetchingProgress = 0.4f
            fetchingStatusText = "UIDAI Aadhaar demographic authentication passed (99.1% match)..."
            delay(700)
            fetchingProgress = 0.65f
            fetchingStatusText = "Pulling e-District ST Certificate (PVTG Birhor) from Bonai..."
            delay(700)
            fetchingProgress = 0.85f
            fetchingStatusText = "Validating Income Certificate & APAAR ID from NAD..."
            delay(600)
            fetchingProgress = 1.0f
            fetchingStatusText = "SHA-256 cryptographic signature verified. Credentials ready!"
            delay(500)
            step = DigiLockerStep.SUCCESS
        }
    }

    LaunchedEffect(step) {
        if (step == DigiLockerStep.OTP) {
            otpTimer = 30
            while (otpTimer > 0) {
                delay(1000)
                otpTimer--
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (step != DigiLockerStep.FETCHING) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .testTag("digilocker_connection_modal"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // DigiLocker Header Banner
                Surface(
                    color = Color(0xFF0284C7).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF0284C7),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "DigiLocker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF059669).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Govt. of India",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "MeriPehchaan National Single Sign-On",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            enabled = step != DigiLockerStep.FETCHING
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Steps Rendering
                AnimatedContent(targetState = step, label = "DigiLockerSteps") { targetStep ->
                    when (targetStep) {
                        DigiLockerStep.LOGIN -> {
                            Column {
                                Text(
                                    text = "Sign In with DigiLocker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Connect your account to pull digitally signed academic and caste credentials instantly without physical scanning.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                                )

                                OutlinedTextField(
                                    value = identifier,
                                    onValueChange = { identifier = it },
                                    label = { Text("Aadhaar / Registered Mobile Number") },
                                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("digilocker_mobile_input"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = pin,
                                    onValueChange = { pin = it },
                                    label = { Text("6-Digit DigiLocker Security PIN") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("digilocker_pin_input"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            identifier = "+91 98765 43210"
                                            pin = "202601"
                                        }
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Auto-fill Student Demo", style = MaterialTheme.typography.labelSmall)
                                    }

                                    Text(
                                        text = "Forgot PIN?",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { step = DigiLockerStep.OTP },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("digilocker_signin_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Text("Proceed to OTP Verification", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        DigiLockerStep.OTP -> {
                            Column {
                                Text(
                                    text = "Enter DigiLocker 2-Step OTP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "A 6-digit security code was dispatched to Aadhaar-linked phone: $identifier",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                                )

                                OutlinedTextField(
                                    value = otp,
                                    onValueChange = { otp = it.take(6) },
                                    label = { Text("6-Digit OTP") },
                                    placeholder = { Text("892410") },
                                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("digilocker_otp_input"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { otp = "892410" }) {
                                        Text("Auto-fill Demo OTP (892410)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF059669))
                                    }

                                    Text(
                                        text = if (otpTimer > 0) "Resend in ${otpTimer}s" else "Resend OTP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (otpTimer > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { step = DigiLockerStep.CONSENT },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("digilocker_submit_otp_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Text("Verify & Continue to Consent", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        DigiLockerStep.CONSENT -> {
                            Column {
                                Text(
                                    text = "Authorize Credential Sharing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Under the Digital Personal Data Protection (DPDP) Act, 2023, Ekikrit requests permission to pull the following records:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                val requestedCredentials = listOf(
                                    Pair("Aadhaar Demographic Record", "UIDAI Government of India"),
                                    Pair("Scheduled Tribe Caste Certificate", "e-District Bonai, Odisha"),
                                    Pair("Annual Family Income Certificate", "Revenue Dept, Odisha"),
                                    Pair("Higher Secondary (Class XII) Marksheet", "CHSE Odisha"),
                                    Pair("One Nation One Student ID (APAAR/ABC)", "Ministry of Education / NAD")
                                )

                                requestedCredentials.forEach { (docTitle, issuer) ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(docTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                Text(issuer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    color = Color(0xFF059669).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Purpose Bound: Used strictly for Ministry of Tribal Affairs 5 Scholarship Schemes. You can revoke access anytime.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF065F46)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { step = DigiLockerStep.FETCHING },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("digilocker_allow_consent_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Grant Consent & Pull Credentials", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        DigiLockerStep.FETCHING -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    progress = { fetchingProgress },
                                    modifier = Modifier.size(54.dp),
                                    color = Color(0xFF0284C7),
                                    trackColor = Color(0xFF0284C7).copy(alpha = 0.2f),
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Syncing Digital Locker...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = fetchingStatusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }

                        DigiLockerStep.SUCCESS -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    color = Color(0xFF059669).copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "DigiLocker Connected Successfully!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "5 verified credentials have been synced into your Ekikrit Wallet. Zero physical scanning or document re-upload required across all 5 schemes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Surface(
                                    color = Color(0xFF0284C7).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Linked Identity: $studentName", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("UIDAI Verification: Aadhaar XXXX-8924 (Active)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0369A1))
                                        Text("Odisha e-District: ST & Income Registered", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0369A1))
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = {
                                        onSuccess(identifier)
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("digilocker_finish_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View Synced Documents Wallet", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
