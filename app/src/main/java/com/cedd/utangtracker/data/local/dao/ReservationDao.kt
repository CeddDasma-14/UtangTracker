package com.cedd.utangtracker.data.local.dao

import androidx.room.*
import com.cedd.utangtracker.data.local.entity.ReservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservationDao {
    @Query("SELECT * FROM reservations ORDER BY plannedDate ASC")
    fun getAll(): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE personId = :personId ORDER BY plannedDate ASC")
    fun getForPerson(personId: Long): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE id = :id")
    suspend fun getById(id: Long): ReservationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(r: ReservationEntity): Long

    @Update
    suspend fun update(r: ReservationEntity)

    @Delete
    suspend fun delete(r: ReservationEntity)
}
