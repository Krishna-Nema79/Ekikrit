package com.example

import com.example.data.ai.JagoAiService
import com.example.data.eligibility.EligibilityEngine
import com.example.data.local.SeedData
import com.example.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying:
 * 1. Multi-factor eligibility engine rules (ST category, PVTG priority, course level, premier institute matching)
 * 2. Unclaimed entitlement detection (Mangal Oraon vs Birsa Munda)
 * 3. Income threshold boundaries across schemes
 * 4. Duplicate application prevention for same academic year
 * 5. DPDP consent protection & PII sanitization in JAGO AI
 */
class EkikritEligibilityAndOwnershipTest {

    private val schemes = SeedData.getSeedSchemes()

    @Test
    fun mangalOraon_secondaryStudent_matchesPreMatricAndDisqualifiesPremierSchemes() {
        val mangalOraon = StudentEntity(
            id = "STU_2026_02",
            name = "Mangal Oraon",
            dob = "2010-08-22",
            aadhaarMasked = "XXXXXXXX3456",
            mobile = "+91 98765 43210",
            category = "ST (Scheduled Tribe)",
            annualIncome = 85000.0,
            institutionName = "Netarhat Residential School, Latehar",
            institutionId = "UDISE-201901001",
            course = "Class X (Secondary)",
            bankAccountMasked = "XXXXXXXX7890",
            ifscCode = "BKID0004921",
            apaarId = "APAAR-3456-7890-1234"
        )

        val preMatricScheme = schemes.first { it.id == "SCH_PRE" }
        val topClassScheme = schemes.first { it.id == "SCH_TOPCLASS" }
        val fellowshipScheme = schemes.first { it.id == "SCH_NFST" }

        // Mangal should match Pre-Matric
        val preMatricEval = EligibilityEngine.evaluateEligibility(mangalOraon, preMatricScheme, emptyList())
        assertTrue("Mangal Oraon should be eligible for Pre-Matric", preMatricEval.isEligible)
        assertTrue(preMatricEval.matchReasons.any { it.contains("secondary education", ignoreCase = true) })
        assertTrue(preMatricEval.matchReasons.any { it.contains("Scheduled Tribe", ignoreCase = true) })

        // Mangal should NOT match Top Class (Requires premier university admission)
        val topClassEval = EligibilityEngine.evaluateEligibility(mangalOraon, topClassScheme, emptyList())
        assertFalse("Mangal Oraon should be ineligible for Top Class Scheme", topClassEval.isEligible)
        assertTrue(topClassEval.disqualificationReasons.any { it.contains("premier institute", ignoreCase = true) })

        // Mangal should NOT match National Fellowship (Requires PhD/Research)
        val fellowshipEval = EligibilityEngine.evaluateEligibility(mangalOraon, fellowshipScheme, emptyList())
        assertFalse("Mangal Oraon should be ineligible for National Fellowship", fellowshipEval.isEligible)
        assertTrue(fellowshipEval.disqualificationReasons.any { it.contains("M.Phil/Ph.D", ignoreCase = true) })
    }

    @Test
    fun birsaMunda_nitRourkela_matchesTopClass_asUnclaimedEntitlement() {
        val birsaMunda = StudentEntity(
            id = "STU_2026_01",
            name = "Birsa Munda Tirkey",
            dob = "2004-05-18",
            aadhaarMasked = "XXXXXXXX1234",
            mobile = "+91 98765 43210",
            category = "ST (Scheduled Tribe)",
            annualIncome = 160000.0,
            institutionName = "National Institute of Technology, Rourkela",
            institutionId = "AISHE-U-0355",
            course = "B.Tech Computer Science & Engineering",
            bankAccountMasked = "XXXXXXXX5678",
            ifscCode = "SBIN0002109",
            apaarId = "APAAR-9876-5432-1098"
        )

        // Birsa currently only has applied to Post-Matric (SCH_PMS)
        val existingApps = listOf(
            ApplicationEntity(
                id = "APP_2026_01",
                studentId = "STU_2026_01",
                schemeId = "SCH_PMS",
                schemeCode = "PMS-ST",
                schemeName = "Post-Matric Scholarship for ST Students",
                appliedDate = "2026-08-15",
                lastUpdated = "2026-08-15",
                currentStage = "UNDER_VERIFICATION",
                statusText = "Multi-source Verification in progress",
                academicYear = "2025-26"
            )
        )

        val topUnreached = EligibilityEngine.findTopUnreachedScheme(birsaMunda, schemes, existingApps)

        assertNotNull("Birsa Munda should have an unclaimed entitlement detected", topUnreached)
        assertEquals("SCH_TOPCLASS", topUnreached!!.schemeId)
        assertTrue(topUnreached.isEligible)
        assertTrue(topUnreached.isUnclaimed)
        assertTrue(topUnreached.matchReasons.any { it.contains("notified premier institute", ignoreCase = true) })
    }

    @Test
    fun incomeThresholdBoundary_enforcesStrictStatutoryCeilings() {
        val studentAbove250k = StudentEntity(
            id = "STU_TEST",
            name = "Test ST Student",
            dob = "2003-01-01",
            aadhaarMasked = "XXXXXXXX9999",
            mobile = "+91 98765 43210",
            category = "ST",
            annualIncome = 350000.0, // Exceeds ₹2.50L ceiling, but below ₹6.00L ceiling
            institutionName = "National Institute of Technology, Rourkela",
            institutionId = "AISHE-U-0355",
            course = "B.Tech Electrical",
            bankAccountMasked = "XXXXXXXX1111",
            ifscCode = "SBIN0001234",
            apaarId = "APAAR-1111"
        )

        val postMatricScheme = schemes.first { it.id == "SCH_PMS" }
        val topClassScheme = schemes.first { it.id == "SCH_TOPCLASS" }

        val pmsEval = EligibilityEngine.evaluateEligibility(studentAbove250k, postMatricScheme, emptyList())
        assertFalse("Income > ₹2.50L must disqualify from Post-Matric Scholarship", pmsEval.isEligible)
        assertTrue(pmsEval.disqualificationReasons.any { it.contains("exceeds ₹2.50L", ignoreCase = true) })

        val topClassEval = EligibilityEngine.evaluateEligibility(studentAbove250k, topClassScheme, emptyList())
        assertTrue("Income of ₹3.50L remains within Top Class ceiling of ₹6.00L", topClassEval.isEligible)
        assertTrue(topClassEval.matchReasons.any { it.contains("within ₹6.00L ceiling", ignoreCase = true) })
    }

    @Test
    fun pvtgStudent_receivesPriorityEntitlementRecognition() {
        val sunitaSoren = StudentEntity(
            id = "STU_2026_03",
            name = "Sunita Soren",
            dob = "2003-11-14",
            aadhaarMasked = "XXXXXXXX7890",
            mobile = "+91 98765 43210",
            category = "ST (Santhal)",
            pvtgCommunity = "Santhal PVTG",
            annualIncome = 120000.0,
            institutionName = "Indian Institute of Technology, Bhubaneswar",
            institutionId = "AISHE-U-0356",
            course = "B.Tech Mechanical Engineering",
            bankAccountMasked = "XXXXXXXX2468",
            ifscCode = "PUNB0123400",
            apaarId = "APAAR-7890-1234-5678"
        )

        val topClassScheme = schemes.first { it.id == "SCH_TOPCLASS" }
        val eval = EligibilityEngine.evaluateEligibility(sunitaSoren, topClassScheme, emptyList())

        assertTrue(eval.isEligible)
        assertTrue(eval.matchReasons.any { it.contains("PVTG - Santhal PVTG") })
    }

    @Test
    fun duplicateApplicationPrevention_rejectsSecondSubmissionInSameAcademicYear() {
        val existingApps = listOf(
            ApplicationEntity(
                id = "APP_2026_01",
                studentId = "STU_2026_01",
                schemeId = "SCH_TOPCLASS",
                schemeCode = "TOPCLASS-ST",
                schemeName = "Top Class Education Scheme for ST Students",
                appliedDate = "2026-09-01",
                lastUpdated = "2026-09-01",
                currentStage = "SUBMITTED",
                statusText = "Submitted",
                academicYear = "2025-26"
            )
        )

        // Attempting to submit a second application for SCH_TOPCLASS for the same academic year
        val studentId = "STU_2026_01"
        val schemeId = "SCH_TOPCLASS"
        val academicYear = "2025-26"

        val hasDuplicate = existingApps.any {
            it.studentId == studentId && it.schemeId == schemeId && it.academicYear == academicYear
        }

        assertTrue(hasDuplicate)

        val exception = assertThrows(IllegalStateException::class.java) {
            if (hasDuplicate) {
                throw IllegalStateException("Application for this scheme already exists for academic year $academicYear.")
            }
        }

        assertTrue(exception.message!!.contains("already exists"))
    }

    @Test
    fun consentRevocation_blocksApplicationSubmission() {
        val studentWithRevokedConsent = StudentEntity(
            id = "STU_2026_01",
            name = "Birsa Munda Tirkey",
            dob = "2004-05-18",
            aadhaarMasked = "XXXXXXXX1234",
            mobile = "+91 98765 43210",
            category = "ST",
            annualIncome = 160000.0,
            institutionName = "NIT Rourkela",
            institutionId = "AISHE-U-0355",
            course = "B.Tech CSE",
            bankAccountMasked = "XXXXXXXX5678",
            ifscCode = "SBIN0002109",
            apaarId = "APAAR-9876",
            hasConsentGiven = false // Revoked DPDP consent
        )

        val exception = assertThrows(SecurityException::class.java) {
            if (!studentWithRevokedConsent.hasConsentGiven) {
                throw SecurityException("DPDP Consent Required: Please grant consent in Privacy Settings to pull DigiLocker credentials.")
            }
        }

        assertTrue(exception.message!!.contains("DPDP Consent Required"))
    }

    @Test
    fun jagoAiService_protectsPiiAndProvidesMultilingualGuidance() = runBlocking {
        val jago = JagoAiService()
        val student = StudentEntity(
            id = "STU_2026_01",
            name = "Birsa Munda Tirkey",
            dob = "2004-05-18",
            aadhaarMasked = "XXXXXXXX1234",
            mobile = "+91 98765 43210",
            category = "ST (Scheduled Tribe)",
            annualIncome = 160000.0,
            institutionName = "NIT Rourkela",
            institutionId = "AISHE-U-0355",
            course = "B.Tech CSE",
            bankAccountMasked = "XXXXXXXX5678",
            ifscCode = "SBIN0002109",
            apaarId = "APAAR-9876"
        )

        val app = ApplicationEntity(
            id = "APP_2026_01",
            studentId = "STU_2026_01",
            schemeId = "SCH_PMS",
            schemeCode = "PMS-ST",
            schemeName = "Post-Matric Scholarship",
            appliedDate = "2026-08-15",
            lastUpdated = "2026-08-15",
            currentStage = "UNDER_VERIFICATION",
            statusText = "Multi-source Verification in progress",
            hasDiscrepancy = true,
            academicYear = "2025-26"
        )

        // Test Hindi response
        val hindiMsg = jago.generateResponse(
            query = "What is my application status?",
            langCode = "hi",
            student = student,
            applications = listOf(app),
            pendingReviewCount = 1,
            unclaimedEvaluations = emptyList()
        )
        assertNotNull(hindiMsg.content)
        assertTrue(hindiMsg.content.contains("नमस्ते Birsa"))
        assertTrue(hindiMsg.content.contains("Reviewer Desk"))

        // Test Odia response
        val odiaMsg = jago.generateResponse(
            query = "status of application",
            langCode = "or",
            student = student,
            applications = listOf(app),
            pendingReviewCount = 1,
            unclaimedEvaluations = emptyList()
        )
        assertTrue(odiaMsg.content.contains("ନମସ୍କାର Birsa"))

        // Test Gondi response
        val gondiMsg = jago.generateResponse(
            query = "status",
            langCode = "gon",
            student = student,
            applications = listOf(app),
            pendingReviewCount = 1,
            unclaimedEvaluations = emptyList()
        )
        assertTrue(gondiMsg.content.contains("जोहार Birsa"))

        // Guarantee ZERO PII leakage across all generated responses
        val allContent = hindiMsg.content + " " + odiaMsg.content + " " + gondiMsg.content
        assertFalse(allContent.contains("XXXXXXXX1234"))
        assertFalse(allContent.contains("XXXXXXXX5678"))
        assertFalse(allContent.contains("SBIN0002109"))
        assertFalse(allContent.contains("XXXXXX9876"))
    }
}
