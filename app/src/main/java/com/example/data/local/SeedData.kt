package com.example.data.local

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SeedData {
    private fun getCurrentDate(): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
    }

    suspend fun populateDatabase(db: EkikritDatabase) {
        val now = getCurrentDate()

        // 1. Student Profiles (Diverse Beneficiary Personas)
        val students = getPresetStudents()
        db.studentDao().insertAll(students)

        // 2. The 5 Tribal Scholarship Schemes (Ministry of Tribal Affairs)
        val schemes = listOf(
            SchemeEntity(
                id = "SCH_PMS",
                code = "PMS-ST",
                name = "Post-Matric Scholarship for ST Students",
                ministry = "Ministry of Tribal Affairs",
                portalOrigin = "National Scholarship Portal (NSP)",
                maxAmount = "₹78,000 / year",
                eligibilityRules = "ST students pursuing recognized post-matric or post-secondary courses. Family income < ₹2.50 Lakh/annum.",
                description = "Centrally sponsored scheme providing financial assistance for post-matriculation or post-secondary stages of education."
            ),
            SchemeEntity(
                id = "SCH_PRE",
                code = "PRE-ST",
                name = "Pre-Matric Scholarship for ST Students (Class 9 & 10)",
                ministry = "Ministry of Tribal Affairs",
                portalOrigin = "National Scholarship Portal (NSP)",
                maxAmount = "₹4,500 / year",
                eligibilityRules = "Regular ST students studying in Class IX or X in Govt or recognized schools.",
                description = "Supports ST parents for education of their children studying in Classes IX and X so that dropout rate is minimized."
            ),
            SchemeEntity(
                id = "SCH_NFST",
                code = "NFST",
                name = "National Fellowship for Higher Education of ST Students",
                ministry = "Ministry of Tribal Affairs",
                portalOrigin = "Canara Bank SFMP Portal",
                maxAmount = "₹35,000 / month + Contingency",
                eligibilityRules = "ST candidates qualified UGC-NET / CSIR-NET pursuing full-time M.Phil & Ph.D courses.",
                description = "Provides financial assistance to ST candidates to pursue higher studies leading to M.Phil and Ph.D degrees."
            ),
            SchemeEntity(
                id = "SCH_NOS",
                code = "NOS-ST",
                name = "National Overseas Scholarship for ST Candidates",
                ministry = "Ministry of Tribal Affairs",
                portalOrigin = "Ministry Standalone NOS Portal",
                maxAmount = "USD 15,400 / yr + Tuition",
                eligibilityRules = "ST candidates selected for Masters / Ph.D in Top 500 QS World University Ranked foreign institutes.",
                description = "Facilitates low-income ST students to obtain higher education abroad in accredited universities."
            ),
            SchemeEntity(
                id = "SCH_TOPCLASS",
                code = "TCE-ST",
                name = "Scholarship for Top Class Education for ST Students",
                ministry = "Ministry of Tribal Affairs",
                portalOrigin = "National Scholarship Portal (NSP)",
                maxAmount = "Full Tuition + ₹2,22,000 allowance",
                eligibilityRules = "ST students admitted to IITs, IIMs, NITs, AIIMS, National Law Schools. Family income < ₹6.0 Lakh.",
                description = "Encourages meritorious ST students to pursue studies at premier institutions notified by the Ministry."
            )
        )
        db.schemeDao().insertAll(schemes)

        // 3. Applications for the schemes
        val applications = listOf(
            ApplicationEntity(
                id = "APP_PMS_2026",
                studentId = "STU_2026_01",
                schemeId = "SCH_PMS",
                schemeCode = "PMS-ST",
                schemeName = "Post-Matric Scholarship for ST Students",
                currentStage = "UNDER_VERIFICATION",
                statusText = "Multi-source Verification in Progress (1 Discrepancy under Manual Review)",
                appliedDate = "12 Aug 2026",
                lastUpdated = now,
                pendingActionDesc = "e-District Income variance under Reviewer Desk evaluation. Auto-routing active.",
                hasDiscrepancy = true,
                sanctionedAmount = 78000.0,
                estimatedDisbursementDays = 14
            ),
            ApplicationEntity(
                id = "APP_NFST_2026",
                studentId = "STU_2026_01",
                schemeId = "SCH_NFST",
                schemeCode = "NFST",
                schemeName = "National Fellowship for Higher Education of ST Students",
                currentStage = "SUBMITTED",
                statusText = "Application Submitted on Canara Bank SFMP rail",
                appliedDate = "05 Sep 2026",
                lastUpdated = "05 Sep 2026",
                pendingActionDesc = null,
                hasDiscrepancy = false,
                sanctionedAmount = 420000.0,
                estimatedDisbursementDays = 30
            ),
            ApplicationEntity(
                id = "APP_NOS_2026",
                studentId = "STU_2026_01",
                schemeId = "SCH_NOS",
                schemeCode = "NOS-ST",
                schemeName = "National Overseas Scholarship for ST Candidates",
                currentStage = "SANCTIONED",
                statusText = "Sanction Order Issued (MoTA/NOS/2026/041)",
                appliedDate = "15 Jun 2026",
                lastUpdated = "10 Sep 2026",
                pendingActionDesc = null,
                hasDiscrepancy = false,
                sanctionedAmount = 1250000.0,
                estimatedDisbursementDays = 7
            ),
            ApplicationEntity(
                id = "APP_PRE_2025",
                studentId = "STU_2026_01",
                schemeId = "SCH_PRE",
                schemeCode = "PRE-ST",
                schemeName = "Pre-Matric Scholarship for ST Students",
                currentStage = "DISBURSED",
                statusText = "Disbursed via Aadhaar-linked DBT into Canara Bank A/C",
                appliedDate = "10 Oct 2024",
                lastUpdated = "20 Dec 2024",
                pendingActionDesc = null,
                hasDiscrepancy = false,
                sanctionedAmount = 4500.0,
                estimatedDisbursementDays = 0
            ),
            ApplicationEntity(
                id = "APP_TOPCLASS_UNCLAIMED",
                studentId = "STU_2026_01",
                schemeId = "SCH_TOPCLASS",
                schemeCode = "TCE-ST",
                schemeName = "Scholarship for Top Class Education for ST Students",
                currentStage = "NOT_APPLIED",
                statusText = "Unclaimed Beneficiary Match Detected (UDISE+ & APAAR)",
                appliedDate = "Not Applied",
                lastUpdated = now,
                pendingActionDesc = "Eligible based on NIT Rourkela enrollment. Tap to apply with single-click DigiLocker credentials!",
                hasDiscrepancy = false,
                sanctionedAmount = 250000.0,
                estimatedDisbursementDays = 0
            )
        )
        db.applicationDao().insertAll(applications)

        // 4. DigiLocker Document Wallet
        val documents = listOf(
            DocumentEntity(
                id = "DOC_AADHAAR",
                studentId = "STU_2026_01",
                type = "Aadhaar Card",
                title = "UIDAI Digital Aadhaar Card",
                docNumberMasked = "XXXX-XXXX-8924",
                source = "DigiLocker (UIDAI)",
                verificationStatus = "VERIFIED",
                issuedDate = "14-04-2018",
                issuedBy = "Unique Identification Authority of India (UIDAI)"
            ),
            DocumentEntity(
                id = "DOC_CASTE",
                studentId = "STU_2026_01",
                type = "ST Caste Certificate",
                title = "Scheduled Tribe Certificate (Birhor PVTG)",
                docNumberMasked = "ST/OD/2021/992418",
                source = "DigiLocker (e-Pramaan)",
                verificationStatus = "VERIFIED",
                issuedDate = "10-06-2021",
                issuedBy = "Tehsildar, Bonai, Sundergarh, Odisha"
            ),
            DocumentEntity(
                id = "DOC_INCOME",
                studentId = "STU_2026_01",
                type = "Income Certificate",
                title = "Annual Household Income Certificate",
                docNumberMasked = "INC/OD/2026/00142",
                source = "DigiLocker (e-District)",
                verificationStatus = "NEEDS_ATTENTION",
                issuedDate = "02-04-2026",
                issuedBy = "Revenue & Disaster Mgmt Dept, Odisha"
            ),
            DocumentEntity(
                id = "DOC_MARKSHEET",
                studentId = "STU_2026_01",
                type = "Class 12 Marksheet",
                title = "CBSE Senior Secondary Exam Marksheet",
                docNumberMasked = "CBSE-12-918237",
                source = "DigiLocker (CBSE)",
                verificationStatus = "VERIFIED",
                issuedDate = "22-05-2022",
                issuedBy = "Central Board of Secondary Education"
            ),
            DocumentEntity(
                id = "DOC_DOMICILE",
                studentId = "STU_2026_01",
                type = "Resident / Domicile",
                title = "Permanent Resident Certificate of Odisha",
                docNumberMasked = "DOM/OD/2020/7192",
                source = "DigiLocker (e-District)",
                verificationStatus = "VERIFIED",
                issuedDate = "12-08-2020",
                issuedBy = "Sub-Collector, Panposh, Sundergarh"
            )
        )
        db.documentDao().insertAll(documents)

        // 5. Verification Records for PMS Application
        val verificationRecords = listOf(
            VerificationRecordEntity(
                id = "VER_UIDAI",
                applicationId = "APP_PMS_2026",
                schemeId = "SCH_PMS",
                sourceSystem = "UIDAI (Aadhaar Rail)",
                fieldChecked = "Demographic & Biometric Match",
                declaredValue = "Birsa Munda Tirkey, 15-08-2003",
                retrievedValue = "Birsa Munda Tirkey, 15-08-2003 (Confidence: 98.4%)",
                status = "VERIFIED",
                timestamp = "12 Aug 2026, 10:14 AM",
                notes = "Demographic parameters matched with 98.4% confidence score."
            ),
            VerificationRecordEntity(
                id = "VER_DIGILOCKER",
                applicationId = "APP_PMS_2026",
                schemeId = "SCH_PMS",
                sourceSystem = "DigiLocker Wallet",
                fieldChecked = "ST Caste & PVTG Community Validation",
                declaredValue = "ST (Birhor PVTG)",
                retrievedValue = "ST - Birhor (Cert: ST/OD/2021/992418)",
                status = "VERIFIED",
                timestamp = "12 Aug 2026, 10:14 AM",
                notes = "Cryptographic digital signature verified against Odisha Revenue Dept root CA."
            ),
            VerificationRecordEntity(
                id = "VER_AISHE",
                applicationId = "APP_PMS_2026",
                schemeId = "SCH_PMS",
                sourceSystem = "AISHE / UDISE+",
                fieldChecked = "Institute Recognition & Regular Enrollment",
                declaredValue = "NIT Rourkela, B.Tech CSE",
                retrievedValue = "AISHE Code U-0355 (National Importance Institute)",
                status = "VERIFIED",
                timestamp = "12 Aug 2026, 10:15 AM",
                notes = "Active student enrollment confirmed via Institute AISHE Portal."
            ),
            VerificationRecordEntity(
                id = "VER_APAAR",
                applicationId = "APP_PMS_2026",
                schemeId = "SCH_PMS",
                sourceSystem = "APAAR / EduLocker",
                fieldChecked = "Academic Bank of Credits (ABC ID)",
                declaredValue = "APAAR-8839-4021-9920",
                retrievedValue = "Credits earned: 114 / 160 (CGPA: 8.42)",
                status = "VERIFIED",
                timestamp = "12 Aug 2026, 10:15 AM",
                notes = "Academic continuity and credit progression authenticated."
            ),
            VerificationRecordEntity(
                id = "VER_UGC_NTA",
                applicationId = "APP_PMS_2026",
                schemeId = "SCH_PMS",
                sourceSystem = "UGC / NTA Rail",
                fieldChecked = "National Entrance / Merit Qualification",
                declaredValue = "JEE Main 2022 Roll # 22031048",
                retrievedValue = "Score: 94.8 Percentile, ST Category Rank 842",
                status = "VERIFIED",
                timestamp = "12 Aug 2026, 10:16 AM",
                notes = "Merit credential confirmed by NTA central score repository."
            ),
            VerificationRecordEntity(
                id = "VER_EDISTRICT",
                applicationId = "APP_PMS_2026",
                schemeId = "SCH_PMS",
                sourceSystem = "e-District Odisha",
                fieldChecked = "Annual Household Income Certificate",
                declaredValue = "₹2,10,000 / annum",
                retrievedValue = "₹2,35,000 / annum (Cert INC/OD/2026/00142)",
                status = "MISMATCH",
                timestamp = "12 Aug 2026, 10:16 AM",
                notes = "Variance of 11.9% detected. Routed to manual Reviewer Desk under non-blocking exception workflow (below ₹2.50L ceiling)."
            )
        )
        db.verificationRecordDao().insertAll(verificationRecords)

        // 6. Review Queue Item for the e-District Income Mismatch
        val reviewItem = ReviewQueueEntity(
            id = "REV_ITEM_01",
            verificationRecordId = "VER_EDISTRICT",
            applicationId = "APP_PMS_2026",
            studentName = "Birsa Munda Tirkey",
            category = "ST (PVTG - Birhor)",
            schemeName = "Post-Matric Scholarship for ST Students",
            sourceSystem = "e-District Odisha",
            fieldName = "Annual Household Income",
            declaredValue = "₹2,10,000",
            retrievedValue = "₹2,35,000",
            mismatchReason = "Self-declared income ₹2,10,000 vs e-District record ₹2,35,000 (+11.9%). Both are strictly below scheme threshold of ₹2,50,000. Eligible for exception clearance.",
            status = "PENDING",
            createdAt = "12 Aug 2026, 10:16 AM",
            resolvedAt = null,
            resolutionNotes = null
        )
        db.reviewQueueDao().insert(reviewItem)

        // 7. Disbursements (PFMS DBT Rail)
        val disbursements = listOf(
            DisbursementEntity(
                id = "DISB_PRE_2024",
                applicationId = "APP_PRE_2025",
                schemeName = "Pre-Matric Scholarship for ST Students",
                amount = 4500.0,
                date = "20 Dec 2024",
                txnRef = "PFMS/DBT/20241220/89127391",
                bankName = "Canara Bank",
                accountMasked = "A/C **4821",
                status = "SUCCESS"
            ),
            DisbursementEntity(
                id = "DISB_NOS_ADVANCE",
                applicationId = "APP_NOS_2026",
                schemeName = "National Overseas Scholarship (Travel Grant)",
                amount = 150000.0,
                date = "08 Sep 2026",
                txnRef = "PFMS/DBT/20260908/77239014",
                bankName = "State Bank of India (Forex Hub)",
                accountMasked = "A/C **8901",
                status = "SUCCESS"
            )
        )
        db.disbursementDao().insertAll(disbursements)

        // 8. Audit Trail Logs
        val auditLogs = listOf(
            AuditLogEntity(
                action = "DPDP Consent Registered",
                actor = "Student (Birsa Munda Tirkey)",
                details = "Granted explicit consent for DigiLocker multi-scheme document reuse and automated e-District verification.",
                timestamp = "12 Aug 2026, 10:12 AM"
            ),
            AuditLogEntity(
                action = "Verification Triggered",
                actor = "System (Ekikrit Orchestrator)",
                details = "Executed automated parallel verification across UIDAI, DigiLocker, AISHE, APAAR, UGC-NTA, e-District.",
                timestamp = "12 Aug 2026, 10:14 AM"
            ),
            AuditLogEntity(
                action = "Exception Auto-Routed",
                actor = "System (Rule Engine)",
                details = "e-District Income variance flagged. Route #REV_ITEM_01 queued to Verification Reviewer Desk without student block.",
                timestamp = "12 Aug 2026, 10:16 AM"
            )
        )
        for (log in auditLogs) {
            db.auditLogDao().insert(log)
        }
    }

    fun getPresetStudents(): List<StudentEntity> {
        return listOf(
            StudentEntity(
                id = "STU_2026_01",
                name = "Birsa Munda Tirkey",
                dob = "15-08-2003",
                mobile = "+91 98765 43210",
                state = "Odisha",
                institutionId = "AISHE-U-0355",
                institutionName = "National Institute of Technology, Rourkela",
                course = "B.Tech Computer Science & Engineering (Sem VI)",
                category = "ST (PVTG - Birhor)",
                pvtgCommunity = "Birhor",
                preferredLanguage = "en",
                apaarId = "APAAR-8839-4021-9920",
                annualIncome = 210000.0,
                aadhaarMasked = "XXXX-XXXX-8924",
                bankAccountMasked = "Canara Bank (A/C **4821)",
                ifscCode = "CNRB0002845",
                isDigiLockerLinked = true,
                hasConsentGiven = true
            ),
            StudentEntity(
                id = "STU_2026_02",
                name = "Sunita Soren",
                dob = "04-11-2004",
                mobile = "+91 91234 56789",
                state = "Jharkhand",
                institutionId = "AISHE-U-0582",
                institutionName = "Indian Institute of Technology (IIT) Kharagpur",
                course = "B.Tech Electrical & Electronics (Sem IV)",
                category = "ST (Santhal)",
                pvtgCommunity = null,
                preferredLanguage = "hi",
                apaarId = "APAAR-5512-8830-1120",
                annualIncome = 180000.0,
                aadhaarMasked = "XXXX-XXXX-4512",
                bankAccountMasked = "State Bank of India (A/C **9023)",
                ifscCode = "SBIN0001234",
                isDigiLockerLinked = true,
                hasConsentGiven = true
            ),
            StudentEntity(
                id = "STU_2026_03",
                name = "Mangal Oraon",
                dob = "19-02-2008",
                mobile = "+91 94567 89012",
                state = "Jharkhand",
                institutionId = "UDISE-200109",
                institutionName = "Govt Boys High School, Ranchi",
                course = "Class X (Secondary Education)",
                category = "ST (Oraon)",
                pvtgCommunity = null,
                preferredLanguage = "hi",
                apaarId = "APAAR-9901-3342-8819",
                annualIncome = 95000.0,
                aadhaarMasked = "XXXX-XXXX-6731",
                bankAccountMasked = "Bank of India (A/C **3319)",
                ifscCode = "BKID0004921",
                isDigiLockerLinked = false,
                hasConsentGiven = false
            )
        )
    }

    suspend fun ensurePresetStudents(db: EkikritDatabase) {
        val existing = db.studentDao().getAllStudents()
        if (existing.size < 3) {
            val presets = getPresetStudents()
            for (p in presets) {
                if (existing.none { it.id == p.id }) {
                    db.studentDao().insertStudent(p)
                }
            }
        }
    }
}
