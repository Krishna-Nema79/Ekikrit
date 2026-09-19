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
    SCHEMES("Schemes (5)"),
    DOCUMENTS("DigiLocker Wallet"),
    DISBURSEMENT("DBT Payments"),
    REVIEWER_QUEUE("Reviewer Desk")
}

class EkikritViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EkikritRepository

    init {
        val db = EkikritDatabase.getDatabase(application, viewModelScope)
        repository = EkikritRepository(db)
        viewModelScope.launch {
            repository.ensurePresetStudents()
        }
    }

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

    val pendingReviewItems = repository.pendingReviewItemsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allReviewItems = repository.allReviewItemsFlow.stateIn(
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

    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedApplicationId = MutableStateFlow<String?>(null)
    val selectedApplicationId: StateFlow<String?> = _selectedApplicationId.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    private val _isJagoChatOpen = MutableStateFlow(false)
    val isJagoChatOpen: StateFlow<Boolean> = _isJagoChatOpen.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _showConsentDialog = MutableStateFlow(false)
    val showConsentDialog: StateFlow<Boolean> = _showConsentDialog.asStateFlow()

    private val _showLoginSheet = MutableStateFlow(false)
    val showLoginSheet: StateFlow<Boolean> = _showLoginSheet.asStateFlow()

    private val _isSimulatingVerification = MutableStateFlow(false)
    val isSimulatingVerification: StateFlow<Boolean> = _isSimulatingVerification.asStateFlow()

    private val _userNotice = MutableStateFlow<String?>(null)
    val userNotice: StateFlow<String?> = _userNotice.asStateFlow()

    private val _jagoMessages = MutableStateFlow<List<JagoMessage>>(
        listOf(
            JagoMessage(
                sender = "JAGO",
                content = "Johar! I am JAGO, your unified tribal scholarship assistant. How can I assist you with your tribal schemes or DigiLocker documents today?",
                quickChips = listOf("What's pending on my application?", "Why was my income flagged?", "Am I eligible for any scheme?", "When will amount disburse?")
            )
        )
    )
    val jagoMessages: StateFlow<List<JagoMessage>> = _jagoMessages.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
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
            repository.switchStudent("REV_OFFICER_01")
            _userNotice.value = "Switched to Reviewer Officer Desk."
        }
    }

    fun switchToStudentRole() {
        viewModelScope.launch {
            repository.switchStudent("STU_2026_01")
            _userNotice.value = "Switched to Beneficiary Student View."
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
            val welcomeMessage = repository.generateJagoResponse("hi", lang.code)
            _jagoMessages.value = _jagoMessages.value + welcomeMessage
        }
    }

    fun setConsent(granted: Boolean) {
        viewModelScope.launch {
            repository.setConsentGiven(granted)
            _showConsentDialog.value = false
            _userNotice.value = if (granted) "DigiLocker consent updated with DPDP Act compliance." else "DigiLocker consent revoked."
        }
    }

    fun clearNotice() {
        _userNotice.value = null
    }

    fun triggerVerification(appId: String) {
        viewModelScope.launch {
            try {
                _isSimulatingVerification.value = true
                repository.triggerLiveMockVerification(appId)
                _isSimulatingVerification.value = false
                _userNotice.value = "Multi-source verification executed."
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
                    "Exception approved! Application promoted to SANCTIONED."
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

    fun applyForUnreachedScheme(schemeId: String) {
        viewModelScope.launch {
            try {
                repository.applyForUnreachedScheme(schemeId)
                _userNotice.value = "Applied successfully with 1-click DigiLocker credentials! No paper re-upload required."
            } catch (e: Exception) {
                _userNotice.value = e.message ?: "Application failed."
            }
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
        viewModelScope.launch {
            try {
                repository.syncDigiLockerFull(mobileOrAadhaar)
                _userNotice.value = "DigiLocker Account Connected! 5 Verified Credentials Synced."
            } catch (e: Exception) {
                _userNotice.value = e.message ?: "Failed to connect DigiLocker."
            }
        }
    }

    fun sendJagoQuery(query: String) {
        val userMsg = JagoMessage(
            sender = "USER",
            content = query
        )
        _jagoMessages.value = _jagoMessages.value + userMsg

        viewModelScope.launch {
            val response = repository.generateJagoResponse(query, _selectedLanguage.value.code)
            _jagoMessages.value = _jagoMessages.value + response
        }
    }

    fun getVerificationRecordsForApp(appId: String): Flow<List<VerificationRecordEntity>> {
        return repository.getVerificationRecordsFlow(appId)
    }
}
