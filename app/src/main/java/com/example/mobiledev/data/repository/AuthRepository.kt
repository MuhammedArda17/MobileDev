package com.example.mobiledev.data.repository

import com.example.mobiledev.data.model.User
import com.example.mobiledev.data.model.UserRole
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

sealed class AuthError : Exception() {
    object InvalidCredentials : AuthError()
    object EmailInUse : AuthError()
    object WeakPassword : AuthError()
    object Network : AuthError()
    object UserNotFound : AuthError()
    object ProfileMissing : AuthError()
    object Unknown : AuthError()
}

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUserId: String? get() = auth.currentUser?.uid

    suspend fun register(name: String, email: String, password: String, role: UserRole): UserRole {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw AuthError.Unknown
            val user = User(uid = uid, name = name, email = email, role = role.value)
            db.collection("users").document(uid).set(user).await()
            return role
        } catch (e: Exception) {
            throw mapError(e)
        }
    }

    suspend fun login(email: String, password: String): UserRole {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw AuthError.Unknown
            return fetchRole(uid)
        } catch (e: Exception) {
            throw mapError(e)
        }
    }

    /** Wordt gebruikt bij het opstarten: ingelogd → meteen juiste startscherm (A2). */
    suspend fun getCurrentRole(): UserRole? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            fetchRole(uid)
        } catch (e: Exception) {
            throw mapError(e)
        }
    }

    suspend fun sendPasswordReset(email: String) {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            throw mapError(e)
        }
    }

    fun logout() = auth.signOut()

    private suspend fun fetchRole(uid: String): UserRole {
        val snapshot = db.collection("users").document(uid).get().await()
        return UserRole.fromValue(snapshot.getString("role")) ?: throw AuthError.ProfileMissing
    }

    private fun mapError(e: Exception): AuthError = when (e) {
        is AuthError -> e
        is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
        is FirebaseAuthInvalidUserException -> AuthError.InvalidCredentials // niet verklappen of e-mail bestaat
        is FirebaseAuthUserCollisionException -> AuthError.EmailInUse
        is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
        is FirebaseNetworkException -> AuthError.Network
        else -> AuthError.Unknown
    }
}