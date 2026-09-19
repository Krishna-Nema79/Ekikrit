package com.example.data.repository

import com.example.data.ai.JagoAiService
import com.example.data.eligibility.EligibilityEngine
import com.example.data.local.EkikritDatabase
import com.example.data.local.SeedData
import com.example.data.model.*
import com.example.data.verification.LocalDemoVerificationDataSource
import com.example.data.verification.VerificationDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EkikritRepository(
    private val db: EkikritDatabase,
    private val verificationDataSource: VerificationDataSource = LocalDemoVerificationDataSource(),
    private val jagoAiService: JagoAiService = JagoAiService()
) {

    private val _activeStudentId = MutableStateFlow("STU_2026_01")
    val activeStudentId = _activeStudentId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val studentFlow: Flow<StudentEntity?> = _activeStudentId.flatMapLatest { id ->
        db.studentDao().getStudentFlow(id)
    }
    val allStudentsFlow: Flow<List<StudentEntity>> = db.studentDao().getAllStudentsFlow()
    val schemesFlow: Flow<List<SchemeEntity>> = db.schemeDao().getAllSchemesFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val applicationsFlow: Flow<List<ApplicationEntity>> = _activeStudentId.flatMapLatest { id ->
        db.applicationDao().getApplicationsForStudentFlow(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val documentsFlow: Flow<List<DocumentEntity>> = _activeStudentId.flatMapLatest { id ->
        db.documentDao().getDocumentsForStudentFlow(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val disbursementsFlow: Flow<List<DisbursementEntity>> = _activeStudentId.flatMapLatest { id ->
        db.disbursementDao().getDisbursementsForStudentFlow(id)
    }

    val pendingReviewItemsFlow: Flow<List<ReviewQueueEntity>> = db.reviewQueueDao().getPendingReviewItemsFlow()
    val allReviewItemsFlow: Flow<List<ReviewQueueEntity>> = db.reviewQueueDao().getAllReviewItemsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val auditLogsFlow: Flow<List<AuditLogEntity>> = _activeStudentId.flatMapLatest { id ->
        db.auditLogDao().getLogsForStudentFlow(id)
    }

    suspend fun ensurePresetStudents() = withContext(Dispatchers.IO) {
        SeedData.ensurePresetStudents(db)
    }

    companion object {
        /**
         * Demo-only pre-seeded identities permitted for role switching during presentations.
         * Production applications must never allow arbitrary client-side profile switching
         * to prevent Insecure Direct Object Reference (IDOR) and unauthorized impersonation.
         */
        val DEMO_PERMITTED_SWITCH_IDS = setOf("STU_2026_01", "REV_OFFICER_01")
    }

    /**
     * Demo-only role switcher strictly limited to the two seeded hackathon identities:
     * - "STU_2026_01": Beneficiary Student (Birsa Munda Tirkey)
     * - "REV_OFFICER_01": Reviewing Officer (Dr. S. K. Mahapatra)
     *
     * In a production environment, switching user contexts without backend-authenticated
     * credentials violates fundamental zero-trust and access-control security standards.
     * To prevent arbitrary callers from switching to unverified student profiles, this method
     * explicitly rejects any identifier other than the two designated demo identities.
     */
    suspend fun switchStudent(studentId: String) = withContext(Dispatchers.IO) {
        if (studentId !in DEMO_PERMITTED_SWITCH_IDS) {
            throw SecurityException(
                "Access Denied: Profile switching is restricted exclusively to seeded demo identities ('STU_2026_01', 'REV_OFFICER_01'). Arbitrary profile switching is prohibited."
            )
        }

        val student = db.studentDao().getStudent(studentId)
            ?: throw IllegalArgumentException("Demo beneficiary profile '$studentId' not found.")

        _activeStudentId.value = studentId

        db.auditLogDao().insert(
            AuditLogEntity(
                action = "Demo Profile Switched",
                actor = if (student.role == UserRole.REVIEWER.name) "Officer (${student.name})" else "Student (${student.name})",
                details = "Active demo session switched to ${student.name} (${student.role}).",
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

        // Local-demo registration intentionally creates an incomplete profile. It does not claim
        // identity, DigiLocker, bank, income, caste, or institution verification.
        val newId = "STU_" + System.currentTimeMillis().toString().takeLast(6)
        val studentName = name?.trim().orEmpty()
        require(studentName.length in 2..80) { "Enter a student name between 2 and 80 characters." }
        val newStudent = StudentEntity(
            id = newId,
            name = studentName,
            dob = "",
            mobile = if (digits.length == 10) trimmed else "",
            state = "",
            institutionId = "",
            institutionName = "",
            course = "",
            category = "",
            pvtgCommunity = "",
            preferredLanguage = "en",
            apaarId = "",
            annualIncome = 0.0,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getVerificationRecordsFlow(applicationId: String): Flow<List<VerificationRecordEntity>> {
        return _activeStudentId.flatMapLatest { currentStudentId ->
            flow {
                val app = db.applicationDao().getApplicationById(applicationId)
                val user = db.studentDao().getStudent(currentStudentId)
                if (app != null && (app.studentId == currentStudentId || user?.role == UserRole.REVIEWER.name)) {
                    emitAll(db.verificationRecordDao().getRecordsForAppFlow(applicationId))
                } else {
                    emit(emptyList())
                }
            }
        }
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
    }

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

    suspend fun setConsentGiven(given: Boolean) = withContext(Dispatchers.IO) {
        val current = db.studentDao().getStudent(_activeStudentId.value) ?: return@withContext
        val updated = current.copy(hasConsentGiven = given)
        db.studentDao().updateStudent(updated)
        db.auditLogDao().insert(
            AuditLogEntity(
                action = if (given) "Consent Granted" else "Consent Revoked",
                actor = "Student (${current.name})",
                details = if (given) "DPDP Act compliance consent recorded for automated DigiLocker document fetch." else "DigiLocker access consent revoked.",
                timestamp = getCurrentTimestamp(),
                studentId = current.id
            )
        )
    }

    suspend fun pullDocumentFromDigiLocker(type: String, title: String, docNumber: String, issuer: String) = withContext(Dispatchers.IO) {
        val student = db.studentDao().getStudent(_activeStudentId.value)
            ?: throw IllegalStateException("No active profile.")
        check(student.hasConsentGiven) { "Consent is required before adding protected credentials." }
        require(type.length in 2..40 && title.trim().length in 3..120 && issuer.trim().length in 3..120) {
            "Document type, title, and issuer must be valid."
        }
        require(docNumber.trim().length in 4..64) { "Document reference must be between 4 and 64 characters." }
        val docId = "DOC_${student.id}_${System.currentTimeMillis()}"
        val newDoc = DocumentEntity(
            id = docId,
            studentId = student.id,
            type = type,
            title = title,
            docNumberMasked = docNumber,
            source = "DigiLocker (Govt Issuer)",
            verificationStatus = "PENDING_LOCAL_VALIDATION",
            issuedDate = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(Date()),
            issuedBy = issuer,
            isReusable = true
        )
        db.documentDao().insert(newDoc)
        db.auditLogDao().insert(
            AuditLogEntity(
                action = "Document Pulled from DigiLocker",
                actor = "DigiLocker Rail",
                details = "Local demo credential '$title' added pending real issuer verification.",
                timestamp = getCurrentTimestamp(),
                studentId = student.id
            )
        )
    }

    suspend fun syncDigiLockerFull(mobileOrAadhaar: String) = withContext(Dispatchers.IO) {
        val student = db.studentDao().getStudent(_activeStudentId.value)
            ?: throw IllegalStateException("No active profile.")
        require(mobileOrAadhaar.filter(Char::isDigit).length in listOf(10, 12)) { "Enter a valid mobile number or Aadhaar identifier." }
        // Local demo sync; consent is explicitly recorded before local fixtures are added.
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
                details = "Local demo credential set added after consent. No live DigiLocker verification was performed.",
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

    suspend fun triggerLiveMockVerification(appId: String) = withContext(Dispatchers.IO) {
        val currentStudentId = _activeStudentId.value
        val currentUser = db.studentDao().getStudent(currentStudentId)
            ?: throw IllegalStateException("No active profile.")
        val app = db.applicationDao().getApplicationById(appId) ?: return@withContext

        // Enforce application ownership unless acting as reviewer
        if (app.studentId != currentStudentId && currentUser.role != UserRole.REVIEWER.name) {
            throw SecurityException("Access denied: cannot trigger verification for application belonging to another student.")
        }

        val targetStudent = db.studentDao().getStudent(app.studentId)
            ?: throw IllegalStateException("Beneficiary profile not found.")

        check(targetStudent.hasConsentGiven) {
            "Active consent is required to trigger automated multi-source verification."
        }

        val now = getCurrentTimestamp()

        // Delegate to clean VerificationDataSource
        val records = verificationDataSource.verifyAllSources(targetStudent, app, now)
        db.verificationRecordDao().insertAll(records)

        val mismatchRecord = records.firstOrNull { it.status == VerificationStatus.MISMATCH.name }

        if (mismatchRecord != null) {
            // Discrepancy detected: route non-blocking exception to Reviewer Desk
            val reviewItem = ReviewQueueEntity(
                id = "REV_${System.currentTimeMillis()}",
                verificationRecordId = mismatchRecord.id,
                applicationId = appId,
                studentName = targetStudent.name,
                category = targetStudent.category,
                schemeName = app.schemeName,
                sourceSystem = mismatchRecord.sourceSystem,
                fieldName = mismatchRecord.fieldChecked,
                declaredValue = mismatchRecord.declaredValue,
                retrievedValue = mismatchRecord.retrievedValue,
                mismatchReason = mismatchRecord.notes,
                status = "PENDING",
                createdAt = now
            )
            db.reviewQueueDao().insert(reviewItem)

            db.applicationDao().updateStage(
                appId = appId,
                stage = "UNDER_VERIFICATION",
                statusText = "Verification In Progress (Income variance routed to Reviewer Desk)",
                timestamp = now,
                hasDiscrepancy = true,
                pendingAction = "Income discrepancy under non-blocking review by Tribal Welfare Officer. Student action not required."
            )

            db.auditLogDao().insert(
                AuditLogEntity(
                    action = "Automated Verification Executed",
                    actor = "Ekikrit Verification Engine",
                    details = "Multi-source check completed. ${mismatchRecord.sourceSystem}: MISMATCH. Non-blocking exception routed to Reviewer Desk.",
                    timestamp = now,
                    studentId = targetStudent.id
                )
            )
        } else {
            // All verified
            db.applicationDao().updateStage(
                appId = appId,
                stage = "SANCTIONED",
                statusText = "Multi-source Verification Cleared — Sanctioned for DBT Disbursement",
                timestamp = now,
                hasDiscrepancy = false,
                pendingAction = null
            )

            db.auditLogDao().insert(
                AuditLogEntity(
                    action = "Automated Verification Executed",
                    actor = "Ekikrit Verification Engine",
                    details = "All 6 digital registries verified successfully with 100% match.",
                    timestamp = now,
                    studentId = targetStudent.id
                )
            )
        }
    }

    suspend fun resolveReviewItem(reviewItemId: String, isApproved: Boolean, notes: String) = withContext(Dispatchers.IO) {
        val currentStudentId = _activeStudentId.value
        val currentUser = db.studentDao().getStudent(currentStudentId)
            ?: throw IllegalStateException("No active profile.")

        // Role-Based Authorization Check: Normal students cannot resolve reviewer items
        if (currentUser.role != UserRole.REVIEWER.name) {
            throw SecurityException("Access denied: Only users with REVIEWER role can resolve reviewer exceptions (current role: ${currentUser.role}).")
        }

        val item = db.reviewQueueDao().getById(reviewItemId)
            ?: throw IllegalArgumentException("Review item $reviewItemId not found.")

        val app = db.applicationDao().getApplicationById(item.applicationId)
            ?: throw IllegalStateException("Application ${item.applicationId} not found.")

        val now = getCurrentTimestamp()

        if (isApproved) {
            val updatedItem = item.copy(
                status = "APPROVED",
                resolvedAt = now,
                resolutionNotes = notes.ifBlank { "Approved on exception basis: Income remains below ₹2,50,000 scheme ceiling." }
            )
            db.reviewQueueDao().update(updatedItem)

            // Update verification record to RESOLVED
            db.verificationRecordDao().updateRecordStatus(
                recordId = item.verificationRecordId,
                status = "RESOLVED",
                notes = "EXCEPTION APPROVED by Reviewer Officer ($now): Variance resolved under Chapter IV Section 12 Rule."
            )

            // Update ONLY the selected application
            db.applicationDao().updateStage(
                appId = app.id,
                stage = "SANCTIONED",
                statusText = "Sanctioned (${app.schemeCode}/2026/OK) — Cleared for DBT Disbursement",
                timestamp = now,
                hasDiscrepancy = false,
                pendingAction = null
            )

            db.auditLogDao().insert(
                AuditLogEntity(
                    action = "Exception Approved by Reviewer",
                    actor = "Tribal Welfare Officer (${currentUser.name})",
                    details = "Approved exception for application ${app.id} (${app.schemeName}). Promoted to SANCTIONED.",
                    timestamp = now,
                    studentId = app.studentId
                )
            )
        } else {
            val updatedItem = item.copy(
                status = "RESUBMIT",
                resolvedAt = now,
                resolutionNotes = notes.ifBlank { "Additional clarification required on income certificate." }
            )
            db.reviewQueueDao().update(updatedItem)

            db.applicationDao().updateStage(
                appId = app.id,
                stage = "UNDER_VERIFICATION",
                statusText = "Clarification Requested by Reviewer",
                timestamp = now,
                hasDiscrepancy = true,
                pendingAction = "Please check JAGO assistant or provide updated income declaration."
            )

            db.auditLogDao().insert(
                AuditLogEntity(
                    action = "Clarification Requested",
                    actor = "Tribal Welfare Officer (${currentUser.name})",
                    details = "Requested clarification for application ${app.id}.",
                    timestamp = now,
                    studentId = app.studentId
                )
            )
        }
    }

    suspend fun applyForUnreachedScheme(schemeId: String, academicYear: String = "2026-27") = withContext(Dispatchers.IO) {
        val student = db.studentDao().getStudent(_activeStudentId.value)
            ?: throw IllegalStateException("No active student profile.")

        check(student.hasConsentGiven) {
            "DPDP Act consent is required before applying for scholarships with shared credentials."
        }

        val scheme = db.schemeDao().getSchemeById(schemeId)
            ?: throw IllegalArgumentException("Scheme not found: $schemeId")

        val now = getCurrentTimestamp()

        // Application Ownership + Duplicate Prevention: Check studentId + schemeId + academicYear
        val existingApp = db.applicationDao().getApplicationByStudentSchemeYear(student.id, schemeId, academicYear)
        if (existingApp != null) {
            if (existingApp.currentStage != "NOT_APPLIED") {
                throw IllegalStateException("An application has already been submitted for ${scheme.name} ($academicYear).")
            }
            val updated = existingApp.copy(
                currentStage = "UNDER_VERIFICATION",
                statusText = "Applied with 1-Click DigiLocker & APAAR credentials",
                appliedDate = now,
                lastUpdated = now,
                pendingActionDesc = "Cross-matching UDISE+ and AISHE enrollment records.",
                hasDiscrepancy = false
            )
            db.applicationDao().updateApplication(updated)
        } else {
            val amount = if (scheme.id == "SCH_TOPCLASS") 250000.0 else if (scheme.id == "SCH_PRE") 4500.0 else 78000.0
            val newApp = ApplicationEntity(
                id = "APP_${scheme.code}_${System.currentTimeMillis()}",
                studentId = student.id,
                schemeId = scheme.id,
                schemeCode = scheme.code,
                schemeName = scheme.name,
                currentStage = "UNDER_VERIFICATION",
                statusText = "Applied with 1-Click DigiLocker & APAAR credentials",
                appliedDate = now,
                lastUpdated = now,
                pendingActionDesc = null,
                hasDiscrepancy = false,
                sanctionedAmount = amount,
                estimatedDisbursementDays = 21,
                academicYear = academicYear
            )
            db.applicationDao().insert(newApp)
        }

        db.auditLogDao().insert(
            AuditLogEntity(
                action = "Unreached Beneficiary Applied",
                actor = "Student (${student.name})",
                details = "Applied for '${scheme.name}' using existing DigiLocker wallet and APAAR credentials without re-uploading documents.",
                timestamp = now,
                studentId = student.id
            )
        )
    }

    suspend fun generateJagoResponse(query: String, lang: String): JagoMessage = withContext(Dispatchers.IO) {
        val studentId = _activeStudentId.value
        val student = db.studentDao().getStudent(studentId)
        val studentApps = db.applicationDao().getApplicationsForStudent(studentId)
        val allSchemes = db.schemeDao().getAllSchemesFlow().firstOrNull() ?: emptyList()
        val pendingReviews = db.reviewQueueDao().getPendingReviewItemsFlow().firstOrNull() ?: emptyList()

        val evaluations = allSchemes.map {
            EligibilityEngine.evaluateEligibility(student, it, studentApps)
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
}
