package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    fun getStudentFlow(id: String = "STU_2026_01"): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudent(id: String = "STU_2026_01"): StudentEntity?

    @Query("SELECT * FROM students ORDER BY id ASC")
    fun getAllStudentsFlow(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY id ASC")
    suspend fun getAllStudents(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE mobile LIKE '%' || :mobile || '%' OR aadhaarMasked LIKE '%' || :mobile || '%' LIMIT 1")
    suspend fun findStudentByPhoneOrAadhaar(mobile: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)
}

@Dao
interface SchemeDao {
    @Query("SELECT * FROM schemes ORDER BY id ASC")
    fun getAllSchemesFlow(): Flow<List<SchemeEntity>>

    @Query("SELECT * FROM schemes WHERE id = :id LIMIT 1")
    suspend fun getSchemeById(id: String): SchemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schemes: List<SchemeEntity>)
}

@Dao
interface ApplicationDao {
    @Query("SELECT * FROM applications WHERE studentId = :studentId ORDER BY id ASC")
    fun getApplicationsForStudentFlow(studentId: String): Flow<List<ApplicationEntity>>

    @Query("SELECT * FROM applications WHERE studentId = :studentId ORDER BY id ASC")
    suspend fun getApplicationsForStudent(studentId: String): List<ApplicationEntity>

    @Query("SELECT * FROM applications WHERE id = :id AND studentId = :studentId LIMIT 1")
    fun getApplicationByIdForStudentFlow(id: String, studentId: String): Flow<ApplicationEntity?>

    @Query("SELECT * FROM applications WHERE id = :id AND studentId = :studentId LIMIT 1")
    suspend fun getApplicationByIdForStudent(id: String, studentId: String): ApplicationEntity?

    @Query("SELECT * FROM applications WHERE studentId = :studentId AND schemeId = :schemeId AND academicYear = :academicYear LIMIT 1")
    suspend fun getApplicationByStudentSchemeYear(studentId: String, schemeId: String, academicYear: String): ApplicationEntity?

    @Query("SELECT * FROM applications WHERE id = :id LIMIT 1")
    suspend fun getApplicationById(id: String): ApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(application: ApplicationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(applications: List<ApplicationEntity>)

    @Update
    suspend fun updateApplication(application: ApplicationEntity)

    @Query("UPDATE applications SET currentStage = :stage, statusText = :statusText, lastUpdated = :timestamp, hasDiscrepancy = :hasDiscrepancy, pendingActionDesc = :pendingAction WHERE id = :appId")
    suspend fun updateStage(
        appId: String,
        stage: String,
        statusText: String,
        timestamp: String,
        hasDiscrepancy: Boolean,
        pendingAction: String?
    )
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE studentId = :studentId ORDER BY id ASC")
    fun getDocumentsForStudentFlow(studentId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id AND studentId = :studentId LIMIT 1")
    suspend fun getDocumentByIdForStudent(id: String, studentId: String): DocumentEntity?

    @Query("DELETE FROM documents WHERE id = :id AND studentId = :studentId")
    suspend fun deleteDocumentForStudent(id: String, studentId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(docs: List<DocumentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doc: DocumentEntity)

    @Update
    suspend fun updateDocument(doc: DocumentEntity)
}

@Dao
interface VerificationRecordDao {
    @Query("SELECT * FROM verification_records WHERE applicationId = :appId ORDER BY id ASC")
    fun getRecordsForAppFlow(appId: String): Flow<List<VerificationRecordEntity>>

    @Query("SELECT * FROM verification_records WHERE applicationId = :appId ORDER BY id ASC")
    suspend fun getRecordsForApp(appId: String): List<VerificationRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<VerificationRecordEntity>)

    @Update
    suspend fun updateRecord(record: VerificationRecordEntity)

    @Query("UPDATE verification_records SET status = :status, notes = :notes WHERE id = :recordId")
    suspend fun updateRecordStatus(recordId: String, status: String, notes: String)
}

@Dao
interface ReviewQueueDao {
    @Query("SELECT * FROM review_queue ORDER BY CASE WHEN status = 'PENDING' THEN 0 ELSE 1 END, createdAt DESC")
    fun getAllReviewItemsFlow(): Flow<List<ReviewQueueEntity>>

    @Query("SELECT * FROM review_queue WHERE status = 'PENDING'")
    fun getPendingReviewItemsFlow(): Flow<List<ReviewQueueEntity>>

    @Query("SELECT * FROM review_queue WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReviewQueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReviewQueueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReviewQueueEntity)

    @Update
    suspend fun update(item: ReviewQueueEntity)
}

@Dao
interface DisbursementDao {
    @Query("SELECT d.* FROM disbursements d INNER JOIN applications a ON a.id = d.applicationId WHERE a.studentId = :studentId ORDER BY d.id DESC")
    fun getDisbursementsForStudentFlow(studentId: String): Flow<List<DisbursementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DisbursementEntity>)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs WHERE studentId = :studentId ORDER BY id DESC")
    fun getLogsForStudentFlow(studentId: String): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insert(log: AuditLogEntity)
}
