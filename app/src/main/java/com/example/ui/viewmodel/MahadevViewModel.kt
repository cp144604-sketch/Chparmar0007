package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.LocalSessionPrefs
import com.example.data.firebase.MahadevRepository
import com.example.data.firebase.SessionPreferences
import com.example.data.model.AdminConfig
import com.example.data.model.AppDownloadConfig
import com.example.data.model.CustomerSession
import com.example.data.model.Grain
import com.example.data.model.GrainCategory
import com.example.data.model.MyListItem
import com.example.data.model.StockStatus
import com.example.data.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MahadevViewModel(
  private val repository: MahadevRepository,
  private val sessionPreferences: SessionPreferences,
  private val currentUserId: String,
) : ViewModel() {

  val localPrefs: StateFlow<LocalSessionPrefs> =
    sessionPreferences.prefsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = LocalSessionPrefs(),
    )

  val grainsState: StateFlow<UiState<List<Grain>>> =
    repository
      .observeGrains()
      .map<List<Grain>, UiState<List<Grain>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing grains", rootError)
        emit(UiState.Error(rootError.message ?: "અનાજની યાદી લોડ કરવામાં ભૂલ આવી."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val myListState: StateFlow<UiState<List<MyListItem>>> =
    repository
      .observeMyList(currentUserId)
      .map<List<MyListItem>, UiState<List<MyListItem>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing my list", rootError)
        emit(UiState.Error(rootError.message ?: "મારી યાદી લોડ કરવામાં ભૂલ આવી."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  private val _isAdminUnlocked = MutableStateFlow(false)
  val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

  private val _activeAdminMobile = MutableStateFlow("")
  val activeAdminMobile: StateFlow<String> = _activeAdminMobile.asStateFlow()

  // Customer login audit stream is ONLY subscribed when Admin has unlocked Admin Panel
  val customerSessionsState: StateFlow<UiState<List<CustomerSession>>> =
    _isAdminUnlocked
      .flatMapLatest { unlocked ->
        if (!unlocked) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeCustomerSessions()
            .map<List<CustomerSession>, UiState<List<CustomerSession>>> { UiState.Success(it) }
            .catch { error ->
              val firestoreError =
                generateSequence(error) { it.cause }
                  .filterIsInstance<FirebaseFirestoreException>()
                  .firstOrNull()
              if (firestoreError == null && error is CancellationException) throw error
              val rootError = firestoreError ?: error
              Log.w(TAG, "Error observing customer sessions", rootError)
              emit(UiState.Error(rootError.message ?: "લોગિન યાદી લોડ કરવામાં ભૂલ આવી."))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val downloadConfigState: StateFlow<AppDownloadConfig> =
    repository
      .observeDownloadConfig()
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing download config", firestoreError ?: error)
        emit(AppDownloadConfig())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AppDownloadConfig(),
      )

  // Independent search queries across every list as requested
  private val _homeSearchQuery = MutableStateFlow("")
  val homeSearchQuery: StateFlow<String> = _homeSearchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow(GrainCategory.ALL)
  val selectedCategory: StateFlow<GrainCategory> = _selectedCategory.asStateFlow()

  private val _myListSearchQuery = MutableStateFlow("")
  val myListSearchQuery: StateFlow<String> = _myListSearchQuery.asStateFlow()

  private val _adminGrainSearchQuery = MutableStateFlow("")
  val adminGrainSearchQuery: StateFlow<String> = _adminGrainSearchQuery.asStateFlow()

  private val _adminLoginSearchQuery = MutableStateFlow("")
  val adminLoginSearchQuery: StateFlow<String> = _adminLoginSearchQuery.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  init {
    viewModelScope.launch {
      // Auto-seed default grains on very first launch if user is bootstrapped admin
      if (repository.isBootstrappedAdmin()) {
        repository.seedDefaultGrainsIfEmpty()
      }
    }
  }

  fun updateHomeSearch(query: String) {
    _homeSearchQuery.value = query
  }

  fun selectCategory(category: GrainCategory) {
    _selectedCategory.value = category
  }

  fun updateMyListSearch(query: String) {
    _myListSearchQuery.value = query
  }

  fun updateAdminGrainSearch(query: String) {
    _adminGrainSearchQuery.value = query
  }

  fun updateAdminLoginSearch(query: String) {
    _adminLoginSearchQuery.value = query
  }

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  fun loginCustomerWithMobile(mobile: String, name: String, onSuccess: () -> Unit = {}) {
    val cleanMobile = mobile.filter { it.isDigit() }
    if (cleanMobile.length != 10) {
      _feedbackMessage.value = "કૃપા કરીને 10 અંકનો માન્ય મોબાઈલ નંબર દાખલ કરો."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      val cleanName = name.trim().ifBlank { "ગ્રાહક ($cleanMobile)" }
      val res =
        repository.recordCustomerLogin(
          mobileNumber = cleanMobile,
          customerName = cleanName,
          roleLabel = "customer",
        )
      res.onSuccess {
        sessionPreferences.saveCustomerLogin(cleanMobile, cleanName)
        _feedbackMessage.value = "સ્વાગત છે! $cleanMobile પર સફળતાપૂર્વક લોગિન થયું."
        onSuccess()
      }
      res.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "લોગિન કરવામાં ભૂલ આવી."
      }
      _isBusy.value = false
    }
  }

  fun logoutCustomerMobile() {
    viewModelScope.launch {
      sessionPreferences.clearCustomerLogin()
    }
  }

  fun loginAdmin(
    mobile: String,
    password: String,
    rememberMe: Boolean,
    onSuccess: () -> Unit = {},
  ) {
    val cleanMobile = mobile.filter { it.isDigit() }
    if (cleanMobile !in AdminConfig.ALLOWED_ADMIN_MOBILES) {
      _feedbackMessage.value = "માત્ર અધિકૃત એડમિન મોબાઈલ નંબર જ માન્ય છે."
      return
    }
    if (password != AdminConfig.ADMIN_PASSCODE) {
      _feedbackMessage.value = "એડમિન પાસવર્ડ ખોટો છે."
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.verifyAndActivateAdmin(cleanMobile, password)
      res.onSuccess {
        sessionPreferences.saveAdminRememberPreference(rememberMe, cleanMobile)
        _activeAdminMobile.value = cleanMobile
        _isAdminUnlocked.value = true
        // Record admin login in session log as well
        repository.recordCustomerLogin(
          mobileNumber = cleanMobile,
          customerName = "Admin (${cleanMobile.takeLast(4)})",
          roleLabel = "admin",
        )
        // Seed defaults if catalog is empty
        repository.seedDefaultGrainsIfEmpty(cleanMobile)
        _feedbackMessage.value = "એડમિન પેનલ સફળતાપૂર્વક ખુલી ગઈ."
        onSuccess()
      }
      res.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "એડમિન લોગિન નિષ્ફળ ગયું."
      }
      _isBusy.value = false
    }
  }

  fun logoutAdmin() {
    viewModelScope.launch {
      _isAdminUnlocked.value = false
      _activeAdminMobile.value = ""
      repository.deactivateAdmin()
      _feedbackMessage.value = "એડમિન લોગઆઉટ સફળ."
    }
  }

  fun addGrain(
    name: String,
    category: String,
    iconKey: String,
    pricePerKg: Double,
    weightLabel: String,
    stockStatus: StockStatus,
    notes: String,
    imageBase64: String,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res =
        repository.addGrain(
          name = name,
          category = category,
          iconKey = iconKey,
          pricePerKg = pricePerKg,
          weightLabel = weightLabel,
          stockStatus = stockStatus,
          notes = notes,
          imageBase64 = imageBase64,
          adminMobile = _activeAdminMobile.value,
        )
      res.onSuccess {
        _feedbackMessage.value = "$name સફળતાપૂર્વક ઉમેરાયું! ગ્રાહકોને તરત દેખાશે."
        onSuccess()
      }
      res.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "અનાજ ઉમેરવામાં ભૂલ આવી."
      }
      _isBusy.value = false
    }
  }

  fun updateGrainFull(
    grainId: String,
    name: String,
    category: String,
    iconKey: String,
    pricePerKg: Double,
    weightLabel: String,
    stockStatus: StockStatus,
    notes: String,
    imageBase64: String,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res =
        repository.updateGrainFull(
          grainId = grainId,
          name = name,
          category = category,
          iconKey = iconKey,
          pricePerKg = pricePerKg,
          weightLabel = weightLabel,
          stockStatus = stockStatus,
          notes = notes,
          imageBase64 = imageBase64,
          adminMobile = _activeAdminMobile.value,
        )
      res.onSuccess {
        _feedbackMessage.value = "$name ની વિગત તરત અપડેટ થઈ ગઈ!"
        onSuccess()
      }
      res.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "અપડેટ કરવામાં ભૂલ આવી."
      }
      _isBusy.value = false
    }
  }

  fun quickUpdatePrice(grain: Grain, newPrice: Double, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.updateGrainPrice(grain.id, newPrice, _activeAdminMobile.value)
      res.onSuccess {
        _feedbackMessage.value = "${grain.name} નો નવો ભાવ ₹ ${formatPrice(newPrice)} / Kg સેવ થયો!"
        onDone()
      }
      res.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "ભાવ અપડેટ નિષ્ફળ ગયું."
      }
      _isBusy.value = false
    }
  }

  fun quickUpdateWeight(grain: Grain, newWeight: String, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.updateGrainWeight(grain.id, newWeight, _activeAdminMobile.value)
      res.onSuccess {
        _feedbackMessage.value = "${grain.name} નું વજન $newWeight સેવ થયું!"
        onDone()
      }
      res.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "વજન અપડેટ નિષ્ફળ ગયું."
      }
      _isBusy.value = false
    }
  }

  fun quickUpdateStock(grain: Grain, newStatus: StockStatus) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.updateGrainStock(grain.id, newStatus, _activeAdminMobile.value)
      res.onSuccess {
        _feedbackMessage.value = "${grain.name} સ્ટોક: ${newStatus.labelGu}"
      }
      res.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "સ્ટોક અપડેટ નિષ્ફળ ગયું."
      }
      _isBusy.value = false
    }
  }

  fun deleteGrain(grain: Grain) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.deleteGrain(grain.id)
      res.onSuccess {
        _feedbackMessage.value = "${grain.name} યાદીમાંથી ડિલીટ થયું."
      }
      res.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "ડિલીટ કરવામાં ભૂલ આવી."
      }
      _isBusy.value = false
    }
  }

  fun seedDefaultsNow() {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.seedDefaultGrainsIfEmpty(_activeAdminMobile.value)
      res.onSuccess {
        _feedbackMessage.value = "ડિફોલ્ટ અનાજ યાદી ઉમેરાઈ ગઈ!"
      }
      res.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "ડેટા ઉમેરવામાં ભૂલ આવી."
      }
      _isBusy.value = false
    }
  }

  fun saveToMyList(grain: Grain, quantityNote: String = "1 બેગ") {
    viewModelScope.launch {
      val res = repository.saveToMyList(grain, quantityNote)
      res.onSuccess {
        _feedbackMessage.value = "${grain.name} 'મારી યાદી' માં ઉમેરાયું."
      }
      res.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "યાદીમાં ઉમેરવામાં ભૂલ આવી."
      }
    }
  }

  fun removeFromMyList(grainId: String, grainName: String) {
    viewModelScope.launch {
      val res = repository.removeFromMyList(grainId)
      res.onSuccess {
        _feedbackMessage.value = "$grainName 'મારી યાદી' માંથી દૂર થયું."
      }
      res.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "દૂર કરવામાં ભૂલ આવી."
      }
    }
  }

  fun updateDownloadConfig(downloadUrl: String, titleGu: String, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.updateDownloadConfig(downloadUrl, titleGu, _activeAdminMobile.value)
      res.onSuccess {
        _feedbackMessage.value = "એપ ડાઉનલોડ લિંક અને QR કોડ સેવ થઈ ગયા!"
        onDone()
      }
      res.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "લિંક સેવ કરવામાં ભૂલ આવી."
      }
      _isBusy.value = false
    }
  }

  private companion object {
    const val TAG = "MahadevVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}

fun formatPrice(price: Double): String {
  return if (price % 1.0 == 0.0) {
    price.toLong().toString()
  } else {
    String.format("%.2f", price)
  }
}
