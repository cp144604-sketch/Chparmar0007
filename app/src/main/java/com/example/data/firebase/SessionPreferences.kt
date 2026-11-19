package com.example.data.firebase

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "mahadev_session_prefs"
)

data class LocalSessionPrefs(
  val customerMobile: String = "",
  val customerName: String = "",
  val rememberAdmin: Boolean = false,
  val savedAdminMobile: String = "",
)

class SessionPreferences(private val context: Context) {
  private object Keys {
    val CUSTOMER_MOBILE = stringPreferencesKey("customer_mobile")
    val CUSTOMER_NAME = stringPreferencesKey("customer_name")
    val REMEMBER_ADMIN = booleanPreferencesKey("remember_admin")
    val SAVED_ADMIN_MOBILE = stringPreferencesKey("saved_admin_mobile")
  }

  val prefsFlow: Flow<LocalSessionPrefs> =
    context.applicationContext.sessionDataStore.data.map { prefs ->
      LocalSessionPrefs(
        customerMobile = prefs[Keys.CUSTOMER_MOBILE] ?: "",
        customerName = prefs[Keys.CUSTOMER_NAME] ?: "",
        rememberAdmin = prefs[Keys.REMEMBER_ADMIN] ?: false,
        savedAdminMobile = prefs[Keys.SAVED_ADMIN_MOBILE] ?: "",
      )
    }

  suspend fun saveCustomerLogin(mobile: String, name: String) {
    context.applicationContext.sessionDataStore.edit { prefs ->
      prefs[Keys.CUSTOMER_MOBILE] = mobile
      prefs[Keys.CUSTOMER_NAME] = name
    }
  }

  suspend fun clearCustomerLogin() {
    context.applicationContext.sessionDataStore.edit { prefs ->
      prefs.remove(Keys.CUSTOMER_MOBILE)
      prefs.remove(Keys.CUSTOMER_NAME)
    }
  }

  suspend fun saveAdminRememberPreference(remember: Boolean, mobile: String) {
    context.applicationContext.sessionDataStore.edit { prefs ->
      prefs[Keys.REMEMBER_ADMIN] = remember
      if (remember) {
        prefs[Keys.SAVED_ADMIN_MOBILE] = mobile
      } else {
        prefs.remove(Keys.SAVED_ADMIN_MOBILE)
      }
    }
  }

  suspend fun clearAdminSession() {
    context.applicationContext.sessionDataStore.edit { prefs ->
      prefs[Keys.REMEMBER_ADMIN] = false
      prefs.remove(Keys.SAVED_ADMIN_MOBILE)
    }
  }
}
