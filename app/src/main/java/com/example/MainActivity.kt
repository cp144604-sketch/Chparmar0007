package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.firebase.MahadevRepository
import com.example.data.firebase.SessionPreferences
import com.example.data.model.UiState
import com.example.ui.screens.AdminTabRoot
import com.example.ui.screens.CustomerMobileLoginScreen
import com.example.ui.screens.GoogleAuthScreen
import com.example.ui.screens.GrainDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyListScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.MahadevTradersTheme
import com.example.ui.viewmodel.MahadevViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MahadevTradersTheme {
        AppRoot()
      }
    }
  }
}

enum class MainTab(
  val labelGu: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  HOME("હોમ", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
  MY_LIST(
    "મારી યાદી",
    Icons.AutoMirrored.Filled.ListAlt,
    Icons.AutoMirrored.Outlined.ListAlt,
    "tab_my_list",
  ),
  ADMIN("એડમિન", Icons.Filled.Person, Icons.Outlined.Person, "tab_admin"),
}

@Composable
fun AppRoot() {
  var showSplash by rememberSaveable { mutableStateOf(true) }
  LaunchedEffect(Unit) {
    delay(1100L)
    showSplash = false
  }

  if (showSplash) {
    SplashScreen()
    return
  }

  val auth = Firebase.auth
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    GoogleAuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      AuthenticatedAppShell(currentUserId = user.uid)
    }
  }
}

@Composable
private fun AuthenticatedAppShell(currentUserId: String) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: MahadevViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            MahadevViewModel(
              repository = MahadevRepository(db),
              sessionPreferences = SessionPreferences(app),
              currentUserId = currentUserId,
            )
          }
        },
    )

  val localPrefs by viewModel.localPrefs.collectAsStateWithLifecycle()
  val grainsState by viewModel.grainsState.collectAsStateWithLifecycle()
  val myListState by viewModel.myListState.collectAsStateWithLifecycle()
  val customerSessionsState by viewModel.customerSessionsState.collectAsStateWithLifecycle()
  val downloadConfig by viewModel.downloadConfigState.collectAsStateWithLifecycle()
  val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsStateWithLifecycle()
  val activeAdminMobile by viewModel.activeAdminMobile.collectAsStateWithLifecycle()
  val homeSearchQuery by viewModel.homeSearchQuery.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val myListSearchQuery by viewModel.myListSearchQuery.collectAsStateWithLifecycle()
  val adminGrainSearch by viewModel.adminGrainSearchQuery.collectAsStateWithLifecycle()
  val adminLoginSearch by viewModel.adminLoginSearchQuery.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
  val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

  var activeTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
  var selectedGrainId by rememberSaveable { mutableStateOf<String?>(null) }
  var bypassMobileForAdminLogin by rememberSaveable { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(feedbackMessage) {
    val msg = feedbackMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearFeedback()
    }
  }

  // Gate customer access behind 10-digit mobile number login unless Admin mode or Admin shortcut is used
  if (localPrefs.customerMobile.isBlank() && !isAdminUnlocked && !bypassMobileForAdminLogin) {
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        CustomerMobileLoginScreen(
          initialMobile = localPrefs.customerMobile,
          initialName = localPrefs.customerName,
          isBusy = isBusy,
          onSubmitMobileLogin = { mobile, name ->
            viewModel.loginCustomerWithMobile(mobile, name)
          },
          onGoToAdminLogin = {
            bypassMobileForAdminLogin = true
            activeTab = MainTab.ADMIN
          },
          onGoogleSignOut = {
            signOut(credentialManager = credentialManager, scope = scope)
          },
        )
      }
    }
    return
  }

  val grains = (grainsState as? UiState.Success)?.data ?: emptyList()
  val selectedGrain =
    remember(grains, selectedGrainId) {
      selectedGrainId?.let { id -> grains.firstOrNull { it.id == id } }
    }
  val savedGrainIds =
    remember(myListState) {
      (myListState as? UiState.Success)?.data?.map { it.grainId }?.toSet() ?: emptySet()
    }

  if (selectedGrain != null) {
    Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        val isSaved = selectedGrain.id in savedGrainIds
        GrainDetailScreen(
          grain = selectedGrain,
          isSaved = isSaved,
          onToggleSave = {
            if (isSaved) {
              viewModel.removeFromMyList(selectedGrain.id, selectedGrain.name)
            } else {
              viewModel.saveToMyList(selectedGrain)
            }
          },
          onBack = { selectedGrainId = null },
        )
      }
    }
    return
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    if (useRail) {
      Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxHeight().safeDrawingPadding(),
        ) {
          Spacer(modifier = Modifier.weight(1f))
          MainTab.entries.forEach { tab ->
            val selected = activeTab == tab
            NavigationRailItem(
              selected = selected,
              onClick = { activeTab = tab },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = tab.labelGu,
                )
              },
              label = {
                Text(
                  text = tab.labelGu,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
              },
              colors =
                NavigationRailItemDefaults.colors(
                  selectedIconColor = CrimsonPrimary,
                  selectedTextColor = CrimsonPrimary,
                  indicatorColor = CrimsonContainer,
                ),
              modifier = Modifier.testTag(tab.testTag),
            )
          }
          Spacer(modifier = Modifier.weight(1f))
        }

        Scaffold(
          snackbarHost = { SnackbarHost(snackbarHostState) },
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          modifier = Modifier.weight(1f),
        ) { innerPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            MainTabContent(
              activeTab = activeTab,
              viewModel = viewModel,
              grainsState = grainsState,
              myListState = myListState,
              customerSessionsState = customerSessionsState,
              downloadConfig = downloadConfig,
              savedGrainIds = savedGrainIds,
              localCustomerMobile = localPrefs.customerMobile,
              rememberAdmin = localPrefs.rememberAdmin,
              savedAdminMobile = localPrefs.savedAdminMobile,
              isAdminUnlocked = isAdminUnlocked,
              activeAdminMobile = activeAdminMobile,
              homeSearchQuery = homeSearchQuery,
              selectedCategory = selectedCategory,
              myListSearchQuery = myListSearchQuery,
              adminGrainSearch = adminGrainSearch,
              adminLoginSearch = adminLoginSearch,
              isBusy = isBusy,
              onCopiedDownloadLink = {
                scope.launch { snackbarHostState.showSnackbar("લિંક કોપી થઈ ગઈ!") }
              },
              onOpenGrainDetail = { selectedGrainId = it.id },
              onCustomerLogout = {
                bypassMobileForAdminLogin = false
                viewModel.logoutCustomerMobile()
              },
            )
          }
        }
      }
    } else {
      Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
          ) {
            MainTab.entries.forEach { tab ->
              val selected = activeTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { activeTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.labelGu,
                  )
                },
                label = {
                  Text(
                    text = tab.labelGu,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                  )
                },
                colors =
                  NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = CrimsonPrimary,
                    indicatorColor = CrimsonPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        },
      ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          MainTabContent(
            activeTab = activeTab,
            viewModel = viewModel,
            grainsState = grainsState,
            myListState = myListState,
            customerSessionsState = customerSessionsState,
            downloadConfig = downloadConfig,
            savedGrainIds = savedGrainIds,
            localCustomerMobile = localPrefs.customerMobile,
            rememberAdmin = localPrefs.rememberAdmin,
            savedAdminMobile = localPrefs.savedAdminMobile,
            isAdminUnlocked = isAdminUnlocked,
            activeAdminMobile = activeAdminMobile,
            homeSearchQuery = homeSearchQuery,
            selectedCategory = selectedCategory,
            myListSearchQuery = myListSearchQuery,
            adminGrainSearch = adminGrainSearch,
            adminLoginSearch = adminLoginSearch,
            isBusy = isBusy,
            onCopiedDownloadLink = {
              scope.launch { snackbarHostState.showSnackbar("લિંક કોપી થઈ ગઈ!") }
            },
            onOpenGrainDetail = { selectedGrainId = it.id },
            onCustomerLogout = {
              bypassMobileForAdminLogin = false
              viewModel.logoutCustomerMobile()
            },
          )
        }
      }
    }
  }
}

@Composable
private fun MainTabContent(
  activeTab: MainTab,
  viewModel: MahadevViewModel,
  grainsState: UiState<List<com.example.data.model.Grain>>,
  myListState: UiState<List<com.example.data.model.MyListItem>>,
  customerSessionsState: UiState<List<com.example.data.model.CustomerSession>>,
  downloadConfig: com.example.data.model.AppDownloadConfig,
  savedGrainIds: Set<String>,
  localCustomerMobile: String,
  rememberAdmin: Boolean,
  savedAdminMobile: String,
  isAdminUnlocked: Boolean,
  activeAdminMobile: String,
  homeSearchQuery: String,
  selectedCategory: com.example.data.model.GrainCategory,
  myListSearchQuery: String,
  adminGrainSearch: String,
  adminLoginSearch: String,
  isBusy: Boolean,
  onCopiedDownloadLink: () -> Unit,
  onOpenGrainDetail: (com.example.data.model.Grain) -> Unit,
  onCustomerLogout: () -> Unit,
) {
  when (activeTab) {
    MainTab.HOME -> {
      HomeScreen(
        grainsState = grainsState,
        downloadConfig = downloadConfig,
        searchQuery = homeSearchQuery,
        selectedCategory = selectedCategory,
        savedGrainIds = savedGrainIds,
        customerMobile = localCustomerMobile,
        onSearchChange = viewModel::updateHomeSearch,
        onSelectCategory = viewModel::selectCategory,
        onGrainClick = onOpenGrainDetail,
        onToggleSave = { grain, isSaved ->
          if (isSaved) viewModel.removeFromMyList(grain.id, grain.name)
          else viewModel.saveToMyList(grain)
        },
        onCopiedDownloadLink = onCopiedDownloadLink,
        onCustomerLogout = onCustomerLogout,
      )
    }
    MainTab.MY_LIST -> {
      MyListScreen(
        myListState = myListState,
        liveGrainsState = grainsState,
        searchQuery = myListSearchQuery,
        onSearchChange = viewModel::updateMyListSearch,
        onOpenGrainDetail = onOpenGrainDetail,
        onUpdateNote = { grain, note -> viewModel.saveToMyList(grain, note) },
        onRemoveItem = { grainId, grainName -> viewModel.removeFromMyList(grainId, grainName) },
      )
    }
    MainTab.ADMIN -> {
      AdminTabRoot(
        isAdminUnlocked = isAdminUnlocked,
        activeAdminMobile = activeAdminMobile,
        rememberAdmin = rememberAdmin,
        savedAdminMobile = savedAdminMobile,
        isBusy = isBusy,
        grainsState = grainsState,
        customerSessionsState = customerSessionsState,
        downloadConfig = downloadConfig,
        adminGrainSearch = adminGrainSearch,
        adminLoginSearch = adminLoginSearch,
        onAdminLogin = { mobile, password, remember ->
          viewModel.loginAdmin(mobile, password, remember)
        },
        onAdminLogout = { viewModel.logoutAdmin() },
        onAdminGrainSearchChange = viewModel::updateAdminGrainSearch,
        onAdminLoginSearchChange = viewModel::updateAdminLoginSearch,
        onAddGrain = viewModel::addGrain,
        onUpdateGrainFull = viewModel::updateGrainFull,
        onQuickUpdatePrice = viewModel::quickUpdatePrice,
        onQuickUpdateWeight = viewModel::quickUpdateWeight,
        onQuickUpdateStock = viewModel::quickUpdateStock,
        onDeleteGrain = viewModel::deleteGrain,
        onUpdateDownloadConfig = viewModel::updateDownloadConfig,
        onCopiedDownloadLink = onCopiedDownloadLink,
        onSeedDefaults = viewModel::seedDefaultsNow,
      )
    }
  }
}
