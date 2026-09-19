package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: EkikritDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): EkikritDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EkikritDatabase::class.java,
                    "ekikrit_scholarship_db"
                )
                .fallbackToDestructiveMigration(true)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        INSTANCE?.let { database ->
                            scope.launch(Dispatchers.IO) {
                                SeedData.populateDatabase(database)
                            }
                        }
                    }

                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        super.onDestructiveMigration(db)
                        INSTANCE?.let { database ->
                            scope.launch(Dispatchers.IO) {
                                SeedData.populateDatabase(database)
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
