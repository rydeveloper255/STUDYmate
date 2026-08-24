package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.local.UserDao
import com.example.data.model.UserProfile
import com.example.data.persistence.PersistenceMonitor
import com.example.data.persistence.PersistenceStatus
import com.example.data.remote.supabase.SupabaseAuthManager
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseResult
import com.example.data.remote.supabase.SupabaseSyncService
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthRepository(
    private val context: Context,
    private val userDao: UserDao,
    val supabaseAuthManager: SupabaseAuthManager? = null,
    val supabaseClient: SupabaseClient? = null,
    val supabaseSyncService: SupabaseSyncService? = null
) {
    private val TAG = "AuthRepository"

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseAuth", e)
            null
        }
    }

    private val credentialManager = CredentialManager.create(context)

    init {
        try {
            firebaseAuth?.addAuthStateListener { auth ->
                val fbUser = auth.currentUser
                Log.d(TAG, "FirebaseAuth AuthStateListener triggered: currentUser = ${fbUser?.uid}, email = ${fbUser?.email}")
                if (fbUser != null) {
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        val localProfile = userDao.getUserProfileOnce()
                        if (localProfile == null || localProfile.uid != fbUser.uid || localProfile.email != (fbUser.email ?: "")) {
                            val profile = localProfile?.copy(
                                id = "current_user",
                                uid = fbUser.uid,
                                email = fbUser.email ?: localProfile.email,
                                name = if (localProfile.name.isNotBlank() && localProfile.name != "Student" && localProfile.name != "Guest Scholar") localProfile.name else (fbUser.displayName ?: "Student"),
                                photoUrl = fbUser.photoUrl?.toString() ?: localProfile.photoUrl,
                                isGuest = false,
                                isOnboardingCompleted = true
                            ) ?: UserProfile(
                                id = "current_user",
                                uid = fbUser.uid,
                                name = fbUser.displayName ?: "Student",
                                email = fbUser.email ?: "",
                                photoUrl = fbUser.photoUrl?.toString(),
                                isGuest = false,
                                isOnboardingCompleted = true
                            )
                            userDao.insertOrUpdateUserProfile(profile)
                            supabaseAuthManager?.associateFirebaseOrLocalUser(profile.uid, profile.email)
                            supabaseSyncService?.syncUserProfile(profile)
                            Log.d(TAG, "FirebaseAuth listener synced user profile into Room and Supabase: ${profile.email}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not attach FirebaseAuth state listener", e)
        }
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun getServerClientId(ctx: Context): String {
        val resId = ctx.resources.getIdentifier("default_web_client_id", "string", ctx.packageName)
        if (resId != 0) {
            val clientId = ctx.getString(resId)
            if (clientId.isNotBlank()) return clientId
        }
        return "902783555948-webclient.apps.googleusercontent.com"
    }

    suspend fun getInitialUser(): UserProfile? = withContext(Dispatchers.IO) {
        val localProfile = userDao.getUserProfileOnce()
        if (localProfile != null) {
            return@withContext localProfile
        }
        val firebaseUser = try { firebaseAuth?.currentUser } catch (e: Exception) { null }
        if (firebaseUser != null) {
            val profile = UserProfile(
                id = "current_user",
                uid = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Student",
                email = firebaseUser.email ?: "",
                photoUrl = firebaseUser.photoUrl?.toString(),
                isOnboardingCompleted = true
            )
            userDao.insertOrUpdateUserProfile(profile)
            return@withContext profile
        }
        null
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<UserProfile> = withContext(Dispatchers.Main) {
        val targetActivity = activityContext.findActivity() ?: activityContext
        val serverClientId = getServerClientId(targetActivity)
        Log.d(TAG, "Initiating Google Sign-In with server client id: $serverClientId")

        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = try {
                credentialManager.getCredential(targetActivity, request)
            } catch (getEx: GetCredentialException) {
                // If GetGoogleIdOption failed, attempt fallback with GetSignInWithGoogleOption
                Log.w(TAG, "GetGoogleIdOption failed: ${getEx.message}, trying GetSignInWithGoogleOption fallback")
                val fallbackOption = try {
                    GetSignInWithGoogleOption.Builder(serverClientId).build()
                } catch (e: Exception) {
                    throw getEx
                }
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption)
                    .build()
                credentialManager.getCredential(targetActivity, fallbackRequest)
            }

            val credential = result.credential

            var googleIdToken: String? = null
            var googleEmail: String = ""
            var googleDisplayName: String? = null
            var googlePhotoUri: String? = null

            if (credential is CustomCredential) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    googleIdToken = googleIdTokenCredential.idToken
                    googleEmail = googleIdTokenCredential.id
                    googleDisplayName = googleIdTokenCredential.displayName
                    googlePhotoUri = googleIdTokenCredential.profilePictureUri?.toString()
                } catch (e: Exception) {
                    Log.w(TAG, "GoogleIdTokenCredential.createFrom failed, attempting direct bundle extraction", e)
                }

                // Fallback direct bundle extraction
                val data = credential.data
                if (googleIdToken.isNullOrBlank()) {
                    googleIdToken = data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                        ?: data.getString("idToken")
                }
                if (googleEmail.isBlank()) {
                    googleEmail = data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID")
                        ?: data.getString("id") ?: ""
                }
                if (googleDisplayName.isNullOrBlank()) {
                    googleDisplayName = data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_DISPLAY_NAME")
                        ?: data.getString("displayName")
                }
                if (googlePhotoUri.isNullOrBlank()) {
                    googlePhotoUri = data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_PROFILE_PICTURE_URI")
                        ?: data.getString("profilePictureUri")
                }
            }

            // Extract JWT payload from ID Token if available
            if (!googleIdToken.isNullOrBlank()) {
                try {
                    val parts = googleIdToken.split(".")
                    if (parts.size >= 2) {
                        val payloadJson = String(
                            android.util.Base64.decode(
                                parts[1],
                                android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
                            )
                        )
                        val jsonObj = org.json.JSONObject(payloadJson)
                        if (googleEmail.isBlank() || !googleEmail.contains("@")) {
                            val jwtEmail = jsonObj.optString("email", "")
                            if (jwtEmail.isNotBlank()) googleEmail = jwtEmail
                        }
                        if (googleDisplayName.isNullOrBlank()) {
                            val name = jsonObj.optString("name", "")
                            if (name.isNotBlank()) googleDisplayName = name
                        }
                        if (googlePhotoUri.isNullOrBlank()) {
                            val pic = jsonObj.optString("picture", "")
                            if (pic.isNotBlank()) googlePhotoUri = pic
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing JWT payload from ID token", e)
                }
            }

            if (googleEmail.isBlank()) {
                googleEmail = if (!googleIdToken.isNullOrBlank()) {
                    "user_${googleIdToken.hashCode()}@gmail.com"
                } else {
                    "google_user@studymate.local"
                }
            }

            // Authenticate with Firebase Authentication on IO thread
            val profile = withContext(Dispatchers.IO) {
                var finalUid = ""
                var finalEmail = googleEmail
                var finalDisplayName = googleDisplayName ?: "Student"
                var finalPhotoUrl = googlePhotoUri

                val auth = firebaseAuth
                if (auth != null && !googleIdToken.isNullOrBlank()) {
                    try {
                        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        val authResult = auth.signInWithCredential(authCredential).await()
                        val fbUser = authResult.user
                        if (fbUser != null) {
                            finalUid = fbUser.uid
                            finalEmail = fbUser.email ?: finalEmail
                            finalDisplayName = fbUser.displayName ?: finalDisplayName
                            finalPhotoUrl = fbUser.photoUrl?.toString() ?: finalPhotoUrl
                        }
                    } catch (fbEx: Exception) {
                        Log.w(TAG, "Firebase sign-in failed, proceeding with Google Id token info: ${fbEx.message}")
                        finalUid = "google_${googleEmail.hashCode()}"
                    }
                } else {
                    finalUid = "google_${googleEmail.hashCode()}"
                }

                val existingProfile = userDao.getUserProfileOnce()
                val userProfile = if (existingProfile != null) {
                    existingProfile.copy(
                        id = "current_user",
                        uid = finalUid,
                        email = finalEmail,
                        name = if (existingProfile.name.isNotBlank() && existingProfile.name != "Student" && existingProfile.name != "Guest Scholar") existingProfile.name else finalDisplayName,
                        photoUrl = finalPhotoUrl ?: existingProfile.photoUrl,
                        isGuest = false,
                        isOnboardingCompleted = true
                    )
                } else {
                    UserProfile(
                        id = "current_user",
                        uid = finalUid,
                        name = finalDisplayName,
                        email = finalEmail,
                        photoUrl = finalPhotoUrl,
                        isGuest = false,
                        isOnboardingCompleted = true
                    )
                }

                userDao.insertOrUpdateUserProfile(userProfile)
                supabaseAuthManager?.associateFirebaseOrLocalUser(userProfile.uid, userProfile.email)
                supabaseSyncService?.syncUserProfile(userProfile)
                supabaseSyncService?.fullCloudRestore()
                userProfile
            }

            Log.d(TAG, "Google Sign-In completed successfully for ${profile.email}")
            Result.success(profile)

        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In cancelled by user")
            Result.failure(Exception("Google Sign-In was cancelled."))
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No credential returned from Credential Manager", e)
            Result.failure(Exception("No Google account selected. Please choose a Google account to continue."))
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(TAG, "Google Sign-In configuration error", e)
            Result.failure(Exception("Google Play Services configuration issue. Please update Google Play Services and try again."))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error", e)
            val msg = when {
                e.message?.contains("16:") == true -> "Google Sign-In was interrupted or cancelled."
                e.message?.contains("NETWORK", ignoreCase = true) == true -> "Network error connecting to Google services. Please check your internet."
                else -> e.localizedMessage ?: "Could not complete Google Sign-In. Please try again."
            }
            Result.failure(Exception(msg))
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Firebase auth error", e)
            Result.failure(Exception("Firebase authentication error: ${e.localizedMessage ?: "Please try again"}"))
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in error", e)
            val msg = when {
                e.message?.contains("16:") == true -> "Google Sign-In was interrupted or cancelled."
                e.message?.contains("NETWORK", ignoreCase = true) == true -> "Network connection issue. Please check your internet."
                else -> e.message ?: "Sign-in failed. Please try again."
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        pass: String,
        displayName: String = "",
        examName: String = "RRB Group D"
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(java.util.Locale.ROOT)
        if (normalizedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (pass.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        try {
            var supabaseUid = ""
            if (supabaseClient?.isReady() == true) {
                when (val signUpRes = supabaseClient.signUp(normalizedEmail, pass, mapOf("full_name" to displayName))) {
                    is SupabaseResult.Success -> {
                        val session = signUpRes.data
                        if (!session.accessToken.isNullOrBlank() && session.user != null) {
                            supabaseUid = session.user.id
                            supabaseAuthManager?.saveSession(
                                accessToken = session.accessToken,
                                refreshToken = session.refreshToken,
                                userId = session.user.id,
                                email = session.user.email ?: normalizedEmail,
                                expiresInSeconds = session.expiresIn ?: 3600L
                            )
                        } else if (session.user != null) {
                            supabaseUid = session.user.id
                        }
                    }
                    is SupabaseResult.Error -> {
                        val err = signUpRes.message
                        if (err.contains("already registered", ignoreCase = true) ||
                            err.contains("already exists", ignoreCase = true) ||
                            signUpRes.code == 422
                        ) {
                            PersistenceMonitor.log("AUTH_SIGNUP", "auth.users", normalizedEmail, normalizedEmail, "FAILED", "ALREADY_EXISTS", err)
                            return@withContext Result.failure(IllegalStateException("An account with this email already exists. Please log in instead."))
                        }
                        PersistenceMonitor.log("AUTH_SIGNUP", "auth.users", normalizedEmail, normalizedEmail, "FAILED", signUpRes.code?.toString(), err)
                        return@withContext Result.failure(Exception("Sign-up failed: $err"))
                    }
                }
            }

            val uidToUse = if (supabaseUid.isNotBlank()) supabaseUid else "usr_${Math.abs(normalizedEmail.hashCode())}"
            val nameToUse = if (displayName.isNotBlank()) displayName else normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

            val profile = UserProfile(
                id = "current_user",
                uid = uidToUse,
                name = nameToUse,
                email = normalizedEmail,
                examName = examName,
                isGuest = false,
                isOnboardingCompleted = false
            )

            val localSaved = try {
                userDao.insertOrUpdateUserProfile(profile)
                true
            } catch (e: Exception) {
                false
            }

            if (!localSaved) {
                PersistenceMonitor.log("PROFILE_CREATE", "user_profile", uidToUse, uidToUse, "FAILED", details = "Room DB insert error")
                return@withContext Result.failure(Exception("Account created, profile setup incomplete. Your account was created, but profile data could not be saved."))
            }

            supabaseAuthManager?.associateFirebaseOrLocalUser(profile.uid, profile.email)
            supabaseSyncService?.syncUserProfile(profile)

            PersistenceMonitor.log("AUTH_SIGNUP", "profiles", uidToUse, uidToUse, "SUCCESS")
            Result.success(profile)
        } catch (e: Exception) {
            PersistenceMonitor.log("AUTH_SIGNUP", "profiles", normalizedEmail, "unknown", "FAILED", details = e.message)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(java.util.Locale.ROOT)
        if (normalizedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (pass.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        try {
            var supabaseUid = ""
            if (supabaseClient?.isReady() == true) {
                when (val signInRes = supabaseClient.signInWithPassword(normalizedEmail, pass)) {
                    is SupabaseResult.Success -> {
                        val session = signInRes.data
                        if (!session.accessToken.isNullOrBlank() && session.user != null) {
                            supabaseUid = session.user.id
                            supabaseAuthManager?.saveSession(
                                accessToken = session.accessToken,
                                refreshToken = session.refreshToken,
                                userId = session.user.id,
                                email = session.user.email ?: normalizedEmail,
                                expiresInSeconds = session.expiresIn ?: 3600L
                            )
                        }
                    }
                    is SupabaseResult.Error -> {
                        val err = signInRes.message
                        PersistenceMonitor.log("AUTH_SIGNIN", "auth.users", normalizedEmail, normalizedEmail, "FAILED", signInRes.code?.toString(), err)
                        if (err.contains("invalid", ignoreCase = true) || signInRes.code == 400) {
                            return@withContext Result.failure(Exception("Invalid email or password. Please try again."))
                        }
                        return@withContext Result.failure(Exception(err))
                    }
                }
            }

            val uidToUse = if (supabaseUid.isNotBlank()) supabaseUid else "usr_${Math.abs(normalizedEmail.hashCode())}"
            val existing = userDao.getUserProfileOnce()
            val profile = UserProfile(
                id = "current_user",
                uid = uidToUse,
                name = existing?.name ?: normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = normalizedEmail,
                isGuest = false,
                isOnboardingCompleted = existing?.isOnboardingCompleted ?: false,
                examName = existing?.examName ?: "RRB Group D"
            )

            userDao.insertOrUpdateUserProfile(profile)
            supabaseAuthManager?.associateFirebaseOrLocalUser(profile.uid, profile.email)
            supabaseSyncService?.syncUserProfile(profile)
            supabaseSyncService?.fullCloudRestore()

            PersistenceMonitor.log("AUTH_SIGNIN", "profiles", uidToUse, uidToUse, "SUCCESS")
            Result.success(profile)
        } catch (e: Exception) {
            PersistenceMonitor.log("AUTH_SIGNIN", "profiles", normalizedEmail, "unknown", "FAILED", details = e.message)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(java.util.Locale.ROOT)
        if (normalizedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }

        try {
            if (supabaseClient?.isReady() == true) {
                when (val res = supabaseClient.recoverPasswordForEmail(normalizedEmail)) {
                    is SupabaseResult.Success -> {
                        PersistenceMonitor.log("AUTH_PASSWORD_RESET", "auth.users", normalizedEmail, normalizedEmail, "SUCCESS")
                        Result.success(Unit)
                    }
                    is SupabaseResult.Error -> {
                        PersistenceMonitor.log("AUTH_PASSWORD_RESET", "auth.users", normalizedEmail, normalizedEmail, "FAILED", res.code?.toString(), res.message)
                        Result.failure(Exception(res.message))
                    }
                }
            } else {
                PersistenceMonitor.log("AUTH_PASSWORD_RESET", "auth.users", normalizedEmail, normalizedEmail, "SUCCESS_LOCAL")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun continueAsGuest(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val guestProfile = UserProfile(
                id = "current_user",
                uid = "guest_uid",
                name = "Guest Scholar",
                email = "guest@studymate.local",
                isGuest = true,
                isOnboardingCompleted = true,
                grade = "Class 12",
                subjects = listOf("Mathematics", "Physics", "Chemistry"),
                goal = "Competitive Exam",
                examName = "Final Board & Entrance",
                dailyTargetMinutes = 180,
                preferredStudyTime = "Evening",
                notificationsEnabled = true,
                xp = 350,
                level = 2,
                streakDays = 4,
                totalFocusMinutes = 135,
                totalQuestionsSolved = 48
            )
            userDao.insertOrUpdateUserProfile(guestProfile)
            supabaseAuthManager?.associateFirebaseOrLocalUser(guestProfile.uid, guestProfile.email)
            supabaseSyncService?.syncUserProfile(guestProfile)
            Result.success(guestProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(updatedProfile: UserProfile): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val currentProfile = userDao.getUserProfileOnce()
            val finalProfile = updatedProfile.copy(
                id = "current_user",
                uid = currentProfile?.uid ?: updatedProfile.uid,
                email = currentProfile?.email ?: updatedProfile.email,
                photoUrl = updatedProfile.photoUrl ?: currentProfile?.photoUrl,
                isOnboardingCompleted = true
            )
            userDao.insertOrUpdateUserProfile(finalProfile)
            supabaseSyncService?.syncUserProfile(finalProfile)
            Result.success(finalProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (newPassword.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        }
        try {
            val token = supabaseAuthManager?.getAccessToken()
            if (supabaseClient?.isReady() == true && !token.isNullOrBlank()) {
                when (val res = supabaseClient.updateUser(accessToken = token, password = newPassword)) {
                    is SupabaseResult.Success -> {
                        PersistenceMonitor.log("AUTH_PASSWORD_CHANGE", "auth.users", "current_user", "current_user", "SUCCESS")
                        Result.success(Unit)
                    }
                    is SupabaseResult.Error -> {
                        PersistenceMonitor.log("AUTH_PASSWORD_CHANGE", "auth.users", "current_user", "current_user", "FAILED", res.code?.toString(), res.message)
                        Result.failure(Exception(res.message))
                    }
                }
            } else {
                // If offline / local fallback
                PersistenceMonitor.log("AUTH_PASSWORD_CHANGE", "auth.users", "current_user", "current_user", "SUCCESS_LOCAL")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestEmailChange(newEmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        val normalizedEmail = newEmail.trim().lowercase(java.util.Locale.ROOT)
        if (normalizedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        try {
            val token = supabaseAuthManager?.getAccessToken()
            if (supabaseClient?.isReady() == true && !token.isNullOrBlank()) {
                when (val res = supabaseClient.updateUser(accessToken = token, email = normalizedEmail)) {
                    is SupabaseResult.Success -> {
                        PersistenceMonitor.log("AUTH_EMAIL_CHANGE", "auth.users", "current_user", normalizedEmail, "SUCCESS")
                        Result.success(Unit)
                    }
                    is SupabaseResult.Error -> {
                        PersistenceMonitor.log("AUTH_EMAIL_CHANGE", "auth.users", "current_user", normalizedEmail, "FAILED", res.code?.toString(), res.message)
                        Result.failure(Exception(res.message))
                    }
                }
            } else {
                Result.failure(IllegalStateException("Active authenticated session is required to change email."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeOnboarding(updatedProfile: UserProfile): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val currentProfile = userDao.getUserProfileOnce()
            val finishedProfile = updatedProfile.copy(
                id = "current_user",
                uid = currentProfile?.uid ?: updatedProfile.uid,
                email = currentProfile?.email ?: updatedProfile.email,
                photoUrl = currentProfile?.photoUrl ?: updatedProfile.photoUrl,
                isOnboardingCompleted = true
            )
            userDao.insertOrUpdateUserProfile(finishedProfile)
            supabaseSyncService?.syncUserProfile(finishedProfile)
            Result.success(finishedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(activityContext: Context? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            try {
                firebaseAuth?.signOut()
            } catch (ignored: Exception) {
                Log.e(TAG, "Error signing out of Firebase", ignored)
            }

            try {
                val mgr = if (activityContext != null) CredentialManager.create(activityContext) else credentialManager
                mgr.clearCredentialState(ClearCredentialStateRequest())
            } catch (ignored: Exception) {
                Log.e(TAG, "Error clearing credential state", ignored)
            }

            supabaseAuthManager?.clearSession()
            userDao.clearUser()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = supabaseAuthManager?.getAccessToken()
            if (supabaseClient?.isReady() == true && !token.isNullOrBlank()) {
                try {
                    supabaseClient.deleteUser(token)
                } catch (ignored: Exception) {}
            }
            try {
                firebaseAuth?.currentUser?.delete()?.await()
            } catch (ignored: Exception) {}
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (ignored: Exception) {}
            supabaseAuthManager?.clearSession()
            userDao.clearUser()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
