package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class Grain(
  @DocumentId val id: String = "",
  val catalogId: String = AdminConfig.CATALOG_ID,
  val name: String = "",
  val category: String = "અનાજ",
  val iconKey: String = "wheat",
  val pricePerKg: Double = 0.0,
  val weightLabel: String = "50 Kg (બેગ)",
  val stockStatus: String = StockStatus.AVAILABLE.labelGu,
  val notes: String = "",
  val imageBase64: String = "",
  val updatedByMobile: String = "",
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CustomerSession(
  @DocumentId val id: String = "",
  val userId: String = "",
  val mobileNumber: String = "",
  val customerName: String = "",
  val email: String = "",
  val roleLabel: String = "customer",
  val loginCount: Long = 1L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val lastLoginAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class MyListItem(
  @DocumentId val id: String = "",
  val grainId: String = "",
  val grainName: String = "",
  val pricePerKg: Double = 0.0,
  val weightLabel: String = "",
  val quantityNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class StockStatus(val labelGu: String) {
  AVAILABLE("ઉપલબ્ધ"),
  LOW_STOCK("ઓછો સ્ટોક"),
  OUT_OF_STOCK("સ્ટોક બંધ");

  companion object {
    fun fromLabel(raw: String?): StockStatus =
      entries.firstOrNull { it.labelGu == raw || it.name == raw } ?: AVAILABLE
  }
}

enum class GrainCategory(val labelGu: String) {
  ALL("બધા"),
  GRAIN("અનાજ"),
  DAL("દાળ"),
  PULSES("કઠોળ"),
  GROCERY("કરીયાણું"),
}

enum class GrainIconKey(val key: String, val labelGu: String) {
  WHEAT("wheat", "ઘઉં"),
  RICE("rice", "ચોખા"),
  BAJRI("bajri", "બાજરી"),
  JOWAR("jowar", "જુવાર"),
  CHANA("chana", "ચણા"),
  TOOR_DAL("toor_dal", "તુવેર દાળ"),
  MOONG("moong", "મગ"),
  MAIZE("maize", "મકાઈ"),
  GROCERY("grocery", "અન્ય");

  companion object {
    fun fromKey(raw: String?): GrainIconKey =
      entries.firstOrNull { it.key == raw } ?: GROCERY
  }
}

@Keep
@IgnoreExtraProperties
data class AppDownloadConfig(
  @DocumentId val id: String = "",
  val downloadUrl: String = AdminConfig.DEFAULT_SHARE_URL,
  val titleGu: String = "મહાદેવ ટ્રેડર્સ એપ ડાઉનલોડ",
  val updatedByMobile: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

object AdminConfig {
  const val CATALOG_ID = "mahadev_traders"
  val ALLOWED_ADMIN_MOBILES = setOf("9879730448", "9727630403")
  const val ADMIN_PASSCODE = "mahadav@2001"
  const val BOOTSTRAPPED_ADMIN_EMAIL = "chparmar0007@gmail.com"
  const val DEFAULT_SHARE_URL =
    "https://ais-pre-oxh6kh4juxvnm73xrw5h3j-309993663201.asia-southeast1.run.app"
}

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}
