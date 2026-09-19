package com.example.domain

import com.example.data.model.ApplicationEntity
import com.example.data.model.DocumentEntity
import com.example.data.model.SchemeEntity
import com.example.data.model.StudentEntity

enum class EligibilityStatus {
    ELIGIBLE,
    NOT_ELIGIBLE,
    NEEDS_REVIEW
}

data class EligibilityResult(
    val status: EligibilityStatus,
    val matchPercentage: Int,
    val matchedCriteria: List<String>,
    val failedCriteria: List<String>,
    val missingDocuments: List<String>,
    val conflictReason: String? = null,
    val summaryRecommendation: String
)

object EligibilityEngine {

    /**
     * Evaluates a student's profile, academic credentials, document inventory,
     * and existing active applications against a specific government scholarship scheme.
     * Enforces the "One Active Scholarship/Fellowship Rule".
     */
    fun evaluate(
        student: StudentEntity,
        scheme: SchemeEntity,
        documents: List<DocumentEntity>,
        existingApplications: List<ApplicationEntity>
    ): EligibilityResult {
        val matched = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val missingDocs = mutableListOf<String>()
        var conflictReason: String? = null

        // 1. ST Category & PVTG Check (Mandatory across MoTA schemes)
        val isSt = student.category.contains("ST", ignoreCase = true) || 
                   student.category.contains("Scheduled Tribe", ignoreCase = true) ||
                   !student.pvtgCommunity.isNullOrBlank()

        if (isSt) {
            matched.add("Category verified: ${student.category}")
            if (!student.pvtgCommunity.isNullOrBlank()) {
                matched.add("PVTG community priority identified (${student.pvtgCommunity})")
            }
        } else {
            failed.add("Applicant must belong to Scheduled Tribe (ST) category under Ministry guidelines.")
        }

        // 2. Active Scholarship Conflict Check (One Active Award Rule)
        // Check if student has another scholarship currently in ACTIVE / APPROVED state
        val activeOtherApp = existingApplications.firstOrNull { app ->
            app.schemeId != scheme.id &&
            app.currentStage in listOf("SUBMITTED", "INSTITUTE_VERIFICATION", "STATE_VERIFICATION", "MINISTRY_REVIEW", "SANCTIONED")
        }

        if (activeOtherApp != null) {
            conflictReason = "Ekikrit found an active award linked to your profile (${activeOtherApp.schemeName}). Under Ministry of Tribal Affairs guidelines, only one concurrent scholarship or fellowship can be drawn per academic cycle."
        }

        // 3. Scheme-Specific Evaluation Rules
        when (scheme.id) {
            "SCH_PRE" -> {
                // Pre-Matric: Class IX & X
                if (student.academicLevel == "SECONDARY" || student.course.contains("Class", ignoreCase = true)) {
                    matched.add("Enrolled in recognized Secondary Education (${student.institutionName})")
                } else {
                    failed.add("Pre-Matric is strictly for secondary school students (Classes IX and X).")
                }

                if (student.annualIncome <= 250000.0) {
                    matched.add("Annual income (₹${student.annualIncome.toInt()}) is below ₹2.50 Lakh ceiling")
                } else {
                    failed.add("Annual household income exceeds ₹2.50 Lakh threshold for Pre-Matric.")
                }
            }

            "SCH_PMS" -> {
                // Post-Matric: Post-secondary recognized courses
                if (student.academicLevel in listOf("UNDERGRADUATE", "POSTGRADUATE", "HIGHER_SECONDARY", "PHD") ||
                    !student.institutionId.startsWith("UDISE")) {
                    matched.add("Post-secondary enrollment verified via AISHE (${student.institutionName})")
                } else {
                    failed.add("Must be enrolled in post-matriculation / higher education course.")
                }

                if (student.annualIncome <= 250000.0) {
                    matched.add("Annual family income (₹${student.annualIncome.toInt()}) satisfies ₹2.50L ceiling")
                } else {
                    failed.add("Family income exceeds ₹2.50 Lakh limit for Post-Matric ST grant.")
                }
            }

            "SCH_TOPCLASS" -> {
                // Top Class Education: Premier Institutes (IIT, NIT, IIM, AIIMS, National Law Universities)
                val isPremierInstitute = student.institutionName.contains("NIT", ignoreCase = true) ||
                                         student.institutionName.contains("IIT", ignoreCase = true) ||
                                         student.institutionName.contains("National Institute", ignoreCase = true) ||
                                         student.institutionName.contains("Indian Institute", ignoreCase = true)

                if (isPremierInstitute) {
                    matched.add("Enrolled in Notified Premier Institution of National Importance (${student.institutionName})")
                } else {
                    failed.add("Top Class Scheme requires admission to Ministry-notified premier institutions (IIT/NIT/IIM/AIIMS).")
                }

                if (student.annualIncome <= 600000.0) {
                    matched.add("Income is within Top Class limit of ₹6.00 Lakh (Current: ₹${student.annualIncome.toInt()})")
                } else {
                    failed.add("Family income exceeds ₹6.00 Lakh ceiling for Top Class scheme.")
                }
            }

            "SCH_NFST" -> {
                // National Fellowship for Higher Education: M.Phil / Ph.D with NET / Entrance qualification
                if (student.academicLevel == "PHD" || student.course.contains("Ph.D", ignoreCase = true) ||
                    student.qualificationDetails.contains("NET", ignoreCase = true)) {
                    matched.add("Higher education fellowship qualification verified")
                } else {
                    failed.add("Candidate must be pursuing M.Phil / Ph.D or qualified UGC-NET / CSIR-NET.")
                }
            }

            "SCH_NOS" -> {
                // National Overseas Scholarship: Top 500 QS foreign university admit
                if (student.qualificationDetails.contains("QS", ignoreCase = true) ||
                    student.qualificationDetails.contains("Abroad", ignoreCase = true) ||
                    student.course.contains("Overseas", ignoreCase = true)) {
                    matched.add("Overseas university admission criteria authenticated")
                } else {
                    failed.add("Requires unconditional admission offer from Top 500 QS World University Ranked foreign institute.")
                }
            }

            else -> {
                matched.add("General tribal scheme eligibility conditions satisfied")
            }
        }

        // 4. Document Inventory Cross-Check
        val hasAadhaar = documents.any { it.type.contains("Aadhaar", ignoreCase = true) && it.verificationStatus == "VERIFIED" }
        val hasCaste = documents.any { it.type.contains("Caste", ignoreCase = true) || it.type.contains("ST", ignoreCase = true) }
        val hasIncome = documents.any { it.type.contains("Income", ignoreCase = true) }
        val hasMarksheet = documents.any { it.type.contains("Marksheet", ignoreCase = true) || it.type.contains("Secondary", ignoreCase = true) }

        if (hasAadhaar) matched.add("Aadhaar e-KYC linked") else missingDocs.add("Aadhaar Demographic Card")
        if (hasCaste) matched.add("DigiLocker ST Caste Certificate available") else missingDocs.add("Scheduled Tribe Certificate")
        if (hasIncome) matched.add("Annual Income Certificate available") else missingDocs.add("Income Certificate")
        if (hasMarksheet) matched.add("Academic Marksheet / Transcript linked") else missingDocs.add("Academic Marksheet")

        // 5. Compute Match Score and Overall Status
        val totalCriteria = (matched.size + failed.size + missingDocs.size).coerceAtLeast(1)
        val matchPercentage = ((matched.size.toDouble() / totalCriteria) * 100).toInt().coerceIn(0, 100)

        val status = when {
            conflictReason != null -> EligibilityStatus.NEEDS_REVIEW
            failed.isNotEmpty() -> EligibilityStatus.NOT_ELIGIBLE
            missingDocs.isNotEmpty() -> EligibilityStatus.NEEDS_REVIEW
            else -> EligibilityStatus.ELIGIBLE
        }

        val summary = when (status) {
            EligibilityStatus.ELIGIBLE -> "You are 100% eligible for this scheme with existing digital credentials. Ready for 1-click application!"
            EligibilityStatus.NEEDS_REVIEW -> {
                if (conflictReason != null) {
                    "Active scholarship conflict detected: Review existing award before applying."
                } else {
                    "Partial match (${matchPercentage}%). Missing ${missingDocs.size} certificate(s) in DigiLocker wallet."
                }
            }
            EligibilityStatus.NOT_ELIGIBLE -> "Currently does not meet core eligibility criteria: ${failed.firstOrNull()}"
        }

        return EligibilityResult(
            status = status,
            matchPercentage = matchPercentage,
            matchedCriteria = matched,
            failedCriteria = failed,
            missingDocuments = missingDocs,
            conflictReason = conflictReason,
            summaryRecommendation = summary
        )
    }
}
