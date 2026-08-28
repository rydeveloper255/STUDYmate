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

data class AuthSignUpResult(
    val isVerificationRequired: Boolean,
    val email: String,
    val fullName: String = "",
    val mobileNumber: String = "",
    val user: UserProfile? = null
)

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
        val storedUserId = supabaseAuthManager?.getStoredUserId()
        val storedEmail = supabaseAuthManager?.getUserEmail()
        if (!storedUserId.isNullOrBlank() && !storedEmail.isNullOrBlank() && storedUserId != "guest_uid") {
            val profile = UserProfile(
                id = "current_user",
                uid = storedUserId,
                name = storedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = storedEmail,
                isOnboardingCompleted = false
            )
            userDao.insertOrUpdateUserProfile(profile)
            return@withContext profile
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

    suspend fun signUpUser(
        fullName: String,
        email: String,
        mobileNumber: String,
        pass: String,
        confirmPass: String
    ): Result<AuthSignUpResult> = withContext(Dispatchers.IO) {
        val trimmedName = fullName.trim()
        val normalizedEmail = email.trim().lowercase(java.util.Locale.ROOT)
        val cleanPhone = mobileNumber.filter { it.isDigit() }

        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Kripya apna pura naam darj karein."))
        }
        if (normalizedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Kripya ek valid email address enter karein."))
        }
        if (cleanPhone.length < 10) {
            return@withContext Result.failure(IllegalArgumentException("Kripya valid 10-digit mobile number enter karein."))
        }
        if (pass.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password kam se kam 6 characters ka hona chahiye."))
        }
        if (pass != confirmPass) {
            return@withContext Result.failure(IllegalArgumentException("Password aur Confirm Password match nahi kar rahe hain."))
        }

        try {
            if (supabaseClient?.isReady() == true) {
                val metadata = mapOf(
                    "full_name" to trimmedName,
                    "phone" to cleanPhone,
                    "mobile_number" to cleanPhone
                )
                when (val signUpRes = supabaseClient.signUp(normalizedEmail, pass, metadata)) {
                    is SupabaseResult.Success -> {
                        val session = signUpRes.data
                        if (!session.accessToken.isNullOrBlank() && session.user != null) {
                            supabaseAuthManager?.saveSession(
                                accessToken = session.accessToken,
                                refreshToken = session.refreshToken,
                                userId = session.user.id,
                                email = session.user.email ?: normalizedEmail,
                                expiresInSeconds = session.expiresIn ?: 3600L
                            )
                        }
                        PersistenceMonitor.log("AUTH_SIGNUP_OTP", "auth.users", normalizedEmail, normalizedEmail, "SUCCESS")
                        return@withContext Result.success(
                            AuthSignUpResult(
                                isVerificationRequired = true,
                                email = normalizedEmail,
                                fullName = trimmedName,
                                mobileNumber = cleanPhone
                            )
                        )
                    }
                    is SupabaseResult.Error -> {
                        val err = signUpRes.message
                        if (err.contains("already registered", ignoreCase = true) ||
                            err.contains("already exists", ignoreCase = true) ||
                            signUpRes.code == 422
                        ) {
                            PersistenceMonitor.log("AUTH_SIGNUP", "auth.users", normalizedEmail, normalizedEmail, "FAILED", "ALREADY_EXISTS", err)
                            return@withContext Result.failure(IllegalStateException("Yeh email pehle se registered hai. Kripya Log In karein."))
                        }
                        if (err.contains("rate limit", ignoreCase = true) || signUpRes.code == 429) {
                            return@withContext Result.failure(IllegalStateException("Email rate limit reached. Kripya 2 minute baad koshish karein."))
                        }
                        PersistenceMonitor.log("AUTH_SIGNUP", "auth.users", normalizedEmail, normalizedEmail, "FAILED", signUpRes.code?.toString(), err)
                        return@withContext Result.failure(Exception("Sign-up failed: $err"))
                    }
                }
            }

            // Fallback if Supabase is offline / local mode
            val localUid = "usr_${Math.abs(normalizedEmail.hashCode())}"
            val profile = UserProfile(
                id = "current_user",
                uid = localUid,
                name = trimmedName,
                email = normalizedEmail,
                isGuest = false,
                isOnboardingCompleted = false
            )
            userDao.insertOrUpdateUserProfile(profile)
            Result.success(
                AuthSignUpResult(
                    isVerificationRequired = true,
                    email = normalizedEmail,
                    fullName = trimmedName,
                    mobileNumber = cleanPhone,
                    user = profile
                )
            )
        } catch (e: Exception) {
            PersistenceMonitor.log("AUTH_SIGNUP", "profiles", normalizedEmail, "unknown", "FAILED", details = e.message)
            Result.failure(e)
        }
    }

    suspend fun verifyEmailOtp(
        email: String,
        otp: String,
        fullName: String = "",
        mobileNumber: String = ""
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(java.util.Locale.ROOT)
        val cleanOtp = otp.trim()

        if (cleanOtp.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Kripya valid OTP darj karein."))
        }

        try {
            var userId = ""
            if (supabaseClient?.isReady() == true) {
                when (val verifyRes = supabaseClient.verifyOtp(normalizedEmail, cleanOtp, type = "signup")) {
                    is SupabaseResult.Success -> {
                        val session = verifyRes.data
                        if (!session.accessToken.isNullOrBlank() && session.user != null) {
                            userId = session.user.id
                            supabaseAuthManager?.saveSession(
                                accessToken = session.accessToken,
                                refreshToken = session.refreshToken,
                                userId = session.user.id,
                                email = session.user.email ?: normalizedEmail,
                                expiresInSeconds = session.expiresIn ?: 3600L
                            )
                        } else if (session.user != null) {
                            userId = session.user.id
                        }
                    }
                    is SupabaseResult.Error -> {
                        val err = verifyRes.message
                        Log.e(TAG, "OTP verification error: $err (code: ${verifyRes.code})")
                        val friendlyErr = when {
                            err.contains("expired", ignoreCase = true) || err.contains("invalid", ignoreCase = true) || verifyRes.code in 400..422 ->
                                "OTP galat hai ya expire ho chuka hai. Kripya naya OTP request karein."
                            err.contains("rate limit", ignoreCase = true) || verifyRes.code == 429 ->
                                "Too many attempts. Kripya thodi der baad koshish karein."
                            else -> err
                        }
                        PersistenceMonitor.log("AUTH_OTP_VERIFY", "auth.users", normalizedEmail, normalizedEmail, "FAILED", verifyRes.code?.toString(), friendlyErr)
                        return@withContext Result.failure(Exception(friendlyErr))
                    }
                }
            }

            val uidToUse = if (userId.isNotBlank()) userId else "usr_${Math.abs(normalizedEmail.hashCode())}"
            supabaseAuthManager?.associateFirebaseOrLocalUser(uidToUse, normalizedEmail)

            // Try restoring profile from Supabase first
            try {
                supabaseSyncService?.fullCloudRestore()
            } catch (ignored: Exception) {}

            val existing = userDao.getUserProfileOnce()
            val nameToUse = when {
                fullName.isNotBlank() -> fullName
                existing != null && existing.name.isNotBlank() && existing.name != "Student" -> existing.name
                else -> normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            }

            val profile = if (existing != null && existing.isOnboardingCompleted) {
                existing.copy(
                    id = "current_user",
                    uid = uidToUse,
                    email = normalizedEmail,
                    name = nameToUse,
                    isGuest = false,
                    isOnboardingCompleted = true
                )
            } else {
                UserProfile(
                    id = "current_user",
                    uid = uidToUse,
                    name = nameToUse,
                    email = normalizedEmail,
                    isGuest = false,
                    isOnboardingCompleted = false,
                    examName = "RRB Group D"
                )
            }

            userDao.insertOrUpdateUserProfile(profile)
            supabaseSyncService?.syncUserProfile(profile)

            PersistenceMonitor.log("AUTH_OTP_VERIFY", "profiles", uidToUse, uidToUse, "SUCCESS")
            Result.success(profile)
        } catch (e: Exception) {
            PersistenceMonitor.log("AUTH_OTP_VERIFY", "profiles", normalizedEmail, "unknown", "FAILED", details = e.message)
            Result.failure(e)
        }
    }

    suspend fun resendEmailOtp(email: String, type: String = "signup"): Result<Unit> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(java.util.Locale.ROOT)
        if (normalizedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Kripya ek valid email address enter karein."))
        }

        try {
            if (supabaseClient?.isReady() == true) {
                when (val res = supabaseClient.resendOtp(normalizedEmail, type)) {
                    is SupabaseResult.Success -> {
                        PersistenceMonitor.log("AUTH_OTP_RESEND", "auth.users", normalizedEmail, normalizedEmail, "SUCCESS")
                        Result.success(Unit)
                    }
                    is SupabaseResult.Error -> {
                        val err = res.message
                        if (err.contains("rate limit", ignoreCase = true) || res.code == 429) {
                            return@withContext Result.failure(Exception("Resend limit reached. Kripya 2 minute countdown ke baad dobara try karein."))
                        }
                        PersistenceMonitor.log("AUTH_OTP_RESEND", "auth.users", normalizedEmail, normalizedEmail, "FAILED", res.code?.toString(), err)
                        Result.failure(Exception(err))
                    }
                }
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailOrPhone(identifier: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val trimmed = identifier.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Kripya Email ya Mobile Number enter karein."))
        }
        if (pass.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password kam se kam 6 characters ka hona chahiye."))
        }

        val isEmail = trimmed.contains("@")
        val emailToUse = if (isEmail) {
            trimmed.lowercase(java.util.Locale.ROOT)
        } else {
            // Check if phone matches any local or remote profile
            val cleanPhone = trimmed.filter { it.isDigit() }
            val existing = userDao.getUserProfileOnce()
            if (existing != null && existing.email.isNotBlank()) {
                existing.email
            } else {
                // If pure digits, attempt to search or format
                return@withContext Result.failure(Exception("Mobile login ke liye kripya apna registered email use karein ya naya account banayein."))
            }
        }

        signInWithEmail(emailToUse, pass)
    }

    suspend fun sendPasswordRecoveryOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(java.util.Locale.ROOT)
        if (normalizedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Kripya ek valid email address enter karein."))
        }

        try {
            if (supabaseClient?.isReady() == true) {
                when (val res = supabaseClient.recoverPasswordForEmail(normalizedEmail)) {
                    is SupabaseResult.Success -> {
                        PersistenceMonitor.log("AUTH_PASSWORD_RECOVER", "auth.users", normalizedEmail, normalizedEmail, "SUCCESS")
                        Result.success(Unit)
                    }
                    is SupabaseResult.Error -> {
                        val err = res.message
                        if (err.contains("rate limit", ignoreCase = true) || res.code == 429) {
                            return@withContext Result.failure(Exception("Rate limit reached. Kripya 2 minute wait karein."))
                        }
                        PersistenceMonitor.log("AUTH_PASSWORD_RECOVER", "auth.users", normalizedEmail, normalizedEmail, "FAILED", res.code?.toString(), err)
                        Result.failure(Exception(err))
                    }
                }
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPasswordRecoveryOtp(email: String, otp: String): Result<String> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(java.util.Locale.ROOT)
        val cleanOtp = otp.trim()

        if (cleanOtp.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Kripya valid OTP darj karein."))
        }

        try {
            if (supabaseClient?.isReady() == true) {
                when (val res = supabaseClient.verifyOtp(normalizedEmail, cleanOtp, type = "recovery")) {
                    is SupabaseResult.Success -> {
                        val token = res.data.accessToken ?: ""
                        if (token.isNotBlank()) {
                            supabaseAuthManager?.saveSession(
                                accessToken = token,
                                refreshToken = res.data.refreshToken,
                                userId = res.data.user?.id ?: "",
                                email = normalizedEmail,
                                expiresInSeconds = res.data.expiresIn ?: 3600L
                            )
                        }
                        Result.success(token)
                    }
                    is SupabaseResult.Error -> {
                        val err = res.message
                        val friendlyErr = when {
                            err.contains("expired", ignoreCase = true) || err.contains("invalid", ignoreCase = true) || res.code in 400..422 ->
                                "OTP galat hai ya expire ho chuka hai. Kripya naya OTP mangwayein."
                            err.contains("rate limit", ignoreCase = true) || res.code == 429 ->
                                "Too many attempts. Kripya thodi der baad dobara koshish karein."
                            else -> err
                        }
                        Result.failure(Exception(friendlyErr))
                    }
                }
            } else {
                Result.success("mock_recovery_token")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPasswordWithToken(accessToken: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (newPassword.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Naya password kam se kam 6 characters ka hona chahiye."))
        }

        try {
            val tokenToUse = if (accessToken.isNotBlank()) accessToken else supabaseAuthManager?.getAccessToken() ?: ""
            if (supabaseClient?.isReady() == true && tokenToUse.isNotBlank()) {
                when (val res = supabaseClient.updateUser(accessToken = tokenToUse, password = newPassword)) {
                    is SupabaseResult.Success -> {
                        PersistenceMonitor.log("AUTH_PASSWORD_RESET", "auth.users", "current_user", "current_user", "SUCCESS")
                        Result.success(Unit)
                    }
                    is SupabaseResult.Error -> {
                        Result.failure(Exception(res.message))
                    }
                }
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        pass: String,
        displayName: String = "",
        examName: String = "RRB Group D"
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val res = signUpUser(
            fullName = displayName.ifBlank { "Student" },
            email = email,
            mobileNumber = "9999999999",
            pass = pass,
            confirmPass = pass
        )
        res.map {
            it.user ?: UserProfile(
                id = "current_user",
                uid = "usr_${Math.abs(email.hashCode())}",
                name = displayName.ifBlank { "Student" },
                email = email.trim().lowercase(java.util.Locale.ROOT),
                examName = examName,
                isGuest = false,
                isOnboardingCompleted = true
            )
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(java.util.Locale.ROOT)
        if (normalizedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Kripya ek valid email address enter karein."))
        }
        if (pass.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password kam se kam 6 characters ka hona chahiye."))
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
                            return@withContext Result.failure(Exception("Email/Password galat hai. Kripya dobara check karein."))
                        }
                        if (err.contains("confirm", ignoreCase = true) || err.contains("not verified", ignoreCase = true)) {
                            return@withContext Result.failure(Exception("Aapka email verify nahi hua hai. Kripya pehle OTP verify karein."))
                        }
                        return@withContext Result.failure(Exception(err))
                    }
                }
            }

            val uidToUse = if (supabaseUid.isNotBlank()) supabaseUid else "usr_${Math.abs(normalizedEmail.hashCode())}"
            supabaseAuthManager?.associateFirebaseOrLocalUser(uidToUse, normalizedEmail)

            // Restore remote profile first if available
            try {
                supabaseSyncService?.fullCloudRestore()
            } catch (ignored: Exception) {
                Log.w(TAG, "Initial cloud restore skipped or failed during sign-in", ignored)
            }

            val existing = userDao.getUserProfileOnce()
            val profile = if (existing != null) {
                existing.copy(
                    id = "current_user",
                    uid = uidToUse,
                    email = normalizedEmail,
                    isGuest = false,
                    isOnboardingCompleted = existing.isOnboardingCompleted
                )
            } else {
                UserProfile(
                    id = "current_user",
                    uid = uidToUse,
                    name = normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = normalizedEmail,
                    isGuest = false,
                    isOnboardingCompleted = false,
                    examName = "RRB Group D"
                )
            }

            userDao.insertOrUpdateUserProfile(profile)
            supabaseSyncService?.syncUserProfile(profile)

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
