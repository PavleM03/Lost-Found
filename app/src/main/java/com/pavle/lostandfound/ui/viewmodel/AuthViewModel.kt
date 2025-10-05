package com.pavle.lostandfound.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()

    private val _userLoggedIn = MutableStateFlow(auth.currentUser != null)

    private fun updateUserFcmToken(uid: String) {
        messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            val token = task.result
            firestore.collection("users").document(uid).update("fcmToken", token)
        }
    }

    fun login(email: String, lozinka: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, lozinka).await()
                result.user?.uid?.let { updateUserFcmToken(it) }
                _userLoggedIn.value = true
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun register(email: String, lozinka: String, ime: String, prezime: String, brojTelefona: String, slikaUri: Uri?, onResult: (Boolean, String?) -> Unit) {
        if (slikaUri == null) {
            onResult(false, "Molimo izaberite sliku.")
            return
        }
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, lozinka).await()
                val userId = authResult.user?.uid ?: throw IllegalStateException("Kreiranje korisnika nije uspelo.")

                val slikaRef = storage.reference.child("profile_images/$userId")
                slikaRef.putFile(slikaUri).await()
                val slikaUrl = slikaRef.downloadUrl.await().toString()

                val fcmToken = messaging.token.await()

                val korisnikData = hashMapOf(
                    "ime" to ime,
                    "prezime" to prezime,
                    "brojTelefona" to brojTelefona,
                    "email" to email,
                    "slikaUrl" to slikaUrl,
                    "points" to 0,
                    "fcmToken" to fcmToken
                )
                firestore.collection("users").document(userId).set(korisnikData).await()

                _userLoggedIn.value = true
                onResult(true, null)

            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}