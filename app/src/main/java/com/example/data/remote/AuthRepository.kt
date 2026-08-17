package com.example.data.remote

import android.content.Context
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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
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
    private val userDao: UserDao
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

    suspend fun signInWithGoogle(activityContext: Context): Result<UserProfile> = withContext(Dispatchers.IO) {
        val serverClientId = getServerClientId(activityContext)
        Log.d(TAG, "Initiating Google Sign-In with server client id: $serverClientId")

        try {
            val signInOption = try {
                GetSignInWithGoogleOption.Builder(serverClientId).build()
            } catch (e: Exception) {
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            }

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = try {
                credentialManager.getCredential(activityContext, request)
            } catch (noCredEx: NoCredentialException) {
                // Fallback to GetGoogleIdOption if GetSignInWithGoogleOption failed with NoCredentialException
                if (signInOption is GetSignInWithGoogleOption) {
                    val fallbackOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(serverClientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(fallbackOption)
                        .build()
                    credentialManager.getCredential(activityContext, fallbackRequest)
                } else {
                    throw noCredEx
                }
            }
            val credential = result.credential

            var googleIdToken: String? = null
            var googleEmail: String = ""
            var googleDisplayName: String? = null
            var googlePhotoUri: String? = null

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                googleIdToken = googleIdTokenCredential.idToken
                googleEmail = googleIdTokenCredential.id
                googleDisplayName = googleIdTokenCredential.displayName
                googlePhotoUri = googleIdTokenCredential.profilePictureUri?.toString()
            }

            if (googleIdToken.isNullOrBlank() && googleEmail.isBlank()) {
                return@withContext Result.failure(Exception("Unable to retrieve Google credential. Please try again."))
            }

            // Authenticate with Firebase Authentication if available
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

            // Check if user has previously completed onboarding
            val existingProfile = userDao.getUserProfileOnce()
            val isAlreadyOnboarded = existingProfile != null &&
                    existingProfile.isOnboardingCompleted &&
                    (existingProfile.uid == finalUid || existingProfile.email == finalEmail)

            val profile = if (existingProfile != null && isAlreadyOnboarded) {
                existingProfile.copy(
                    id = "current_user",
                    uid = finalUid,
                    email = finalEmail,
                    name = if (existingProfile.name.isNotBlank() && existingProfile.name != "Student") existingProfile.name else finalDisplayName,
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
                    isOnboardingCompleted = false
                )
            }

            userDao.insertOrUpdateUserProfile(profile)
            Result.success(profile)

        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In cancelled by user")
            Result.failure(Exception("Sign-in cancelled."))
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No credential returned from Credential Manager", e)
            Result.failure(Exception(e.localizedMessage ?: "Google Sign-In failed to retrieve credentials. Please try again."))
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(TAG, "Google Sign-In configuration error", e)
            Result.failure(Exception("Google Sign-In configuration error. Please check Google Play Services."))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error", e)
            Result.failure(Exception("Could not complete Google Sign-In: ${e.message}"))
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Firebase auth error", e)
            Result.failure(Exception("Authentication failed with Firebase: ${e.localizedMessage ?: "Please try again"}"))
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in error", e)
            Result.failure(Exception(e.message ?: "Sign-in failed. Please try again."))
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            if (email.isBlank() || pass.length < 6) {
                return@withContext Result.failure(IllegalArgumentException("Please enter a valid email and at least 6 characters password."))
            }
            val auth = firebaseAuth
            if (auth != null) {
                try {
                    val authResult = auth.signInWithEmailAndPassword(email, pass).await()
                    val user = authResult.user
                    val existing = userDao.getUserProfileOnce()
                    val profile = UserProfile(
                        id = "current_user",
                        uid = user?.uid ?: "",
                        name = user?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() },
                        email = email,
                        isGuest = false,
                        isOnboardingCompleted = existing?.isOnboardingCompleted == true
                    )
                    userDao.insertOrUpdateUserProfile(profile)
                    return@withContext Result.success(profile)
                } catch (e: Exception) {
                    try {
                        val createResult = auth.createUserWithEmailAndPassword(email, pass).await()
                        val user = createResult.user
                        val profile = UserProfile(
                            id = "current_user",
                            uid = user?.uid ?: "",
                            name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                            email = email,
                            isGuest = false,
                            isOnboardingCompleted = false
                        )
                        userDao.insertOrUpdateUserProfile(profile)
                        return@withContext Result.success(profile)
                    } catch (ce: Exception) {
                        return@withContext Result.failure(Exception("Authentication failed: ${ce.localizedMessage ?: ce.message}"))
                    }
                }
            } else {
                val profile = UserProfile(
                    id = "current_user",
                    name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = email,
                    isGuest = false,
                    isOnboardingCompleted = false
                )
                userDao.insertOrUpdateUserProfile(profile)
                Result.success(profile)
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
            Result.success(finalProfile)
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

            userDao.clearUser()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            try {
                firebaseAuth?.currentUser?.delete()?.await()
            } catch (ignored: Exception) {}
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (ignored: Exception) {}
            userDao.clearUser()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
