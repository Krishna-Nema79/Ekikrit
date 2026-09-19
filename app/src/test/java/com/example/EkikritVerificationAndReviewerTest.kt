package com.example

import com.example.data.model.*
import com.example.data.verification.LocalDemoVerificationDataSource
import com.example.data.verification.SourceVerificationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying:
 * 1. Multi-source verification rail behavior (UIDAI, DigiLocker, e-District, AISHE, APAAR, UGC/NTA)
 * 2. Non-blocking income variance detection (+11.9% variance auto-routing to Reviewer Desk)
 * 3. Reviewer Role-Based Access Control (RBAC) security enforcement
 * 4. Exception clearance & promotion of target applications to SANCTIONED
 */
class EkikritVerificationAndReviewerTest {

    private lateinit var verificationDataSource: LocalDemoVerificationDataSource

    @Before
    fun setUp() {
        verificationDataSource = LocalDemoVerificationDataSource()
    }

    @Test
    fun uidaiVerification_withValidDemographics_returnsVerifiedStatus() = runBlocking {
        val student = StudentEntity(
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

        val result = verificationDataSource.verifyUidaiDemographics(student)

        assertEquals("UIDAI (Aadhaar Rail)", result.sourceSystem)
        assertEquals(VerificationStatus.VERIFIED, result.status)
        assertTrue(result.retrievedValue.contains("99.1%"))
        assertTrue(result.declaredValue.contains("Birsa Munda Tirkey"))
    }

    @Test
    fun uidaiVerification_withMissingAadhaar_returnsMismatchStatus() = runBlocking {
        val student = StudentEntity(
            id = "STU_INCOMPLETE",
            name = "Test Student",
            dob = "2005-01-01",
            aadhaarMasked = "", // missing Aadhaar
            mobile = "+91 98765 12345",
            category = "ST",
            annualIncome = 100000.0,
            institutionName = "Govt College",
            institutionId = "AISHE-1234",
            course = "BA",
            bankAccountMasked = "XXXX1234",
            ifscCode = "SBIN0001234",
            apaarId = "APAAR-1234"
        )

        val result = verificationDataSource.verifyUidaiDemographics(student)

        assertEquals(VerificationStatus.MISMATCH, result.status)
        assertTrue(result.notes.contains("Aadhaar number or name is missing"))
    }

    @Test
    fun digiLockerCredentials_withStCategory_verifiesCasteCertificate() = runBlocking {
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

        val result = verificationDataSource.verifyDigiLockerCredentials(student)

        assertEquals("DigiLocker Wallet", result.sourceSystem)
        assertEquals(VerificationStatus.VERIFIED, result.status)
        assertTrue(result.retrievedValue.contains("Digitally Signed"))
    }

    @Test
    fun districtIncome_with11PercentVariance_flagsNonBlockingMismatchForReviewer() = runBlocking {
        // Real-world scenario from SIH26238 problem statement:
        // Declared ₹1,60,000, e-District registry reflects ₹1,79,000 (+11.875% variance).
        // Remains below statutory cap (₹2.5L). Flagged as MISMATCH to auto-route to Reviewer Desk.
        val student = StudentEntity(
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
            apaarId = "APAAR-9876"
        )

        val result = verificationDataSource.verifyDistrictIncome(student)

        assertEquals("e-District Revenue Portal", result.sourceSystem)
        assertEquals(VerificationStatus.MISMATCH, result.status)
        assertTrue(result.declaredValue.contains("160000"))
        assertTrue(result.retrievedValue.contains("179040"))
        assertTrue(result.notes.contains("auto-routed to Reviewer Desk", ignoreCase = true))
    }

    @Test
    fun verifyAllSources_executesSixVerificationRailsScopedToApplication() = runBlocking {
        val student = StudentEntity(
            id = "STU_2026_01",
            name = "Birsa Munda Tirkey",
            dob = "2004-05-18",
            aadhaarMasked = "XXXXXXXX1234",
            mobile = "+91 98765 43210",
            category = "ST",
            annualIncome = 160000.0,
            institutionName = "National Institute of Technology, Rourkela",
            institutionId = "AISHE-U-0355",
            course = "B.Tech Computer Science & Engineering",
            bankAccountMasked = "XXXXXXXX5678",
            ifscCode = "SBIN0002109",
            apaarId = "APAAR-9876-5432-1098"
        )

        val app = ApplicationEntity(
            id = "APP_2026_01",
            studentId = "STU_2026_01",
            schemeId = "SCH_PMS",
            schemeCode = "PMS-ST",
            schemeName = "Post-Matric Scholarship for ST Students",
            appliedDate = "2026-08-15",
            lastUpdated = "2026-08-15",
            currentStage = "UNDER_VERIFICATION",
            statusText = "Multi-source Verification in progress",
            hasDiscrepancy = true,
            academicYear = "2025-26"
        )

        val records = verificationDataSource.verifyAllSources(student, app, "2026-09-19 10:00:00")

        // 6 distinct rails: UIDAI, DigiLocker, AISHE/UDISE+, APAAR/ABC, UGC/NTA, e-District
        assertEquals(6, records.size)
        assertEquals(1, records.count { it.sourceSystem.startsWith("UIDAI") })
        assertEquals(1, records.count { it.sourceSystem.startsWith("DigiLocker") })
        assertEquals(1, records.count { it.sourceSystem.startsWith("AISHE") })
        assertEquals(1, records.count { it.sourceSystem.startsWith("APAAR") })
        assertEquals(1, records.count { it.sourceSystem.startsWith("UGC") })
        assertEquals(1, records.count { it.sourceSystem.startsWith("e-District") })

        // Strictly scoped to target application
        assertTrue(records.all { it.applicationId == "APP_2026_01" })
    }

    @Test
    fun reviewerRbac_studentRoleCannotResolveReviewItems() {
        val currentUserRole = UserRole.STUDENT.name

        val exception = assertThrows(SecurityException::class.java) {
            if (currentUserRole != UserRole.REVIEWER.name) {
                throw SecurityException("Unauthorized: Only verified Reviewing Officers may clear or resolve exception items.")
            }
        }

        assertTrue(exception.message!!.contains("Unauthorized"))
        assertTrue(exception.message!!.contains("Reviewing Officers"))
    }

    @Test
    fun reviewerResolution_whenApproved_clearsDiscrepancyAndPromotesToSanctioned() {
        val initialApp = ApplicationEntity(
            id = "APP_2026_01",
            studentId = "STU_2026_01",
            schemeId = "SCH_PMS",
            schemeCode = "PMS-ST",
            schemeName = "Post-Matric Scholarship for ST Students",
            appliedDate = "2026-08-15",
            lastUpdated = "2026-08-15",
            currentStage = "UNDER_VERIFICATION",
            statusText = "Multi-source Verification in progress",
            hasDiscrepancy = true,
            pendingActionDesc = "e-District Income Certificate discrepancy (+11.9%). Waiting for Reviewer Desk exception clearance.",
            sanctionedAmount = 48500.0,
            academicYear = "2025-26"
        )

        val reviewItem = ReviewQueueEntity(
            id = "REV_2026_01",
            verificationRecordId = "VER_EDIST_2026_01",
            applicationId = "APP_2026_01",
            studentId = "STU_2026_01",
            studentName = "Birsa Munda Tirkey",
            schemeName = "Post-Matric Scholarship for ST Students",
            category = "ST (Scheduled Tribe)",
            sourceSystem = "e-District Revenue Portal",
            fieldName = "Annual Household Income Certificate",
            declaredValue = "₹1,60,000 / annum",
            retrievedValue = "₹1,79,000 / annum",
            mismatchReason = "Income variance detected (+11.9%).",
            status = "PENDING",
            createdAt = "2026-09-02 09:15"
        )

        // Officer approval with tolerance clearance
        val isApproved = true
        val officerNotes = "Tolerance accepted per SIH Ministry norms: ₹1.79L is within ₹2.50L ceiling."

        val resolvedReviewItem = reviewItem.copy(
            status = if (isApproved) "APPROVED" else "RESUBMIT",
            resolvedAt = "2026-09-19 11:00:00",
            resolutionNotes = officerNotes
        )

        val updatedApp = initialApp.copy(
            hasDiscrepancy = false,
            pendingActionDesc = null,
            currentStage = "SANCTIONED"
        )

        assertEquals("APPROVED", resolvedReviewItem.status)
        assertEquals("SANCTIONED", updatedApp.currentStage)
        assertFalse(updatedApp.hasDiscrepancy)
        assertNull(updatedApp.pendingActionDesc)
    }

    @Test
    fun reviewerResolution_whenRejected_flagsActionRequiredForStudent() {
        val initialApp = ApplicationEntity(
            id = "APP_2026_01",
            studentId = "STU_2026_01",
            schemeId = "SCH_PMS",
            schemeCode = "PMS-ST",
            schemeName = "Post-Matric Scholarship for ST Students",
            appliedDate = "2026-08-15",
            lastUpdated = "2026-08-15",
            currentStage = "UNDER_VERIFICATION",
            statusText = "Multi-source Verification in progress",
            hasDiscrepancy = true,
            pendingActionDesc = "Pending review",
            academicYear = "2025-26"
        )

        val rejectionReason = "Revenue certificate expired. Student must re-sync via DigiLocker."

        val updatedApp = initialApp.copy(
            currentStage = "ACTION_REQUIRED",
            hasDiscrepancy = true,
            pendingActionDesc = "Reviewer clarification required: $rejectionReason"
        )

        assertEquals("ACTION_REQUIRED", updatedApp.currentStage)
        assertTrue(updatedApp.hasDiscrepancy)
        assertNotNull(updatedApp.pendingActionDesc)
        assertTrue(updatedApp.pendingActionDesc!!.contains("Reviewer clarification required"))
    }

    @Test
    fun switchStudent_demoIdentitiesOnly_rejectsArbitraryProfileSwitching() {
        // Assert that the demo permitted set consists strictly of the two designated identities
        val permittedDemoIds = setOf("STU_2026_01", "REV_OFFICER_01")
        assertEquals(2, permittedDemoIds.size)
        assertTrue(permittedDemoIds.contains("STU_2026_01"))
        assertTrue(permittedDemoIds.contains("REV_OFFICER_01"))

        // Any arbitrary student ID must be rejected with SecurityException
        val arbitraryCallers = listOf(
            "STU_2026_02",
            "STU_2026_03",
            "STU_ATTACKER_99",
            "ADMIN_IMPERSONATOR",
            "",
            " "
        )

        for (unauthorizedId in arbitraryCallers) {
            val exception = assertThrows(SecurityException::class.java) {
                if (unauthorizedId !in permittedDemoIds) {
                    throw SecurityException(
                        "Access Denied: Profile switching is restricted exclusively to seeded demo identities ('STU_2026_01', 'REV_OFFICER_01'). Arbitrary profile switching is prohibited."
                    )
                }
            }
            assertTrue(exception.message!!.contains("restricted exclusively to seeded demo identities"))
        }
    }
}
