package com.example.janaushadifinder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.janaushadifinder.data.local.dao.MedicineDao
import com.example.janaushadifinder.data.local.entity.MedicineEntity

@Database(entities = [MedicineEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract val medicineDao: MedicineDao

    companion object {
        const val DATABASE_NAME = "jan_aushadhi_db"
    }
}
