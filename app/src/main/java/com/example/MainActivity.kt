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
import com.example.ui.viewmodel.UserMode

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
    val reviewQueue by viewModel.reviewQueue.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val userMode by viewModel.userMode.collectAsStateWithLifecycle()

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedAppId by viewModel.selectedApplicationId.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isJagoOpen by viewModel.isJagoChatOpen.collectAsStateWithLifecycle()
    val showConsentDialog by viewModel.showConsentDialog.collectAsStateWithLifecycle()
    val showLoginSheet by viewModel.showLoginSheet.collectAsStateWithLifecycle()
    val showNotificationsSheet by viewModel.showNotificationsSheet.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulatingVerification.collectAsStateWithLifecycle()
    val userNotice by viewModel.userNotice.collectAsStateWithLifecycle()
    val jagoMessages by viewModel.jagoMessages.collectAsStateWithLifecycle()
    val scholarshipMatch by viewModel.scholarshipMatch.collectAsStateWithLifecycle()

    val strings = getAppStrings(selectedLanguage)

    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("ekikrit_prefs", android.content.Context.MODE_PRIVATE) }
    var showIntroTour by remember {
        mutableStateOf(!sharedPrefs.getBoolean("has_completed_onboarding", false))
    }
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

    val unreadNotifCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    val pendingReviewCount = remember(reviewQueue) {
        reviewQueue.count { it.status == "PENDING" }
    }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column {
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
                                        text = if (userMode == UserMode.OFFICER) "District Tribal Welfare Officer Desk" else strings.ministryName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        actions = {
                            // 1. Notification Bell
                            IconButton(
                                onClick = { viewModel.toggleNotificationsSheet(true) },
                                modifier = Modifier.testTag("notifications_bell_btn")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifCount > 0) {
                                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                Text("$unreadNotifCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 2. Officer / Student Mode Switcher
                            Button(
                                onClick = {
                                    if (userMode == UserMode.STUDENT) {
                                        viewModel.setUserMode(UserMode.OFFICER)
                                    } else {
                                        viewModel.setUserMode(UserMode.STUDENT)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (userMode == UserMode.OFFICER) Color(0xFF059669) else Color(0xFFD97706)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("demo_switcher_btn")
                            ) {
                                Icon(
                                    imageVector = if (userMode == UserMode.OFFICER) Icons.Default.School else Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (userMode == UserMode.OFFICER) strings.studentMode else strings.reviewerDeskMode,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // 3. Overflow Menu
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
                                        text = { Text("Switch Student Persona (${student?.name?.split(" ")?.firstOrNull() ?: "User"})") },
                                        leadingIcon = {
                                            Icon(Icons.Default.SwitchAccount, contentDescription = null, tint = Color(0xFFD97706))
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.toggleLoginSheet(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isOfflineMode) "Simulate Online Mode" else "Simulate Offline Mode (Draft Queue)") },
                                        leadingIcon = {
                                            Icon(if (isOfflineMode) Icons.Default.Wifi else Icons.Default.WifiOff, contentDescription = null, tint = if (isOfflineMode) Color(0xFF059669) else Color(0xFFEF4444))
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.toggleOfflineMode(!isOfflineMode)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Reset Demo to Clean State") },
                                        leadingIcon = {
                                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFEF4444))
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.resetDemoData()
                                        }
                                    )
                                    HorizontalDivider()
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

                    // Offline simulation banner
                    if (isOfflineMode) {
                        Surface(
                            color = Color(0xFFEF4444),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Offline Mode Active • Applications saved to local draft queue",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.toggleOfflineMode(false) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Go Online", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (selectedAppId == null) {
                    NavigationBar(
                        modifier = Modifier.testTag("main_navigation_bar"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        if (userMode == UserMode.STUDENT) {
                            val tabs = listOf(
                                Triple(AppTab.DASHBOARD, Icons.Default.Dashboard, strings.tabDashboard),
                                Triple(AppTab.SCHEMES, Icons.Default.School, strings.tabSchemes),
                                Triple(AppTab.DOCUMENTS, Icons.Default.FolderShared, strings.tabWallet),
                                Triple(AppTab.DISBURSEMENT, Icons.Default.Payments, strings.tabDbtRail)
                            )

                            tabs.forEach { (tab, icon, label) ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(imageVector = icon, contentDescription = label) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_tab_${tab.name}")
                                )
                            }
                        } else {
                            // Officer Mode Navigation
                            NavigationBarItem(
                                selected = currentTab == AppTab.REVIEWER_QUEUE,
                                onClick = { viewModel.selectTab(AppTab.REVIEWER_QUEUE) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (pendingReviewCount > 0) {
                                                Badge(containerColor = Color(0xFFD97706)) {
                                                    Text("$pendingReviewCount")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = "Reviewer Desk")
                                    }
                                },
                                label = { Text("Pending Review Queue ($pendingReviewCount)", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_tab_officer_review")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (!isJagoOpen && selectedAppId == null && userMode == UserMode.STUDENT) {
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
                if (selectedAppId != null) {
                    val selectedApp = applications.find { it.id == selectedAppId }
                    val selectedScheme = schemes.find { it.id == selectedApp?.schemeId }
                    val verRecordsFlow = viewModel.getVerificationRecordsForAppFlow(selectedAppId ?: "")
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
                            viewModel.setUserMode(UserMode.OFFICER)
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
                                documents = documents,
                                onSelectScheme = { viewModel.openApplicationDetail(it) },
                                onOpenReviewDesk = {
                                    viewModel.setUserMode(UserMode.OFFICER)
                                    viewModel.selectTab(AppTab.REVIEWER_QUEUE)
                                },
                                onOpenJago = { viewModel.toggleJagoChat(true) },
                                onApplyUnreached = { viewModel.applyForScheme(it) },
                                onOpenConsentDialog = { viewModel.toggleConsentDialog(true) },
                                onOpenSecurityModal = { showSecurityModal = true },
                                onOpenIntroTour = { showIntroTour = true },
                                onOpenLoginSheet = { viewModel.toggleLoginSheet(true) }
                            )
                        }
                        AppTab.SCHEMES -> {
                            ScholarshipsScreen(
                                student = student,
                                schemes = schemes,
                                applications = applications,
                                documents = documents,
                                onSelectApplication = { viewModel.openApplicationDetail(it) },
                                onApplyScheme = { schemeId, income ->
                                    viewModel.applyForScheme(schemeId, income)
                                }
                            )
                        }
                        AppTab.DOCUMENTS -> {
                            DocumentsWalletScreen(
                                documents = documents,
                                isDigiLockerLinked = student?.isDigiLockerLinked ?: true,
                                onConnectDigiLocker = { phoneOrAadhaar ->
                                    viewModel.loginWithMobileOrAadhaar(phoneOrAadhaar)
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
                                reviewItems = reviewQueue,
                                onResolve = { id, approved, notes ->
                                    viewModel.resolveReviewItem(id, approved, notes)
                                },
                                onBackToStudentView = {
                                    viewModel.setUserMode(UserMode.STUDENT)
                                    viewModel.selectTab(AppTab.DASHBOARD)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Starting Intro Tour
        if (showIntroTour) {
            AppIntroTourModal(
                onDismiss = {
                    sharedPrefs.edit().putBoolean("has_completed_onboarding", true).apply()
                    showIntroTour = false
                }
            )
        }

        // JAGO Multilingual Assistant
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
                onConfirm = { granted ->
                    viewModel.updateStudentConsent(granted)
                    viewModel.toggleConsentDialog(false)
                },
                onDismiss = { viewModel.toggleConsentDialog(false) }
            )
        }

        // Security & Privacy Modal
        if (showSecurityModal) {
            SecurityPrivacyModal(
                hasConsent = student?.hasConsentGiven ?: true,
                onRevokeOrGrantConsent = { granted ->
                    viewModel.updateStudentConsent(granted)
                    showSecurityModal = false
                },
                onDismiss = { showSecurityModal = false }
            )
        }

        // Notifications Modal
        if (showNotificationsSheet) {
            NotificationsModal(
                notifications = notifications,
                onMarkAsRead = { viewModel.markNotificationAsRead(it) },
                onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
                onDismiss = { viewModel.toggleNotificationsSheet(false) }
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
