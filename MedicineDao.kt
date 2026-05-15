package com.example.janaushadifinder.data.local.dao

import androidx.room.*
import com.example.janaushadifinder.data.local.entity.MedicineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicine_reminders WHERE userId = :userId")
    fun getMedicineReminders(userId: String): Flow<List<MedicineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicineReminder(medicine: MedicineEntity)

    @Update
    suspend fun updateMedicineReminder(medicine: MedicineEntity)

    @Query("DELETE FROM medicine_reminders WHERE id = :id")
    suspend fun deleteMedicineReminder(id: String)

    @Query("SELECT * FROM medicine_reminders WHERE id = :id")
    suspend fun getMedicineById(id: String): MedicineEntity?
}
