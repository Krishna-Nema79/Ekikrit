package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.eligibility.EligibilityEngine
import com.example.data.eligibility.EligibilityEvaluation
import com.example.data.local.EkikritDatabase
import com.example.data.model.*
import com.example.data.repository.EkikritRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    SCHEMES("5 Schemes"),
    DOCUMENTS("DigiLocker Wallet"),
    DISBURSEMENT("DBT Payments"),
    REVIEWER_QUEUE("Reviewer Desk")
}

enum class UserMode {
    STUDENT,
    OFFICER
}

class EkikritViewModel(application: Application) : AndroidViewModel(application) {

    val repository: EkikritRepository

    init {
        val db = EkikritDatabase.getDatabase(application, viewModelScope)
        repository = EkikritRepository(db, viewModelScope)
    }

    val activeStudentId = repository.activeStudentId

    val student = repository.studentFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allStudents = repository.allStudentsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val schemes = repository.schemesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val applications = repository.applicationsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val documents = repository.documentsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val disbursements = repository.disbursementsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notifications = repository.notificationsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val reviewQueue = repository.reviewQueueFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val auditLogs = repository.auditLogsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val topUnreachedScheme: StateFlow<EligibilityEvaluation?> = combine(student, schemes, applications) { stu, schList, appList ->
        EligibilityEngine.findTopUnreachedScheme(stu, schList, appList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val scholarshipMatch = repository.scholarshipMatchFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val isOfflineMode = repository.isOfflineMode

    private val _userMode = MutableStateFlow(UserMode.STUDENT)
    val userMode: StateFlow<UserMode> = _userMode.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedApplicationId = MutableStateFlow<String?>(null)
    val selectedApplicationId: StateFlow<String?> = _selectedApplicationId.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    private val _isJagoChatOpen = MutableStateFlow(false)
    val isJagoChatOpen: StateFlow<Boolean> = _isJagoChatOpen.asStateFlow()

    private val _showConsentDialog = MutableStateFlow(false)
    val showConsentDialog: StateFlow<Boolean> = _showConsentDialog.asStateFlow()

    private val _showLoginSheet = MutableStateFlow(false)
    val showLoginSheet: StateFlow<Boolean> = _showLoginSheet.asStateFlow()

    private val _showNotificationsSheet = MutableStateFlow(false)
    val showNotificationsSheet: StateFlow<Boolean> = _showNotificationsSheet.asStateFlow()

    private val _isSimulatingVerification = MutableStateFlow(false)
    val isSimulatingVerification: StateFlow<Boolean> = _isSimulatingVerification.asStateFlow()

    private val _userNotice = MutableStateFlow<String?>(null)
    val userNotice: StateFlow<String?> = _userNotice.asStateFlow()

    private val _jagoMessages = MutableStateFlow<List<JagoMessage>>(
        listOf(
            JagoMessage(
                sender = "JAGO",
                content = "Johar! I am JAGO, your unified tribal scholarship assistant. How can I assist you with your 5 tribal schemes, multi-system verification, or DigiLocker credentials today?",
                quickChips = listOf("What is my application status?", "Why is my income flagged?", "Am I eligible for any scheme?", "When will amount disburse?")
            )
        )
    )
    val jagoMessages: StateFlow<List<JagoMessage>> = _jagoMessages.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setUserMode(mode: UserMode) {
        _userMode.value = mode
        if (mode == UserMode.OFFICER) {
            _currentTab.value = AppTab.REVIEWER_QUEUE
        } else if (_currentTab.value == AppTab.REVIEWER_QUEUE) {
            _currentTab.value = AppTab.DASHBOARD
        }
    }

    fun openApplicationDetail(appId: String) {
        _selectedApplicationId.value = appId
    }

    fun closeApplicationDetail() {
        _selectedApplicationId.value = null
    }

    fun toggleJagoChat(open: Boolean? = null) {
        _isJagoChatOpen.value = open ?: !_isJagoChatOpen.value
    }

    fun toggleConsentDialog(show: Boolean) {
        _showConsentDialog.value = show
    }

    fun toggleLoginSheet(show: Boolean) {
        _showLoginSheet.value = show
    }

    fun toggleNotificationsSheet(show: Boolean) {
        _showNotificationsSheet.value = show
    }

    fun toggleOfflineMode(offline: Boolean) {
        repository.setOfflineMode(offline)
        _userNotice.value = if (offline) "Offline mode simulated. Changes will queue locally." else "Back online! Syncing queued applications with national servers."
    }

    fun switchStudent(studentId: String) {
        viewModelScope.launch {
            try {
                repository.switchStudent(studentId)
                _showLoginSheet.value = false
                _userNotice.value = "Switched beneficiary profile successfully."
            } catch (e: Exception) {
                _userNotice.value = e.message ?: "Profile switch denied."
            }
        }
    }

    fun switchToReviewerRole() {
        viewModelScope.launch {
            try {
                repository.switchStudent("REV_OFFICER_01")
                _userMode.value = UserMode.OFFICER
                _userNotice.value = "Switched to Reviewer Officer Desk."
            } catch (e: Exception) {
                _userNotice.value = e.message ?: "Role switch denied."
            }
        }
    }

    fun switchToStudentRole() {
        viewModelScope.launch {
            try {
                repository.switchStudent("STU_2026_01")
                _userMode.value = UserMode.STUDENT
                _userNotice.value = "Switched to Beneficiary Student View."
            } catch (e: Exception) {
                _userNotice.value = e.message ?: "Role switch denied."
            }
        }
    }

    fun loginWithMobileOrAadhaar(identifier: String, name: String? = null) {
        viewModelScope.launch {
            try {
                val loggedIn = repository.loginWithMobileOrAadhaar(identifier, name)
                _showLoginSheet.value = false
                _userNotice.value = "Authenticated as ${loggedIn.name}."
            } catch (e: Exception) {
                _userNotice.value = e.message ?: "Authentication failed."
            }
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _selectedLanguage.value = lang
        viewModelScope.launch {
            repository.setLanguage(lang.code)
        }
    }

    fun setConsent(granted: Boolean) {
        updateStudentConsent(granted)
    }

    fun clearNotice() {
        _userNotice.value = null
    }

    fun triggerVerification(appId: String) {
        viewModelScope.launch {
            try {
                _isSimulatingVerification.value = true
                repository.runSevenSourceVerification(appId)
                _isSimulatingVerification.value = false
                _userNotice.value = "Multi-source verification executed across 7 national registries."
            } catch (e: Exception) {
                _isSimulatingVerification.value = false
                _userNotice.value = e.message ?: "Verification failed."
            }
        }
    }

    fun resolveReviewItem(itemId: String, isApproved: Boolean, notes: String = "") {
        viewModelScope.launch {
            try {
                repository.resolveReviewItem(itemId, isApproved, notes)
                _userNotice.value = if (isApproved) {
                    "Verification issue resolved. Application moved to State Verification."
                } else {
                    "Clarification requested from student."
                }
            } catch (e: SecurityException) {
                _userNotice.value = "Permission Denied: Switch to Reviewing Officer profile to resolve exceptions."
            } catch (e: Exception) {
                _userNotice.value = e.message ?: "Resolution failed."
            }
        }
    }

    fun applyForScheme(schemeId: String, declaredIncome: Double? = null) {
        viewModelScope.launch {
            try {
                val (success, msg) = repository.applyForScheme(schemeId, declaredIncome)
                _userNotice.value = msg
            } catch (e: Exception) {
                _userNotice.value = e.message ?: "Application failed."
            }
        }
    }

    fun applyForUnreachedScheme(schemeId: String) {
        applyForScheme(schemeId)
    }

    fun updateStudentConsent(granted: Boolean) {
        viewModelScope.launch {
            repository.updateStudentConsent(repository.activeStudentId.value, granted)
            _userNotice.value = if (granted) "DPDP Act 2023 consent granted." else "DPDP Act 2023 consent revoked."
        }
    }

    fun pullDigiLockerDocument(type: String, title: String, docNumber: String, issuer: String) {
        viewModelScope.launch {
            try {
                repository.pullDocumentFromDigiLocker(type, title, docNumber, issuer)
                _userNotice.value = "Successfully pulled '$title' from DigiLocker wallet."
            } catch (e: Exception) {
                _userNotice.value = e.message ?: "Failed to pull document."
            }
        }
    }

    fun connectDigiLocker(mobileOrAadhaar: String) {
        syncDigiLocker(mobileOrAadhaar)
    }

    fun syncDigiLocker(mobileOrAadhaar: String) {
        viewModelScope.launch {
            try {
                repository.syncDigiLockerFull(mobileOrAadhaar)
                _userNotice.value = "DigiLocker Account Connected! 5 Verified Credentials Synced."
            } catch (e: Exception) {
                _userNotice.value = e.message ?: "Failed to connect DigiLocker."
            }
        }
    }

    fun markNotificationAsRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notifId)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetAllDemoData()
            _userNotice.value = "Demo database reset to clean default state."
        }
    }

    fun sendJagoQuery(query: String) {
        val userMsg = JagoMessage(
            sender = "USER",
            content = query
        )
        _jagoMessages.value = _jagoMessages.value + userMsg

        viewModelScope.launch {
            val responseText = repository.generateJagoResponse(query)
            val jagoResponse = JagoMessage(
                sender = "JAGO",
                content = responseText,
                quickChips = listOf("Check my DBT payment", "Explain income tolerance", "View 5 Schemes")
            )
            _jagoMessages.value = _jagoMessages.value + jagoResponse
        }
    }

    fun getVerificationRecordsForAppFlow(appId: String): Flow<List<VerificationRecordEntity>> {
        return repository.getVerificationRecordsForAppFlow(appId)
    }
}
