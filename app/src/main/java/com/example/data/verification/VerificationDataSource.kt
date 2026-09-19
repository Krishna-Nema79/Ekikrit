package com.example.data.verification

import com.example.data.model.ApplicationEntity
import com.example.data.model.StudentEntity
import com.example.data.model.VerificationRecordEntity
import com.example.data.model.VerificationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SourceVerificationResult(
    val sourceSystem: String,
    val fieldChecked: String,
    val declaredValue: String,
    val retrievedValue: String,
    val status: VerificationStatus,
    val notes: String
)

/**
 * Clean architectural abstraction for multi-source scholarship verification.
 * Decouples repository orchestration from verification data sources.
 *
 * NOTE: This application is a LOCAL DEMO / PROTOTYPE.
 * The implementation provides authentic verification workflows and tolerance rules,
 * but does NOT connect to live government servers (UIDAI, DigiLocker, AISHE, UDISE+, APAAR, UGC-NTA, or e-District).
 */
interface VerificationDataSource {
    suspend fun verifyUidaiDemographics(student: StudentEntity): SourceVerificationResult
    suspend fun verifyDigiLockerCredentials(student: StudentEntity): SourceVerificationResult
    suspend fun verifyInstitutionalEnrollment(student: StudentEntity): SourceVerificationResult
    suspend fun verifyAcademicBankOfCredits(student: StudentEntity): SourceVerificationResult
    suspend fun verifyMeritQualification(student: StudentEntity): SourceVerificationResult
    suspend fun verifyDistrictIncome(student: StudentEntity): SourceVerificationResult

    suspend fun verifyAllSources(
        student: StudentEntity,
        application: ApplicationEntity,
        timestamp: String
    ): List<VerificationRecordEntity>
}

/**
 * Local demo implementation of [VerificationDataSource].
 * Computes deterministic verification results based on active student profile parameters
 * and simulates real government registry checks without fake network calls.
 */
class LocalDemoVerificationDataSource : VerificationDataSource {

    override suspend fun verifyUidaiDemographics(student: StudentEntity): SourceVerificationResult {
        val hasNameAndAadhaar = student.name.isNotBlank() && student.aadhaarMasked.isNotBlank()
        return if (hasNameAndAadhaar) {
            SourceVerificationResult(
                sourceSystem = "UIDAI (Aadhaar Rail)",
                fieldChecked = "Demographic & Aadhaar Authentication",
                declaredValue = "${student.name}, ${student.dob.ifBlank { "DOB Declared" }}",
                retrievedValue = "${student.name}, ${student.dob.ifBlank { "DOB Declared" }} (Score: 99.1%)",
                status = VerificationStatus.VERIFIED,
                notes = "Aadhaar demographic authentication verified successfully with UIDAI central registry."
            )
        } else {
            SourceVerificationResult(
                sourceSystem = "UIDAI (Aadhaar Rail)",
                fieldChecked = "Demographic & Aadhaar Authentication",
                declaredValue = student.name.ifBlank { "Not provided" },
                retrievedValue = "Profile incomplete",
                status = VerificationStatus.MISMATCH,
                notes = "Demographic authentication pending: Aadhaar number or name is missing."
            )
        }
    }

    override suspend fun verifyDigiLockerCredentials(student: StudentEntity): SourceVerificationResult {
        val hasST = student.category.contains("ST", ignoreCase = true) || student.pvtgCommunity.isNotBlank()
        return if (hasST) {
            SourceVerificationResult(
                sourceSystem = "DigiLocker Wallet",
                fieldChecked = "ST Caste & Community Validation",
                declaredValue = student.category,
                retrievedValue = "${student.category} (Digitally Signed Issuer Seal)",
                status = VerificationStatus.VERIFIED,
                notes = "Digital caste credential cryptographically authenticated via state revenue root authority."
            )
        } else {
            SourceVerificationResult(
                sourceSystem = "DigiLocker Wallet",
                fieldChecked = "ST Caste & Community Validation",
                declaredValue = student.category.ifBlank { "Not provided" },
                retrievedValue = "No ST Certificate found in DigiLocker",
                status = VerificationStatus.MISMATCH,
                notes = "ST community validation pending DigiLocker credential link."
            )
        }
    }

    override suspend fun verifyInstitutionalEnrollment(student: StudentEntity): SourceVerificationResult {
        val hasInst = student.institutionName.isNotBlank()
        return if (hasInst) {
            SourceVerificationResult(
                sourceSystem = "AISHE / UDISE+",
                fieldChecked = "Institute Recognition & Regular Enrollment",
                declaredValue = "${student.institutionName}, ${student.course}",
                retrievedValue = "Code: ${student.institutionId.ifBlank { "AISHE Verified" }} (Active Regular Student)",
                status = VerificationStatus.VERIFIED,
                notes = "Institutional registration and student roll progression confirmed."
            )
        } else {
            SourceVerificationResult(
                sourceSystem = "AISHE / UDISE+",
                fieldChecked = "Institute Recognition & Regular Enrollment",
                declaredValue = "Not provided",
                retrievedValue = "No institutional record found",
                status = VerificationStatus.PENDING,
                notes = "Institutional verification pending valid AISHE/UDISE registration."
            )
        }
    }

    override suspend fun verifyAcademicBankOfCredits(student: StudentEntity): SourceVerificationResult {
        val hasApaar = student.apaarId.isNotBlank()
        return if (hasApaar) {
            SourceVerificationResult(
                sourceSystem = "APAAR / ABC Registry",
                fieldChecked = "Academic Bank of Credits (ABC ID)",
                declaredValue = student.apaarId,
                retrievedValue = "Active ABC Account (Credit Continuity Confirmed)",
                status = VerificationStatus.VERIFIED,
                notes = "Academic progression and credit depository active."
            )
        } else {
            SourceVerificationResult(
                sourceSystem = "APAAR / ABC Registry",
                fieldChecked = "Academic Bank of Credits (ABC ID)",
                declaredValue = "Not linked",
                retrievedValue = "Unlinked ABC",
                status = VerificationStatus.PENDING,
                notes = "APAAR / One Nation One Student ID not linked in wallet."
            )
        }
    }

    override suspend fun verifyMeritQualification(student: StudentEntity): SourceVerificationResult {
        return SourceVerificationResult(
            sourceSystem = "UGC / NTA Rail",
            fieldChecked = "National Entrance / Academic Qualification",
            declaredValue = "Course: ${student.course.ifBlank { "Undergraduate" }}",
            retrievedValue = "Merit Qualification Verified (Eligible Category)",
            status = VerificationStatus.VERIFIED,
            notes = "Academic qualification cleared under Ministry scheme guidelines."
        )
    }

    override suspend fun verifyDistrictIncome(student: StudentEntity): SourceVerificationResult {
        // Demonstrate the real-world non-blocking tolerance scenario:
        // Declared income has a small variance (+11.9%) against state revenue certificate,
        // but remains strictly below the scheme ceiling (₹2,50,000).
        val declared = student.annualIncome
        val retrieved = if (declared in 100000.0..240000.0) declared * 1.119 else declared
        val hasVariance = retrieved > declared && declared > 0.0

        return if (hasVariance) {
            SourceVerificationResult(
                sourceSystem = "e-District Revenue Portal",
                fieldChecked = "Annual Household Income Certificate",
                declaredValue = "₹${declared.toInt()} / annum",
                retrievedValue = "₹${retrieved.toInt()} / annum (e-District Registry)",
                status = VerificationStatus.MISMATCH,
                notes = "Income variance detected (+11.9%). Auto-routed to Reviewer Desk for officer tolerance clearance."
            )
        } else {
            SourceVerificationResult(
                sourceSystem = "e-District Revenue Portal",
                fieldChecked = "Annual Household Income Certificate",
                declaredValue = "₹${declared.toInt()} / annum",
                retrievedValue = "₹${declared.toInt()} / annum (Matches Revenue Vault)",
                status = VerificationStatus.VERIFIED,
                notes = "Income certificate matches state revenue vault within statutory ceiling."
            )
        }
    }

    override suspend fun verifyAllSources(
        student: StudentEntity,
        application: ApplicationEntity,
        timestamp: String
    ): List<VerificationRecordEntity> {
        val results = listOf(
            verifyUidaiDemographics(student),
            verifyDigiLockerCredentials(student),
            verifyInstitutionalEnrollment(student),
            verifyAcademicBankOfCredits(student),
            verifyMeritQualification(student),
            verifyDistrictIncome(student)
        )

        val prefix = System.currentTimeMillis()
        return results.mapIndexed { index, res ->
            VerificationRecordEntity(
                id = "VER_${prefix}_$index",
                applicationId = application.id,
                schemeId = application.schemeId,
                sourceSystem = res.sourceSystem,
                fieldChecked = res.fieldChecked,
                declaredValue = res.declaredValue,
                retrievedValue = res.retrievedValue,
                status = res.status.name,
                timestamp = timestamp,
                notes = res.notes
            )
        }
    }
}
