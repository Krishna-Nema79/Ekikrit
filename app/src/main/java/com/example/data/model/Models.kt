package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    ODIA("or", "Odia", "ଓଡ଼ିଆ"),
    GONDI("gon", "Gondi", "गोंडी")
}

enum class ApplicationStage(val displayName: String, val stepIndex: Int) {
    SUBMITTED("Applied", 0),
    VERIFICATION("Under Verification", 1),
    SANCTIONED("Sanctioned", 2),
    DISBURSED("Disbursed", 3)
}

enum class VerificationStatus {
    VERIFIED,
    MISMATCH,
    RESOLVED,
    PENDING
}

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String = "STU_2026_01",
    val name: String = "Birsa Munda Tirkey",
    val dob: String = "15-08-2003",
    val mobile: String = "+91 98765 43210",
    val state: String = "Odisha",
    val institutionId: String = "AISHE-U-0355",
    val institutionName: String = "National Institute of Technology, Rourkela",
    val course: String = "B.Tech Computer Science & Engineering",
    val category: String = "ST (PVTG - Birhor)",
    val pvtgCommunity: String = "Birhor",
    val preferredLanguage: String = "en",
    val apaarId: String = "APAAR-8839-4021-9920",
    val annualIncome: Double = 210000.0,
    val aadhaarMasked: String = "XXXX-XXXX-8924",
    val bankAccountMasked: String = "Canara Bank (A/C **4821)",
    val ifscCode: String = "CNRB0002845",
    val isDigiLockerLinked: Boolean = true,
    val hasConsentGiven: Boolean = true
)

@Entity(tableName = "schemes")
data class SchemeEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val ministry: String,
    val portalOrigin: String,
    val maxAmount: String,
    val eligibilityRules: String,
    val description: String,
    val deadline: String = "31 Oct 2026"
)

@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey val id: String,
    val studentId: String = "STU_2026_01",
    val schemeId: String,
    val schemeCode: String,
    val schemeName: String,
    val currentStage: String, // SUBMITTED, VERIFICATION, SANCTIONED, DISBURSED
    val statusText: String,
    val appliedDate: String,
    val lastUpdated: String,
    val pendingActionDesc: String? = null,
    val hasDiscrepancy: Boolean = false,
    val sanctionedAmount: Double = 0.0,
    val estimatedDisbursementDays: Int = 0
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val studentId: String = "STU_2026_01",
    val type: String, // Aadhaar, Caste, Income, Marksheet, Domicile
    val title: String,
    val docNumberMasked: String,
    val source: String = "DigiLocker",
    val verificationStatus: String, // VERIFIED, NEEDS_ATTENTION, EXPIRED
    val issuedDate: String,
    val issuedBy: String,
    val isReusable: Boolean = true
)

@Entity(tableName = "verification_records")
data class VerificationRecordEntity(
    @PrimaryKey val id: String,
    val applicationId: String,
    val schemeId: String,
    val sourceSystem: String, // UIDAI, DigiLocker, AISHE / UDISE+, APAAR, UGC-NTA, e-District
    val fieldChecked: String,
    val declaredValue: String,
    val retrievedValue: String,
    val status: String, // VERIFIED, MISMATCH, RESOLVED, PENDING
    val timestamp: String,
    val notes: String
)

@Entity(tableName = "review_queue")
data class ReviewQueueEntity(
    @PrimaryKey val id: String,
    val verificationRecordId: String,
    val applicationId: String,
    val studentName: String,
    val category: String,
    val schemeName: String,
    val sourceSystem: String,
    val fieldName: String,
    val declaredValue: String,
    val retrievedValue: String,
    val mismatchReason: String,
    val status: String = "PENDING", // PENDING, APPROVED, RESUBMIT
    val createdAt: String,
    val resolvedAt: String? = null,
    val resolutionNotes: String? = null
)

@Entity(tableName = "disbursements")
data class DisbursementEntity(
    @PrimaryKey val id: String,
    val applicationId: String,
    val schemeName: String,
    val amount: Double,
    val date: String,
    val txnRef: String,
    val bankName: String,
    val accountMasked: String,
    val status: String = "SUCCESS"
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,
    val actor: String,
    val details: String,
    val timestamp: String
)

data class JagoMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "JAGO"
    val content: String,
    val timestamp: String = "Just now",
    val quickChips: List<String> = emptyList()
)
