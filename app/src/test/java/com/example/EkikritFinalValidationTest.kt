package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EkikritDatabase
import com.example.data.local.SeedData
import com.example.data.model.ApplicationDraftEntity
import com.example.data.model.ApplicationEntity
import com.example.data.repository.EkikritRepository
import com.example.domain.EligibilityEngine
import com.example.domain.EligibilityStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EkikritFinalValidationTest {

    private lateinit var db: EkikritDatabase
    private lateinit var repository: EkikritRepository

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, EkikritDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        SeedData.resetDemo(db)
        repository = EkikritRepository(db, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testEligibilityEnforcementBlocksIneligibleAndConflicts() = runBlocking {
        // Student 3 (Mangal Oraon, secondary school student) trying to apply for Top Class (Higher Ed)
        val student3 = SeedData.students[2]
        repository.switchStudent(student3.id)

        val (success, msg) = repository.applyForScheme("SCH_TOPCLASS")
        assertFalse(success)
        assertTrue(msg.contains("eligible", ignoreCase = true))

        // Student 1 (Birsa Munda) trying to apply for Top Class when already having an active PMS award
        val student1 = SeedData.students[0]
        repository.switchStudent(student1.id)

        // Birsa already has an active Post-Matric (SCH_PMS) application
        val (topSuccess, topMsg) = repository.applyForScheme("SCH_TOPCLASS")
        assertFalse(topSuccess)
        assertTrue(topMsg.contains("active scholarship", ignoreCase = true) || topMsg.contains("Conflict", ignoreCase = true))
    }

    @Test
    fun testScholarshipConflictLogicIgnoresCompletedHistoricalAwards() = runBlocking {
        val student1 = SeedData.students[0]
        val topClassScheme = SeedData.schemes.first { it.id == "SCH_TOPCLASS" }
        val docs = SeedData.documents.filter { it.studentId == student1.id }

        // Historical application that has been fully disbursed/completed
        val historicalApp = ApplicationEntity(
            id = "APP_HISTORICAL_001",
            studentId = student1.id,
            schemeId = "SCH_PRE",
            schemeCode = "PRE-MATRIC-ST",
            schemeName = "Pre-Matric Scholarship Scheme for ST Students",
            currentStage = "DISBURSED",
            statusText = "DBT Disbursed",
            appliedDate = "01 Jan 2025",
            lastUpdated = "01 Jan 2025"
        )

        val eval = EligibilityEngine.evaluate(
            student = student1,
            scheme = topClassScheme,
            documents = docs,
            existingApplications = listOf(historicalApp)
        )

        assertEquals(EligibilityStatus.ELIGIBLE, eval.status)
        assertNull(eval.conflictReason)
    }

    @Test
    fun testOfflineDraftStudentIsolation() = runBlocking {
        val studentA = SeedData.students[0] // STU_2026_01
        val studentB = SeedData.students[1] // STU_2026_02

        // Save draft for Student A
        db.applicationDraftDao().insert(
            ApplicationDraftEntity(
                id = "DRAFT_TEST_001",
                studentId = studentA.id,
                schemeId = "SCH_PRE",
                declaredIncome = 180000.0,
                lastSavedTimestamp = "2026-09-19 10:00:00",
                isPendingSync = true
            )
        )

        // Switch active student to Student B
        repository.switchStudent(studentB.id)

        // Trigger sync of pending drafts
        repository.syncPendingDrafts()

        // Verify Student A received the application, NOT Student B
        val studentAApps = db.applicationDao().getApplicationsForStudent(studentA.id)
        val studentBApps = db.applicationDao().getApplicationsForStudent(studentB.id)

        assertTrue(studentAApps.any { it.schemeId == "SCH_PRE" })
        assertFalse(studentBApps.any { it.schemeId == "SCH_PRE" })
    }

    @Test
    fun testVerificationStateTransitionsAndOfficerResolution() = runBlocking {
        val student1 = SeedData.students[0]
        repository.switchStudent(student1.id)

        val app = db.applicationDao().getApplicationsForStudent(student1.id).first()

        // Execute verification across 7 national registries
        repository.runSevenSourceVerification(app.id)

        val updatedApp = db.applicationDao().getApplicationById(app.id)
        assertNotNull(updatedApp)
        assertTrue(updatedApp!!.hasDiscrepancy)
        assertEquals("INSTITUTE_VERIFICATION", updatedApp.currentStage)

        // Officer resolves review item
        val reviewItem = db.reviewQueueDao().getByAppId(app.id)
        assertNotNull(reviewItem)

        repository.resolveReviewItem(reviewItem!!.id, isApproved = true, notes = "Verified within tolerance")

        val clearedApp = db.applicationDao().getApplicationById(app.id)
        assertNotNull(clearedApp)
        assertFalse(clearedApp!!.hasDiscrepancy)
        assertEquals("STATE_VERIFICATION", clearedApp.currentStage)
    }

    @Test
    fun testJagoSourceOfTruthReadsActualDatabaseRecords() = runBlocking {
        val student1 = SeedData.students[0]
        repository.switchStudent(student1.id)

        val statusResponse = repository.generateJagoResponse("What is my application status?")
        assertTrue(statusResponse.contains("Top Class") || statusResponse.contains("INSTITUTE") || statusResponse.contains("Stage"))

        val paymentResponse = repository.generateJagoResponse("When will my payment disburse?")
        assertTrue(paymentResponse.contains("DBT") || paymentResponse.contains("Aadhaar Rail"))
    }

    @Test
    fun testJagoOfflineFallbackIndicator() = runBlocking {
        repository.setOfflineMode(true)
        val response = repository.generateJagoResponse("Check my eligibility")
        assertTrue(response.contains("Offline Assistance Mode Active"))
    }
}
