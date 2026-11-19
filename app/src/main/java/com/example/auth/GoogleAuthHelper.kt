package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

@Suppress("DEPRECATION")
fun getAppSigningSha1(context: Context): String? {
  return try {
    val pkgInfo =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.packageManager.getPackageInfo(
          context.packageName,
          PackageManager.GET_SIGNING_CERTIFICATES,
        )
      } else {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
      }
    val certBytes =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info = pkgInfo.signingInfo
        (info?.apkContentsSigners?.firstOrNull()
          ?: info?.signingCertificateHistory?.firstOrNull())
          ?.toByteArray()
      } else {
        pkgInfo.signatures?.firstOrNull()?.toByteArray()
      } ?: return null
    val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
    digest.joinToString(":") { "%02X".format(it) }
  } catch (_: Exception) {
    null
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

private fun getDefaultWebClientId(context: Context): String? {
  val resId =
    context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
  if (resId == 0) return null
  return try {
    context.getString(resId).takeIf { it.isNotBlank() }
  } catch (_: Exception) {
    null
  }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId =
    getDefaultWebClientId(context)
      ?: run {
        onUnauthenticated()
        return
      }
  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
  scope.launch {
    try {
      val activityContext = context.findActivity() ?: context
      val cred = credentialManager.getCredential(activityContext, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId =
    getDefaultWebClientId(context)
      ?: run {
        onAuthError("Google Sign-In રૂપરેખાંકન ખૂટે છે (default_web_client_id મળ્યું નથી).")
        return
      }
  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
  scope.launch {
    try {
      val activityContext = context.findActivity() ?: context
      val cred = credentialManager.getCredential(activityContext, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onAuthSuccess()
      } else {
        onAuthError("અમાન્ય ઓળખપત્ર પ્રાપ્ત થયું (${cred.type}).")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      val sha1 = getAppSigningSha1(context)
      onAuthCancelled()
      onAuthError(
        buildString {
          append("Google Sign-In પૂર્ણ થયું નથી (રદ થયું અથવા SHA-1 કી Firebase Console માં નોંધાયેલ નથી).")
          if (!sha1.isNullOrBlank()) {
            append("\n\nઆ એપનો SHA-1 કોડ:\n$sha1")
          }
        }
      )
    } catch (e: NoCredentialException) {
      Log.e(AUTH_TAG, "Google Sign-In NoCredentialException", e)
      val sha1 = getAppSigningSha1(context)
      onAuthError(
        buildString {
          append("Google ખાતું ચકાસી શકાયું નથી. Firebase Console માં આ APK નો SHA-1 ઉમેરો.")
          if (!sha1.isNullOrBlank()) {
            append("\n\nઆ એપનો SHA-1 કોડ:\n$sha1")
          }
          if (!e.message.isNullOrBlank()) {
            append("\nવિગત: ${e.message}")
          }
        }
      )
    } catch (e: GetCredentialException) {
      Log.e(AUTH_TAG, "Google Sign-In GetCredentialException: ${e.type}", e)
      val raw = e.message.orEmpty()
      val sha1 = getAppSigningSha1(context)
      val msg =
        buildString {
          append("Google Sign-In નિષ્ફળ: ${raw.ifBlank { e::class.java.simpleName }}")
          if (!sha1.isNullOrBlank()) {
            append("\n\nઆ એપનો SHA-1 કોડ:\n$sha1")
          }
        }
      onAuthError(msg)
    } catch (e: FirebaseNetworkException) {
      Log.e(AUTH_TAG, "Firebase network error during sign-in", e)
      onAuthError("ઇન્ટરનેટ કનેક્શન ચાલુ નથી અથવા નેટવર્ક ધીમું છે. ફરી પ્રયત્ન કરો.")
    } catch (e: Exception) {
      Log.e(AUTH_TAG, "Google Sign-In failed", e)
      val sha1 = getAppSigningSha1(context)
      onAuthError(
        buildString {
          append("લોગિન નિષ્ફળ: ${e.localizedMessage ?: e.message ?: e::class.java.simpleName}")
          if (!sha1.isNullOrBlank()) {
            append("\n\nSHA-1: $sha1")
          }
        }
      )
    }
  }
}

fun signOut(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}
