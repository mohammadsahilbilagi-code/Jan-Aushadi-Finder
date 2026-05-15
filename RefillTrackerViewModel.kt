package com.example.janaushadifinder.ui.refilltracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.janaushadifinder.data.repository.MedicineRepositoryImpl
import com.example.janaushadifinder.domain.model.MedicineReminder
import com.example.janaushadifinder.domain.repository.AuthRepository
import com.example.janaushadifinder.domain.repository.MedicineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import android.util.Log

@OptIn(ExperimentalCoroutinesApi::class)
class RefillTrackerViewModel(
    private val repository: MedicineRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val medicineReminders: StateFlow<List<MedicineReminder>> = authRepository.currentUserId
        .flatMapLatest { userId ->
            val effectiveUserId = userId ?: "anonymous_user"
            Log.d("RefillTrackerVM", "Using Effective User ID: $effectiveUserId")
            
            if (userId != null && repository is MedicineRepositoryImpl) {
                viewModelScope.launch {
                    repository.syncWithFirestore(userId)
                }
            }
            repository.getMedicineReminders(effectiveUserId)
        }
        .onEach { reminders ->
            Log.d("RefillTrackerVM", "Total Reminders in State: ${reminders.size}")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun logDose(reminderId: String) {
        viewModelScope.launch {
            repository.logDose(reminderId)
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMedicineReminder(reminderId)
                Log.d("RefillTrackerVM", "Reminder deleted: $reminderId")
            } catch (e: Exception) {
                Log.e("RefillTrackerVM", "Error deleting reminder", e)
            }
        }
    }

    fun addReminder(name: String, quantity: Int, dosage: Int, notifyDays: Int) {
        viewModelScope.launch {
            try {
                // Get the current ID or use fallback immediately instead of waiting/blocking
                val userId = authRepository.currentUserId.first() ?: "anonymous_user"
                Log.d("RefillTrackerVM", "Adding reminder for user: $userId")
                
                val newReminder = MedicineReminder(
                    id = java.util.UUID.randomUUID().toString(),
                    medicineName = name,
                    currentQuantity = quantity,
                    dosagePerDay = dosage,
                    notifyDaysBefore = notifyDays,
                    userId = userId
                )
                repository.addMedicineReminder(newReminder)
                Log.d("RefillTrackerVM", "Reminder added successfully with ID: ${newReminder.id}")
            } catch (e: Exception) {
                Log.e("RefillTrackerVM", "Error adding reminder", e)
            }
        }
    }
}
