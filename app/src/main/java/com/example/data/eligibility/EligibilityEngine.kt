package com.example.data.eligibility

import com.example.data.model.ApplicationEntity
import com.example.data.model.SchemeEntity
import com.example.data.model.StudentEntity

data class EligibilityEvaluation(
    val schemeId: String,
    val schemeCode: String,
    val schemeName: String,
    val isEligible: Boolean,
    val matchReasons: List<String>,
    val disqualificationReasons: List<String>,
    val estimatedGrant: String,
    val isUnclaimed: Boolean,
    val isDemoRule: Boolean = true
)

/**
 * Evaluator for student scholarship eligibility and unreached beneficiary matching.
 *
 * Considers available beneficiary profile attributes:
 * - Category/caste (ST / PVTG validation)
 * - Household annual income thresholds
 * - Current course / educational level (Secondary vs Post-Secondary vs Premier Higher Ed)
 * - Enrolled institution (Premier institutes like IIT/NIT/IIM vs High School vs University)
 * - Existing applications (detects unclaimed entitlements)
 *
 * NOTE: Clearly distinguished as a local demo rule engine for prototype demonstration,
 * not official government determination.
 */
object EligibilityEngine {

    fun evaluateEligibility(
        student: StudentEntity?,
        scheme: SchemeEntity,
        existingApplications: List<ApplicationEntity>
    ): EligibilityEvaluation {
        if (student == null) {
            return EligibilityEvaluation(
                schemeId = scheme.id,
                schemeCode = scheme.code,
                schemeName = scheme.name,
                isEligible = false,
                matchReasons = emptyList(),
                disqualificationReasons = listOf("No active student profile selected."),
                estimatedGrant = scheme.maxAmount,
                isUnclaimed = false
            )
        }

        val matchReasons = mutableListOf<String>()
        val disqualifications = mutableListOf<String>()

        // 1. Beneficiary Category Check (Schemes are for ST beneficiaries)
        val isST = student.category.contains("ST", ignoreCase = true) ||
                   student.category.contains("Scheduled Tribe", ignoreCase = true) ||
                   student.pvtgCommunity.isNotBlank()

        if (isST) {
            val label = if (student.pvtgCommunity.isNotBlank()) "ST (PVTG - ${student.pvtgCommunity})" else student.category
            matchReasons.add("Beneficiary verified as Scheduled Tribe ($label)")
        } else if (student.category.isBlank()) {
            disqualifications.add("Category not specified in profile. Complete profile verification.")
        } else {
            disqualifications.add("Scheme is reserved for ST beneficiaries (Current category: ${student.category})")
        }

        // 2. Scheme-Specific Multi-Factor Checks
        when (scheme.id) {
            "SCH_PRE" -> {
                // Pre-Matric Scholarship for ST Students (Class 9 & 10)
                val isSecondary = student.course.contains("Class IX", ignoreCase = true) ||
                                  student.course.contains("Class 9", ignoreCase = true) ||
                                  student.course.contains("Class X", ignoreCase = true) ||
                                  student.course.contains("Class 10", ignoreCase = true) ||
                                  student.course.contains("Secondary", ignoreCase = true) ||
                                  student.institutionId.startsWith("UDISE")

                if (isSecondary) {
                    matchReasons.add("Enrolled in recognized secondary education (${student.course.ifBlank { "Secondary School" }})")
                } else {
                    disqualifications.add("Requires regular enrollment in Class IX or X (Current: ${student.course.ifBlank { "Higher Education" }})")
                }

                if (student.annualIncome in 1.0..250000.0) {
                    matchReasons.add("Family income within ₹2.50L threshold (Declared: ₹${student.annualIncome.toInt()})")
                } else if (student.annualIncome > 250000.0) {
                    disqualifications.add("Family income exceeds ₹2.50L statutory ceiling (Declared: ₹${student.annualIncome.toInt()})")
                } else if (student.annualIncome == 0.0) {
                    disqualifications.add("Annual income certificate required.")
                }
            }

            "SCH_PMS" -> {
                // Post-Matric Scholarship for ST Students
                val isSecondary = student.course.contains("Class IX", ignoreCase = true) ||
                                  student.course.contains("Class X", ignoreCase = true)

                if (!isSecondary && (student.course.isNotBlank() || student.institutionName.isNotBlank())) {
                    matchReasons.add("Enrolled in post-matric / post-secondary course (${student.course.ifBlank { "Higher Education" }})")
                } else {
                    disqualifications.add("Requires post-matriculation stage of education.")
                }

                if (student.annualIncome in 1.0..250000.0) {
                    matchReasons.add("Family income within ₹2.50L threshold (Declared: ₹${student.annualIncome.toInt()})")
                } else if (student.annualIncome > 250000.0) {
                    disqualifications.add("Family income exceeds ₹2.50L ceiling (Declared: ₹${student.annualIncome.toInt()})")
                } else if (student.annualIncome == 0.0) {
                    disqualifications.add("Annual income certificate required.")
                }
            }

            "SCH_TOPCLASS" -> {
                // Scholarship for Top Class Education for ST Students
                val premierKeywords = listOf("IIT", "NIT", "IIM", "AIIMS", "National Institute", "Indian Institute")
                val isPremierInstitute = premierKeywords.any { student.institutionName.contains(it, ignoreCase = true) }

                if (isPremierInstitute) {
                    matchReasons.add("Enrolled in notified premier institute (${student.institutionName})")
                } else {
                    disqualifications.add("Requires admission to notified premier institute (IIT/NIT/IIM/AIIMS) (Current: ${student.institutionName.ifBlank { "Non-premier" }})")
                }

                if (student.annualIncome in 1.0..600000.0) {
                    matchReasons.add("Family income within ₹6.00L ceiling (Declared: ₹${student.annualIncome.toInt()})")
                } else if (student.annualIncome > 600000.0) {
                    disqualifications.add("Family income exceeds ₹6.00L ceiling (Declared: ₹${student.annualIncome.toInt()})")
                } else if (student.annualIncome == 0.0) {
                    disqualifications.add("Annual income certificate required.")
                }
            }

            "SCH_NFST" -> {
                // National Fellowship for Higher Education of ST Students (M.Phil / Ph.D)
                val isResearch = student.course.contains("Ph.D", ignoreCase = true) ||
                                 student.course.contains("M.Phil", ignoreCase = true) ||
                                 student.course.contains("Research", ignoreCase = true)
                if (isResearch) {
                    matchReasons.add("Enrolled in full-time M.Phil / Ph.D research program")
                } else {
                    disqualifications.add("Requires qualification in UGC-NET and full-time M.Phil/Ph.D enrollment.")
                }
            }

            "SCH_NOS" -> {
                // National Overseas Scholarship
                val isOverseas = student.institutionName.contains("Foreign", ignoreCase = true) ||
                                 student.institutionName.contains("Abroad", ignoreCase = true)
                if (isOverseas) {
                    matchReasons.add("Selected for accredited overseas institution")
                } else {
                    disqualifications.add("Requires selection in Top 500 QS World University Ranked foreign institute.")
                }
            }
        }

        val isEligible = isST && disqualifications.isEmpty()
        val hasActiveApp = existingApplications.any {
            it.schemeId == scheme.id && it.currentStage !in listOf("NOT_APPLIED", "REJECTED")
        }
        val isUnclaimed = isEligible && !hasActiveApp

        return EligibilityEvaluation(
            schemeId = scheme.id,
            schemeCode = scheme.code,
            schemeName = scheme.name,
            isEligible = isEligible,
            matchReasons = matchReasons,
            disqualificationReasons = disqualifications,
            estimatedGrant = scheme.maxAmount,
            isUnclaimed = isUnclaimed
        )
    }

    /**
     * Finds the highest priority unclaimed scholarship match for the student.
     */
    fun findTopUnreachedScheme(
        student: StudentEntity?,
        schemes: List<SchemeEntity>,
        existingApplications: List<ApplicationEntity>
    ): EligibilityEvaluation? {
        if (student == null || student.role != "STUDENT") return null
        return schemes
            .map { evaluateEligibility(student, it, existingApplications) }
            .firstOrNull { it.isUnclaimed }
    }
}
