package com.example.data.repository

import com.example.data.local.EkikritDatabase
import com.example.data.local.SeedData
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EkikritRepository(private val db: EkikritDatabase) {

    private val _activeStudentId = MutableStateFlow("STU_2026_01")
    val activeStudentId = _activeStudentId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val studentFlow: Flow<StudentEntity?> = _activeStudentId.flatMapLatest { id ->
        db.studentDao().getStudentFlow(id)
    }
    val allStudentsFlow: Flow<List<StudentEntity>> = db.studentDao().getAllStudentsFlow()
    val schemesFlow: Flow<List<SchemeEntity>> = db.schemeDao().getAllSchemesFlow()
    val applicationsFlow: Flow<List<ApplicationEntity>> = db.applicationDao().getAllApplicationsFlow()
    val documentsFlow: Flow<List<DocumentEntity>> = db.documentDao().getAllDocumentsFlow()
    val disbursementsFlow: Flow<List<DisbursementEntity>> = db.disbursementDao().getAllDisbursementsFlow()
    val pendingReviewItemsFlow: Flow<List<ReviewQueueEntity>> = db.reviewQueueDao().getPendingReviewItemsFlow()
    val allReviewItemsFlow: Flow<List<ReviewQueueEntity>> = db.reviewQueueDao().getAllReviewItemsFlow()
    val auditLogsFlow: Flow<List<AuditLogEntity>> = db.auditLogDao().getAllLogsFlow()

    suspend fun ensurePresetStudents() = withContext(Dispatchers.IO) {
        SeedData.ensurePresetStudents(db)
    }

    suspend fun switchStudent(studentId: String) = withContext(Dispatchers.IO) {
        _activeStudentId.value = studentId
        val student = db.studentDao().getStudent(studentId)
        if (student != null) {
            db.auditLogDao().insert(
                AuditLogEntity(
                    action = "User Switched Profile",
                    actor = "Student (${student.name})",
                    details = "Active beneficiary session switched to ${student.name} (${student.category}, ${student.institutionName}).",
                    timestamp = getCurrentTimestamp()
                )
            )
        }
    }

    suspend fun loginWithMobileOrAadhaar(identifier: String, name: String? = null): StudentEntity = withContext(Dispatchers.IO) {
        val trimmed = identifier.trim()
        val existing = db.studentDao().findStudentByPhoneOrAadhaar(trimmed)
        if (existing != null) {
            _activeStudentId.value = existing.id
            db.auditLogDao().insert(
                AuditLogEntity(
                    action = "Student Authenticated",
                    actor = "Student (${existing.name})",
                    details = "Logged in via registered mobile/Aadhaar ($trimmed).",
                    timestamp = getCurrentTimestamp()
                )
            )
            return@withContext existing
        }

        // Create new student persona
        val newId = "STU_" + System.currentTimeMillis().toString().takeLast(6)
        val studentName = if (!name.isNullOrBlank()) name.trim() else "Beneficiary Student"
        val newStudent = StudentEntity(
            id = newId,
            name = studentName,
            dob = "10-06-2004",
            mobile = if (trimmed.startsWith("+91")) trimmed else "+91 $trimmed",
            state = "Jharkhand",
            institutionId = "AISHE-U-0199",
            institutionName = "Birsa Agricultural University",
            course = "B.Sc (Hons) Agriculture",
            category = "ST (Tribal Beneficiary)",
            pvtgCommunity = "",
            preferredLanguage = "en",
            apaarId = "APAAR-${(1000..9999).random()}-${(1000..9999).random()}-${(1000..9999).random()}",
            annualIncome = 150000.0,
            aadhaarMasked = "XXXX-XXXX-${trimmed.takeLast(4).padStart(4, '0')}",
            bankAccountMasked = "State Bank of India (A/C **${(1000..9999).random()})",
            ifscCode = "SBIN0004920",
            isDigiLockerLinked = true,
            hasConsentGiven = true
        )
        db.studentDao().insertStudent(newStudent)
        _activeStudentId.value = newId
        db.auditLogDao().insert(
            AuditLogEntity(
                action = "New Student Registered & Logged In",
                actor = "Student ($studentName)",
                details = "Registered and authenticated session via Aadhaar/Mobile ($trimmed).",
                timestamp = getCurrentTimestamp()
            )
        )
        return@withContext newStudent
    }

    fun getVerificationRecordsFlow(applicationId: String): Flow<List<VerificationRecordEntity>> {
        return db.verificationRecordDao().getRecordsForAppFlow(applicationId)
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
    }

    suspend fun saveStudentProfile(student: StudentEntity) = withContext(Dispatchers.IO) {
        db.studentDao().insertStudent(student)
        db.auditLogDao().insert(
            AuditLogEntity(
                action = "Profile Updated",
                actor = "Student (${student.name})",
                details = "Updated category (${student.category}), preferred language (${student.preferredLanguage}), and institution.",
                timestamp = getCurrentTimestamp()
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
                timestamp = getCurrentTimestamp()
            )
        )
    }

    suspend fun pullDocumentFromDigiLocker(type: String, title: String, docNumber: String, issuer: String) = withContext(Dispatchers.IO) {
        val docId = "DOC_${System.currentTimeMillis()}"
        val newDoc = DocumentEntity(
            id = docId,
            studentId = "STU_2026_01",
            type = type,
            title = title,
            docNumberMasked = docNumber,
            source = "DigiLocker (Govt Issuer)",
            verificationStatus = "VERIFIED",
            issuedDate = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(Date()),
            issuedBy = issuer,
            isReusable = true
        )
        db.documentDao().insert(newDoc)
        db.auditLogDao().insert(
            AuditLogEntity(
                action = "Document Pulled from DigiLocker",
                actor = "DigiLocker Rail",
                details = "Fetched and cryptographically verified '$title' (Ref $docNumber) without requiring physical paper upload.",
                timestamp = getCurrentTimestamp()
            )
        )
    }

    suspend fun syncDigiLockerFull(mobileOrAadhaar: String) = withContext(Dispatchers.IO) {
        val student = db.studentDao().getStudent(_activeStudentId.value)
        if (student != null) {
            db.studentDao().updateStudent(student.copy(isDigiLockerLinked = true, hasConsentGiven = true))
        }

        val initialDocs = listOf(
            DocumentEntity(
                id = "DOC_AADHAAR_01",
                studentId = "STU_2026_01",
                type = "Aadhaar",
                title = "Aadhaar Demographic Card",
                docNumberMasked = "XXXX-XXXX-8924",
                source = "UIDAI DigiLocker",
                verificationStatus = "VERIFIED",
                issuedDate = "12-04-2019",
                issuedBy = "UIDAI, Govt of India",
                isReusable = true
            ),
            DocumentEntity(
                id = "DOC_CASTE_01",
                studentId = "STU_2026_01",
                type = "Caste",
                title = "Scheduled Tribe Certificate (PVTG - Birhor)",
                docNumberMasked = "ST/OD/2026/8912",
                source = "e-District Odisha (DigiLocker)",
                verificationStatus = "VERIFIED",
                issuedDate = "10-01-2026",
                issuedBy = "Tehsildar, Bonai, Sundargarh",
                isReusable = true
            ),
            DocumentEntity(
                id = "DOC_INCOME_01",
                studentId = "STU_2026_01",
                type = "Income",
                title = "Annual Family Income Certificate",
                docNumberMasked = "INC/OD/2026/4102",
                source = "e-District Odisha (DigiLocker)",
                verificationStatus = "VERIFIED",
                issuedDate = "15-02-2026",
                issuedBy = "Revenue Officer, Sundargarh",
                isReusable = true
            ),
            DocumentEntity(
                id = "DOC_MARKSHEET_01",
                studentId = "STU_2026_01",
                type = "Marksheet",
                title = "Higher Secondary Examination (Class XII)",
                docNumberMasked = "CHSE/2022/88219",
                source = "CHSE Odisha (DigiLocker)",
                verificationStatus = "VERIFIED",
                issuedDate = "22-06-2022",
                issuedBy = "Council of Higher Secondary Education",
                isReusable = true
            ),
            DocumentEntity(
                id = "DOC_APAAR_01",
                studentId = "STU_2026_01",
                type = "APAAR",
                title = "One Nation One Student ID (APAAR/ABC)",
                docNumberMasked = "APAAR-8839-4021-9920",
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
                details = "Student authenticated via DigiLocker OTP ($mobileOrAadhaar). Synced 5 digital credentials with 100% cryptographic integrity under DPDP Act.",
                timestamp = getCurrentTimestamp()
            )
        )
    }

    suspend fun triggerLiveMockVerification(appId: String) = withContext(Dispatchers.IO) {
        val app = db.applicationDao().getApplicationById(appId) ?: return@withContext
        val now = getCurrentTimestamp()

        // 1. Clear existing verification records and re-run with live statuses
        val uidaiRecord = VerificationRecordEntity(
            id = "VER_UIDAI_${System.currentTimeMillis()}",
            applicationId = appId,
            schemeId = app.schemeId,
            sourceSystem = "UIDAI (Aadhaar Rail)",
            fieldChecked = "Demographic & Aadhaar Authentication",
            declaredValue = "Birsa Munda Tirkey, 15-08-2003",
            retrievedValue = "Birsa Munda Tirkey, 15-08-2003 (Score: 99.1%)",
            status = "VERIFIED",
            timestamp = now,
            notes = "Aadhaar demographic authentication verified successfully with UIDAI central vault."
        )

        val edistrictRecord = VerificationRecordEntity(
            id = "VER_EDIST_${System.currentTimeMillis()}",
            applicationId = appId,
            schemeId = app.schemeId,
            sourceSystem = "e-District Revenue Portal",
            fieldChecked = "Annual Household Income Certificate",
            declaredValue = "₹2,10,000 / annum",
            retrievedValue = "₹2,35,000 / annum (Cert INC/OD/2026/00142)",
            status = "MISMATCH",
            timestamp = now,
            notes = "Discrepancy: Income variance detected (+11.9%). Auto-routed to Reviewer Queue instead of rejection."
        )

        db.verificationRecordDao().insertAll(listOf(uidaiRecord, edistrictRecord))

        // Create or update review queue item
        val reviewItem = ReviewQueueEntity(
            id = "REV_${System.currentTimeMillis()}",
            verificationRecordId = edistrictRecord.id,
            applicationId = appId,
            studentName = "Birsa Munda Tirkey",
            category = "ST (PVTG - Birhor)",
            schemeName = app.schemeName,
            sourceSystem = "e-District Odisha",
            fieldName = "Annual Household Income",
            declaredValue = "₹2,10,000",
            retrievedValue = "₹2,35,000",
            mismatchReason = "Variance within 15% tribal tolerance limit. e-District certificate valid through 2027.",
            status = "PENDING",
            createdAt = now
        )
        db.reviewQueueDao().insert(reviewItem)

        // Update application state
        db.applicationDao().updateStage(
            appId = appId,
            stage = "UNDER_VERIFICATION",
            statusText = "Verification In Progress (Income variance routed to Reviewer Desk)",
            timestamp = now,
            hasDiscrepancy = true,
            pendingAction = "Income discrepancy under manual review by Tribal Welfare Officer. Student action not required."
        )

        db.auditLogDao().insert(
            AuditLogEntity(
                action = "Automated Verification Executed",
                actor = "Ekikrit Verification Engine",
                details = "UIDAI match: VERIFIED. e-District Income: MISMATCH (+11.9%). Non-blocking exception routed to Reviewer Desk.",
                timestamp = now
            )
        )
    }

    suspend fun resolveReviewItem(reviewItemId: String, isApproved: Boolean, notes: String) = withContext(Dispatchers.IO) {
        val item = db.reviewQueueDao().getById(reviewItemId) ?: return@withContext
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

            // Update application: Discrepancy cleared, promoted to SANCTIONED!
            db.applicationDao().updateStage(
                appId = item.applicationId,
                stage = "SANCTIONED",
                statusText = "Sanctioned (MoTA/ST/2026/PMS-8902) — Cleared for DBT Disbursement",
                timestamp = now,
                hasDiscrepancy = false,
                pendingAction = null
            )

            db.auditLogDao().insert(
                AuditLogEntity(
                    action = "Exception Approved by Reviewer",
                    actor = "Tribal Welfare Officer (Desk #4)",
                    details = "Approved income variance for application ${item.applicationId}. Application promoted to SANCTIONED.",
                    timestamp = now
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
                appId = item.applicationId,
                stage = "UNDER_VERIFICATION",
                statusText = "Clarification Requested by Reviewer",
                timestamp = now,
                hasDiscrepancy = true,
                pendingAction = "Please check JAGO assistant or provide updated income declaration."
            )

            db.auditLogDao().insert(
                AuditLogEntity(
                    action = "Clarification Requested",
                    actor = "Tribal Welfare Officer",
                    details = "Requested additional clarification for ${item.studentName}.",
                    timestamp = now
                )
            )
        }
    }

    suspend fun applyForUnreachedScheme(schemeId: String) = withContext(Dispatchers.IO) {
        val scheme = db.schemeDao().getSchemeById(schemeId) ?: return@withContext
        val now = getCurrentTimestamp()

        val existingApp = db.applicationDao().getApplicationBySchemeId(schemeId)
        if (existingApp != null) {
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
            val newApp = ApplicationEntity(
                id = "APP_${scheme.code}_${System.currentTimeMillis()}",
                studentId = "STU_2026_01",
                schemeId = scheme.id,
                schemeCode = scheme.code,
                schemeName = scheme.name,
                currentStage = "UNDER_VERIFICATION",
                statusText = "Applied with 1-Click DigiLocker & APAAR credentials",
                appliedDate = now,
                lastUpdated = now,
                pendingActionDesc = null,
                hasDiscrepancy = false,
                sanctionedAmount = 250000.0,
                estimatedDisbursementDays = 21
            )
            db.applicationDao().insertAll(listOf(newApp))
        }

        db.auditLogDao().insert(
            AuditLogEntity(
                action = "Unreached Beneficiary Applied",
                actor = "Student (Birsa Munda Tirkey)",
                details = "Applied for '${scheme.name}' using existing DigiLocker wallet and APAAR ID credentials without re-uploading documents.",
                timestamp = now
            )
        )
    }

    suspend fun generateJagoResponse(query: String, lang: String): JagoMessage = withContext(Dispatchers.IO) {
        val student = db.studentDao().getStudent()
        val apps = db.applicationDao().getAllApplicationsFlow() // get current snapshot
        val pendingReviews = db.reviewQueueDao().getById("REV_ITEM_01")
        val q = query.trim().lowercase(Locale.ROOT)

        val isOdia = lang == "or"
        val isHindi = lang == "hi"
        val isGondi = lang == "gon"

        val responseText: String
        val quickChips: List<String>

        if (q.contains("pending") || q.contains("status") || q.contains("flag") || q.contains("अधूरा") || q.contains("ସ୍ଥିତି")) {
            val hasPendingReview = pendingReviews?.status == "PENDING"
            responseText = when {
                isHindi -> if (hasPendingReview) {
                    "नमस्ते ${student?.name ?: ""}! आपके 'पोस्ट-मैट्रिक छात्रवृत्ति' आवेदन में ई-डिस्ट्रिक्ट आय प्रमाणपत्र में ₹2,10,000 बनाम ₹2,35,000 का अंतर पाया गया था। अच्छी बात यह है कि आपका आवेदन रुका नहीं है — यह समीक्षा अधिकारी (Reviewer Desk) को गैर-अवरोधक (Non-blocking) अपवाद के रूप में भेजा गया है। अनुमोदन के बाद छात्रवृत्ति स्वीकृत हो जाएगी।"
                } else {
                    "बधाई! आपके सभी आवेदन जांच में सफल रहे हैं। 'पोस्ट-मैट्रिक छात्रवृत्ति' अधिकारी द्वारा अनुमोदित कर दी गई है और डीबीटी भुगतान पाइपलाइन में है!"
                }
                isOdia -> if (hasPendingReview) {
                    "ନମସ୍କାର ${student?.name ?: ""}! ଆପଣଙ୍କର 'ପୋଷ୍ଟ-ମାଟ୍ରିକ ଛାତ୍ରବୃତ୍ତି' ଆବେଦନରେ e-District ଆୟ ତାଲିକାରେ ₹୨,୧୦,୦୦୦ ବନାମ ₹୨,୩୫,୦୦୦ ତାରତମ୍ୟ ଥିଲା। ଏଥିପାଇଁ ଆପଣଙ୍କୁ ବନ୍ଦ କରାଯାଇ ନାହିଁ — ଏହା ଅଧିକାରୀଙ୍କ ସମୀକ୍ଷା (Reviewer Queue) କୁ ପଠାଯାଇଛି। ଖୁବଶୀଘ୍ର ମଞ୍ଜୁର ହୋଇଯିବ।"
                } else {
                    "ଅଭିନନ୍ଦନ! ଆପଣଙ୍କର ସମସ୍ତ ଆବେଦନ ସଫଳତାର ସହ ଯାଞ୍ଚ ହୋଇଛି। DBT ମାଧ୍ୟମରେ ଆପଣଙ୍କ କାନାରା ବ୍ୟାଙ୍କ ଖାତାକୁ ଟଙ୍କା ପ୍ରଦାନ କରାଯିବ।"
                }
                isGondi -> if (hasPendingReview) {
                    "जोहार ${student?.name ?: ""}! नीवा पोस्ट-मैट्रिक स्कॉलरशिप अर्जी ते ई-डिस्ट्रिक्ट आमदनी ते थोड़ो फरक वातुर। अर्जी बंद आयो, अफसर review desk ते जांच कींतोर। जल्दी clear आता।"
                } else {
                    "जोहार! नीवा सप्पो अर्जी मंजूर आतुंग। Canara Bank खाते ते DBT पैसा वायताल!"
                }
                else -> if (hasPendingReview) {
                    "Hello ${student?.name ?: "Student"}! On your 'Post-Matric Scholarship for ST Students', an 11.9% income variance was detected via e-District (Declared ₹2,10,000 vs Registry ₹2,35,000). Crucially, your application is NOT blocked — it was auto-routed to the Reviewer Desk exception queue. Since both figures are below the ₹2.5 Lakh ceiling, approval is anticipated shortly!"
                } else {
                    "Great news! Your applications are progressing smoothly. The income exception on Post-Matric Scholarship has been cleared by the reviewer officer, and sanction order MoTA/ST/2026 is released for DBT disbursement."
                }
            }
            quickChips = listOf("Why was my income flagged?", "When will amount disburse?", "Am I eligible for Top Class?")
        } else if (q.contains("eligible") || q.contains("top class") || q.contains("पात्र") || q.contains("ଯୋଗ୍ୟ")) {
            responseText = when {
                isHindi -> "हाँ! आपके UDISE+ और APAAR (NIT राउरकेला) रिकॉर्ड के आधार पर आप 'शीर्ष श्रेणी शिक्षा छात्रवृत्ति (Top Class Education for ST Students)' के लिए पूर्णतः पात्र हैं। इसमें पूरी ट्यूशन फीस + ₹2,22,000 वार्षिक भत्ता मिलता है। आप डिजीलॉकर के माध्यम से बिना नया फॉर्म भरे तुरंत आवेदन कर सकते हैं!"
                isOdia -> "ହଁ! ଆପଣଙ୍କର UDISE+ ଏବଂ APAAR ରେକର୍ଡ (NIT ରାଉରକେଲା) ଅନୁସାରେ ଆପଣ 'Top Class Education for ST Students' ପାଇଁ ସମ୍ପୂର୍ଣ୍ଣ ଯୋଗ୍ୟ। ଏଥିରେ ସମସ୍ତ ଫିସ୍ ସହ ବାର୍ଷିକ ₹୨,୨୨,୦୦୦ ମିଳିବ। DigiLocker ଦ୍ୱାରା ଆପଣ ତୁରନ୍ତ ୧-କ୍ଲିକ୍ରେ ଆବେଦନ କରିପାରିବେ।"
                isGondi -> "होय! नीवा NIT राउरकेला UDISE+/APAAR दाखिला लेका नीकु 'Top Class Education' स्कॉलरशिप पायसी हकदार आंतोर। पूरी फीस + ₹2,22,000 मियांतोर। DigiLocker ते एक क्लिक ते अर्जी कूट।"
                else -> "Yes! Based on your active UDISE+ and APAAR enrollment at National Institute of Technology, Rourkela (AISHE-U-0355), you are 100% eligible for the 'Top Class Education for ST Students' scheme (Full Tuition + ₹2,22,000/yr allowance). You haven't claimed it yet — tap the banner on your dashboard to apply instantly via DigiLocker!"
            }
            quickChips = listOf("Apply for Top Class Scheme", "What documents are needed?", "Check application status")
        } else if (q.contains("disburs") || q.contains("payment") || q.contains("dbt") || q.contains("भुगतान") || q.contains("ଟଙ୍କା")) {
            responseText = when {
                isHindi -> "आपकी छात्रवृत्ति सीधे आपके आधार-सीडेड बैंक खाते (Canara Bank A/C **4821) में DBT (PFMS) के जरिए भेजी जाती है। आपके पिछले वर्ष की प्री-मैट्रिक छात्रवृत्ति (₹4,500) सफलतापूर्वक जमा हो चुकी है। पोस्ट-मैट्रिक छात्रवृत्ति की ₹78,000 राशि अगले 14 दिनों में अनुमानित है।"
                isOdia -> "ଆପଣଙ୍କ ଛାତ୍ରବୃତ୍ତି ସିଧାସଳଖ ଆଧାର-ଲିଙ୍କ୍ଡ କାନାରା ବ୍ୟାଙ୍କ ଖାତା (A/C **4821) ରେ PFMS-DBT ଦ୍ୱାରା ଜମା ହେବ। ପୂର୍ବ ପ୍ରି-ମାଟ୍ରିକ ଛାତ୍ରବୃତ୍ତି (₹୪,୫୦୦) ଜମା ହୋଇସାରିଛି ଏବଂ ପରବର୍ତ୍ତୀ ₹୭୮,୦୦୦ ଆଗାମୀ ୧୪ ଦିନ ମଧ୍ୟରେ ମିଳିବ।"
                isGondi -> "नीवा स्कॉलरशिप सीधा आधार-सीडेड Canara Bank (A/C **4821) ते PFMS DBT रेल ते वायताल। ₹78,000 पैसा 14 दिवस ते खाते ते जमा आता।"
                else -> "Your scholarships are disbursed directly to your Aadhaar-seeded Canara Bank account (A/C **4821) via the PFMS-DBT rail. Your prior Pre-Matric grant of ₹4,500 was successfully disbursed (UTR: PFMS/DBT/20241220/89127391). The current Post-Matric grant of ₹78,000 is estimated within 14 days following reviewer clearance."
            }
            quickChips = listOf("View DBT Statement", "What is my bank account?", "Is Aadhaar seeded?")
        } else if (q.contains("document") || q.contains("digilocker") || q.contains("कागजात") || q.contains("ଦସ୍ତାବିଜ")) {
            responseText = when {
                isHindi -> "आपके डिजीलॉकर वॉलेट में 5 दस्तावेज सुरक्षित जुड़े हैं: आधार कार्ड, एसटी जाति प्रमाणपत्र (बिरहोर PVTG), आय प्रमाणपत्र, 12वीं की मार्कशीट, और मूल निवास प्रमाणपत्र। एक बार जोड़े गए ये कागजात सभी 5 योजनाओं में दोबारा अपलोड किए बिना उपयोग होते हैं।"
                isOdia -> "ଆପଣଙ୍କ DigiLocker ୱାଲେଟରେ ୫ଟି ପ୍ରମାଣପତ୍ର ସଂଯୁକ୍ତ ଅଛି: ଆଧାର କାର୍ଡ, ଏସଟି ଜାତି ପ୍ରମାଣପତ୍ର (ବିରହୋର PVTG), ଆୟ ପ୍ରମାଣପତ୍ର, ଯୁକ୍ତ ଦୁଇ ମାର୍କସିଟ୍ ଏବଂ ବାସିନ୍ଦା ପ୍ରମାଣପତ୍ର। କୌଣସି ପେପର ପୁନଃ-ଅପଲୋଡ୍ କରିବାକୁ ପଡିବ ନାହିଁ।"
                isGondi -> "नीवा DigiLocker ते 5 कागजात जुड़ल मंतुर: आधार, एसटी जाति (बिरहोर), आमदनी, मार्कशीट, निवास। दुबारा अपलोड कीया जरूरत सिला।"
                else -> "Your DigiLocker wallet currently holds 5 verified digital credentials: UIDAI Aadhaar, ST Caste Certificate (Birhor PVTG), Income Certificate, CBSE Class 12 Marksheet, and Odisha Resident Certificate. Under Ekikrit's single-wallet architecture, these are shared across all 5 schemes without redundant re-uploads."
            }
            quickChips = listOf("View Documents Tab", "Add New Document", "Check validity")
        } else {
            responseText = when {
                isHindi -> "जोहार! मैं जागो (JAGO) — जनजातीय छात्रों के लिए एकीकृत छात्रवृत्ति सहायक हूँ। आप मुझसे अपनी छात्रवृत्ति की स्थिति, अटके हुए कारणों, नई योजनाओं या डिजीलॉकर दस्तावेजों के बारे में पूछ सकते हैं।"
                isOdia -> "ଜୋହାର! ମୁଁ ଜାଗୋ (JAGO) — ଜନଜାତି ଛାତ୍ରଛାତ୍ରୀଙ୍କ ପାଇଁ ଏକୀକୃତ ଛାତ୍ରବୃତ୍ତି ସହାୟକ। ଆପଣଙ୍କ ଆବେଦନ ସ୍ଥିତି, ଯୋଗ୍ୟତା ବା ଦସ୍ତାବିଜ ସମ୍ପର୍କରେ ଯେକୌଣସି ପ୍ରଶ୍ନ ପଚାରନ୍ତୁ।"
                isGondi -> "जोहार! नन्ना जागो (JAGO) — आदिवासी पोरा-पोरियाल साटी स्कॉलरशिप सहायक। अर्जी, पात्रता, कागजात बारे ते केंज।"
                else -> "Johar! I am JAGO — your unified multilingual assistant for Ministry of Tribal Affairs scholarships. How can I help you today? You can check pending items, scheme eligibility, DBT disbursement timelines, or DigiLocker records."
            }
            quickChips = listOf("What's pending on my application?", "Am I eligible for Top Class?", "When will amount disburse?", "Which documents are linked?")
        }

        JagoMessage(
            sender = "JAGO",
            content = responseText,
            timestamp = "Just now",
            quickChips = quickChips
        )
    }
}
