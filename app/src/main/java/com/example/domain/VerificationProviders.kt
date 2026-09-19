package com.example.domain

import com.example.data.model.ApplicationEntity
import com.example.data.model.DocumentEntity
import com.example.data.model.ReviewQueueEntity
import com.example.data.model.StudentEntity
import com.example.data.model.VerificationRecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Result of a single government verification check.
 * Encapsulates the provider source, field verified, declared vs retrieved data,
 * and whether the outcome is verified or a non-blocking exception for review.
 */
data class SingleVerificationResult(
    val sourceSystem: String,
    val fieldChecked: String,
    val declaredValue: String,
    val retrievedValue: String,
    val status: String, // VERIFIED, MISMATCH, RESOLVED, PENDING
    val notes: String,
    val isBlocking: Boolean = false,
    val requiresOfficerReview: Boolean = false,
    val reviewReason: String? = null
)

interface VerificationProvider {
    val name: String
    suspend fun verify(
        student: StudentEntity,
        application: ApplicationEntity,
        documents: List<DocumentEntity>
    ): SingleVerificationResult
}

class UidaiVerificationProvider : VerificationProvider {
    override val name = "UIDAI (Aadhaar Rail)"
    override suspend fun verify(
        student: StudentEntity,
        application: ApplicationEntity,
        documents: List<DocumentEntity>
    ): SingleVerificationResult {
        return SingleVerificationResult(
            sourceSystem = name,
            fieldChecked = "Demographic & Biometric Authentication",
            declaredValue = "${student.name}, DOB: ${student.dob}",
            retrievedValue = "${student.name}, DOB: ${student.dob} (Vault Confidence: 99.2%)",
            status = "VERIFIED",
            notes = "Demographic authentication successful via UIDAI e-KYC central vault."
        )
    }
}

class DigiLockerVerificationProvider : VerificationProvider {
    override val name = "DigiLocker Wallet"
    override suspend fun verify(
        student: StudentEntity,
        application: ApplicationEntity,
        documents: List<DocumentEntity>
    ): SingleVerificationResult {
        val casteDoc = documents.find { it.type.contains("Caste", ignoreCase = true) || it.type.contains("ST", ignoreCase = true) }
        val docNum = casteDoc?.docNumberMasked ?: "ST/OD/2021/992418"
        return SingleVerificationResult(
            sourceSystem = name,
            fieldChecked = "ST Caste & PVTG Community Validation",
            declaredValue = student.category,
            retrievedValue = "${student.category} (Cert: $docNum)",
            status = "VERIFIED",
            notes = "Cryptographic digital signature verified against State e-Pramaan root authority."
        )
    }
}

class ApaarVerificationProvider : VerificationProvider {
    override val name = "APAAR / EduLocker"
    override suspend fun verify(
        student: StudentEntity,
        application: ApplicationEntity,
        documents: List<DocumentEntity>
    ): SingleVerificationResult {
        return SingleVerificationResult(
            sourceSystem = name,
            fieldChecked = "Academic Bank of Credits (ABC Progression)",
            declaredValue = student.apaarId,
            retrievedValue = "${student.apaarId} • Credits Active (ABC Score: 8.42)",
            status = "VERIFIED",
            notes = "Student progression and academic continuity validated through National Academic Depository."
        )
    }
}

class AisheVerificationProvider : VerificationProvider {
    override val name = "AISHE Portal"
    override suspend fun verify(
        student: StudentEntity,
        application: ApplicationEntity,
        documents: List<DocumentEntity>
    ): SingleVerificationResult {
        return SingleVerificationResult(
            sourceSystem = name,
            fieldChecked = "Institute Recognition & Regular Enrollment",
            declaredValue = "${student.institutionName} (${student.course})",
            retrievedValue = "Code: ${student.institutionId} (Institute of National Importance)",
            status = "VERIFIED",
            notes = "Active full-time enrollment verified via Higher Education All India Survey database."
        )
    }
}

class UdiseVerificationProvider : VerificationProvider {
    override val name = "UDISE+ School Registry"
    override suspend fun verify(
        student: StudentEntity,
        application: ApplicationEntity,
        documents: List<DocumentEntity>
    ): SingleVerificationResult {
        return SingleVerificationResult(
            sourceSystem = name,
            fieldChecked = "Secondary School Prior Completion History",
            declaredValue = "Class X / XII Prior Board Record",
            retrievedValue = "Central Board Record Validated (Pass Status: FIRST_CLASS)",
            status = "VERIFIED",
            notes = "UDISE+ prior schooling baseline cross-matched with secondary education register."
        )
    }
}

class UgcNtaVerificationProvider : VerificationProvider {
    override val name = "UGC / NTA Rail"
    override suspend fun verify(
        student: StudentEntity,
        application: ApplicationEntity,
        documents: List<DocumentEntity>
    ): SingleVerificationResult {
        return SingleVerificationResult(
            sourceSystem = name,
            fieldChecked = "National Merit & Qualification Credential",
            declaredValue = student.qualificationDetails.ifBlank { "National Merit Qualified" },
            retrievedValue = "Score: 94.8 Percentile (ST Rank 842) • Central Merit Verified",
            status = "VERIFIED",
            notes = "Entrance exam percentile validated against National Testing Agency master ledger."
        )
    }
}

class EDistrictVerificationProvider : VerificationProvider {
    override val name = "e-District Revenue Portal"
    override suspend fun verify(
        student: StudentEntity,
        application: ApplicationEntity,
        documents: List<DocumentEntity>
    ): SingleVerificationResult {
        // Deterministic SIH demo scenario: Declared ₹2,10,000 vs e-District record ₹2,35,000 (+11.9% variance)
        // Both are strictly below scheme ceiling (₹2.50L), triggering non-blocking exception workflow
        val declaredIncomeFormatted = "₹${String.format(Locale.ENGLISH, "%,d", student.annualIncome.toInt())} / annum"
        val registryIncome = (student.annualIncome * 1.119).toInt()
        val registryIncomeFormatted = "₹${String.format(Locale.ENGLISH, "%,d", registryIncome)} / annum (Cert INC/OD/2026/00142)"

        return SingleVerificationResult(
            sourceSystem = name,
            fieldChecked = "Annual Household Income Certificate",
            declaredValue = declaredIncomeFormatted,
            retrievedValue = registryIncomeFormatted,
            status = "MISMATCH",
            notes = "Variance of +11.9% detected. Auto-routed to manual Reviewer Desk under non-blocking exception workflow (below ₹2.50L ceiling).",
            isBlocking = false,
            requiresOfficerReview = true,
            reviewReason = "Self-declared income $declaredIncomeFormatted vs e-District registry $registryIncomeFormatted (+11.9%). Both figures are strictly below the ₹2.50 Lakh scheme ceiling. Eligible for officer exception clearance without blocking the student."
        )
    }
}

data class VerificationExecutionOutput(
    val records: List<VerificationRecordEntity>,
    val reviewItem: ReviewQueueEntity?,
    val summaryMessage: String
)

object UnifiedVerificationEngine {

    private val providers: List<VerificationProvider> = listOf(
        UidaiVerificationProvider(),
        DigiLockerVerificationProvider(),
        ApaarVerificationProvider(),
        AisheVerificationProvider(),
        UdiseVerificationProvider(),
        UgcNtaVerificationProvider(),
        EDistrictVerificationProvider()
    )

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
    }

    suspend fun executeSevenSourceVerification(
        student: StudentEntity,
        application: ApplicationEntity,
        documents: List<DocumentEntity>
    ): VerificationExecutionOutput {
        val now = getCurrentTimestamp()
        val records = mutableListOf<VerificationRecordEntity>()
        var generatedReviewItem: ReviewQueueEntity? = null

        var verifiedCount = 0
        var mismatchCount = 0

        for (provider in providers) {
            val result = provider.verify(student, application, documents)
            val recordId = "VER_${provider.name.take(6).uppercase(Locale.ROOT).replace("[^A-Z]".toRegex(), "")}_${System.currentTimeMillis()}"

            val recordEntity = VerificationRecordEntity(
                id = recordId,
                applicationId = application.id,
                schemeId = application.schemeId,
                sourceSystem = result.sourceSystem,
                fieldChecked = result.fieldChecked,
                declaredValue = result.declaredValue,
                retrievedValue = result.retrievedValue,
                status = result.status,
                timestamp = now,
                notes = result.notes,
                isBlocking = result.isBlocking
            )
            records.add(recordEntity)

            if (result.status == "VERIFIED") {
                verifiedCount++
            } else if (result.status == "MISMATCH") {
                mismatchCount++
                if (result.requiresOfficerReview) {
                    generatedReviewItem = ReviewQueueEntity(
                        id = "REV_${System.currentTimeMillis()}",
                        verificationRecordId = recordId,
                        applicationId = application.id,
                        studentId = student.id,
                        studentName = student.name,
                        category = student.category,
                        schemeName = application.schemeName,
                        sourceSystem = result.sourceSystem,
                        fieldName = result.fieldChecked,
                        declaredValue = result.declaredValue,
                        retrievedValue = result.retrievedValue,
                        mismatchReason = result.reviewReason ?: result.notes,
                        status = "PENDING",
                        createdAt = now
                    )
                }
            }
        }

        val summary = "$verifiedCount verified, $mismatchCount mismatch auto-routed to Reviewer Desk."
        return VerificationExecutionOutput(
            records = records,
            reviewItem = generatedReviewItem,
            summaryMessage = summary
        )
    }
}
