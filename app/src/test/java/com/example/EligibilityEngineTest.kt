package com.example

import com.example.data.local.SeedData
import com.example.data.model.ApplicationEntity
import com.example.data.model.StudentEntity
import com.example.domain.EligibilityEngine
import com.example.domain.EligibilityStatus
import org.junit.Assert.*
import org.junit.Test

class EligibilityEngineTest {

    private val birsaMunda = SeedData.students[0]
    private val sunitaSoren = SeedData.students[1]
    private val mangalOraon = SeedData.students[2]

    private val preMatricScheme = SeedData.schemes.first { it.id == "SCH_PRE" }
    private val postMatricScheme = SeedData.schemes.first { it.id == "SCH_PMS" }
    private val topClassScheme = SeedData.schemes.first { it.id == "SCH_TOPCLASS" }

    @Test
    fun testBirsaMundaEligibleForTopClass() {
        val docs = SeedData.documents.filter { it.studentId == birsaMunda.id }
        val result = EligibilityEngine.evaluate(
            student = birsaMunda,
            scheme = topClassScheme,
            documents = docs,
            existingApplications = emptyList()
        )

        assertEquals(EligibilityStatus.ELIGIBLE, result.status)
        assertTrue(result.matchPercentage >= 90)
        assertTrue(result.matchedCriteria.any { it.contains("PVTG") })
        assertTrue(result.matchedCriteria.any { it.contains("Premier Institution") || it.contains("NIT") })
    }

    @Test
    fun testSecondaryStudentEligibleForPreMatric() {
        val docs = SeedData.documents.filter { it.studentId == mangalOraon.id }
        val result = EligibilityEngine.evaluate(
            student = mangalOraon,
            scheme = preMatricScheme,
            documents = docs,
            existingApplications = emptyList()
        )

        assertEquals(EligibilityStatus.ELIGIBLE, result.status)
        assertTrue(result.matchedCriteria.any { it.contains("Secondary Education") })
    }

    @Test
    fun testActiveAwardConflictRule() {
        val docs = SeedData.documents.filter { it.studentId == birsaMunda.id }
        val activeApp = ApplicationEntity(
            id = "APP_ACTIVE_001",
            studentId = birsaMunda.id,
            schemeId = "SCH_PMS",
            schemeCode = "POST-MATRIC-ST",
            schemeName = "Post-Matric Scholarship Scheme for ST Students",
            currentStage = "INSTITUTE_VERIFICATION",
            statusText = "Under review",
            appliedDate = "01 Sep 2026",
            lastUpdated = "01 Sep 2026",
            sanctionedAmount = 28000.0
        )

        val result = EligibilityEngine.evaluate(
            student = birsaMunda,
            scheme = topClassScheme,
            documents = docs,
            existingApplications = listOf(activeApp)
        )

        // Enforces One Active Award Conflict detection
        assertEquals(EligibilityStatus.NEEDS_REVIEW, result.status)
        assertNotNull(result.conflictReason)
        assertTrue(result.conflictReason!!.contains("active award"))
    }
}
