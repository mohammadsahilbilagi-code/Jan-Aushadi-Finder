package com.example.janaushadifinder.data.repository

import com.example.janaushadifinder.data.local.dao.MedicineDao
import com.example.janaushadifinder.data.local.entity.toDomain
import com.example.janaushadifinder.data.local.entity.toEntity
import com.example.janaushadifinder.data.remote.api.MedicineApiService
import com.example.janaushadifinder.domain.model.MedicineReminder
import com.example.janaushadifinder.domain.model.MedicineSearch
import com.example.janaushadifinder.domain.repository.MedicineRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class MedicineRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val dao: MedicineDao,
    private val apiService: MedicineApiService
) : MedicineRepository {

    private val reminderCollection = firestore.collection("medicine_reminders")

    override fun getMedicineReminders(userId: String): Flow<List<MedicineReminder>> {
        return dao.getMedicineReminders(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addMedicineReminder(reminder: MedicineReminder) {
        val documentRef = if (reminder.id.isNotEmpty()) {
            reminderCollection.document(reminder.id)
        } else {
            reminderCollection.document()
        }
        val reminderWithId = if (reminder.id.isEmpty()) reminder.copy(id = documentRef.id) else reminder
        
        android.util.Log.d("FirestoreDebug", "Saving locally: ${reminderWithId.medicineName} with ID: ${reminderWithId.id}")
        dao.insertMedicineReminder(reminderWithId.toEntity())
        
        try {
            android.util.Log.d("FirestoreDebug", "Attempting Firestore sync for: ${reminderWithId.medicineName}")
            documentRef.set(reminderWithId).await()
            android.util.Log.d("FirestoreDebug", "Firestore Sync SUCCESSFUL")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreDebug", "Firestore Sync FAILED: ${e.message}", e)
        }
    }

    override suspend fun updateMedicineReminder(reminder: MedicineReminder) {
        dao.updateMedicineReminder(reminder.toEntity())
        try {
            reminderCollection.document(reminder.id).set(reminder).await()
        } catch (e: Exception) {}
    }

    override suspend fun deleteMedicineReminder(reminderId: String) {
        dao.deleteMedicineReminder(reminderId)
        try {
            reminderCollection.document(reminderId).delete().await()
        } catch (e: Exception) {}
    }

    override suspend fun logDose(reminderId: String) {
        val localReminder = dao.getMedicineById(reminderId)?.toDomain()
        localReminder?.let {
            if (it.currentQuantity > 0) {
                val updatedReminder = it.copy(
                    currentQuantity = it.currentQuantity - 1,
                    lastLogDate = Date()
                )
                dao.updateMedicineReminder(updatedReminder.toEntity())
                try {
                    reminderCollection.document(reminderId).set(updatedReminder).await()
                } catch (e: Exception) {}
            }
        }
    }

    override suspend fun searchMedicines(query: String): List<MedicineSearch> {
        if (query.length < 2) return emptyList()
        
        return try {
            // Try API first
            val apiResults = apiService.searchMedicines(query)
            if (apiResults.isEmpty()) {
                getMockMedicines(query)
            } else {
                apiResults
            }
        } catch (e: Exception) {
            getMockMedicines(query)
        }
    }

    override suspend fun getMedicinesByCategory(categoryName: String): List<MedicineSearch> {
        return getMockMedicines("").filter { 
            it.category.equals(categoryName, ignoreCase = true) 
        }
    }

    private fun getMockMedicines(query: String): List<MedicineSearch> {
        val q = query.lowercase().trim()
        val allMedicines = listOf(
            // --- BLOOD PRESSURE ---
            MedicineSearch("Telma 40", "Telmisartan 40mg", 150.0, 25.40, "Blood Pressure"),
            MedicineSearch("Telpres 40", "Telmisartan 40mg", 140.0, 25.40, "Blood Pressure"),
            MedicineSearch("Amlokind 5", "Amlodipine 5mg", 25.0, 6.0, "Blood Pressure"),
            MedicineSearch("Cilacar 10", "Cidnidipine 10mg", 120.0, 32.0, "Blood Pressure"),
            
            // --- FEVER & PAIN ---
            MedicineSearch("Dolo 650", "Paracetamol 650mg", 30.0, 10.20, "Fever"),
            MedicineSearch("Calpol 650", "Paracetamol 650mg", 28.0, 10.20, "Fever"),
            MedicineSearch("Crocin 650", "Paracetamol 650mg", 32.0, 10.20, "Fever"),
            MedicineSearch("Voveran 50", "Diclofenac 50mg", 45.0, 15.0, "Fever"),
            MedicineSearch("Combiflam", "Ibuprofen + Paracetamol", 40.0, 12.50, "Fever"),
            
            // --- HIV (Anti-Retrovirals) ---
            MedicineSearch("Viraday", "Efavirenz + Emtricitabine + Tenofovir", 3500.0, 1200.0, "HIV"),
            MedicineSearch("Tenvir-EM", "Tenofovir + Emtricitabine", 2500.0, 850.0, "HIV"),
            MedicineSearch("Spegra", "Dolutegravir + Emtricitabine + Tenofovir", 4200.0, 1500.0, "HIV"),
            MedicineSearch("Instgra", "Dolutegravir 50mg", 2800.0, 950.0, "HIV"),
            MedicineSearch("Lopimune", "Lopinavir + Ritonavir", 3800.0, 1300.0, "HIV"),
            
            // --- CANCER (Oncology) ---
            MedicineSearch("Imatib 400", "Imatinib 400mg", 12000.0, 2500.0, "Cancer"),
            MedicineSearch("Geftinat 250", "Gefitinib 250mg", 10500.0, 1800.0, "Cancer"),
            MedicineSearch("Erlocip 150", "Erlotinib 150mg", 18000.0, 4500.0, "Cancer"),
            MedicineSearch("Sorafenat", "Sorafenib 200mg", 25000.0, 6000.0, "Cancer"),
            MedicineSearch("Cytoblastin", "Vinblastine 10mg", 1200.0, 450.0, "Cancer"),
            
            // --- DIABETES ---
            MedicineSearch("Glycomet 500", "Metformin 500mg", 50.0, 12.0, "Diabetes"),
            MedicineSearch("Janumet 50/500", "Sitagliptin + Metformin", 450.0, 120.0, "Diabetes"),
            MedicineSearch("Galvus Met", "Vildagliptin + Metformin", 380.0, 110.0, "Diabetes"),
            MedicineSearch("Glyciphage SR 500", "Metformin 500mg", 45.0, 11.50, "Diabetes"),
            
            // --- STOMACH ---
            MedicineSearch("Pan 40", "Pantoprazole 40mg", 160.0, 48.50, "Stomach"),
            MedicineSearch("Pantocid 40", "Pantoprazole 40mg", 155.0, 48.50, "Stomach"),
            MedicineSearch("Omez 20", "Omeprazole 20mg", 60.0, 18.0, "Stomach"),
            MedicineSearch("Digene Gel", "Antacid Antigas", 140.0, 65.0, "Stomach"),
            
            // --- INFECTIONS ---
            MedicineSearch("Augmentin 625", "Amoxycillin + Clavulanic Acid", 200.0, 95.0, "Infections"),
            MedicineSearch("Zifi 200", "Cefixime 200mg", 110.0, 45.0, "Infections"),
            MedicineSearch("Azithral 500", "Azithromycin 500mg", 120.0, 40.0, "Infections"),
            MedicineSearch("Taxim-O 200", "Cefixime 200mg", 105.0, 42.0, "Infections")
        )
        
        if (q.isEmpty()) return allMedicines
        
        // Simple "Fuzzy" match: contains or closely starts with
        return allMedicines.filter { 
            it.brandedName.lowercase().contains(q) || 
            it.saltName.lowercase().contains(q) ||
            q.contains(it.brandedName.lowercase().take(3))
        }.sortedByDescending { it.savingsPercentage }
    }

    suspend fun syncWithFirestore(userId: String) {
        try {
            android.util.Log.d("FirestoreSync", "Starting sync for user: $userId")
            val snapshot = reminderCollection.whereEqualTo("userId", userId).get().await()
            
            val remoteReminders = snapshot.documents.mapNotNull { doc ->
                doc.toObject<MedicineReminder>()?.copy(id = doc.id)
            }
            
            android.util.Log.d("FirestoreSync", "Found ${remoteReminders.size} remote reminders")
            
            remoteReminders.forEach { reminder ->
                dao.insertMedicineReminder(reminder.toEntity())
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSync", "Sync failed: ${e.message}", e)
        }
    }
}
