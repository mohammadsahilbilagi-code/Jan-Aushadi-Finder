package com.example.janaushadifinder

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.janaushadifinder.data.local.AppDatabase
import com.example.janaushadifinder.data.remote.api.MedicineApiService
import com.example.janaushadifinder.data.repository.AuthRepositoryImpl
import com.example.janaushadifinder.data.repository.MedicineRepositoryImpl
import com.example.janaushadifinder.ui.about.AboutScreen
import com.example.janaushadifinder.ui.refilltracker.RefillTrackerScreen
import com.example.janaushadifinder.ui.refilltracker.RefillTrackerViewModel
import com.example.janaushadifinder.ui.savings.SavingsScreen
import com.example.janaushadifinder.ui.savings.SavingsViewModel
import com.example.janaushadifinder.ui.search.CategorySearchScreen
import com.example.janaushadifinder.ui.search.MedicineSearchScreen
import com.example.janaushadifinder.ui.search.MedicineSearchViewModel
import com.example.janaushadifinder.ui.storelocator.StoreLocatorScreen
import com.example.janaushadifinder.ui.theme.JanAushadiFinderTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase init failed", e)
        }
        
        val firebaseAuth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
        val dao = database.medicineDao
        
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.janaushadhi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(MedicineApiService::class.java)
        
        val authRepository = AuthRepositoryImpl(firebaseAuth)
        val repository = MedicineRepositoryImpl(firestore, dao, apiService)
        
        lifecycleScope.launch {
            if (firebaseAuth.currentUser == null) {
                try {
                    authRepository.signInAnonymously()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Anonymous sign in failed", e)
                }
            }
        }
        
        val refillViewModel = RefillTrackerViewModel(repository, authRepository)
        val searchViewModel = MedicineSearchViewModel(repository)
        val savingsViewModel = SavingsViewModel()

        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            JanAushadiFinderTheme(darkTheme = isDarkMode) {
                MainContent(
                    refillViewModel = refillViewModel,
                    searchViewModel = searchViewModel,
                    savingsViewModel = savingsViewModel,
                    isDarkMode = isDarkMode,
                    onThemeToggle = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    refillViewModel: RefillTrackerViewModel,
    searchViewModel: MedicineSearchViewModel,
    savingsViewModel: SavingsViewModel,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 600
    var currentScreen by remember { mutableIntStateOf(0) }
    
    val mainGradient = if (isDarkMode) {
        Brush.linearGradient(
            colors = listOf(
                com.example.janaushadifinder.ui.theme.DarkPurple,
                com.example.janaushadifinder.ui.theme.Blue,
                com.example.janaushadifinder.ui.theme.Cyan
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFF1F5F9),
                Color(0xFFE2E8F0)
            )
        )
    }

    Row(modifier = Modifier.fillMaxSize().background(mainGradient)) {
        if (isWideScreen) {
            NavigationRail(
                containerColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White,
                header = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Medication, 
                            contentDescription = null, 
                            tint = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF008080),
                            modifier = Modifier.padding(vertical = 24.dp).size(32.dp)
                        )
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = if (isDarkMode) Color.White else Color.Black
                            )
                        }
                    }
                }
            ) {
                NavigationRailItem(
                    selected = currentScreen == 0,
                    onClick = { currentScreen = 0 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "SEARCH") },
                    label = { Text("Search") }
                )
                NavigationRailItem(
                    selected = currentScreen == 5,
                    onClick = { currentScreen = 5 },
                    icon = { Icon(Icons.Default.Category, contentDescription = "CATEGORIES") },
                    label = { Text("Diseases") }
                )
                NavigationRailItem(
                    selected = currentScreen == 1,
                    onClick = { currentScreen = 1 },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = "STORES") },
                    label = { Text("Stores") }
                )
                NavigationRailItem(
                    selected = currentScreen == 2,
                    onClick = { currentScreen = 2 },
                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = "SAVINGS") },
                    label = { Text("Savings") }
                )
                NavigationRailItem(
                    selected = currentScreen == 3,
                    onClick = { currentScreen = 3 },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "REFILLS") },
                    label = { Text("Refills") }
                )
                NavigationRailItem(
                    selected = currentScreen == 4,
                    onClick = { currentScreen = 4 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "ABOUT") },
                    label = { Text("About") }
                )
            }
        }

        Scaffold(
            topBar = {
                if (!isWideScreen) {
                    CenterAlignedTopAppBar(
                        title = { 
                            Text(
                                "JanAushadhi Finder", 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color.Black
                            ) 
                        },
                        actions = {
                            IconButton(onClick = onThemeToggle) {
                                Icon(
                                    if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = if (isDarkMode) Color.White else Color.Black
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    FloatingBottomNavigation(
                        selectedItem = currentScreen,
                        isDarkMode = isDarkMode,
                        onItemSelected = { currentScreen = it }
                    )
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier
                .padding(padding)
                .fillMaxSize()) {
                when (currentScreen) {
                    0 -> MedicineSearchScreen(
                        viewModel = searchViewModel,
                        onAddToSavings = { medicine ->
                            savingsViewModel.addMedicine(medicine)
                            currentScreen = 2
                        }
                    )
                    5 -> CategorySearchScreen(
                        viewModel = searchViewModel,
                        onAddToSavings = { medicine ->
                            savingsViewModel.addMedicine(medicine)
                            currentScreen = 2
                        }
                    )
                    1 -> StoreLocatorScreen()
                    2 -> SavingsScreen(viewModel = savingsViewModel)
                    3 -> RefillTrackerScreen(viewModel = refillViewModel)
                    4 -> AboutScreen()
                }
            }
        }
    }
}

@Composable
fun FloatingBottomNavigation(selectedItem: Int, isDarkMode: Boolean, onItemSelected: (Int) -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(36.dp),
        color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.White, // Glassmorphism only in dark
        shadowElevation = if (isDarkMode) 0.dp else 8.dp,
        border = if (isDarkMode) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize().background(if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationIcon(Icons.Default.Search, "SEARCH", selectedItem == 0, isDarkMode) { onItemSelected(0) }
            NavigationIcon(Icons.Default.Category, "DISEASES", selectedItem == 5, isDarkMode) { onItemSelected(5) }
            NavigationIcon(Icons.Default.Storefront, "STORES", selectedItem == 1, isDarkMode) { onItemSelected(1) }
            NavigationIcon(Icons.AutoMirrored.Filled.TrendingDown, "SAVINGS", selectedItem == 2, isDarkMode) { onItemSelected(2) }
            NavigationIcon(Icons.Default.Notifications, "REFILLS", selectedItem == 3, isDarkMode) { onItemSelected(3) }
            NavigationIcon(Icons.Default.Info, "ABOUT", selectedItem == 4, isDarkMode) { onItemSelected(4) }
        }
    }
}

@Composable
fun NavigationIcon(icon: ImageVector, label: String, isSelected: Boolean, isDarkMode: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) {
                    if (isDarkMode) com.example.janaushadifinder.ui.theme.NeonCyan else com.example.janaushadifinder.ui.theme.PrimaryPurple
                } else {
                    if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Gray
                },
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                if (isDarkMode) com.example.janaushadifinder.ui.theme.NeonCyan else com.example.janaushadifinder.ui.theme.PrimaryPurple
            } else {
                if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Gray
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.dp)
                    .background(
                        if (isDarkMode) com.example.janaushadifinder.ui.theme.NeonCyan else com.example.janaushadifinder.ui.theme.PrimaryPurple, 
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}
