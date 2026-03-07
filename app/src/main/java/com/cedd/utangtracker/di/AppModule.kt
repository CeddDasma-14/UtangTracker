package com.cedd.utangtracker.di

import android.content.Context
import androidx.room.Room
import com.cedd.utangtracker.data.local.UtangDatabase
import com.cedd.utangtracker.data.local.dao.ComakerDao
import com.cedd.utangtracker.data.local.dao.ContractDao
import com.cedd.utangtracker.data.local.dao.DebtDao
import com.cedd.utangtracker.data.local.dao.PaymentDao
import com.cedd.utangtracker.data.local.dao.PersonDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): UtangDatabase =
        Room.databaseBuilder(context, UtangDatabase::class.java, "utang_tracker.db")
            .addMigrations(
                UtangDatabase.MIGRATION_1_2,
                UtangDatabase.MIGRATION_2_3,
                UtangDatabase.MIGRATION_3_4,
                UtangDatabase.MIGRATION_4_5,
                UtangDatabase.MIGRATION_5_6,
                UtangDatabase.MIGRATION_6_7,
                UtangDatabase.MIGRATION_7_8,
                UtangDatabase.MIGRATION_8_9,
                UtangDatabase.MIGRATION_9_10,
                UtangDatabase.MIGRATION_10_11,
                UtangDatabase.MIGRATION_11_12,
                UtangDatabase.MIGRATION_12_13
            )
            .build()

    @Singleton @Provides fun providePersonDao(db: UtangDatabase): PersonDao = db.personDao()
    @Singleton @Provides fun provideDebtDao(db: UtangDatabase): DebtDao = db.debtDao()
    @Singleton @Provides fun providePaymentDao(db: UtangDatabase): PaymentDao = db.paymentDao()
    @Singleton @Provides fun provideContractDao(db: UtangDatabase): ContractDao = db.contractDao()
    @Singleton @Provides fun provideComakerDao(db: UtangDatabase): ComakerDao = db.comakerDao()

    @Singleton @Provides fun provideFirestore(): FirebaseFirestore = Firebase.firestore
}
