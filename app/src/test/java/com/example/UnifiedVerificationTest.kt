package com.example

import com.example.data.local.SeedData
import com.example.domain.UnifiedVerificationEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class UnifiedVerificationTest {

    private val student = SeedData.students[0]
    private val application = SeedData.applicationsStu1[0]
    private val documents = SeedData.documents.filter { it.studentId == student.id }

    @Test
    fun testSevenSourceVerificationExecution() = runBlocking {
        val output = UnifiedVerificationEngine.executeSevenSourceVerification(
            student = student,
            application = application,
            documents = documents
        )

        // Verifies exactly 7 provider checks
        assertEquals(7, output.records.size)

        val verifiedCount = output.records.count { it.status == "VERIFIED" }
        val mismatchCount = output.records.count { it.status == "MISMATCH" }

        assertEquals(6, verifiedCount)
        assertEquals(1, mismatchCount)

        // Check that non-blocking review item was generated for income variance
        assertNotNull(output.reviewItem)
        assertEquals("e-District Revenue Portal", output.reviewItem?.sourceSystem)
        assertEquals("PENDING", output.reviewItem?.status)
    }
}
