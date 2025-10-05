package com.pavle.lostandfound.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.pavle.lostandfound.ui.viewmodel.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    onNavigateToItemList: () -> Unit,
    onNavigateToLeaderboard: () -> Unit
) {
    val context = LocalContext.current

    val lastKnownLocation by mapViewModel.lastKnownLocation.collectAsState()
    val isLocationEnabled by mapViewModel.isLocationEnabled.collectAsState()
    val filteredItems by mapViewModel.filteredItems.collectAsState()
    val isAddItemSheetVisible by mapViewModel.isAddItemSheetVisible.collectAsState()
    val isFilterSheetVisible by mapViewModel.isFilterSheetVisible.collectAsState()
    val temporaryMarkerLocation by mapViewModel.temporaryMarkerLocation.collectAsState()
    var hasLocationPermission by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(44.7866, 20.4489), 12f)
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        mapViewModel.checkLocationSetting()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        if (isGranted) {
            mapViewModel.checkLocationSetting()
            mapViewModel.startLocationUpdates()
        }
    }

    DisposableEffect(Unit) {
        val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        hasLocationPermission = permissionCheckResult == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            mapViewModel.checkLocationSetting()
            mapViewModel.startLocationUpdates()
            mapViewModel.listenForAllItems()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        onDispose {
            if (hasLocationPermission) {
                mapViewModel.stopLocationUpdates()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasLocationPermission) {
            mapViewModel.checkLocationSetting()
            mapViewModel.startLocationUpdates()
        }
    }

    LaunchedEffect(lastKnownLocation) {
        lastKnownLocation?.let { location ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(location, 15f),
                durationMs = 1000
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lost & Found") },
                actions = {
                    TextButton(onClick = { mapViewModel.resetFilters() }) {
                        Text("Resetuj filtere")
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                FloatingActionButton(
                    onClick = { mapViewModel.showFilterSheet() },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Filteri")
                }
                FloatingActionButton(
                    onClick = { onNavigateToLeaderboard() },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Rang lista")
                }
                FloatingActionButton(
                    onClick = { onNavigateToItemList() },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lista predmeta")
                }
                FloatingActionButton(
                    onClick = { mapViewModel.showAddItemSheet() },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Dodaj predmet")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission && isLocationEnabled
                ),
                onMapLongClick = { latLng ->
                    mapViewModel.setTemporaryMarker(latLng)
                    mapViewModel.showAddItemSheet()
                }
            ) {
                filteredItems.forEach { item ->
                    val itemLatLng = LatLng(item.location.latitude, item.location.longitude)
                    Marker(
                        state = MarkerState(position = itemLatLng),
                        title = item.category,
                        snippet = item.description,
                        icon = if (item.lostStatus) {
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        } else {
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                        }
                    )
                }
                temporaryMarkerLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Nova lokacija",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
            }

            if (isAddItemSheetVisible) {
                AddItemSheet(
                    onAddItem = { isLost, category, description, secretDetails, imageUri ->
                        mapViewModel.addItem(isLost, category, description, secretDetails, imageUri)
                    },
                    onDismiss = { mapViewModel.hideAddItemSheet() }
                )
            }

            if (isFilterSheetVisible) {
                FilterSheet(
                    onDismiss = { mapViewModel.hideFilterSheet() },
                    onApplyFilters = { radius, category ->
                        mapViewModel.applyFilters(radius, category)
                    }
                )
            }

            if (hasLocationPermission && !isLocationEnabled) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Lokacija na Vašem uređaju je isključena.",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        settingsLauncher.launch(intent)
                    }) {
                        Text(text = "Uključi lokaciju")
                    }
                }
            }
        }
    }
}