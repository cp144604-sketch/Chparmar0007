package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AdminConfig
import com.example.data.model.AppDownloadConfig
import com.example.data.model.CustomerSession
import com.example.data.model.Grain
import com.example.data.model.MyListItem
import com.example.data.model.StockStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MahadevRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Firebase માં લોગિન કરવું જરૂરી છે.")
  }

  fun isBootstrappedAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    return user.email == AdminConfig.BOOTSTRAPPED_ADMIN_EMAIL && user.isEmailVerified
  }

  fun observeGrains(): Flow<List<Grain>> = flow {
    val uid = requireUserId()
    val path = "grains"
    emitAll(
      db.collection(path)
        .whereEqualTo("catalogId", AdminConfig.CATALOG_ID)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(Grain::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.name }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeMyList(userId: String): Flow<List<MyListItem>> = flow {
    val uid = requireUserId()
    val path = "users/$userId/my_list"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("my_list")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(MyListItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt?.seconds ?: 0L }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeCustomerSessions(): Flow<List<CustomerSession>> = flow {
    val uid = requireUserId()
    val path = "customer_sessions"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              CustomerSession::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.lastLoginAt?.seconds ?: 0L }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun recordCustomerLogin(
    mobileNumber: String,
    customerName: String,
    roleLabel: String = "customer",
  ): Result<Unit> {
    val cleanMobile = mobileNumber.filter { it.isDigit() }
    val cleanName = customerName.trim().ifBlank { "ગ્રાહક ($cleanMobile)" }.take(60)
    var docPath = "customer_sessions/unknown"
    return try {
      val uid = requireUserId()
      require(cleanMobile.length in 10..15) { "કૃપા કરીને 10 અંકનો સાચો મોબાઈલ નંબર દાખલ કરો." }
      val sessionId = "${uid}_$cleanMobile"
      val docRef = db.collection("customer_sessions").document(sessionId)
      docPath = docRef.path
      val existingDoc = docRef.get().await()
      val email = auth.currentUser?.email?.take(100)
      if (existingDoc.exists()) {
        val updateMap =
          mutableMapOf<String, Any>(
            "customerName" to cleanName,
            "roleLabel" to roleLabel,
            "loginCount" to FieldValue.increment(1L),
            "lastLoginAt" to FieldValue.serverTimestamp(),
          )
        if (!email.isNullOrBlank()) {
          updateMap["email"] = email
        }
        docRef.update(updateMap).await()
      } else {
        val createMap =
          mutableMapOf<String, Any>(
            "userId" to uid,
            "mobileNumber" to cleanMobile,
            "customerName" to cleanName,
            "roleLabel" to roleLabel,
            "loginCount" to 1L,
            "createdAt" to FieldValue.serverTimestamp(),
            "lastLoginAt" to FieldValue.serverTimestamp(),
          )
        if (!email.isNullOrBlank()) {
          createMap["email"] = email
        }
        docRef.set(createMap).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun verifyAndActivateAdmin(mobileNumber: String, password: String): Result<Unit> {
    val cleanMobile = mobileNumber.filter { it.isDigit() }
    var docPath = "admins/unknown"
    return try {
      val uid = requireUserId()
      if (cleanMobile !in AdminConfig.ALLOWED_ADMIN_MOBILES || password != AdminConfig.ADMIN_PASSCODE) {
        return Result.failure(IllegalArgumentException("એડમિન મોબાઈલ નંબર અથવા પાસવર્ડ ખોટો છે."))
      }
      val docRef = db.collection("admins").document(uid)
      docPath = docRef.path
      val existingDoc = docRef.get().await()
      val displayName = "Admin (${cleanMobile.takeLast(4)})"
      if (existingDoc.exists()) {
        docRef
          .update(
            mapOf(
              "adminMobile" to cleanMobile,
              "passcode" to password,
              "displayName" to displayName,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "adminMobile" to cleanMobile,
              "passcode" to password,
              "displayName" to displayName,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun hasAdminDocument(): Boolean {
    return try {
      val uid = auth.currentUser?.uid ?: return false
      db.collection("admins").document(uid).get().await().exists()
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  suspend fun deactivateAdmin(): Result<Unit> {
    var docPath = "admins/unknown"
    return try {
      val uid = requireUserId()
      val docRef = db.collection("admins").document(uid)
      docPath = docRef.path
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun addGrain(
    name: String,
    category: String,
    iconKey: String,
    pricePerKg: Double,
    weightLabel: String,
    stockStatus: StockStatus,
    notes: String,
    imageBase64: String,
    adminMobile: String,
  ): Result<Unit> {
    var docPath = "grains"
    return try {
      val uid = requireUserId()
      val cleanName = name.trim()
      val cleanWeight = weightLabel.trim()
      require(cleanName.isNotBlank() && cleanName.length <= 80) { "અનાજનું સાચું નામ લખો." }
      require(pricePerKg in 0.0..100000.0) { "માન્ય ભાવ (₹ 0 થી 1,00,000) લખો." }
      require(cleanWeight.isNotBlank() && cleanWeight.length <= 50) { "વજનની વિગત લખો." }
      require(imageBase64.length <= 120000) { "ફોટોનું કદ વધુ મોટું છે." }

      val docRef = db.collection("grains").document()
      docPath = docRef.path
      val payload =
        mutableMapOf<String, Any>(
          "catalogId" to AdminConfig.CATALOG_ID,
          "name" to cleanName,
          "category" to category.trim().ifBlank { "અનાજ" }.take(40),
          "iconKey" to iconKey.ifBlank { "grocery" }.take(30),
          "pricePerKg" to pricePerKg,
          "weightLabel" to cleanWeight,
          "stockStatus" to stockStatus.labelGu,
          "createdBy" to uid,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (notes.isNotBlank()) payload["notes"] = notes.trim().take(300)
      if (imageBase64.isNotBlank()) payload["imageBase64"] = imageBase64
      if (adminMobile.isNotBlank()) payload["updatedByMobile"] = adminMobile.take(20)

      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateGrainFull(
    grainId: String,
    name: String,
    category: String,
    iconKey: String,
    pricePerKg: Double,
    weightLabel: String,
    stockStatus: StockStatus,
    notes: String,
    imageBase64: String,
    adminMobile: String,
  ): Result<Unit> {
    var docPath = "grains/$grainId"
    return try {
      requireUserId()
      require(grainId.isNotBlank()) { "Grain ID ખાલી ન હોઈ શકે." }
      val cleanName = name.trim()
      val cleanWeight = weightLabel.trim()
      require(cleanName.isNotBlank() && cleanName.length <= 80) { "અનાજનું સાચું નામ લખો." }
      require(pricePerKg in 0.0..100000.0) { "માન્ય ભાવ લખો." }
      require(cleanWeight.isNotBlank() && cleanWeight.length <= 50) { "વજનની વિગત લખો." }
      require(imageBase64.length <= 120000) { "ફોટોનું કદ વધુ મોટું છે." }

      val docRef = db.collection("grains").document(grainId)
      docPath = docRef.path
      val updates =
        mutableMapOf<String, Any>(
          "name" to cleanName,
          "category" to category.trim().ifBlank { "અનાજ" }.take(40),
          "iconKey" to iconKey.ifBlank { "grocery" }.take(30),
          "pricePerKg" to pricePerKg,
          "weightLabel" to cleanWeight,
          "stockStatus" to stockStatus.labelGu,
          "notes" to notes.trim().take(300),
          "imageBase64" to imageBase64,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (adminMobile.isNotBlank()) updates["updatedByMobile"] = adminMobile.take(20)
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateGrainPrice(
    grainId: String,
    newPricePerKg: Double,
    adminMobile: String,
  ): Result<Unit> {
    var docPath = "grains/$grainId"
    return try {
      requireUserId()
      require(grainId.isNotBlank())
      require(newPricePerKg in 0.0..100000.0) { "માન્ય ભાવ લખો." }
      val docRef = db.collection("grains").document(grainId)
      docPath = docRef.path
      val updates =
        mutableMapOf<String, Any>(
          "pricePerKg" to newPricePerKg,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (adminMobile.isNotBlank()) updates["updatedByMobile"] = adminMobile.take(20)
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateGrainWeight(
    grainId: String,
    newWeightLabel: String,
    adminMobile: String,
  ): Result<Unit> {
    var docPath = "grains/$grainId"
    return try {
      requireUserId()
      require(grainId.isNotBlank())
      val cleanWeight = newWeightLabel.trim()
      require(cleanWeight.isNotBlank() && cleanWeight.length <= 50) { "વજનની વિગત લખો." }
      val docRef = db.collection("grains").document(grainId)
      docPath = docRef.path
      val updates =
        mutableMapOf<String, Any>(
          "weightLabel" to cleanWeight,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (adminMobile.isNotBlank()) updates["updatedByMobile"] = adminMobile.take(20)
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateGrainStock(
    grainId: String,
    newStatus: StockStatus,
    adminMobile: String,
  ): Result<Unit> {
    var docPath = "grains/$grainId"
    return try {
      requireUserId()
      require(grainId.isNotBlank())
      val docRef = db.collection("grains").document(grainId)
      docPath = docRef.path
      val updates =
        mutableMapOf<String, Any>(
          "stockStatus" to newStatus.labelGu,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (adminMobile.isNotBlank()) updates["updatedByMobile"] = adminMobile.take(20)
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteGrain(grainId: String): Result<Unit> {
    var docPath = "grains/$grainId"
    return try {
      requireUserId()
      require(grainId.isNotBlank())
      val docRef = db.collection("grains").document(grainId)
      docPath = docRef.path
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun seedDefaultGrainsIfEmpty(adminMobile: String = ""): Result<Unit> {
    val path = "grains"
    return try {
      val uid = requireUserId()
      val existing =
        db.collection(path)
          .whereEqualTo("catalogId", AdminConfig.CATALOG_ID)
          .limit(1)
          .get()
          .await()
      if (!existing.isEmpty) return Result.success(Unit)

      data class SeedItem(
        val id: String,
        val name: String,
        val category: String,
        val iconKey: String,
        val price: Double,
        val weight: String,
      )

      val defaults =
        listOf(
          SeedItem("grain_wheat", "ઘઉં", "અનાજ", "wheat", 32.0, "50 Kg (બેગ)"),
          SeedItem("grain_rice", "ચોખા", "અનાજ", "rice", 58.0, "25 Kg (બેગ)"),
          SeedItem("grain_bajri", "બાજરી", "અનાજ", "bajri", 40.0, "50 Kg (બેગ)"),
          SeedItem("grain_jowar", "જુવાર", "અનાજ", "jowar", 38.0, "50 Kg (બેગ)"),
          SeedItem("grain_chana", "ચણા", "કઠોળ", "chana", 72.0, "50 Kg (બેગ)"),
          SeedItem("grain_toor_dal", "તુવેર દાળ", "દાળ", "toor_dal", 120.0, "30 Kg (બેગ)"),
          SeedItem("grain_moong", "મગ", "કઠોળ", "moong", 98.0, "30 Kg (બેગ)"),
          SeedItem("grain_maize", "મકાઈ", "અનાજ", "maize", 30.0, "50 Kg (બેગ)"),
        )

      val batch = db.batch()
      for (item in defaults) {
        val docRef = db.collection(path).document(item.id)
        val payload =
          mutableMapOf<String, Any>(
            "catalogId" to AdminConfig.CATALOG_ID,
            "name" to item.name,
            "category" to item.category,
            "iconKey" to item.iconKey,
            "pricePerKg" to item.price,
            "weightLabel" to item.weight,
            "stockStatus" to StockStatus.AVAILABLE.labelGu,
            "notes" to "શુદ્ધ અને તાજો માલ • હળવદ",
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (adminMobile.isNotBlank()) payload["updatedByMobile"] = adminMobile.take(20)
        batch.set(docRef, payload)
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun saveToMyList(grain: Grain, quantityNote: String = "1 બેગ"): Result<Unit> {
    var docPath = "users/unknown/my_list/${grain.id}"
    return try {
      val uid = requireUserId()
      require(grain.id.isNotBlank()) { "Grain ID ખાલી છે." }
      val docRef = db.collection("users").document(uid).collection("my_list").document(grain.id)
      docPath = docRef.path
      val existingDoc = docRef.get().await()
      val cleanNote = quantityNote.trim().take(60)
      if (existingDoc.exists()) {
        docRef
          .update(
            mapOf(
              "grainName" to grain.name,
              "pricePerKg" to grain.pricePerKg,
              "weightLabel" to grain.weightLabel,
              "quantityNote" to cleanNote,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        val payload =
          mutableMapOf<String, Any>(
            "grainId" to grain.id,
            "grainName" to grain.name,
            "pricePerKg" to grain.pricePerKg,
            "weightLabel" to grain.weightLabel,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (cleanNote.isNotBlank()) payload["quantityNote"] = cleanNote
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun removeFromMyList(grainId: String): Result<Unit> {
    var docPath = "users/unknown/my_list/$grainId"
    return try {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid).collection("my_list").document(grainId)
      docPath = docRef.path
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  fun observeDownloadConfig(): Flow<AppDownloadConfig> = flow {
    val uid = requireUserId()
    val path = "app_settings/download_config"
    emitAll(
      db.collection("app_settings")
        .document("download_config")
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              AppDownloadConfig::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            ) ?: AppDownloadConfig()
          } else {
            AppDownloadConfig()
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  suspend fun updateDownloadConfig(
    downloadUrl: String,
    titleGu: String,
    adminMobile: String,
  ): Result<Unit> {
    val path = "app_settings/download_config"
    return try {
      requireUserId()
      val cleanUrl = downloadUrl.trim()
      val cleanTitle = titleGu.trim().ifBlank { "મહાદેવ ટ્રેડર્સ એપ ડાઉનલોડ" }.take(80)
      require(cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) {
        "કૃપા કરીને https:// અથવા http:// થી શરૂ થતી માન્ય લિંક લખો."
      }
      require(cleanUrl.length in 8..500) { "લિંકની લંબાઈ અમાન્ય છે." }

      val docRef = db.collection("app_settings").document("download_config")
      val existingDoc = docRef.get().await()
      if (existingDoc.exists()) {
        val updates =
          mutableMapOf<String, Any>(
            "downloadUrl" to cleanUrl,
            "titleGu" to cleanTitle,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (adminMobile.isNotBlank()) updates["updatedByMobile"] = adminMobile.take(20)
        docRef.update(updates).await()
      } else {
        val payload =
          mutableMapOf<String, Any>(
            "downloadUrl" to cleanUrl,
            "titleGu" to cleanTitle,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (adminMobile.isNotBlank()) payload["updatedByMobile"] = adminMobile.take(20)
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }
}
