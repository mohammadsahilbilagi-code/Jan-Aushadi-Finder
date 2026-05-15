package com.example.janaushadifinder.ui.storelocator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class KendraStore(
    val id: String,
    val name: String,
    val address: String,
    val location: LatLng,
    val distance: String,
    val isOpen: Boolean,
    val timings: String
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StoreLocatorScreen() {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isWideScreen = configuration.screenWidthDp > 600
    
    // Initialize Maps SDK
    LaunchedEffect(Unit) {
        MapsInitializer.initialize(context)
    }

    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )

    var viewMode by remember { mutableStateOf(if (isWideScreen) "MAP" else "LIST") }
    var searchQuery by remember { mutableStateOf("") }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var cityLabel by remember { mutableStateOf("Nearby") }

    // Start with a neutral view (e.g., India-wide) if location is unknown
    val defaultPos = LatLng(20.5937, 78.9629) 
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 4f)
    }

    // Dynamic mock stores based on current location and search query
    val mockStores = remember(userLocation, cityLabel) {
        val base = userLocation ?: LatLng(28.6139, 77.2090)
        
        listOf(
            KendraStore("1", "PMBI Store - Central $cityLabel", "Main Road, Block A, $cityLabel", LatLng(base.latitude + 0.015, base.longitude + 0.015), "1.2 KM AWAY", true, "9 AM - 9 PM"),
            KendraStore("2", "Jan Aushadhi Kendra - West $cityLabel", "Market Complex, Sector 4, $cityLabel", LatLng(base.latitude - 0.010, base.longitude + 0.012), "2.5 KM AWAY", false, "10 AM - 8 PM"),
            KendraStore("3", "Kendra - East $cityLabel", "Health Center Road, $cityLabel", LatLng(base.latitude + 0.005, base.longitude - 0.014), "3.1 KM AWAY", true, "24/7 Available")
        )
    }

    val scope = rememberCoroutineScope()

    // Move geocoding to background thread to prevent ANR
    val updateCityLabel: suspend (LatLng) -> Unit = { latLng ->
        withContext(Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(context)
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    cityLabel = addresses[0].locality ?: addresses[0].subAdminArea ?: "Nearby"
                }
            } catch (e: Exception) {
                cityLabel = "Nearby"
            }
        }
    }

    val moveToCurrentLocation = {
        if (locationPermissionsState.allPermissionsGranted) {
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                android.util.Log.e("LocationDebug", "GPS Provider is OFF")
                android.widget.Toast.makeText(context, "Please turn on Location/GPS", android.widget.Toast.LENGTH_LONG).show()
                try {
                    context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (e: Exception) {
                    android.util.Log.e("LocationDebug", "Failed to open location settings", e)
                }
            } else {
                android.util.Log.d("LocationDebug", "Starting location fetch...")
                isFetchingLocation = true
                
                // 1. Try Last Known Location first
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        android.util.Log.d("LocationDebug", "Last known location found: ${location.latitude}, ${location.longitude}")
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        userLocation = userLatLng
                        scope.launch {
                            updateCityLabel(userLatLng)
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                        }
                        isFetchingLocation = false
                    } else {
                        android.util.Log.w("LocationDebug", "Last location was NULL, requesting fresh current location")
                        // 2. Request Fresh Current Location if lastLocation was null
                        val request = CurrentLocationRequest.Builder()
                            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                            .build()
                        
                        fusedLocationClient.getCurrentLocation(request, null)
                            .addOnSuccessListener { freshLocation ->
                                isFetchingLocation = false
                                if (freshLocation != null) {
                                    android.util.Log.d("LocationDebug", "Fresh location found: ${freshLocation.latitude}")
                                    val userLatLng = LatLng(freshLocation.latitude, freshLocation.longitude)
                                    userLocation = userLatLng
                                    scope.launch {
                                        updateCityLabel(userLatLng)
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                                    }
                                } else {
                                    android.util.Log.e("LocationDebug", "Current location is still NULL after request")
                                    android.widget.Toast.makeText(context, "Could not fix GPS signal. Try moving near a window.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }.addOnFailureListener { e ->
                                isFetchingLocation = false
                                android.util.Log.e("LocationDebug", "CurrentLocationRequest FAILED", e)
                            }
                    }
                }.addOnFailureListener { e ->
                    android.util.Log.e("LocationDebug", "lastLocation request FAILED", e)
                    isFetchingLocation = false
                }
            }
        } else {
            android.util.Log.e("LocationDebug", "Permissions not granted. Launching request.")
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    // Search function to handle city/pincode search - Async
    val onSearch = {
        if (searchQuery.isNotEmpty()) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val geocoder = android.location.Geocoder(context)
                    try {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(searchQuery, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val searchLatLng = LatLng(address.latitude, address.longitude)
                            userLocation = searchLatLng
                            cityLabel = address.locality ?: address.subAdminArea ?: searchQuery
                            withContext(Dispatchers.Main) {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(searchLatLng, 14f))
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
        }
    }

    // Initial load
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            moveToCurrentLocation()
        }
    }

    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 6) { // Specifically for Pincodes
            delay(500)
            onSearch()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        // Responsive Header
        val paddingHorizontal = if (isWideScreen) 48.dp else 24.dp
        Column(modifier = Modifier.padding(top = 40.dp, start = paddingHorizontal, end = paddingHorizontal, bottom = 8.dp)) {
            Text(
                text = "Kendra Locator",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Find generic medicine stores using real-time GPS.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }

        // Search Bar Area - Responsive
        Row(
            modifier = Modifier.padding(horizontal = paddingHorizontal, vertical = 16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search area or pincode (e.g. 580030)...", fontSize = 14.sp) },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearch() }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Search")
                        }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                    keyboardType = if (searchQuery.all { it.isDigit() }) androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Text
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() }),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            ViewToggleIcon(Icons.Default.List, viewMode == "LIST") { viewMode = "LIST" }
            Spacer(modifier = Modifier.width(8.dp))
            ViewToggleIcon(Icons.Default.Map, viewMode == "MAP") { viewMode = "MAP" }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (!locationPermissionsState.allPermissionsGranted) {
                PermissionRequiredState { locationPermissionsState.launchMultiplePermissionRequest() }
            } else {
                // Map Container
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false)
                ) {
                    mockStores.forEach { store ->
                        Marker(
                            state = MarkerState(position = store.location),
                            title = store.name,
                            snippet = store.address
                        )
                    }
                }

                // List Overlay (Optimized for Foldable/Tablet)
                if (viewMode == "LIST") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.1f))
                            .padding(top = 16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = paddingHorizontal),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { viewMode = "MAP" },
                                    modifier = Modifier.background(Color.White, CircleShape).shadow(2.dp, CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close List")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = paddingHorizontal)
                            ) {
                                items(mockStores) { store ->
                                    StoreCard(
                                        store = store, 
                                        width = if (isWideScreen) 450.dp else 300.dp,
                                        onDirectionsClick = {
                                            val gmmIntentUri = Uri.parse("google.navigation:q=${store.location.latitude},${store.location.longitude}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            try {
                                                context.startActivity(mapIntent)
                                            } catch (e: Exception) {}
                                        },
                                        onExitClick = { viewMode = "MAP" }
                                    )
                                }
                            }
                        }
                    }
                }

                // GPS FAB
                FloatingActionButton(
                    onClick = { moveToCurrentLocation() },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(paddingHorizontal),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    if (isFetchingLocation) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                    }
                }
            }
        }
    }
}

@Composable
fun ViewToggleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White, RoundedCornerShape(16.dp))
            .shadow(if (isSelected) 0.dp else 2.dp, RoundedCornerShape(16.dp))
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF64748B))
    }
}

@Composable
fun StoreCard(store: KendraStore, width: androidx.compose.ui.unit.Dp, onDirectionsClick: () -> Unit, onExitClick: () -> Unit) {
    Card(
        modifier = Modifier.width(width).height(220.dp).shadow(12.dp, RoundedCornerShape(24.dp)),
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
                        text = store.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = if (store.isOpen) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (store.isOpen) "OPEN NOW" else "CLOSED",
                            color = if (store.isOpen) Color(0xFF166534) else Color(0xFF64748B),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(store.distance.split(" ")[0], fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF1E293B))
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row {
                Icon(Icons.Default.LocationOn, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = store.address, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color(0xFF64748B), 
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { /* Call */ },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text("CALL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                Button(
                    onClick = onDirectionsClick,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Text("DIRECTIONS", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = onExitClick,
                    modifier = Modifier.weight(0.8f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text("EXIT", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun PermissionRequiredState(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Location Access Required", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("To find the nearest Jan Aushadhi Kendra, please grant location access.", textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onGrantClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.7f).height(56.dp)) { 
            Text("Grant Permission", fontWeight = FontWeight.Bold) 
        }
    }
}
