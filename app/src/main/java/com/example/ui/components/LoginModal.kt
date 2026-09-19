package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.StudentEntity
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginModal(
    currentStudent: StudentEntity?,
    allStudents: List<StudentEntity>,
    onSelectStudent: (String) -> Unit,
    onLoginWithPhone: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Quick Switch Persona, 1: Mobile / Aadhaar OTP
    var mobileOrAadhaar by remember { mutableStateOf("+91 98765 43210") }
    var studentNameInput by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("123456") }
    var loginStep by remember { mutableStateOf(1) } // 1: Enter Number, 2: Enter OTP
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var timerSeconds by remember { mutableStateOf(30) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(loginStep) {
        if (loginStep == 2) {
            timerSeconds = 30
            isTimerRunning = true
            while (timerSeconds > 0 && isTimerRunning) {
                delay(1000)
                timerSeconds--
            }
            isTimerRunning = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("login_modal_sheet"),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Student Beneficiary Authentication",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Prototype authentication • UIDAI Aadhaar e-KYC & Multi-Persona Rail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        errorMessage = null
                    },
                    text = { Text("Demo Personas (3)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        errorMessage = null
                    },
                    text = { Text("Aadhaar / Mobile OTP", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Tab 0: Demo Personas
                Text(
                    text = "Select a pre-configured student profile to test different scholarship categories, academic levels, and eligibility scenarios:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                allStudents.forEach { stu ->
                    val isCurrent = stu.id == currentStudent?.id
                    OutlinedCard(
                        onClick = {
                            onSelectStudent(stu.id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .testTag("persona_card_${stu.id}"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) MaterialTheme.colorScheme.primary else Color(0xFF64748B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stu.name.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = stu.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${stu.category} • ${stu.course}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stu.institutionName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (isCurrent) {
                                Surface(
                                    color = Color(0xFF059669).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF059669),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Live Mock OTP State Machine
                if (loginStep == 1) {
                    Text(
                        text = "Enter any 10-digit mobile number or 12-digit Aadhaar number to simulate live OTP authentication with UIDAI:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = studentNameInput,
                        onValueChange = { studentNameInput = it },
                        label = { Text("Full Name (Optional)") },
                        placeholder = { Text("e.g. Ramesh Hansda") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_student_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = mobileOrAadhaar,
                        onValueChange = {
                            mobileOrAadhaar = it
                            errorMessage = null
                        },
                        label = { Text("Mobile Number or Aadhaar (12 Digits)") },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mobile_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (mobileOrAadhaar.isBlank()) {
                                errorMessage = "Please enter a valid mobile number or Aadhaar."
                            } else {
                                errorMessage = null
                                loginStep = 2
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_otp_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get OTP (UIDAI Rail)")
                    }
                } else {
                    // Step 2: OTP Verification
                    Text(
                        text = "Enter the 6-digit OTP sent to $mobileOrAadhaar",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = otp,
                        onValueChange = {
                            otp = it
                            errorMessage = null
                        },
                        label = { Text("6-Digit OTP") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = errorMessage != null
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 SIH Demo: Mock OTP is 123456",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF059669)
                        )

                        if (timerSeconds > 0) {
                            Text(
                                text = "Resend in ${timerSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            TextButton(
                                onClick = {
                                    timerSeconds = 30
                                    isTimerRunning = true
                                    otp = "123456"
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Resend OTP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (timerSeconds <= 0 && !isTimerRunning) {
                                errorMessage = "This OTP has expired. Request a new OTP."
                            } else if (otp.trim() == "123456") {
                                errorMessage = null
                                onLoginWithPhone(mobileOrAadhaar, studentNameInput.ifBlank { null })
                            } else {
                                errorMessage = "That OTP is not correct. Please try again."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_otp_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify & Authenticate Profile")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            loginStep = 1
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("← Change Mobile / Aadhaar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Disclaimer Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔒 DPDP Act 2023 Compliant: Session tokens and personal identities are encrypted with AES-256 and never shared with unauthorized 3rd parties.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
