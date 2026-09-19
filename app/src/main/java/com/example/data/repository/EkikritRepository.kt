package com.example.data.repository

import com.example.data.local.EkikritDatabase
import com.example.data.local.SeedData
import com.example.data.model.*
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class EkikritRepository(
    private val database: EkikritDatabase,
    private val scope: CoroutineScope
) {
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
        database.studentDao().getStudentFlow(id)
    }

    // All available schemes across MoTA
    val schemesFlow: Flow<List<SchemeEntity>> = database.schemeDao().getAllSchemesFlow()

    // Applications strictly filtered to the active student
    val applicationsFlow: Flow<List<ApplicationEntity>> = _activeStudentId.flatMapLatest { id ->
        database.applicationDao().getApplicationsForStudentFlow(id)
    }

    // Documents strictly filtered to the active student
    val documentsFlow: Flow<List<DocumentEntity>> = _activeStudentId.flatMapLatest { id ->
        database.documentDao().getDocumentsForStudentFlow(id)
    }

    // Disbursements strictly filtered to the active student
    val disbursementsFlow: Flow<List<DisbursementEntity>> = _activeStudentId.flatMapLatest { id ->
        database.disbursementDao().getDisbursementsForStudentFlow(id)
    }

    // Notifications strictly filtered to the active student
    val notificationsFlow: Flow<List<NotificationEntity>> = _activeStudentId.flatMapLatest { id ->
        database.notificationDao().getNotificationsForStudentFlow(id)
    }

    // Officer Review Queue (All pending verification exceptions)
    val reviewQueueFlow: Flow<List<ReviewQueueEntity>> = database.reviewQueueDao().getAllReviewItemsFlow()

    // Audit Log Trail
    val auditLogsFlow: Flow<List<AuditLogEntity>> = database.auditLogDao().getAllLogsFlow()

    // All Students list for quick demo persona switching
    val allStudentsFlow: Flow<List<StudentEntity>> = database.studentDao().getAllStudentsFlow()

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

    suspend fun switchStudent(studentId: String) = withContext(Dispatchers.IO) {
        val student = database.studentDao().getStudent(studentId)
        if (student != null) {
            _activeStudentId.value = studentId
            database.auditLogDao().insert(
                AuditLogEntity(
                    action = "AUTH_USER_SWITCH",
                    actor = student.name,
                    details = "Session switched to student ${student.name} (ID: $studentId, Category: ${student.category}).",
                    timestamp = getCurrentTimestamp()
                )
            )
        }
    }

    suspend fun authenticateWithPhoneOrAadhaar(phoneOrAadhaar: String, customName: String? = null): StudentEntity = withContext(Dispatchers.IO) {
        val cleanInput = phoneOrAadhaar.trim().replace("\\s+".toRegex(), "").replace("-", "")
        val existing = database.studentDao().findStudentByPhoneOrAadhaar(cleanInput)
        
        val student = if (existing != null) {
            existing
        } else {
            val newId = "STU_MOCK_${System.currentTimeMillis().toString().takeLast(4)}"
            val name = customName?.ifBlank { null } ?: "Student User"
            val newStudent = StudentEntity(
                id = newId,
                name = name,
                mobile = phoneOrAadhaar,
                category = "ST (Tribal Scholar)",
                academicLevel = "UNDERGRADUATE",
                institutionName = "National Institute of Technology, Rourkela",
                annualIncome = 220000.0,
                aadhaarMasked = "XXXX-XXXX-${cleanInput.takeLast(4).padStart(4, '0')}"
            )
            database.studentDao().insertStudent(newStudent)
            newStudent
        }

        _activeStudentId.value = student.id
        database.auditLogDao().insert(
            AuditLogEntity(
                action = "AUTH_OTP_LOGIN",
                actor = student.name,
                details = "Student logged in via Aadhaar / Mobile OTP mock rail (UIDAI Level-2).",
                timestamp = getCurrentTimestamp()
            )
        )
        student
    }

    fun getActiveStudentSync(): StudentEntity? {
        val currentId = _activeStudentId.value
        // Helper to grab synchronous student or default
        return null
    }

    suspend fun getVerificationRecordsForApp(appId: String): List<VerificationRecordEntity> = withContext(Dispatchers.IO) {
        database.verificationRecordDao().getRecordsForApp(appId)
    }

    fun getVerificationRecordsForAppFlow(appId: String): Flow<List<VerificationRecordEntity>> {
        return database.verificationRecordDao().getRecordsForAppFlow(appId)
    }

    /**
     * Executes multi-source verification across all 7 providers.
     */
    suspend fun runSevenSourceVerification(applicationId: String, studentIdOverride: String? = null) = withContext(Dispatchers.IO) {
        val application = database.applicationDao().getApplicationById(applicationId) ?: return@withContext
        val currentStudentId = studentIdOverride ?: application.studentId
        val student = database.studentDao().getStudent(currentStudentId) ?: return@withContext
        val documents = database.documentDao().getDocumentsForStudent(currentStudentId)

        val output = UnifiedVerificationEngine.executeSevenSourceVerification(student, application, documents)

        // Clear previous records for this application and save new ones
        database.verificationRecordDao().deleteForApp(applicationId)
        database.verificationRecordDao().insertAll(output.records)

        val hasMismatch = output.records.any { it.status == "MISMATCH" }

        if (output.reviewItem != null) {
            database.reviewQueueDao().insert(output.reviewItem)
        }

        val newStage = if (hasMismatch) "INSTITUTE_VERIFICATION" else "STATE_VERIFICATION"
        val statusText = if (hasMismatch) {
            "Multi-source API verification completed (6/7 verified). 1 minor income variance auto-routed to District Review Desk."
        } else {
            "All 7 government registries verified. State Tribal Welfare clearance in progress."
        }

        database.applicationDao().updateStage(
            appId = applicationId,
            stage = newStage,
            statusText = statusText,
            timestamp = getCurrentTimestamp(),
            hasDiscrepancy = hasMismatch,
            pendingAction = if (hasMismatch) "Officer Review Desk is clearing the income certificate tolerance." else null
        )

        database.auditLogDao().insert(
            AuditLogEntity(
                action = "VERIFICATION_EXECUTE",
                actor = "Unified Verification Engine",
                details = "Executed 7-source verification for ${application.schemeCode}: ${output.summaryMessage}",
                timestamp = getCurrentTimestamp()
            )
        )

        database.notificationDao().insert(
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

    /**
     * Officer Review Desk Action: Approves or clarifies an exception.
     */
    suspend fun resolveReviewItem(reviewItemId: String, isApproved: Boolean, notes: String) = withContext(Dispatchers.IO) {
        val reviewItem = database.reviewQueueDao().getById(reviewItemId) ?: return@withContext
        val now = getCurrentTimestamp()

        val updatedStatus = if (isApproved) "APPROVED" else "RESUBMIT"
        database.reviewQueueDao().update(
            reviewItem.copy(
                status = updatedStatus,
                resolvedAt = now,
                resolutionNotes = notes.ifBlank { if (isApproved) "Income variance verified within allowable scheme ceiling." else "Clarification requested from student." }
            )
        )

        // Update corresponding verification record
        database.verificationRecordDao().updateRecordStatus(
            recordId = reviewItem.verificationRecordId,
            status = if (isApproved) "RESOLVED" else "MISMATCH",
            notes = "Officer cleared variance: $notes"
        )

        // Update application state
        val application = database.applicationDao().getApplicationById(reviewItem.applicationId)
        if (application != null) {
            val newStage = if (isApproved) "STATE_VERIFICATION" else "INSTITUTE_VERIFICATION"
            val newStatusText = if (isApproved) {
                "Officer review complete. Exception cleared. Sent for State Tribal Welfare clearance."
            } else {
                "Action requested: Officer requested additional clarification on income certificate."
            }

            database.applicationDao().updateStage(
                appId = application.id,
                stage = newStage,
                statusText = newStatusText,
                timestamp = now,
                hasDiscrepancy = !isApproved,
                pendingAction = if (isApproved) null else "Please review officer remarks: $notes"
            )

            database.notificationDao().insert(
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

        database.auditLogDao().insert(
            AuditLogEntity(
                action = if (isApproved) "OFFICER_APPROVE_EXCEPTION" else "OFFICER_REQUEST_CLARIFICATION",
                actor = "District Tribal Welfare Officer",
                details = "Item $reviewItemId (${reviewItem.fieldName}) resolved with status $updatedStatus. Notes: $notes",
                timestamp = now
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
        val student = database.studentDao().getStudent(targetStudentId) ?: return@withContext Pair(false, "Student not found")
        val scheme = database.schemeDao().getSchemeById(schemeId) ?: return@withContext Pair(false, "Scheme not found")
        val docs = database.documentDao().getDocumentsForStudent(targetStudentId)
        val existingApps = database.applicationDao().getApplicationsForStudent(targetStudentId)

        // Check eligibility engine
        val eligibility = EligibilityEngine.evaluate(student, scheme, docs, existingApps)

        if (eligibility.status == com.example.domain.EligibilityStatus.NOT_ELIGIBLE) {
            val errorReason = eligibility.failedCriteria.firstOrNull() ?: "Your profile does not currently meet this scheme's eligibility requirements."
            val userMsg = "You're not currently eligible for this scholarship. $errorReason"
            database.notificationDao().insert(
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
                database.notificationDao().insert(
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
                database.notificationDao().insert(
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
            syncState = if (isOffline) "PENDING_SYNC" else "SYNCED"
        )

        database.applicationDao().insert(newApp)

        // Clear any saved draft for this specific student
        database.applicationDraftDao().deleteDraft(student.id, scheme.id)

        database.auditLogDao().insert(
            AuditLogEntity(
                action = "APPLICATION_SUBMIT_ONE_CLICK",
                actor = student.name,
                details = "Submitted 1-click application for ${scheme.name} (ID: $newAppId, Student: ${student.id}). Zero paper re-upload.",
                timestamp = now
            )
        )

        database.notificationDao().insert(
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
        database.applicationDraftDao().insert(draft)
    }

    suspend fun getApplicationDraft(schemeId: String): ApplicationDraftEntity? = withContext(Dispatchers.IO) {
        val currentStudentId = _activeStudentId.value
        database.applicationDraftDao().getDraft(currentStudentId, schemeId)
    }

    suspend fun syncPendingDrafts() = withContext(Dispatchers.IO) {
        val drafts = database.applicationDraftDao().getPendingSyncDrafts()
        for (draft in drafts) {
            applyForScheme(draft.schemeId, draft.declaredIncome, studentIdOverride = draft.studentId)
        }
    }

    suspend fun updateStudentConsent(studentId: String, hasConsent: Boolean) = withContext(Dispatchers.IO) {
        database.studentDao().updateStudentConsent(studentId, hasConsent)
        val student = database.studentDao().getStudent(studentId)
        database.auditLogDao().insert(
            AuditLogEntity(
                action = if (hasConsent) "CONSENT_GRANT" else "CONSENT_REVOKE",
                actor = student?.name ?: "Student",
                details = "DPDP Act 2023 digital consent ${if (hasConsent) "GRANTED" else "REVOKED/RESTRICTED"} for student ID $studentId.",
                timestamp = getCurrentTimestamp()
            )
        )
    }

    suspend fun pullDocumentFromDigiLocker(type: String, title: String, number: String, issuer: String) = withContext(Dispatchers.IO) {
        val currentStudentId = _activeStudentId.value
        val student = database.studentDao().getStudent(currentStudentId) ?: return@withContext
        val newDocId = "DOC_${System.currentTimeMillis().toString().takeLast(5)}"
        val doc = DocumentEntity(
            id = newDocId,
            studentId = student.id,
            type = type,
            title = title,
            docNumberMasked = number,
            source = "DigiLocker National Depository",
            verificationStatus = "VERIFIED",
            issuedDate = "01 Jan 2026",
            issuedBy = issuer,
            isReusable = true
        )
        database.documentDao().insert(doc)

        database.auditLogDao().insert(
            AuditLogEntity(
                action = "DIGILOCKER_DOC_PULL",
                actor = student.name,
                details = "Pulled $title ($number) into single-wallet. Reusable across all 5 schemes.",
                timestamp = getCurrentTimestamp()
            )
        )
    }

    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {
        database.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        val currentStudentId = _activeStudentId.value
        database.notificationDao().markAllAsRead(currentStudentId)
    }

    suspend fun resetAllDemoData() = withContext(Dispatchers.IO) {
        SeedData.resetDemo(database)
        _activeStudentId.value = "STU_2026_01"
    }

    /**
     * Live State-Aware Dynamic JAGO AI Assistant
     */
    suspend fun generateJagoResponse(userQuery: String): String = withContext(Dispatchers.IO) {
        val currentStudentId = _activeStudentId.value
        val student = database.studentDao().getStudent(currentStudentId)
        val applications = database.applicationDao().getApplicationsForStudent(currentStudentId)
        val documents = database.documentDao().getDocumentsForStudent(currentStudentId)
        val disbursements = database.disbursementDao().getDisbursementsForStudent(currentStudentId)
        val schemes = database.schemeDao().getAllSchemes()

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
                    val records = database.verificationRecordDao().getRecordsForApp(appWithIssue.id)
                    val mismatchRecord = records.find { it.status == "MISMATCH" }
                    val reviewItem = database.reviewQueueDao().getByAppId(appWithIssue.id)

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
                        val eval = com.example.domain.EligibilityEngine.evaluate(student, sc, documents, applications)
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
