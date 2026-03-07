package com.cedd.utangtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cedd.utangtracker.data.local.dao.ComakerDao
import com.cedd.utangtracker.data.local.dao.ContractDao
import com.cedd.utangtracker.data.local.dao.DebtDao
import com.cedd.utangtracker.data.local.dao.PaymentDao
import com.cedd.utangtracker.data.local.dao.PersonDao
import com.cedd.utangtracker.data.local.dao.ReservationDao
import com.cedd.utangtracker.data.local.entity.ComakerEntity
import com.cedd.utangtracker.data.local.entity.ContractEntity
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.PaymentEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.local.entity.ReservationEntity

@Database(
    entities = [PersonEntity::class, DebtEntity::class, PaymentEntity::class, ContractEntity::class, ComakerEntity::class, ReservationEntity::class],
    version = 14,
    exportSchema = false
)
abstract class UtangDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun debtDao(): DebtDao
    abstract fun paymentDao(): PaymentDao
    abstract fun contractDao(): ContractDao
    abstract fun comakerDao(): ComakerDao
    abstract fun reservationDao(): ReservationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS contracts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        debtId INTEGER NOT NULL,
                        contractNumber TEXT NOT NULL,
                        lenderName TEXT NOT NULL,
                        borrowerName TEXT NOT NULL,
                        amount REAL NOT NULL,
                        purpose TEXT NOT NULL,
                        dateCreated INTEGER NOT NULL,
                        dateDue INTEGER,
                        interestRate REAL NOT NULL DEFAULT 0.0,
                        lenderSignaturePath TEXT,
                        borrowerSignaturePath TEXT,
                        witnessName TEXT,
                        witnessSignaturePath TEXT,
                        pdfPath TEXT,
                        isSigned INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(debtId) REFERENCES debts(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contracts_debtId ON contracts(debtId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN autoApplyInterest INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN contractEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contracts ADD COLUMN secureLinkToken TEXT")
                db.execSQL("ALTER TABLE contracts ADD COLUMN secureLinkExpiresAt INTEGER")
                db.execSQL("ALTER TABLE contracts ADD COLUMN remoteBorrowerStatus TEXT NOT NULL DEFAULT 'none'")
                db.execSQL("ALTER TABLE contracts ADD COLUMN remoteBorrowerFullName TEXT")
                db.execSQL("ALTER TABLE contracts ADD COLUMN remoteBorrowerAddress TEXT")
                db.execSQL("ALTER TABLE contracts ADD COLUMN remoteBorrowerIdType TEXT")
                db.execSQL("ALTER TABLE contracts ADD COLUMN remoteBorrowerIdNumber TEXT")
                db.execSQL("ALTER TABLE contracts ADD COLUMN remoteBorrowerIdImagePath TEXT")
                db.execSQL("ALTER TABLE contracts ADD COLUMN remoteBorrowerSignaturePath TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contracts ADD COLUMN collateral TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contracts ADD COLUMN language TEXT NOT NULL DEFAULT 'en'")
                db.execSQL("ALTER TABLE contracts ADD COLUMN borrowerSignedAt INTEGER")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contracts ADD COLUMN remoteBorrowerIdVerificationStatus TEXT NOT NULL DEFAULT 'none'")
                db.execSQL("ALTER TABLE contracts ADD COLUMN remoteBorrowerIdVerificationNote TEXT")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS comakers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        contractId INTEGER NOT NULL,
                        fullName TEXT NOT NULL,
                        mobileNumber TEXT NOT NULL,
                        address TEXT NOT NULL,
                        FOREIGN KEY(contractId) REFERENCES contracts(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_comakers_contractId ON comakers(contractId)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contracts ADD COLUMN disbursementReceiptPaths TEXT")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN disbursementReceiptPaths TEXT")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN lastInterestAppliedAt INTEGER")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE persons ADD COLUMN notes TEXT")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        personId INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        amount REAL NOT NULL,
                        purpose TEXT NOT NULL,
                        plannedDate INTEGER NOT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(personId) REFERENCES persons(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reservations_personId ON reservations(personId)")
            }
        }
    }
}
