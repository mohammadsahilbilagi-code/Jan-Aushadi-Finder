package com.example.janaushadifinder.domain.repository

import com.example.janaushadifinder.domain.model.MedicineReminder
import com.example.janaushadifinder.domain.model.MedicineSearch
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {
    fun getMedicineReminders(userId: String): Flow<List<MedicineReminder>>
    suspend fun addMedicineReminder(reminder: MedicineReminder)
    suspend fun updateMedicineReminder(reminder: MedicineReminder)
    suspend fun deleteMedicineReminder(reminderId: String)
    suspend fun logDose(reminderId: String)
    suspend fun searchMedicines(query: String): List<MedicineSearch>
    suspend fun getMedicinesByCategory(categoryName: String): List<MedicineSearch>
}
