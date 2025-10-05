package com.pavle.lostandfound.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.Timestamp

data class Item(
    val id: String = "",
    val userId: String = "",
    val lostStatus: Boolean = true,
    val category: String = "",
    val description: String = "",
    val secretDetails: String = "",
    val imageUrl: String? = null,
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val timestamp: Timestamp = Timestamp.now()
)