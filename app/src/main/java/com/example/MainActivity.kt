package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppLanguage
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.EkikritTheme
import com.example.ui.util.LocalAppStrings
import com.example.ui.util.getAppStrings
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.EkikritViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EkikritTheme {
                EkikritMainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EkikritMainApp(
    viewModel: EkikritViewModel = viewModel()
) {
    val student by viewModel.student.collectAsStateWithLifecycle()
    val schemes by viewModel.schemes.collectAsStateWithLifecycle()
    val applications by viewModel.applications.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val disbursements by viewModel.disbursements.collectAsStateWithLifecycle()
    val allReviewItems by viewModel.allReviewItems.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedAppId by viewModel.selectedApplicationId.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isJagoOpen by viewModel.isJagoChatOpen.collectAsStateWithLifecycle()
    val showConsentDialog by viewModel.showConsentDialog.collectAsStateWithLifecycle()
    val showLoginSheet by viewModel.showLoginSheet.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulatingVerification.collectAsStateWithLifecycle()
    val userNotice by viewModel.userNotice.collectAsStateWithLifecycle()
    val jagoMessages by viewModel.jagoMessages.collectAsStateWithLifecycle()

    val strings = getAppStrings(selectedLanguage)

    val snackbarHostState = remember { SnackbarHostState() }
    var showIntroTour by remember { mutableStateOf(true) } // Shows on start for all users
    var showMoreMenu by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAuditModal by remember { mutableStateOf(false) }
    var showSecurityModal by remember { mutableStateOf(false) }
    var showShareModal by remember { mutableStateOf(false) }

    LaunchedEffect(userNotice) {
        userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.clearNotice()
        }
    }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Surface(
                                color = Color(0xFFD97706),
                                shape = CircleShape,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "ए",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = strings.appTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFF059669).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "SIH26238",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = strings.ministryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        // 1. App Intro Tour Button
                        IconButton(
                            onClick = { showIntroTour = true },
                            modifier = Modifier.testTag("app_intro_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = strings.appGuide,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 2. Demo Mode Switcher: Jump between Student & Reviewer Desk
                        val isAtReviewerDesk = currentTab == AppTab.REVIEWER_QUEUE
                        Button(
                            onClick = {
                                viewModel.selectTab(
                                    if (isAtReviewerDesk) AppTab.DASHBOARD else AppTab.REVIEWER_QUEUE
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAtReviewerDesk) Color(0xFF059669) else Color(0xFFD97706)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("demo_switcher_btn")
                        ) {
                            Icon(
                                imageVector = if (isAtReviewerDesk) Icons.Default.School else Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAtReviewerDesk) strings.studentMode else strings.reviewerDeskMode,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // 3. Overflow Menu for Security, Audit, and Language
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("top_app_bar_more_menu")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Switch User / Login") },
                                    leadingIcon = {
                                        Icon(Icons.Default.SwitchAccount, contentDescription = null, tint = Color(0xFFD97706))
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.toggleLoginSheet(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share App with Friends") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF2563EB))
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showShareModal = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.securityMenu) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF059669))
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showSecurityModal = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.auditTrailMenu) },
                                    leadingIcon = {
                                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showAuditModal = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("${strings.languageMenu} (${selectedLanguage.displayName})") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Translate, contentDescription = null, tint = Color(0xFFD97706))
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showLanguageDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.replayTourMenu) },
                                    leadingIcon = {
                                        Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color(0xFF2563EB))
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showIntroTour = true
                                    }
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (selectedAppId == null) {
                    NavigationBar(
                        modifier = Modifier.testTag("main_navigation_bar"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        val tabs = listOf(
                            Triple(AppTab.DASHBOARD, Icons.Default.Dashboard, strings.tabDashboard),
                            Triple(AppTab.SCHEMES, Icons.Default.School, strings.tabSchemes),
                            Triple(AppTab.DOCUMENTS, Icons.Default.FolderShared, strings.tabWallet),
                            Triple(AppTab.DISBURSEMENT, Icons.Default.Payments, strings.tabDbtRail),
                            Triple(AppTab.REVIEWER_QUEUE, Icons.Default.AdminPanelSettings, strings.tabReviewDesk)
                        )

                        tabs.forEach { (tab, icon, label) ->
                            val hasPendingItems = tab == AppTab.REVIEWER_QUEUE && allReviewItems.any { it.status == "PENDING" }

                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (hasPendingItems) {
                                                Badge(containerColor = Color(0xFFD97706)) {
                                                    Text("1")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_tab_${tab.name}")
                            )
                        }
                    }
                }
            },
        floatingActionButton = {
            if (!isJagoOpen && selectedAppId == null) {
                JagoFloatingButton(
                    onClick = { viewModel.toggleJagoChat(true) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Check if user drilled into a scheme detail
            if (selectedAppId != null) {
                val selectedApp = applications.find { it.id == selectedAppId }
                val selectedScheme = schemes.find { it.id == selectedApp?.schemeId }
                val verRecordsFlow = viewModel.getVerificationRecordsForApp(selectedAppId ?: "")
                val verRecords by verRecordsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                SchemeDetailScreen(
                    application = selectedApp,
                    scheme = selectedScheme,
                    documents = documents,
                    verificationRecords = verRecords,
                    onBack = { viewModel.closeApplicationDetail() },
                    onTriggerVerification = { viewModel.triggerVerification(it) },
                    onOpenReviewDesk = {
                        viewModel.closeApplicationDetail()
                        viewModel.selectTab(AppTab.REVIEWER_QUEUE)
                    },
                    onPullDocument = { type, title, num, issuer ->
                        viewModel.pullDigiLockerDocument(type, title, num, issuer)
                    },
                    isSimulating = isSimulating
                )
            } else {
                when (currentTab) {
                    AppTab.DASHBOARD -> {
                        DashboardScreen(
                            student = student,
                            applications = applications,
                            schemes = schemes,
                            onSelectScheme = { viewModel.openApplicationDetail(it) },
                            onOpenReviewDesk = { viewModel.selectTab(AppTab.REVIEWER_QUEUE) },
                            onOpenJago = { viewModel.toggleJagoChat(true) },
                            onApplyUnreached = { viewModel.applyForUnreachedScheme(it) },
                            onOpenConsentDialog = { viewModel.toggleConsentDialog(true) },
                            onOpenSecurityModal = { showSecurityModal = true },
                            onOpenIntroTour = { showIntroTour = true },
                            onOpenLoginSheet = { viewModel.toggleLoginSheet(true) }
                        )
                    }
                    AppTab.SCHEMES -> {
                        DashboardScreen(
                            student = student,
                            applications = applications,
                            schemes = schemes,
                            onSelectScheme = { viewModel.openApplicationDetail(it) },
                            onOpenReviewDesk = { viewModel.selectTab(AppTab.REVIEWER_QUEUE) },
                            onOpenJago = { viewModel.toggleJagoChat(true) },
                            onApplyUnreached = { viewModel.applyForUnreachedScheme(it) },
                            onOpenConsentDialog = { viewModel.toggleConsentDialog(true) },
                            onOpenSecurityModal = { showSecurityModal = true },
                            onOpenIntroTour = { showIntroTour = true },
                            onOpenLoginSheet = { viewModel.toggleLoginSheet(true) }
                        )
                    }
                    AppTab.DOCUMENTS -> {
                        DocumentsWalletScreen(
                            documents = documents,
                            isDigiLockerLinked = student?.isDigiLockerLinked ?: true,
                            onConnectDigiLocker = { phoneOrAadhaar ->
                                viewModel.connectDigiLocker(phoneOrAadhaar)
                            },
                            onPullNewDocument = { type, title, num, issuer ->
                                viewModel.pullDigiLockerDocument(type, title, num, issuer)
                            },
                            onOpenConsentDialog = { viewModel.toggleConsentDialog(true) }
                        )
                    }
                    AppTab.DISBURSEMENT -> {
                        DisbursementScreen(
                            student = student,
                            disbursements = disbursements,
                            applications = applications
                        )
                    }
                    AppTab.REVIEWER_QUEUE -> {
                        ReviewerDeskScreen(
                            reviewItems = allReviewItems,
                            onResolve = { id, approved, notes ->
                                viewModel.resolveReviewItem(id, approved, notes)
                            },
                            onBackToStudentView = {
                                viewModel.selectTab(AppTab.DASHBOARD)
                            }
                        )
                    }
                }
            }
        }
    }

    // Starting Intro Tour (Walkthrough of all sections & app information)
    if (showIntroTour) {
        AppIntroTourModal(
            onDismiss = { showIntroTour = false }
        )
    }

    // JAGO Floating Multilingual Assistant
    if (isJagoOpen) {
        JagoChatModal(
            messages = jagoMessages,
            currentLanguage = selectedLanguage,
            onLanguageSelect = { viewModel.setLanguage(it) },
            onSendMessage = { viewModel.sendJagoQuery(it) },
            onDismiss = { viewModel.toggleJagoChat(false) }
        )
    }

    // DPDP Act Consent Dialog
    if (showConsentDialog) {
        DpdpConsentDialog(
            hasConsentGiven = student?.hasConsentGiven ?: true,
            onConfirm = { granted -> viewModel.setConsent(granted) },
            onDismiss = { viewModel.toggleConsentDialog(false) }
        )
    }

    // Security & DPDP Compliance Modal
    if (showSecurityModal) {
        SecurityPrivacyModal(
            hasConsent = student?.hasConsentGiven ?: true,
            onRevokeOrGrantConsent = { granted -> viewModel.setConsent(granted) },
            onDismiss = { showSecurityModal = false }
        )
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = Color(0xFFD97706))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.selectLanguageTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLanguage == lang,
                                onClick = {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${lang.nativeName} (${lang.displayName})", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Login & Switch User Modal
    if (showLoginSheet) {
        LoginModal(
            currentStudent = student,
            allStudents = allStudents,
            onSelectStudent = { studentId ->
                viewModel.switchStudent(studentId)
            },
            onLoginWithPhone = { identifier, name ->
                viewModel.loginWithMobileOrAadhaar(identifier, name)
            },
            onDismiss = { viewModel.toggleLoginSheet(false) }
        )
    }

    // Share App Modal
    if (showShareModal) {
        ShareAppModal(
            onDismiss = { showShareModal = false }
        )
    }

    // DPDP Immutable Audit Log Modal
    if (showAuditModal) {
        AlertDialog(
            onDismissRequest = { showAuditModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DPDP Act Governance Audit Trail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxHeight(0.7f)) {
                    AuditTrailScreen(auditLogs = auditLogs)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuditModal = false }) {
                    Text(strings.close)
                }
            }
        )
    }
    }
}
