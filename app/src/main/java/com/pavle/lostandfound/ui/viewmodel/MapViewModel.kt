package com.pavle.lostandfound.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.pavle.lostandfound.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val _lastKnownLocation = MutableStateFlow<LatLng?>(null)
    val lastKnownLocation: StateFlow<LatLng?> = _lastKnownLocation

    private val _isLocationEnabled = MutableStateFlow(false)
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled

    private val _isUpdatingLocation = MutableStateFlow(false)

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()

    private val _filteredItems = MutableStateFlow<List<Item>>(emptyList())
    val filteredItems: StateFlow<List<Item>> = _filteredItems.asStateFlow()

    private val _isAddItemSheetVisible = MutableStateFlow(false)
    val isAddItemSheetVisible: StateFlow<Boolean> = _isAddItemSheetVisible.asStateFlow()

    private val _isFilterSheetVisible = MutableStateFlow(false)
    val isFilterSheetVisible: StateFlow<Boolean> = _isFilterSheetVisible.asStateFlow()

    private val _temporaryMarkerLocation = MutableStateFlow<LatLng?>(null)
    val temporaryMarkerLocation: StateFlow<LatLng?> = _temporaryMarkerLocation.asStateFlow()

    private val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application.applicationContext)
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage = Firebase.storage
    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                _lastKnownLocation.value = LatLng(location.latitude, location.longitude)
            }
        }
    }

    fun setTemporaryMarker(location: LatLng) {
        _temporaryMarkerLocation.value = location
    }

    fun showAddItemSheet() { _isAddItemSheetVisible.value = true }
    fun hideAddItemSheet() {
        _isAddItemSheetVisible.value = false
        _temporaryMarkerLocation.value = null
    }
    fun showFilterSheet() { _isFilterSheetVisible.value = true }
    fun hideFilterSheet() { _isFilterSheetVisible.value = false }

    fun applyFilters(radius: Float, category: String?) {
        var tempItems = _items.value

        if (category != null) {
            tempItems = tempItems.filter { it.category == category }
        }

        val myLocation = _lastKnownLocation.value
        if (myLocation != null) {
            val myAndroidLocation = Location("").apply {
                latitude = myLocation.latitude
                longitude = myLocation.longitude
            }
            tempItems = tempItems.filter {
                val itemLocation = Location("").apply {
                    latitude = it.location.latitude
                    longitude = it.location.longitude
                }
                myAndroidLocation.distanceTo(itemLocation) <= radius
            }
        }

        _filteredItems.value = tempItems
    }

    fun resetFilters() {
        _filteredItems.value = _items.value
    }

    fun addItem(isLost: Boolean, category: String, description: String, secretDetails: String, imageUri: Uri?) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val locationToAdd = _temporaryMarkerLocation.value ?: _lastKnownLocation.value ?: return@launch
            var imageUrl: String? = null

            if (imageUri != null) {
                val imageRef = storage.reference.child("item_images/${UUID.randomUUID()}")
                imageRef.putFile(imageUri).await()
                imageUrl = imageRef.downloadUrl.await().toString()
            }

            val newItem = Item(
                id = UUID.randomUUID().toString(),
                userId = uid,
                lostStatus = isLost,
                category = category,
                description = description,
                secretDetails = secretDetails,
                imageUrl = imageUrl,
                location = GeoPoint(locationToAdd.latitude, locationToAdd.longitude),
                timestamp = com.google.firebase.Timestamp.now()
            )

            firestore.collection("items").document(newItem.id).set(newItem).await()

            if (!isLost) {
                val userRef = firestore.collection("users").document(uid)
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(userRef)
                    val newPoints = (snapshot.getLong("points") ?: 0) + 5
                    transaction.update(userRef, "points", newPoints)
                    null
                }.await()
            }
        }
    }

    fun listenForAllItems() {
        firestore.collection("items")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    return@addSnapshotListener
                }
                val itemList = snapshot.toObjects(Item::class.java)
                _items.value = itemList
                _filteredItems.value = itemList
            }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        checkLocationSetting()
        if (_isLocationEnabled.value && !_isUpdatingLocation.value) {
            _isUpdatingLocation.value = true
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    fun stopLocationUpdates() {
        _isUpdatingLocation.value = false
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    fun checkLocationSetting() {
        _isLocationEnabled.value = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}