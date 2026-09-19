package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    fun getStudentFlow(id: String): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudent(id: String): StudentEntity?

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

    @Query("UPDATE students SET hasConsentGiven = :hasConsent WHERE id = :studentId")
    suspend fun updateStudentConsent(studentId: String, hasConsent: Boolean)

    @Query("DELETE FROM students")
    suspend fun deleteAll()
}

@Dao
interface SchemeDao {
    @Query("SELECT * FROM schemes ORDER BY id ASC")
    fun getAllSchemesFlow(): Flow<List<SchemeEntity>>

    @Query("SELECT * FROM schemes ORDER BY id ASC")
    suspend fun getAllSchemes(): List<SchemeEntity>

    @Query("SELECT * FROM schemes WHERE id = :id LIMIT 1")
    suspend fun getSchemeById(id: String): SchemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schemes: List<SchemeEntity>)

    @Query("DELETE FROM schemes")
    suspend fun deleteAll()
}

@Dao
interface ApplicationDao {
    @Query("SELECT * FROM applications WHERE studentId = :studentId ORDER BY id ASC")
    fun getApplicationsForStudentFlow(studentId: String): Flow<List<ApplicationEntity>>
    @Query("SELECT * FROM applications WHERE studentId = :studentId ORDER BY id ASC")
    suspend fun getApplicationsForStudent(studentId: String): List<ApplicationEntity>

    @Query("SELECT * FROM applications ORDER BY id ASC")
    fun getAllApplicationsFlow(): Flow<List<ApplicationEntity>>

    @Query("SELECT * FROM applications WHERE id = :id AND studentId = :studentId LIMIT 1")
    fun getApplicationByIdForStudentFlow(id: String, studentId: String): Flow<ApplicationEntity?>

    @Query("SELECT * FROM applications WHERE id = :id AND studentId = :studentId LIMIT 1")
    suspend fun getApplicationByIdForStudent(id: String, studentId: String): ApplicationEntity?

    @Query("SELECT * FROM applications WHERE studentId = :studentId AND schemeId = :schemeId LIMIT 1")
    suspend fun getApplicationByStudentAndScheme(studentId: String, schemeId: String): ApplicationEntity?

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

    @Query("DELETE FROM applications")
    suspend fun deleteAll()
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE studentId = :studentId ORDER BY id ASC")
    fun getDocumentsForStudentFlow(studentId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE studentId = :studentId ORDER BY id ASC")
    suspend fun getDocumentsForStudent(studentId: String): List<DocumentEntity>

    @Query("SELECT * FROM documents ORDER BY id ASC")
    fun getAllDocumentsFlow(): Flow<List<DocumentEntity>>

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

    @Query("DELETE FROM documents")
    suspend fun deleteAll()
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

    @Query("DELETE FROM verification_records WHERE applicationId = :appId")
    suspend fun deleteForApp(appId: String)

    @Query("DELETE FROM verification_records")
    suspend fun deleteAll()
}

@Dao
interface ReviewQueueDao {
    @Query("SELECT * FROM review_queue ORDER BY CASE WHEN status = 'PENDING' THEN 0 ELSE 1 END, createdAt DESC")
    fun getAllReviewItemsFlow(): Flow<List<ReviewQueueEntity>>

    @Query("SELECT * FROM review_queue WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingReviewItemsFlow(): Flow<List<ReviewQueueEntity>>

    @Query("SELECT * FROM review_queue WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun getReviewItemsForStudentFlow(studentId: String): Flow<List<ReviewQueueEntity>>

    @Query("SELECT * FROM review_queue WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReviewQueueEntity?

    @Query("SELECT * FROM review_queue WHERE applicationId = :appId LIMIT 1")
    suspend fun getByAppId(appId: String): ReviewQueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReviewQueueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReviewQueueEntity)

    @Update
    suspend fun update(item: ReviewQueueEntity)

    @Query("DELETE FROM review_queue")
    suspend fun deleteAll()
}

@Dao
interface DisbursementDao {
    @Query("SELECT * FROM disbursements WHERE studentId = :studentId ORDER BY id DESC")
    fun getDisbursementsForStudentFlow(studentId: String): Flow<List<DisbursementEntity>>

    @Query("SELECT * FROM disbursements WHERE studentId = :studentId ORDER BY id DESC")
    suspend fun getDisbursementsForStudent(studentId: String): List<DisbursementEntity>

    @Query("SELECT * FROM disbursements ORDER BY id DESC")
    fun getAllDisbursementsFlow(): Flow<List<DisbursementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DisbursementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(disbursement: DisbursementEntity)

    @Query("DELETE FROM disbursements")
    suspend fun deleteAll()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getNotificationsForStudentFlow(studentId: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE studentId = :studentId AND isRead = 0")
    fun getUnreadNotificationsForStudentFlow(studentId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE studentId = :studentId")
    suspend fun markAllAsRead(studentId: String)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}

@Dao
interface ApplicationDraftDao {
    @Query("SELECT * FROM application_drafts WHERE studentId = :studentId AND schemeId = :schemeId LIMIT 1")
    suspend fun getDraft(studentId: String, schemeId: String): ApplicationDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(draft: ApplicationDraftEntity)

    @Query("DELETE FROM application_drafts WHERE studentId = :studentId AND schemeId = :schemeId")
    suspend fun deleteDraft(studentId: String, schemeId: String)

    @Query("SELECT * FROM application_drafts WHERE isPendingSync = 1")
    suspend fun getPendingSyncDrafts(): List<ApplicationDraftEntity>
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs WHERE studentId = :studentId ORDER BY id DESC")
    fun getLogsForStudentFlow(studentId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY id DESC")
    fun getAllLogsFlow(): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insert(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun deleteAll()
}
