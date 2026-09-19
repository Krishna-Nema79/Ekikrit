package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        StudentEntity::class,
        SchemeEntity::class,
        ApplicationEntity::class,
        DocumentEntity::class,
        VerificationRecordEntity::class,
        ReviewQueueEntity::class,
        DisbursementEntity::class,
        NotificationEntity::class,
        ApplicationDraftEntity::class,
        AuditLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class EkikritDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun schemeDao(): SchemeDao
    abstract fun applicationDao(): ApplicationDao
    abstract fun documentDao(): DocumentDao
    abstract fun verificationRecordDao(): VerificationRecordDao
    abstract fun reviewQueueDao(): ReviewQueueDao
    abstract fun disbursementDao(): DisbursementDao
    abstract fun notificationDao(): NotificationDao
    abstract fun applicationDraftDao(): ApplicationDraftDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile private var INSTANCE: EkikritDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): EkikritDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EkikritDatabase::class.java,
                    "ekikrit_scholarship_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                scope.launch(Dispatchers.IO) {
                                    SeedData.populateInitialDatabase(database)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE students ADD COLUMN role TEXT NOT NULL DEFAULT 'STUDENT'")
                db.execSQL("ALTER TABLE students ADD COLUMN qualificationDetails TEXT NOT NULL DEFAULT 'JEE Main 94.8 Percentile, ST Category Rank 842'")
                db.execSQL("ALTER TABLE schemes ADD COLUMN targetLevel TEXT NOT NULL DEFAULT 'ALL'")
                db.execSQL("ALTER TABLE schemes ADD COLUMN incomeCeiling REAL NOT NULL DEFAULT 250000.0")
                db.execSQL("ALTER TABLE applications ADD COLUMN academicYear TEXT NOT NULL DEFAULT '2026-27'")
                db.execSQL("ALTER TABLE applications ADD COLUMN syncState TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_applications_studentId_schemeId_academicYear ON applications(studentId, schemeId, academicYear)")
                db.execSQL("ALTER TABLE audit_logs ADD COLUMN studentId TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE audit_logs SET studentId = 'STU_2026_01' WHERE studentId = ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS notifications (id TEXT NOT NULL PRIMARY KEY, studentId TEXT NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, type TEXT NOT NULL, timestamp TEXT NOT NULL, isRead INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS application_drafts (id TEXT NOT NULL PRIMARY KEY, studentId TEXT NOT NULL, schemeId TEXT NOT NULL, currentStep INTEGER NOT NULL, declaredIncome REAL NOT NULL, selectedDocIds TEXT NOT NULL, lastSavedTimestamp TEXT NOT NULL, isPendingSync INTEGER NOT NULL)")
            }
        }
    }
}
