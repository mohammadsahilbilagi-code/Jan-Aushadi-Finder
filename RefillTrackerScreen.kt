package com.example.janaushadifinder.ui.refilltracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.janaushadifinder.domain.model.MedicineReminder

@Composable
fun RefillTrackerScreen(viewModel: RefillTrackerViewModel) {
    val context = LocalContext.current
    val reminders by viewModel.medicineReminders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Refill Tracker",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Track your stock and get refill alerts.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF64748B)
                )
            }
            
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(56.dp)
                    .shadow(4.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (reminders.isEmpty()) {
            EmptyRemindersState { showAddDialog = true }
        } else {
            ReminderHeader(reminders.count { it.isRefillNeeded })
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    MedicineReminderItem(
                        reminder = reminder,
                        onLogDose = { viewModel.logDose(reminder.id) },
                        onDelete = { viewModel.deleteReminder(reminder.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddMedicineDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, quantity, dosage, notifyDays ->
                    viewModel.addReminder(name, quantity, dosage, notifyDays)
                    Toast.makeText(context, "Saving $name...", Toast.LENGTH_SHORT).show()
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun EmptyRemindersState(onAddClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 100.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = Color(0xFFF1F5F9)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Medication,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color.LightGray.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Your medicine list is empty",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add your first medicine to start tracking.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(56.dp).width(200.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ADD MEDICINE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ReminderHeader(refillNeededCount: Int) {
    if (refillNeededCount > 0) {
        Surface(
            color = Color(0xFFFFE4E6),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .shadow(1.dp, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFE11D48))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "You have $refillNeededCount refills pending!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE11D48),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MedicineReminderItem(
    reminder: MedicineReminder,
    onLogDose: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (reminder.currentQuantity.toFloat() / 30f).coerceIn(0f, 1f)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Medicine?") },
            text = { Text("Are you sure you want to remove ${reminder.medicineName} from your tracker?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("DELETE", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.medicineName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${reminder.currentQuantity} doses remaining",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (reminder.isRefillNeeded) Color(0xFFE11D48) else Color(0xFF64748B)
                    )
                }
                
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color.LightGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(CircleShape),
                    color = if (reminder.isRefillNeeded) Color(0xFFE11D48) else Color(0xFF10B981),
                    trackColor = Color(0xFFF1F5F9)
                )

                Button(
                    onClick = onLogDose,
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = reminder.currentQuantity > 0
                ) {
                    Text("LOG DOSE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            
            if (reminder.isRefillNeeded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "REFILL RECOMMENDED IMMEDIATELY",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE11D48),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun AddMedicineDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("1") }
    var notifyDays by remember { mutableStateOf("3") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add Medicine", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Medicine Name") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity, 
                    onValueChange = { quantity = it }, 
                    label = { Text("Total Quantity (Tablets/Doses)") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dosage, 
                        onValueChange = { dosage = it }, 
                        label = { Text("Daily Dose") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = notifyDays, 
                        onValueChange = { notifyDays = it }, 
                        label = { Text("Alert Days") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && quantity.isNotBlank()) {
                        onConfirm(name, quantity.toIntOrNull() ?: 0, dosage.toIntOrNull() ?: 1, notifyDays.toIntOrNull() ?: 3)
                    }
                },
                modifier = Modifier.height(56.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SAVE MEDICINE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("CANCEL", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = Color.White
    )
}
