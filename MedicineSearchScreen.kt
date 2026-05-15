package com.example.janaushadifinder.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.janaushadifinder.domain.model.MedicineSearch

@Composable
fun MedicineSearchScreen(
    viewModel: MedicineSearchViewModel,
    onAddToSavings: (MedicineSearch) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedMedicine by remember { mutableStateOf<MedicineSearch?>(null) }

    LaunchedEffect(query) {
        if (query.length >= 2) {
            kotlinx.coroutines.delay(500)
            viewModel.search(query)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Jan-Aushadhi Finder",
                style = MaterialTheme.typography.displayLarge,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Search any branded medicine to find its local generic equivalent.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Enter branded name (e.g. Dolo, Telma)...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray)
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { 
                        if (query.isNotEmpty()) viewModel.search(query)
                    }
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "POPULAR MEDICINES",
                style = MaterialTheme.typography.labelMedium,
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    if (searchResults.isEmpty() && query.isEmpty()) {
                        val popular = listOf(
                            MedicineSearch("Crocin Pain Relief", "Paracetamol 500mg", 35.0, 5.50),
                            MedicineSearch("Augmentin 625 Duo", "Amoxycillin + Clavulanic Acid", 220.0, 65.0),
                            MedicineSearch("Glycomet GP 1", "Metformin + Glimepiride", 110.0, 24.0),
                            MedicineSearch("Telma 40", "Telmisartan 40mg", 150.0, 25.40)
                        )
                        items(popular) { result ->
                            MedicineSearchResultItem(result) { selectedMedicine = it }
                        }
                    } else {
                        items(searchResults) { result ->
                            MedicineSearchResultItem(result) { selectedMedicine = it }
                        }
                    }
                }
            }
        }

        selectedMedicine?.let { medicine ->
            MedicineDetailDialog(
                medicine = medicine,
                onDismiss = { selectedMedicine = null },
                onAddClick = {
                    onAddToSavings(medicine)
                    selectedMedicine = null
                }
            )
        }
    }
}

@Composable
fun MedicineSearchResultItem(result: MedicineSearch, onClick: (MedicineSearch) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .clickable { onClick(result) },
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
                        text = result.brandedName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF1E293B)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = result.saltName,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    color = Color(0xFFD1FAE5),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "-${result.savingsPercentage}%",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GENERIC",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = result.saltName.split(" ")[0],
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BRANDED PRICE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${result.brandedPrice}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = Color(0xFF10B981)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "GENERIC PRICE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${result.genericPrice}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MedicineDetailDialog(
    medicine: MedicineSearch,
    onDismiss: () -> Unit,
    onAddClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = medicine.brandedName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "${medicine.saltName.split(" ").lastOrNull() ?: ""} • Analgesics",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f).height(100.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("BRANDED", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text("₹${medicine.brandedPrice}0", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f).height(100.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("GENERIC", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE11D48))
                                Text("₹${medicine.genericPrice}0", style = MaterialTheme.typography.titleLarge, color = Color(0xFFE11D48))
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.offset(y = 0.dp),
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SAVE ₹${medicine.brandedPrice - medicine.genericPrice}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFE11D48), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "AI Explanation",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFE11D48)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"Both medicines contain the exact same active ingredient, ensuring identical therapeutic efficacy at a significantly lower cost.\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onAddClick,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF472B6))
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Savings List", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
