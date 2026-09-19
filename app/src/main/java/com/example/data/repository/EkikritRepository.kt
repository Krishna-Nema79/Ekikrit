package com.example.data.repository

import com.example.data.ai.JagoAiService
import com.example.data.local.EkikritDatabase
import com.example.data.local.SeedData
import com.example.data.model.*
import com.example.data.verification.LocalDemoVerificationDataSource
import com.example.data.verification.VerificationDataSource
import com.example.domain.EligibilityEngine
import com.example.domain.UnifiedVerificationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class EkikritRepository(
    private val db: EkikritDatabase,
    private val scope: CoroutineScope,
    private val verificationDataSource: VerificationDataSource = LocalDemoVerificationDataSource(),
    private val jagoAiService: JagoAiService = JagoAiService()
) {
    // Database reference alias for internal consistency across merged branches
    private val database: EkikritDatabase get() = db

    // Single Source of Truth for the logged-in student
    private val _activeStudentId = MutableStateFlow("STU_2026_01")
    val activeStudentId: StateFlow<String> = _activeStudentId.asStateFlow()

    // Offline / Connectivity Simulation State
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    fun setOfflineMode(offline: Boolean) {
        _isOfflineMode.value = offline
        if (!offline) {
            // Trigger background sync when back online
            scope.launch { syncPendingDrafts() }
        }
    }

    // Active Student Flow
    val studentFlow: Flow<StudentEntity?> = _activeStudentId.flatMapLatest { id ->
        db.studentDao().getStudentFlow(id)
    }

    // All available schemes across MoTA
    val schemesFlow: Flow<List<SchemeEntity>> = db.schemeDao().getAllSchemesFlow()

    // Applications strictly filtered to the active student
    val applicationsFlow: Flow<List<ApplicationEntity>> = _activeStudentId.flatMapLatest { id ->
        db.applicationDao().getApplicationsForStudentFlow(id)
    }

    // Documents strictly filtered to the active student
    val documentsFlow: Flow<List<DocumentEntity>> = _activeStudentId.flatMapLatest { id ->
        db.documentDao().getDocumentsForStudentFlow(id)
    }

    // Disbursements strictly filtered to the active student
    val disbursementsFlow: Flow<List<DisbursementEntity>> = _activeStudentId.flatMapLatest { id ->
        db.disbursementDao().getDisbursementsForStudentFlow(id)
    }

    // Notifications strictly filtered to the active student
    val notificationsFlow: Flow<List<NotificationEntity>> = _activeStudentId.flatMapLatest { id ->
        db.notificationDao().getNotificationsForStudentFlow(id)
    }

    // Officer Review Queue
    val reviewQueueFlow: Flow<List<ReviewQueueEntity>> = db.reviewQueueDao().getAllReviewItemsFlow()
    val pendingReviewItemsFlow: Flow<List<ReviewQueueEntity>> = db.reviewQueueDao().getPendingReviewItemsFlow()
    val allReviewItemsFlow: Flow<List<ReviewQueueEntity>> = db.reviewQueueDao().getAllReviewItemsFlow()

    // Audit Log Trail (all logs or active student)
    val auditLogsFlow: Flow<List<AuditLogEntity>> = db.auditLogDao().getAllLogsFlow()

    // All Students list for quick demo persona switching
    val allStudentsFlow: Flow<List<StudentEntity>> = db.studentDao().getAllStudentsFlow()

    // Dynamic Scholarship Match Flow based on active student profile, documents, and schemes
    val scholarshipMatchFlow: Flow<ScholarshipMatch?> = combine(
        studentFlow,
        schemesFlow,
        documentsFlow,
        applicationsFlow
    ) { student, schemes, docs, apps ->
        if (student == null) return@combine null
        val unappliedSchemes = schemes.filter { scheme ->
            apps.none { it.schemeId == scheme.id }
        }
        var bestMatch: ScholarshipMatch? = null
        var highestScore = -1

        for (scheme in unappliedSchemes) {
            val eval = EligibilityEngine.evaluate(student, scheme, docs, apps)
            if (eval.status == com.example.domain.EligibilityStatus.ELIGIBLE && eval.matchPercentage > highestScore) {
                highestScore = eval.matchPercentage
                bestMatch = ScholarshipMatch(
                    scheme = scheme,
                    whyMatched = "Matched using your verified academic record at ${student.institutionName} and ${student.category} tribal profile.",
                    eligibilityStatus = eval.status.name,
                    matchPercentage = eval.matchPercentage,
                    requiredDocuments = listOf("Aadhaar", "ST Caste", "Income", "Marksheet"),
                    reusableDocuments = docs.filter { it.isReusable }.map { it.type },
                    nextAction = "Claim with 1-Click (No Paperwork)"
                )
            }
        }
        bestMatch
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
    }

    companion object {
        /**
         * Demo-only pre-seeded identities permitted for role switching during presentations.
         * Production applications must never allow arbitrary client-side profile switching
         * to prevent Insecure Direct Object Reference (IDOR) and unauthorized impersonation.
         */
        val DEMO_PERMITTED_SWITCH_IDS = setOf("STU_2026_01", "STU_2026_02", "STU_2026_03", "REV_OFFICER_01")
    }

    suspend fun switchStudent(studentId: String) = withContext(Dispatchers.IO) {
        if (studentId !in DEMO_PERMITTED_SWITCH_IDS) {
            throw SecurityException(
                "Access Denied: Profile switching is restricted exclusively to seeded demo identities ('STU_2026_01', 'STU_2026_02', 'STU_2026_03', 'REV_OFFICER_01'). Arbitrary profile switching is prohibited."
            )
        }

        val student = db.studentDao().getStudent(studentId)
            ?: throw IllegalArgumentException("Demo beneficiary profile '$studentId' not found.")

        _activeStudentId.value = studentId

        db.auditLogDao().insert(
            AuditLogEntity(
                action = "AUTH_USER_SWITCH",
                actor = if (student.role == UserRole.REVIEWER.name) "Officer (${student.name})" else "Student (${student.name})",
                details = "Session switched to student ${student.name} (ID: $studentId, Category: ${student.category}).",
                timestamp = getCurrentTimestamp(),
                studentId = student.id
            )
        )
    }

    suspend fun loginWithMobileOrAadhaar(identifier: String, name: String? = null): StudentEntity = withContext(Dispatchers.IO) {
        val digits = identifier.filter(Char::isDigit)
        require(digits.length == 10 || digits.length == 12) { "Enter a valid 10-digit mobile number or 12-digit Aadhaar identifier." }
        val trimmed = if (digits.length == 10) "+91 $digits" else digits
        val existing = db.studentDao().findStudentByPhoneOrAadhaar(trimmed)
            ?: db.studentDao().findStudentByPhoneOrAadhaar(digits)
        if (existing != null) {
            _activeStudentId.value = existing.id
            db.auditLogDao().insert(
                AuditLogEntity(
                    action = "Student Authenticated",
                    actor = "Student (${existing.name})",
                    details = "Logged in through local demo identifier flow.",
                    timestamp = getCurrentTimestamp(),
                    studentId = existing.id
                )
            )
            return@withContext existing
        }

        val newId = "STU_" + System.currentTimeMillis().toString().takeLast(6)
        val studentName = name?.trim().takeUnless { it.isNullOrBlank() } ?: "Student User"
        val newStudent = StudentEntity(
            id = newId,
            name = studentName,
            dob = "",
            mobile = if (digits.length == 10) trimmed else "",
            state = "",
            institutionId = "",
            institutionName = "",
            course = "",
            category = "ST (Tribal Scholar)",
            pvtgCommunity = "",
            preferredLanguage = "en",
            apaarId = "",
            annualIncome = 220000.0,
            aadhaarMasked = if (digits.length == 12) "XXXX-XXXX-${digits.takeLast(4)}" else "",
            bankAccountMasked = "",
            ifscCode = "",
            isDigiLockerLinked = false,
            hasConsentGiven = false,
            role = UserRole.STUDENT.name
        )
        db.studentDao().insertStudent(newStudent)
        _activeStudentId.value = newId
        db.auditLogDao().insert(
            AuditLogEntity(
                action = "New Student Registered & Logged In",
                actor = "Student ($studentName)",
                details = "Local demo profile created. Complete and verify profile data before applying.",
                timestamp = getCurrentTimestamp(),
                studentId = newId
            )
        )
        return@withContext newStudent
    }

    suspend fun authenticateWithPhoneOrAadhaar(phoneOrAadhaar: String, customName: String? = null): StudentEntity =
        loginWithMobileOrAadhaar(phoneOrAadhaar, customName)

    fun getActiveStudentSync(): StudentEntity? {
        val currentId = _activeStudentId.value
        // Helper to grab synchronous student or default
        return null
    }

    suspend fun getVerificationRecordsForApp(appId: String): List<VerificationRecordEntity> = withContext(Dispatchers.IO) {
        db.verificationRecordDao().getRecordsForApp(appId)
    }

    fun getVerificationRecordsForAppFlow(appId: String): Flow<List<VerificationRecordEntity>> {
        return db.verificationRecordDao().getRecordsForAppFlow(appId)
    }

    fun getVerificationRecordsFlow(applicationId: String): Flow<List<VerificationRecordEntity>> =
        getVerificationRecordsForAppFlow(applicationId)

    suspend fun saveStudentProfile(student: StudentEntity) = withContext(Dispatchers.IO) {
        val currentId = _activeStudentId.value
        require(student.id == currentId) { "Cannot modify another student's profile." }
        db.studentDao().insertStudent(student)
        db.auditLogDao().insert(
            AuditLogEntity(
                action = "Profile Updated",
                actor = "Student (${student.name})",
                details = "Updated category (${student.category}), preferred language (${student.preferredLanguage}), and institution.",
                timestamp = getCurrentTimestamp(),
                studentId = student.id
            )
        )
    }

    suspend fun setLanguage(languageCode: String) = withContext(Dispatchers.IO) {
        val current = db.studentDao().getStudent(_activeStudentId.value) ?: return@withContext
        val updated = current.copy(preferredLanguage = languageCode)
        db.studentDao().updateStudent(updated)
    }

    suspend fun updateStudentConsent(studentId: String, hasConsent: Boolean) = withContext(Dispatchers.IO) {
        db.studentDao().updateStudentConsent(studentId, hasConsent)
        val student = db.studentDao().getStudent(studentId)
        db.auditLogDao().insert(
            AuditLogEntity(
                action = if (hasConsent) "CONSENT_GRANT" else "CONSENT_REVOKE",
                actor = student?.name ?: "Student",
                details = "DPDP Act 2023 digital consent ${if (hasConsent) "GRANTED" else "REVOKED/RESTRICTED"} for student ID $studentId.",
                timestamp = getCurrentTimestamp(),
                studentId = studentId
            )
        )
    }

    suspend fun setConsentGiven(given: Boolean) = withContext(Dispatchers.IO) {
        updateStudentConsent(_activeStudentId.value, given)
    }

    suspend fun pullDocumentFromDigiLocker(type: String, title: String, number: String, issuer: String) = withContext(Dispatchers.IO) {
        val currentStudentId = _activeStudentId.value
        val student = db.studentDao().getStudent(currentStudentId) ?: return@withContext
        val newDocId = "DOC_${System.currentTimeMillis().toString().takeLast(5)}"
        val doc = DocumentEntity(
            id = newDocId,
            studentId = student.id,
            type = type,
            title = title,
            docNumberMasked = number,
            source = "DigiLocker National Depository",
            verificationStatus = "VERIFIED",
            issuedDate = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date()),
            issuedBy = issuer,
            isReusable = true
        )
        db.documentDao().insert(doc)

        db.auditLogDao().insert(
            AuditLogEntity(
                action = "DIGILOCKER_DOC_PULL",
                actor = student.name,
                details = "Pulled $title ($number) into single-wallet. Reusable across all 5 schemes.",
                timestamp = getCurrentTimestamp(),
                studentId = student.id
            )
        )
    }

    suspend fun syncDigiLockerFull(mobileOrAadhaar: String) = withContext(Dispatchers.IO) {
        val student = db.studentDao().getStudent(_activeStudentId.value)
            ?: throw IllegalStateException("No active profile.")
        require(mobileOrAadhaar.filter(Char::isDigit).length in listOf(10, 12)) { "Enter a valid mobile number or Aadhaar identifier." }
        db.studentDao().updateStudent(student.copy(isDigiLockerLinked = true, hasConsentGiven = true))

        val initialDocs = listOf(
            DocumentEntity(
                id = "DOC_AADHAAR_${student.id}",
                studentId = student.id,
                type = "Aadhaar",
                title = "Aadhaar Demographic Card",
                docNumberMasked = student.aadhaarMasked.ifBlank { "XXXX-XXXX-8924" },
                source = "UIDAI DigiLocker",
                verificationStatus = "VERIFIED",
                issuedDate = "12-04-2019",
                issuedBy = "UIDAI, Govt of India",
                isReusable = true
            ),
            DocumentEntity(
                id = "DOC_CASTE_${student.id}",
                studentId = student.id,
                type = "Caste",
                title = "Scheduled Tribe Certificate (${student.category.ifBlank { "ST Beneficiary" }})",
                docNumberMasked = "ST/OD/2026/8912",
                source = "e-District (DigiLocker)",
                verificationStatus = "VERIFIED",
                issuedDate = "10-01-2026",
                issuedBy = "Tehsildar Office",
                isReusable = true
            ),
            DocumentEntity(
                id = "DOC_INCOME_${student.id}",
                studentId = student.id,
                type = "Income",
                title = "Annual Family Income Certificate",
                docNumberMasked = "INC/OD/2026/4102",
                source = "e-District (DigiLocker)",
                verificationStatus = "VERIFIED",
                issuedDate = "15-02-2026",
                issuedBy = "Revenue Department",
                isReusable = true
            ),
            DocumentEntity(
                id = "DOC_MARKSHEET_${student.id}",
                studentId = student.id,
                type = "Marksheet",
                title = "Higher Secondary Examination (Class XII)",
                docNumberMasked = "CHSE/2022/88219",
                source = "Education Board (DigiLocker)",
                verificationStatus = "VERIFIED",
                issuedDate = "22-06-2022",
                issuedBy = "Council of Higher Secondary Education",
                isReusable = true
            ),
            DocumentEntity(
                id = "DOC_APAAR_${student.id}",
                studentId = student.id,
                type = "APAAR",
                title = "One Nation One Student ID (APAAR/ABC)",
                docNumberMasked = student.apaarId.ifBlank { "APAAR-8839-4021-9920" },
                source = "Ministry of Education (DigiLocker)",
                verificationStatus = "VERIFIED",
                issuedDate = "05-08-2023",
                issuedBy = "National Academic Depository (NAD)",
                isReusable = true
            )
        )

        for (d in initialDocs) {
            db.documentDao().insert(d)
        }

        db.auditLogDao().insert(
            AuditLogEntity(
                action = "DigiLocker OAuth2 Account Linked",
                actor = "MeriPehchaan Gateway",
                details = "Local demo credential set added after consent. Reusable across schemes.",
                timestamp = getCurrentTimestamp(),
                studentId = student.id
            )
        )
    }

    suspend fun deleteDocument(docId: String) = withContext(Dispatchers.IO) {
        val studentId = _activeStudentId.value
        val doc = db.documentDao().getDocumentByIdForStudent(docId, studentId)
            ?: throw IllegalStateException("Document not found or access denied.")
        db.documentDao().deleteDocumentForStudent(docId, studentId)
        db.auditLogDao().insert(
            AuditLogEntity(
                action = "Document Removed",
                actor = "Student",
                details = "Removed credential '${doc.title}' from wallet.",
                timestamp = getCurrentTimestamp(),
                studentId = studentId
            )
        )
    }

    /**
     * Executes multi-source verification across all 7 providers.
     */
    suspend fun runSevenSourceVerification(applicationId: String, studentIdOverride: String? = null) = withContext(Dispatchers.IO) {
        val application = db.applicationDao().getApplicationById(applicationId) ?: return@withContext
        val currentStudentId = studentIdOverride ?: application.studentId
        val student = db.studentDao().getStudent(currentStudentId) ?: return@withContext
        val documents = db.documentDao().getDocumentsForStudent(currentStudentId)

        val output = UnifiedVerificationEngine.executeSevenSourceVerification(student, application, documents)

        // Clear previous records for this application and save new ones
        db.verificationRecordDao().deleteForApp(applicationId)
        db.verificationRecordDao().insertAll(output.records)

        val hasMismatch = output.records.any { it.status == "MISMATCH" }

        if (output.reviewItem != null) {
            db.reviewQueueDao().insert(output.reviewItem)
        }

        val newStage = if (hasMismatch) "INSTITUTE_VERIFICATION" else "STATE_VERIFICATION"
        val statusText = if (hasMismatch) {
            "Multi-source API verification completed (6/7 verified). 1 minor income variance auto-routed to District Review Desk."
        } else {
            "All 7 government registries verified. State Tribal Welfare clearance in progress."
        }

        db.applicationDao().updateStage(
            appId = applicationId,
            stage = newStage,
            statusText = statusText,
            timestamp = getCurrentTimestamp(),
            hasDiscrepancy = hasMismatch,
            pendingAction = if (hasMismatch) "Officer Review Desk is clearing the income certificate tolerance." else null
        )

        db.auditLogDao().insert(
            AuditLogEntity(
                action = "VERIFICATION_EXECUTE",
                actor = "Unified Verification Engine",
                details = "Executed 7-source verification for ${application.schemeCode}: ${output.summaryMessage}",
                timestamp = getCurrentTimestamp(),
                studentId = student.id
            )
        )

        db.notificationDao().insert(
            NotificationEntity(
                id = "NOTIF_${System.currentTimeMillis()}",
                studentId = student.id,
                title = "Verification Updated",
                message = "Verification checks refreshed for ${application.schemeCode}: ${output.summaryMessage}",
                type = "VERIFICATION",
                timestamp = getCurrentTimestamp(),
                isRead = false
            )
        )
    }

    suspend fun triggerLiveMockVerification(appId: String) = runSevenSourceVerification(appId)

    /**
     * Officer Review Desk Action: Approves or clarifies an exception.
     */
    suspend fun resolveReviewItem(reviewItemId: String, isApproved: Boolean, notes: String) = withContext(Dispatchers.IO) {
        val reviewItem = db.reviewQueueDao().getById(reviewItemId) ?: return@withContext
        val now = getCurrentTimestamp()

        val updatedStatus = if (isApproved) "APPROVED" else "RESUBMIT"
        db.reviewQueueDao().update(
            reviewItem.copy(
                status = updatedStatus,
                resolvedAt = now,
                resolutionNotes = notes.ifBlank { if (isApproved) "Income variance verified within allowable scheme ceiling." else "Clarification requested from student." }
            )
        )

        // Update corresponding verification record
        db.verificationRecordDao().updateRecordStatus(
            recordId = reviewItem.verificationRecordId,
            status = if (isApproved) "RESOLVED" else "MISMATCH",
            notes = "Officer cleared variance: $notes"
        )

        // Update application state
        val application = db.applicationDao().getApplicationById(reviewItem.applicationId)
        if (application != null) {
            val newStage = if (isApproved) "STATE_VERIFICATION" else "INSTITUTE_VERIFICATION"
            val newStatusText = if (isApproved) {
                "Officer review complete. Exception cleared. Sent for State Tribal Welfare clearance."
            } else {
                "Action requested: Officer requested additional clarification on income certificate."
            }

            db.applicationDao().updateStage(
                appId = application.id,
                stage = newStage,
                statusText = newStatusText,
                timestamp = now,
                hasDiscrepancy = !isApproved,
                pendingAction = if (isApproved) null else "Please review officer remarks: $notes"
            )

            db.notificationDao().insert(
                NotificationEntity(
                    id = "NOTIF_${System.currentTimeMillis()}",
                    studentId = reviewItem.studentId,
                    title = if (isApproved) "Verification issue resolved" else "Action Required on Review",
                    message = if (isApproved) "Your verification issue was cleared and your application has moved to State Verification." else "Officer note: $notes",
                    type = "REVIEW",
                    timestamp = now,
                    isRead = false
                )
            )
        }

        db.auditLogDao().insert(
            AuditLogEntity(
                action = if (isApproved) "OFFICER_APPROVE_EXCEPTION" else "OFFICER_REQUEST_CLARIFICATION",
                actor = "District Tribal Welfare Officer",
                details = "Item $reviewItemId (${reviewItem.fieldName}) resolved with status $updatedStatus. Notes: $notes",
                timestamp = now,
                studentId = reviewItem.studentId
            )
        )
    }

    /**
     * Applies for an unreached or new scheme for the specified or active student.
     * Enforces eligibility evaluation and active scholarship conflict rules.
     */
    suspend fun applyForScheme(
        schemeId: String,
        declaredIncome: Double? = null,
        studentIdOverride: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val targetStudentId = studentIdOverride ?: _activeStudentId.value
        val student = db.studentDao().getStudent(targetStudentId) ?: return@withContext Pair(false, "Student not found")
        val scheme = db.schemeDao().getSchemeById(schemeId) ?: return@withContext Pair(false, "Scheme not found")
        val docs = db.documentDao().getDocumentsForStudent(targetStudentId)
        val existingApps = db.applicationDao().getApplicationsForStudent(targetStudentId)

        // Check eligibility engine
        val eligibility = EligibilityEngine.evaluate(student, scheme, docs, existingApps)

        if (eligibility.status == com.example.domain.EligibilityStatus.NOT_ELIGIBLE) {
            val errorReason = eligibility.failedCriteria.firstOrNull() ?: "Your profile does not currently meet this scheme's eligibility requirements."
            val userMsg = "You're not currently eligible for this scholarship. $errorReason"
            db.notificationDao().insert(
                NotificationEntity(
                    id = "NOTIF_${System.currentTimeMillis()}",
                    studentId = student.id,
                    title = "Application Blocked: Ineligible",
                    message = userMsg,
                    type = "SCHEME",
                    timestamp = getCurrentTimestamp(),
                    isRead = false
                )
            )
            return@withContext Pair(false, userMsg)
        }

        if (eligibility.status == com.example.domain.EligibilityStatus.NEEDS_REVIEW) {
            if (eligibility.conflictReason != null) {
                val conflictMsg = "You already have an active scholarship/fellowship. ${eligibility.conflictReason}"
                db.notificationDao().insert(
                    NotificationEntity(
                        id = "NOTIF_${System.currentTimeMillis()}",
                        studentId = student.id,
                        title = "Application Blocked: Active Award Conflict",
                        message = conflictMsg,
                        type = "SCHEME",
                        timestamp = getCurrentTimestamp(),
                        isRead = false
                    )
                )
                return@withContext Pair(false, conflictMsg)
            } else {
                val reviewMsg = "Your eligibility needs review. ${eligibility.summaryRecommendation}"
                db.notificationDao().insert(
                    NotificationEntity(
                        id = "NOTIF_${System.currentTimeMillis()}",
                        studentId = student.id,
                        title = "Application Flagged: Eligibility Review",
                        message = reviewMsg,
                        type = "SCHEME",
                        timestamp = getCurrentTimestamp(),
                        isRead = false
                    )
                )
                return@withContext Pair(false, reviewMsg)
            }
        }

        val newAppId = "APP_${scheme.code.take(4)}_${System.currentTimeMillis().toString().takeLast(6)}"
        val now = getCurrentTimestamp()

        val sanctionedAmt = when (scheme.id) {
            "SCH_PRE" -> 4500.0
            "SCH_PMS" -> 28000.0
            "SCH_TOPCLASS" -> 142000.0
            "SCH_NFST" -> 420000.0
            "SCH_NOS" -> 3200000.0
            else -> 25000.0
        }

        val isOffline = _isOfflineMode.value

        val newApp = ApplicationEntity(
            id = newAppId,
            studentId = student.id,
            schemeId = scheme.id,
            schemeCode = scheme.code,
            schemeName = scheme.name,
            currentStage = "SUBMITTED",
            statusText = if (isOffline) "Saved locally in offline draft queue. Will automatically submit when online." else "Application submitted via Unified Tribal Scholarship Rail. Auto-attaching 5 verified DigiLocker credentials.",
            appliedDate = now,
            lastUpdated = now,
            pendingActionDesc = null,
            hasDiscrepancy = false,
            sanctionedAmount = sanctionedAmt,
            estimatedDisbursementDays = 14,
            syncState = if (isOffline) "PENDING_SYNC" else "SYNCED",
            academicYear = "2026-27"
        )

        db.applicationDao().insert(newApp)

        // Clear any saved draft for this specific student
        db.applicationDraftDao().deleteDraft(student.id, scheme.id)

        db.auditLogDao().insert(
            AuditLogEntity(
                action = "APPLICATION_SUBMIT_ONE_CLICK",
                actor = student.name,
                details = "Submitted 1-click application for ${scheme.name} (ID: $newAppId, Student: ${student.id}). Zero paper re-upload.",
                timestamp = now,
                studentId = student.id
            )
        )

        db.notificationDao().insert(
            NotificationEntity(
                id = "NOTIF_${System.currentTimeMillis()}",
                studentId = student.id,
                title = "Application Submitted Successfully",
                message = "Your 1-click application for ${scheme.name} is received. Pre-attached DigiLocker credentials verified.",
                type = "STATUS",
                timestamp = now,
                isRead = false
            )
        )

        // If online, immediately run multi-source verification for this application and target student
        if (!isOffline) {
            runSevenSourceVerification(newAppId, student.id)
        }

        Pair(true, "Applied successfully with 1-click DigiLocker credentials! Zero paper re-upload.")
    }

    suspend fun applyForUnreachedScheme(schemeId: String, academicYear: String = "2026-27"): Pair<Boolean, String> {
        return applyForScheme(schemeId)
    }

    suspend fun saveApplicationDraft(schemeId: String, currentStep: Int, declaredIncome: Double, selectedDocIds: String) = withContext(Dispatchers.IO) {
        val currentStudentId = _activeStudentId.value
        val draft = ApplicationDraftEntity(
            id = "DRAFT_${currentStudentId}_${schemeId}",
            studentId = currentStudentId,
            schemeId = schemeId,
            currentStep = currentStep,
            declaredIncome = declaredIncome,
            selectedDocIds = selectedDocIds,
            lastSavedTimestamp = getCurrentTimestamp(),
            isPendingSync = _isOfflineMode.value
        )
        db.applicationDraftDao().insert(draft)
    }

    suspend fun getApplicationDraft(schemeId: String): ApplicationDraftEntity? = withContext(Dispatchers.IO) {
        val currentStudentId = _activeStudentId.value
        db.applicationDraftDao().getDraft(currentStudentId, schemeId)
    }

    suspend fun syncPendingDrafts() = withContext(Dispatchers.IO) {
        val drafts = db.applicationDraftDao().getPendingSyncDrafts()
        for (draft in drafts) {
            applyForScheme(draft.schemeId, draft.declaredIncome, studentIdOverride = draft.studentId)
        }
    }

    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        val currentStudentId = _activeStudentId.value
        db.notificationDao().markAllAsRead(currentStudentId)
    }

    suspend fun resetAllDemoData() = withContext(Dispatchers.IO) {
        SeedData.resetDemo(db)
        _activeStudentId.value = "STU_2026_01"
    }

    suspend fun ensurePresetStudents() = withContext(Dispatchers.IO) {
        SeedData.ensurePresetStudents(db)
    }

    /**
     * Multilingual AI assistant returning structured JagoMessage.
     */
    suspend fun generateJagoResponse(query: String, lang: String): JagoMessage = withContext(Dispatchers.IO) {
        val studentId = _activeStudentId.value
        val student = db.studentDao().getStudent(studentId)
        val studentApps = db.applicationDao().getApplicationsForStudent(studentId)
        val allSchemes = db.schemeDao().getAllSchemesFlow().firstOrNull() ?: emptyList()
        val pendingReviews = db.reviewQueueDao().getPendingReviewItemsFlow().firstOrNull() ?: emptyList()

        val evaluations = allSchemes.map {
            com.example.data.eligibility.EligibilityEngine.evaluateEligibility(student, it, studentApps)
        }

        jagoAiService.generateResponse(
            query = query,
            langCode = lang,
            student = student,
            applications = studentApps,
            pendingReviewCount = pendingReviews.size,
            unclaimedEvaluations = evaluations
        )
    }

    /**
     * Live State-Aware Dynamic JAGO AI Assistant
     */
    suspend fun generateJagoResponse(userQuery: String): String = withContext(Dispatchers.IO) {
        val currentStudentId = _activeStudentId.value
        val student = db.studentDao().getStudent(currentStudentId)
        val applications = db.applicationDao().getApplicationsForStudent(currentStudentId)
        val documents = db.documentDao().getDocumentsForStudent(currentStudentId)
        val disbursements = db.disbursementDao().getDisbursementsForStudent(currentStudentId)
        val schemes = db.schemeDao().getAllSchemes()

        val studentName = student?.name ?: "Student"
        val q = userQuery.lowercase(Locale.ENGLISH)

        val prefix = if (_isOfflineMode.value) "⚡ [JAGO Offline Assistance Mode Active]\n\n" else ""

        val response = when {
            q.contains("status") || q.contains("application") || q.contains("track") -> {
                if (applications.isEmpty()) {
                    "Johar $studentName! You currently have no active scholarship applications. You can explore and apply for eligible schemes in the 5 Schemes tab."
                } else {
                    val appSummaries = applications.joinToString("\n• ") { app ->
                        "${app.schemeName}: Current Stage is '${app.currentStage.replace("_", " ")}'. ${app.statusText}"
                    }
                    "Johar $studentName! Here is the live status of your applications:\n\n• $appSummaries"
                }
            }

            q.contains("discrepancy") || q.contains("mismatch") || q.contains("income") || q.contains("issue") || q.contains("review") -> {
                val appWithIssue = applications.find { it.hasDiscrepancy }
                if (appWithIssue != null) {
                    val records = db.verificationRecordDao().getRecordsForApp(appWithIssue.id)
                    val mismatchRecord = records.find { it.status == "MISMATCH" }
                    val reviewItem = db.reviewQueueDao().getByAppId(appWithIssue.id)

                    val provider = mismatchRecord?.sourceSystem ?: "State Revenue Registry"
                    val declared = mismatchRecord?.declaredValue ?: "Self-Declared"
                    val retrieved = mismatchRecord?.retrievedValue ?: "Verified Record"
                    val reason = mismatchRecord?.notes ?: appWithIssue.pendingActionDesc ?: "Variance under officer review"
                    val desk = reviewItem?.sourceSystem ?: "District Review Desk"

                    "Regarding your ${appWithIssue.schemeCode} application:\n\nAn automated check identified a data variance with $provider:\n" +
                    "• Declared Value: $declared\n" +
                    "• Verified Registry Value: $retrieved\n" +
                    "• Status / Reason: $reason\n\n" +
                    "✨ Note: This item is under active non-blocking evaluation by $desk."
                } else {
                    "Great news $studentName! All your current applications and documents have zero unresolved discrepancies. All automated checks are green."
                }
            }

            q.contains("eligible") || q.contains("apply") || q.contains("top class") || q.contains("fellowship") || q.contains("scheme") -> {
                if (student == null) {
                    "That information is not currently available."
                } else {
                    val unappliedSchemes = schemes.filter { sc -> applications.none { it.schemeId == sc.id } }
                    val evaluations = unappliedSchemes.map { sc ->
                        val eval = EligibilityEngine.evaluate(student, sc, documents, applications)
                        Pair(sc, eval)
                    }

                    val eligibleList = evaluations.filter { it.second.status == com.example.domain.EligibilityStatus.ELIGIBLE }
                    val reviewList = evaluations.filter { it.second.status == com.example.domain.EligibilityStatus.NEEDS_REVIEW }

                    if (eligibleList.isNotEmpty()) {
                        val first = eligibleList.first()
                        val sc = first.first
                        val eval = first.second
                        "Based on EligibilityEngine evaluation for your authenticated profile as a ${student.category} at ${student.institutionName}:\n\n" +
                        "• **${sc.name}** (${eval.matchPercentage}% Match)\n" +
                        "Benefit: ${sc.maxAmount}\n" +
                        "Matched Criteria: ${eval.matchedCriteria.joinToString(", ")}\n\n" +
                        "You can apply in 1 click using your linked DigiLocker single-wallet credentials!"
                    } else if (reviewList.isNotEmpty()) {
                        val first = reviewList.first()
                        val sc = first.first
                        val eval = first.second
                        "For **${sc.name}**, review is required: ${eval.summaryRecommendation} ${eval.conflictReason ?: ""}"
                    } else {
                        "You are already enrolled or have applied for all relevant MoTA schemes matching your current academic level."
                    }
                }
            }

            q.contains("payment") || q.contains("dbt") || q.contains("money") || q.contains("bank") || q.contains("disburs") -> {
                val total = disbursements.sumOf { it.amount }
                if (disbursements.isNotEmpty()) {
                    val last = disbursements.first()
                    "Your DBT status is active on Section 7 Aadhaar Rail.\n\n• Total Received: ₹${String.format(Locale.ENGLISH, "%,d", total.toInt())}\n• Recent Credit: ₹${String.format(Locale.ENGLISH, "%,d", last.amount.toInt())} on ${last.date} to ${last.bankName} (${last.accountMasked})\n• Transaction Ref: ${last.txnRef}"
                } else {
                    "No payments have been disbursed yet for this session. Approved grants will credit directly into your Aadhaar-seeded bank account (${student?.bankAccountMasked})."
                }
            }

            q.contains("document") || q.contains("digilocker") || q.contains("upload") || q.contains("wallet") -> {
                val docCount = documents.size
                "Your single-wallet contains $docCount verified digital credentials from DigiLocker & UIDAI (including ST Caste, Income, and APAAR ID). You never have to upload physical photocopies for any of the 5 MoTA schemes."
            }

            else -> {
                "Johar $studentName! I am JAGO, your AI Tribal Scholarship Guide. I can help you check your application status, explain verification checks, recommend unreached schemes, or track DBT bank transfers. What would you like to know?"
            }
        }

        prefix + response
    }
}
