package com.example.data.local

import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SeedData {

    // 1. Five Core MoTA Schemes (Smart India Hackathon SIH26238 Mandate)
    val schemes = listOf(
        SchemeEntity(
            id = "SCH_PRE",
            code = "PRE-MATRIC-ST",
            name = "Pre-Matric Scholarship for ST Students (Class IX & X)",
            ministry = "Ministry of Tribal Affairs",
            portalOrigin = "NSP & State Tribal Portal",
            maxAmount = "₹4,500 / year",
            eligibilityRules = "Class IX or X regular students belonging to ST community; Family annual income ≤ ₹2.50 Lakh.",
            description = "Centrally sponsored financial assistance to curb dropout rates at secondary transition stage in tribal areas.",
            deadline = "15 Nov 2026",
            targetLevel = "SECONDARY",
            incomeCeiling = 250000.0
        ),
        SchemeEntity(
            id = "SCH_PMS",
            code = "POST-MATRIC-ST",
            name = "Post-Matric Scholarship Scheme for ST Students",
            ministry = "Ministry of Tribal Affairs",
            portalOrigin = "Unified State / NSP Portal",
            maxAmount = "₹28,000 / year",
            eligibilityRules = "Post-secondary recognized degree/diploma courses; Family income ≤ ₹2.50 Lakh; Mandatory Aadhaar-linked bank account.",
            description = "Comprehensive tuition and non-refundable maintenance allowance for ST youth pursuing higher education across India.",
            deadline = "30 Nov 2026",
            targetLevel = "POST_MATRIC",
            incomeCeiling = 250000.0
        ),
        SchemeEntity(
            id = "SCH_TOPCLASS",
            code = "TOP-CLASS-ST",
            name = "National Fellowship and Scholarship for Higher Education (Top Class ST)",
            ministry = "Ministry of Tribal Affairs",
            portalOrigin = "MoTA Direct Portal",
            maxAmount = "Full Tuition + ₹86,000 allowance",
            eligibilityRules = "Secured admission in premier institutions (IITs, NITs, IIMs, AIIMS, NLUs, etc.); Family annual income ≤ ₹6.00 Lakh.",
            description = "100% tuition coverage, computer allowance, living stipend, and book grant for ST students in premier national institutions.",
            deadline = "31 Dec 2026",
            targetLevel = "PREMIER_INSTITUTE",
            incomeCeiling = 600000.0
        ),
        SchemeEntity(
            id = "SCH_NFST",
            code = "NFST-FELLOWSHIP",
            name = "National Fellowship for ST Students (M.Phil / Ph.D)",
            ministry = "Ministry of Tribal Affairs",
            portalOrigin = "MoTA NFST Portal",
            maxAmount = "₹35,000 / month + HRA",
            eligibilityRules = "Enrolled in regular full-time M.Phil/Ph.D program in recognized Universities; Qualified UGC-NET / CSIR-NET / GATE.",
            description = "Junior Research Fellowship (JRF) and Senior Research Fellowship (SRF) promoting tribal doctoral research scholars.",
            deadline = "15 Jan 2027",
            targetLevel = "HIGHER_EDUCATION",
            incomeCeiling = 9999999.0
        ),
        SchemeEntity(
            id = "SCH_NOS",
            code = "NOS-TRIBAL",
            name = "National Overseas Scholarship for ST Candidates",
            ministry = "Ministry of Tribal Affairs",
            portalOrigin = "Overseas Portal (MoTA)",
            maxAmount = "$40,000 / year + Tuition",
            eligibilityRules = "Unconditional offer from Top 500 QS World University Ranked overseas institutions for Masters/Ph.D; Family income ≤ ₹8.00 Lakh.",
            description = "Prestigious international scholarship supporting tribal scholars pursuing advanced degrees in leading global universities.",
            deadline = "28 Feb 2027",
            targetLevel = "OVERSEAS",
            incomeCeiling = 800000.0
        )
    )

    // 2. Preset Personas for Multi-User Demos
    val students = listOf(
        // Persona 1: Birsa Munda Tirkey (Primary SIH Persona - PVTG Birhor at NIT Rourkela)
        StudentEntity(
            id = "STU_2026_01",
            name = "Birsa Munda Tirkey",
            dob = "15-08-2003",
            mobile = "+91 98765 43210",
            state = "Odisha",
            institutionId = "AISHE-U-0355",
            institutionName = "National Institute of Technology, Rourkela",
            course = "B.Tech Computer Science & Engineering",
            academicLevel = "UNDERGRADUATE",
            category = "ST (PVTG - Birhor)",
            pvtgCommunity = "Birhor",
            preferredLanguage = "en",
            apaarId = "APAAR-8839-4021-9920",
            annualIncome = 210000.0,
            aadhaarMasked = "XXXX-XXXX-8924",
            bankAccountMasked = "Canara Bank (A/C **4821)",
            ifscCode = "CNRB0002845",
            isDigiLockerLinked = true,
            hasConsentGiven = true,
            qualificationDetails = "JEE Main 94.8 Percentile, ST Category Rank 842"
        ),
        // Persona 2: Sunita Soren (IIT Kharagpur - Top Class Scheme Sanctioned)
        StudentEntity(
            id = "STU_2026_02",
            name = "Sunita Soren",
            dob = "22-11-2002",
            mobile = "+91 91234 56789",
            state = "Jharkhand",
            institutionId = "AISHE-U-0570",
            institutionName = "Indian Institute of Technology, Kharagpur",
            course = "B.Tech Metallurgical Engineering",
            academicLevel = "UNDERGRADUATE",
            category = "ST (Santhal)",
            pvtgCommunity = null,
            preferredLanguage = "hi",
            apaarId = "APAAR-7712-3901-8451",
            annualIncome = 320000.0,
            aadhaarMasked = "XXXX-XXXX-6192",
            bankAccountMasked = "State Bank of India (A/C **1904)",
            ifscCode = "SBIN0000202",
            isDigiLockerLinked = true,
            hasConsentGiven = true,
            qualificationDetails = "JEE Advanced ST Rank 114"
        ),
        // Persona 3: Mangal Oraon (Secondary Student in Tribal School)
        StudentEntity(
            id = "STU_2026_03",
            name = "Mangal Oraon",
            dob = "04-03-2009",
            mobile = "+91 94370 11223",
            state = "Odisha",
            institutionId = "UDISE-21051200101",
            institutionName = "Eklavya Model Residential School, Sundargarh",
            course = "Secondary Education (Class X)",
            academicLevel = "SECONDARY",
            category = "ST (Oraon)",
            pvtgCommunity = null,
            preferredLanguage = "or",
            apaarId = "APAAR-1192-4820-3301",
            annualIncome = 120000.0,
            aadhaarMasked = "XXXX-XXXX-3341",
            bankAccountMasked = "Punjab National Bank (A/C **7721)",
            ifscCode = "PUNB0021300",
            isDigiLockerLinked = true,
            hasConsentGiven = true,
            qualificationDetails = "Class IX Annual Score 88.4%"
        )
    )

    // 3. Applications for Birsa Munda Tirkey (STU_2026_01)
    val applicationsStu1 = listOf(
        ApplicationEntity(
            id = "APP_PMS_2026_001",
            studentId = "STU_2026_01",
            schemeId = "SCH_PMS",
            schemeCode = "POST-MATRIC-ST",
            schemeName = "Post-Matric Scholarship Scheme for ST Students",
            currentStage = "INSTITUTE_VERIFICATION",
            statusText = "Multi-source API verification completed (6/7 verified). 1 minor income variance auto-routed to District Officer Desk.",
            appliedDate = "02 Sep 2026",
            lastUpdated = "18 Sep 2026, 02:40 PM",
            pendingActionDesc = "Officer Review Desk #4 is reviewing income certificate variance against e-District (+11.9% tolerance).",
            hasDiscrepancy = true,
            sanctionedAmount = 28000.0,
            estimatedDisbursementDays = 14,
            syncState = "SYNCED"
        ),
        ApplicationEntity(
            id = "APP_PRE_2024_982",
            studentId = "STU_2026_01",
            schemeId = "SCH_PRE",
            schemeCode = "PRE-MATRIC-ST",
            schemeName = "Pre-Matric Scholarship for ST Students (Class IX & X)",
            currentStage = "DISBURSED",
            statusText = "Grant disbursed successfully via Aadhaar Payment Bridge (PFMS DBT Rail).",
            appliedDate = "10 Aug 2024",
            lastUpdated = "15 Oct 2024, 11:30 AM",
            pendingActionDesc = null,
            hasDiscrepancy = false,
            sanctionedAmount = 4500.0,
            estimatedDisbursementDays = 0,
            syncState = "SYNCED"
        )
    )

    // Applications for Sunita Soren (STU_2026_02)
    val applicationsStu2 = listOf(
        ApplicationEntity(
            id = "APP_TOPCLASS_2026_810",
            studentId = "STU_2026_02",
            schemeId = "SCH_TOPCLASS",
            schemeCode = "TOP-CLASS-ST",
            schemeName = "National Fellowship and Scholarship for Higher Education (Top Class ST)",
            currentStage = "SANCTIONED",
            statusText = "Sanction Order MoTA/2026/09 issued. PFMS payment batch queued for Aadhaar-seeded bank credit.",
            appliedDate = "14 Aug 2026",
            lastUpdated = "17 Sep 2026, 10:15 AM",
            pendingActionDesc = null,
            hasDiscrepancy = false,
            sanctionedAmount = 142000.0,
            estimatedDisbursementDays = 5,
            syncState = "SYNCED"
        )
    )

    // Applications for Mangal Oraon (STU_2026_03)
    val applicationsStu3 = listOf(
        ApplicationEntity(
            id = "APP_PRE_2026_331",
            studentId = "STU_2026_03",
            schemeId = "SCH_PRE",
            schemeCode = "PRE-MATRIC-ST",
            schemeName = "Pre-Matric Scholarship for ST Students (Class IX & X)",
            currentStage = "SUBMITTED",
            statusText = "Application submitted via Eklavya UDISE+ school node. Awaiting district verification clearance.",
            appliedDate = "12 Sep 2026",
            lastUpdated = "12 Sep 2026, 04:00 PM",
            pendingActionDesc = null,
            hasDiscrepancy = false,
            sanctionedAmount = 4500.0,
            estimatedDisbursementDays = 21,
            syncState = "SYNCED"
        )
    )

    // 4. DigiLocker Attached Documents for Students
    val documents = listOf(
        DocumentEntity(
            id = "DOC_01",
            studentId = "STU_2026_01",
            type = "Aadhaar",
            title = "Aadhaar Demographic e-Card",
            docNumberMasked = "XXXX-XXXX-8924",
            source = "UIDAI DigiLocker Vault",
            verificationStatus = "VERIFIED",
            issuedDate = "15 Jan 2020",
            issuedBy = "Unique Identification Authority of India",
            isReusable = true
        ),
        DocumentEntity(
            id = "DOC_02",
            studentId = "STU_2026_01",
            type = "Caste",
            title = "Scheduled Tribe Certificate (PVTG Birhor)",
            docNumberMasked = "ST/OD/2021/992418",
            source = "e-Pramaan Odisha / DigiLocker",
            verificationStatus = "VERIFIED",
            issuedDate = "10 May 2021",
            issuedBy = "Tahasildar, Bonai, Sundargarh",
            isReusable = true
        ),
        DocumentEntity(
            id = "DOC_03",
            studentId = "STU_2026_01",
            type = "Income",
            title = "Annual Household Income Certificate",
            docNumberMasked = "INC/OD/2026/00142",
            source = "e-District Revenue Portal",
            verificationStatus = "NEEDS_ATTENTION",
            issuedDate = "05 Apr 2026",
            issuedBy = "Revenue Officer, Sundargarh",
            isReusable = true
        ),
        DocumentEntity(
            id = "DOC_04",
            studentId = "STU_2026_01",
            type = "Marksheet",
            title = "Class XII Science Passing Certificate",
            docNumberMasked = "CBSE/2021/882190",
            source = "CBSE DigiLocker Depository",
            verificationStatus = "VERIFIED",
            issuedDate = "30 Jul 2021",
            issuedBy = "Central Board of Secondary Education",
            isReusable = true
        ),
        DocumentEntity(
            id = "DOC_05",
            studentId = "STU_2026_01",
            type = "APAAR",
            title = "APAAR Academic Bank of Credits ID",
            docNumberMasked = "APAAR-8839-4021-9920",
            source = "Ministry of Education ABC",
            verificationStatus = "VERIFIED",
            issuedDate = "12 Oct 2023",
            issuedBy = "National Academic Depository (NAD)",
            isReusable = true
        ),
        // Documents for Sunita Soren
        DocumentEntity(
            id = "DOC_06",
            studentId = "STU_2026_02",
            type = "Caste",
            title = "Scheduled Tribe Certificate (Santhal)",
            docNumberMasked = "ST/JH/2020/41029",
            source = "e-JharSewa / DigiLocker",
            verificationStatus = "VERIFIED",
            issuedDate = "18 Sep 2020",
            issuedBy = "Sub-Divisional Officer, Dumka",
            isReusable = true
        ),
        DocumentEntity(
            id = "DOC_07",
            studentId = "STU_2026_02",
            type = "Aadhaar",
            title = "Aadhaar Card",
            docNumberMasked = "XXXX-XXXX-6192",
            source = "UIDAI DigiLocker Vault",
            verificationStatus = "VERIFIED",
            issuedDate = "10 Jan 2019",
            issuedBy = "UIDAI",
            isReusable = true
        ),
        DocumentEntity(
            id = "DOC_07_INC",
            studentId = "STU_2026_02",
            type = "Income",
            title = "Income Certificate",
            docNumberMasked = "INC/JH/2026/8819",
            source = "e-JharSewa",
            verificationStatus = "VERIFIED",
            issuedDate = "12 Apr 2026",
            issuedBy = "Circle Officer, Dumka",
            isReusable = true
        ),
        DocumentEntity(
            id = "DOC_07_MRK",
            studentId = "STU_2026_02",
            type = "Marksheet",
            title = "Class XII Marksheet",
            docNumberMasked = "JAC/2020/77192",
            source = "JAC DigiLocker Vault",
            verificationStatus = "VERIFIED",
            issuedDate = "15 Jul 2020",
            issuedBy = "Jharkhand Academic Council",
            isReusable = true
        ),
        // Documents for Mangal Oraon
        DocumentEntity(
            id = "DOC_08",
            studentId = "STU_2026_03",
            type = "Caste",
            title = "ST Certificate (Oraon)",
            docNumberMasked = "ST/OD/2023/11094",
            source = "e-Pramaan Odisha",
            verificationStatus = "VERIFIED",
            issuedDate = "22 Jun 2023",
            issuedBy = "Tahasildar, Sundargarh",
            isReusable = true
        ),
        DocumentEntity(
            id = "DOC_08_AAD",
            studentId = "STU_2026_03",
            type = "Aadhaar",
            title = "Aadhaar e-Card",
            docNumberMasked = "XXXX-XXXX-3341",
            source = "UIDAI Vault",
            verificationStatus = "VERIFIED",
            issuedDate = "14 Mar 2020",
            issuedBy = "UIDAI",
            isReusable = true
        ),
        DocumentEntity(
            id = "DOC_08_INC",
            studentId = "STU_2026_03",
            type = "Income",
            title = "Annual Family Income Certificate",
            docNumberMasked = "INC/OD/2026/44021",
            source = "e-District Odisha",
            verificationStatus = "VERIFIED",
            issuedDate = "02 May 2026",
            issuedBy = "Revenue Inspector, Sundargarh",
            isReusable = true
        ),
        DocumentEntity(
            id = "DOC_08_MRK",
            studentId = "STU_2026_03",
            type = "Marksheet",
            title = "Class IX Annual Exam Report Card",
            docNumberMasked = "EMRS/2026/IX-441",
            source = "EMRS School Records",
            verificationStatus = "VERIFIED",
            issuedDate = "31 Mar 2026",
            issuedBy = "Principal, Eklavya Model School",
            isReusable = true
        )
    )

    // 5. Verification Records for APP_PMS_2026_001
    val verificationRecords = listOf(
        VerificationRecordEntity(
            id = "VER_UIDAI_01",
            applicationId = "APP_PMS_2026_001",
            schemeId = "SCH_PMS",
            sourceSystem = "UIDAI (Aadhaar Rail)",
            fieldChecked = "Demographic & Biometric Authentication",
            declaredValue = "Birsa Munda Tirkey, DOB: 15-08-2003",
            retrievedValue = "Birsa Munda Tirkey, DOB: 15-08-2003 (Match: 99.2%)",
            status = "VERIFIED",
            timestamp = "18 Sep 2026, 02:30 PM",
            notes = "Demographic verification passed with Aadhaar OTP authentication."
        ),
        VerificationRecordEntity(
            id = "VER_DIGILOCKER_02",
            applicationId = "APP_PMS_2026_001",
            schemeId = "SCH_PMS",
            sourceSystem = "DigiLocker Wallet",
            fieldChecked = "ST Caste & PVTG Community Validation",
            declaredValue = "ST (PVTG - Birhor)",
            retrievedValue = "ST (Birhor) • Cert ST/OD/2021/992418",
            status = "VERIFIED",
            timestamp = "18 Sep 2026, 02:31 PM",
            notes = "Cryptographic digital signature verified against State e-Pramaan root authority."
        ),
        VerificationRecordEntity(
            id = "VER_APAAR_03",
            applicationId = "APP_PMS_2026_001",
            schemeId = "SCH_PMS",
            sourceSystem = "APAAR / EduLocker",
            fieldChecked = "Academic Bank of Credits (ABC Progression)",
            declaredValue = "APAAR-8839-4021-9920",
            retrievedValue = "APAAR-8839-4021-9920 (B.Tech 3rd Year Active)",
            status = "VERIFIED",
            timestamp = "18 Sep 2026, 02:31 PM",
            notes = "Student progression validated through National Academic Depository."
        ),
        VerificationRecordEntity(
            id = "VER_AISHE_04",
            applicationId = "APP_PMS_2026_001",
            schemeId = "SCH_PMS",
            sourceSystem = "AISHE Portal",
            fieldChecked = "Institute Recognition & Regular Enrollment",
            declaredValue = "NIT Rourkela (B.Tech CSE)",
            retrievedValue = "Code: AISHE-U-0355 (National Institute of Technology)",
            status = "VERIFIED",
            timestamp = "18 Sep 2026, 02:32 PM",
            notes = "Active full-time enrollment verified in Higher Education Survey database."
        ),
        VerificationRecordEntity(
            id = "VER_UDISE_05",
            applicationId = "APP_PMS_2026_001",
            schemeId = "SCH_PMS",
            sourceSystem = "UDISE+ School Registry",
            fieldChecked = "Secondary School Prior Completion History",
            declaredValue = "Class X / XII Prior Board Record",
            retrievedValue = "Central Board Record Validated (Pass Status: FIRST_CLASS)",
            status = "VERIFIED",
            timestamp = "18 Sep 2026, 02:32 PM",
            notes = "Prior schooling baseline authenticated via UDISE+ database."
        ),
        VerificationRecordEntity(
            id = "VER_NTA_06",
            applicationId = "APP_PMS_2026_001",
            schemeId = "SCH_PMS",
            sourceSystem = "UGC / NTA Rail",
            fieldChecked = "National Merit & Qualification Credential",
            declaredValue = "JEE Main 94.8%ile (ST Rank 842)",
            retrievedValue = "Score: 94.8 Percentile • Central Merit Verified",
            status = "VERIFIED",
            timestamp = "18 Sep 2026, 02:33 PM",
            notes = "National Testing Agency rank card authenticated."
        ),
        VerificationRecordEntity(
            id = "VER_EDIST_07",
            applicationId = "APP_PMS_2026_001",
            schemeId = "SCH_PMS",
            sourceSystem = "e-District Revenue Portal",
            fieldChecked = "Annual Household Income Certificate",
            declaredValue = "₹2,10,000 / annum",
            retrievedValue = "₹2,35,000 / annum (Cert INC/OD/2026/00142)",
            status = "MISMATCH",
            timestamp = "18 Sep 2026, 02:35 PM",
            notes = "Variance of +11.9% detected. Auto-routed to manual Reviewer Desk under non-blocking exception workflow (below ₹2.50L ceiling).",
            isBlocking = false
        )
    )

    // 6. Review Queue Item
    val reviewQueueItems = listOf(
        ReviewQueueEntity(
            id = "REV_001",
            verificationRecordId = "VER_EDIST_07",
            applicationId = "APP_PMS_2026_001",
            studentId = "STU_2026_01",
            studentName = "Birsa Munda Tirkey",
            category = "ST (PVTG - Birhor)",
            schemeName = "Post-Matric Scholarship Scheme for ST Students",
            sourceSystem = "e-District Revenue Portal",
            fieldName = "Annual Household Income Certificate",
            declaredValue = "₹2,10,000 / annum",
            retrievedValue = "₹2,35,000 / annum (INC/OD/2026/00142)",
            mismatchReason = "Self-declared income ₹2,10,000 vs e-District registry ₹2,35,000 (+11.9% variance). Both figures are strictly below the ₹2.50 Lakh scheme ceiling. Auto-flagged for officer discretion without blocking the student.",
            status = "PENDING",
            createdAt = "18 Sep 2026, 02:35 PM"
        )
    )

    // 7. Disbursements
    val disbursements = listOf(
        DisbursementEntity(
            id = "TXN_PFMS_99214",
            studentId = "STU_2026_01",
            applicationId = "APP_PRE_2024_982",
            schemeName = "Pre-Matric Scholarship for ST Students (Class IX & X)",
            amount = 4500.0,
            date = "15 Oct 2024",
            txnRef = "PFMS/APBS/2024/0091823",
            bankName = "Canara Bank",
            accountMasked = "A/C **4821",
            status = "SUCCESS"
        )
    )

    // 8. Notifications
    val notifications = listOf(
        NotificationEntity(
            id = "NOTIF_01",
            studentId = "STU_2026_01",
            title = "Non-Blocking Income Check in Review",
            message = "Your Post-Matric application passed 6/7 automated verifications. A minor income variance (+11.9%) is being reviewed by Officer Desk #4. No action required.",
            type = "VERIFICATION",
            timestamp = "18 Sep 2026, 02:35 PM",
            isRead = false
        ),
        NotificationEntity(
            id = "NOTIF_02",
            studentId = "STU_2026_01",
            title = "New Unreached Scheme Matched!",
            message = "As a PVTG ST student at NIT Rourkela, you are 100% eligible for the Top Class ST Scholarship (Full Tuition + ₹86,000 allowance). Apply in 1 click!",
            type = "SCHEME",
            timestamp = "18 Sep 2026, 09:00 AM",
            isRead = false
        ),
        NotificationEntity(
            id = "NOTIF_03",
            studentId = "STU_2026_01",
            title = "Aadhaar e-KYC Seeded",
            message = "UIDAI and DigiLocker single-wallet synchronization confirmed. All 5 schemes now share your authentic credentials.",
            type = "STATUS",
            timestamp = "15 Sep 2026, 10:00 AM",
            isRead = true
        )
    )

    suspend fun populateInitialDatabase(database: EkikritDatabase) = withContext(Dispatchers.IO) {
        database.studentDao().insertAll(students)
        database.schemeDao().insertAll(schemes)
        database.applicationDao().insertAll(applicationsStu1 + applicationsStu2 + applicationsStu3)
        database.documentDao().insertAll(documents)
        database.verificationRecordDao().insertAll(verificationRecords)
        database.reviewQueueDao().insertAll(reviewQueueItems)
        database.disbursementDao().insertAll(disbursements)
        database.notificationDao().insertAll(notifications)

        database.auditLogDao().insert(
            AuditLogEntity(
                action = "SYSTEM_INITIALIZE",
                actor = "Ekikrit Framework",
                details = "Deterministic Smart India Hackathon database initialized with 3 student personas and 5 MoTA schemes.",
                timestamp = "18 Sep 2026, 02:30 PM"
            )
        )
    }

    suspend fun resetDemo(database: EkikritDatabase) = withContext(Dispatchers.IO) {
        database.studentDao().deleteAll()
        database.schemeDao().deleteAll()
        database.applicationDao().deleteAll()
        database.documentDao().deleteAll()
        database.verificationRecordDao().deleteAll()
        database.reviewQueueDao().deleteAll()
        database.disbursementDao().deleteAll()
        database.notificationDao().deleteAll()
        database.auditLogDao().deleteAll()

        populateInitialDatabase(database)
    }
}
