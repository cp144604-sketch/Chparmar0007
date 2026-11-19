package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AppDownloadConfig
import com.example.data.model.CustomerSession
import com.example.data.model.Grain
import com.example.data.model.StockStatus
import com.example.data.model.UiState
import com.example.ui.components.MahadevLogoBadge
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.SacredGold
import com.example.ui.theme.SacredGoldLight
import com.example.ui.theme.TileBlue
import com.example.ui.theme.TileCyan
import com.example.ui.theme.TileGreen
import com.example.ui.theme.TileOrange
import com.example.ui.theme.TilePurple
import com.example.ui.theme.TileRed
import com.example.ui.theme.TileTeal
import com.example.ui.theme.WarmOutline

enum class AdminRoute {
  DASHBOARD,
  ADD_GRAIN,
  EDIT_GRAIN,
  UPDATE_PRICE,
  UPDATE_WEIGHT,
  UPDATE_STOCK,
  DELETE_GRAIN,
  VIEW_LIST,
  CUSTOMER_LOGINS,
  APP_QR_SHARE,
}

@Composable
fun AdminTabRoot(
  isAdminUnlocked: Boolean,
  activeAdminMobile: String,
  rememberAdmin: Boolean,
  savedAdminMobile: String,
  isBusy: Boolean,
  grainsState: UiState<List<Grain>>,
  customerSessionsState: UiState<List<CustomerSession>>,
  downloadConfig: AppDownloadConfig,
  adminGrainSearch: String,
  adminLoginSearch: String,
  onAdminLogin: (mobile: String, password: String, rememberMe: Boolean) -> Unit,
  onAdminLogout: () -> Unit,
  onAdminGrainSearchChange: (String) -> Unit,
  onAdminLoginSearchChange: (String) -> Unit,
  onAddGrain:
    (
      name: String,
      category: String,
      iconKey: String,
      pricePerKg: Double,
      weightLabel: String,
      stockStatus: StockStatus,
      notes: String,
      imageBase64: String,
      onSuccess: () -> Unit,
    ) -> Unit,
  onUpdateGrainFull:
    (
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
    ) -> Unit,
  onQuickUpdatePrice: (Grain, Double, () -> Unit) -> Unit,
  onQuickUpdateWeight: (Grain, String, () -> Unit) -> Unit,
  onQuickUpdateStock: (Grain, StockStatus) -> Unit,
  onDeleteGrain: (Grain) -> Unit,
  onUpdateDownloadConfig: (String, String, () -> Unit) -> Unit,
  onCopiedDownloadLink: () -> Unit,
  onSeedDefaults: () -> Unit,
) {
  if (!isAdminUnlocked) {
    AdminLoginScreen(
      initialMobile = if (rememberAdmin) savedAdminMobile else "",
      initialRemember = rememberAdmin,
      isBusy = isBusy,
      onLogin = onAdminLogin,
    )
    return
  }

  var currentRoute by rememberSaveable { mutableStateOf(AdminRoute.DASHBOARD) }
  var editingGrain by remember { mutableStateOf<Grain?>(null) }

  val grains = (grainsState as? UiState.Success)?.data ?: emptyList()
  val sessions = (customerSessionsState as? UiState.Success)?.data ?: emptyList()

  when (currentRoute) {
    AdminRoute.DASHBOARD -> {
      AdminDashboardScreen(
        activeAdminMobile = activeAdminMobile,
        grainCount = grains.size,
        loginCount = sessions.size,
        isBusy = isBusy,
        onNavigate = { route -> currentRoute = route },
        onSeedDefaults = onSeedDefaults,
        onLogout = onAdminLogout,
      )
    }
    AdminRoute.ADD_GRAIN -> {
      AddOrEditGrainScreen(
        existingGrain = null,
        isBusy = isBusy,
        onSave = { name, cat, icon, price, weight, stock, notes, img ->
          onAddGrain(name, cat, icon, price, weight, stock, notes, img) {
            currentRoute = AdminRoute.DASHBOARD
          }
        },
        onCancel = { currentRoute = AdminRoute.DASHBOARD },
      )
    }
    AdminRoute.EDIT_GRAIN -> {
      AddOrEditGrainScreen(
        existingGrain = editingGrain,
        isBusy = isBusy,
        onSave = { name, cat, icon, price, weight, stock, notes, img ->
          val target = editingGrain
          if (target != null) {
            onUpdateGrainFull(target.id, name, cat, icon, price, weight, stock, notes, img) {
              editingGrain = null
              currentRoute = AdminRoute.VIEW_LIST
            }
          }
        },
        onCancel = {
          editingGrain = null
          currentRoute = AdminRoute.VIEW_LIST
        },
      )
    }
    AdminRoute.UPDATE_PRICE -> {
      AdminGrainActionListScreen(
        title = "ભાવ અપડેટ કરો",
        mode = AdminActionMode.PRICE,
        grainsState = grainsState,
        searchQuery = adminGrainSearch,
        onSearchChange = onAdminGrainSearchChange,
        onQuickUpdatePrice = onQuickUpdatePrice,
        onQuickUpdateWeight = onQuickUpdateWeight,
        onQuickUpdateStock = onQuickUpdateStock,
        onDeleteGrain = onDeleteGrain,
        onEditFullGrain = {},
        onBack = { currentRoute = AdminRoute.DASHBOARD },
      )
    }
    AdminRoute.UPDATE_WEIGHT -> {
      AdminGrainActionListScreen(
        title = "વજન અપડેટ કરો",
        mode = AdminActionMode.WEIGHT,
        grainsState = grainsState,
        searchQuery = adminGrainSearch,
        onSearchChange = onAdminGrainSearchChange,
        onQuickUpdatePrice = onQuickUpdatePrice,
        onQuickUpdateWeight = onQuickUpdateWeight,
        onQuickUpdateStock = onQuickUpdateStock,
        onDeleteGrain = onDeleteGrain,
        onEditFullGrain = {},
        onBack = { currentRoute = AdminRoute.DASHBOARD },
      )
    }
    AdminRoute.UPDATE_STOCK -> {
      AdminGrainActionListScreen(
        title = "સ્ટોક અપડેટ કરો",
        mode = AdminActionMode.STOCK,
        grainsState = grainsState,
        searchQuery = adminGrainSearch,
        onSearchChange = onAdminGrainSearchChange,
        onQuickUpdatePrice = onQuickUpdatePrice,
        onQuickUpdateWeight = onQuickUpdateWeight,
        onQuickUpdateStock = onQuickUpdateStock,
        onDeleteGrain = onDeleteGrain,
        onEditFullGrain = {},
        onBack = { currentRoute = AdminRoute.DASHBOARD },
      )
    }
    AdminRoute.DELETE_GRAIN -> {
      AdminGrainActionListScreen(
        title = "અનાજ ડિલીટ કરો",
        mode = AdminActionMode.DELETE,
        grainsState = grainsState,
        searchQuery = adminGrainSearch,
        onSearchChange = onAdminGrainSearchChange,
        onQuickUpdatePrice = onQuickUpdatePrice,
        onQuickUpdateWeight = onQuickUpdateWeight,
        onQuickUpdateStock = onQuickUpdateStock,
        onDeleteGrain = onDeleteGrain,
        onEditFullGrain = {},
        onBack = { currentRoute = AdminRoute.DASHBOARD },
      )
    }
    AdminRoute.VIEW_LIST -> {
      AdminGrainActionListScreen(
        title = "અનાજ યાદી જુઓ",
        mode = AdminActionMode.VIEW_ALL,
        grainsState = grainsState,
        searchQuery = adminGrainSearch,
        onSearchChange = onAdminGrainSearchChange,
        onQuickUpdatePrice = onQuickUpdatePrice,
        onQuickUpdateWeight = onQuickUpdateWeight,
        onQuickUpdateStock = onQuickUpdateStock,
        onDeleteGrain = onDeleteGrain,
        onEditFullGrain = { grain ->
          editingGrain = grain
          currentRoute = AdminRoute.EDIT_GRAIN
        },
        onBack = { currentRoute = AdminRoute.DASHBOARD },
      )
    }
    AdminRoute.CUSTOMER_LOGINS -> {
      AdminCustomerLoginsScreen(
        sessionsState = customerSessionsState,
        searchQuery = adminLoginSearch,
        onSearchChange = onAdminLoginSearchChange,
        onBack = { currentRoute = AdminRoute.DASHBOARD },
      )
    }
    AdminRoute.APP_QR_SHARE -> {
      AdminAppDownloadQrScreen(
        downloadConfig = downloadConfig,
        isBusy = isBusy,
        onSaveConfig = onUpdateDownloadConfig,
        onCopiedLink = onCopiedDownloadLink,
        onBack = { currentRoute = AdminRoute.DASHBOARD },
      )
    }
  }
}

@Composable
private fun AdminLoginScreen(
  initialMobile: String,
  initialRemember: Boolean,
  isBusy: Boolean,
  onLogin: (mobile: String, password: String, rememberMe: Boolean) -> Unit,
) {
  var mobile by remember(initialMobile) { mutableStateOf(initialMobile) }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var rememberMe by remember(initialRemember) { mutableStateOf(initialRemember) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val performLogin = {
    val cleanMobile = mobile.filter { it.isDigit() }
    if (cleanMobile.length != 10) {
      validationError = "અધિકૃત એડમિનનો 10 અંકનો મોબાઈલ નંબર લખો."
    } else if (password.isBlank()) {
      validationError = "કૃપા કરીને એડમિન પાસવર્ડ લખો."
    } else {
      validationError = null
      onLogin(cleanMobile, password, rememberMe)
    }
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(Brush.verticalGradient(colors = listOf(CrimsonPrimary, CrimsonDeep)))
          .padding(top = 36.dp, bottom = 32.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MahadevLogoBadge(size = 96.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "મહાદેવ ટ્રેડર્સ",
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
        )
        Text(
          text = "એડમિન લોગિન",
          style = MaterialTheme.typography.titleMedium,
          color = SacredGoldLight,
        )
      }
    }

    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "એડમિન મોબાઈલ નંબર",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = mobile,
            onValueChange = { input ->
              mobile = input.filter { it.isDigit() }.take(10)
              validationError = null
            },
            placeholder = { Text("મોબાઈલ નંબર દાખલ કરો") },
            leadingIcon = {
              Icon(Icons.Filled.Phone, contentDescription = null, tint = CrimsonPrimary)
            },
            singleLine = true,
            keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonPrimary,
                unfocusedBorderColor = WarmOutline,
              ),
            modifier = Modifier.fillMaxWidth().testTag("admin_mobile_input"),
          )

          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "પાસવર્ડ",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = password,
            onValueChange = {
              password = it
              validationError = null
            },
            placeholder = { Text("પાસવર્ડ દાખલ કરો") },
            leadingIcon = {
              Icon(Icons.Filled.Lock, contentDescription = null, tint = CrimsonPrimary)
            },
            trailingIcon = {
              IconButton(
                onClick = { passwordVisible = !passwordVisible },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector =
                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                  contentDescription = if (passwordVisible) "પાસવર્ડ છુપાવો" else "પાસવર્ડ જુઓ",
                )
              }
            },
            visualTransformation =
              if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { performLogin() }),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonPrimary,
                unfocusedBorderColor = WarmOutline,
              ),
            modifier = Modifier.fillMaxWidth().testTag("admin_password_input"),
          )

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { rememberMe = !rememberMe }
                .padding(vertical = 4.dp),
          ) {
            Checkbox(
              checked = rememberMe,
              onCheckedChange = { rememberMe = it },
              colors = CheckboxDefaults.colors(checkedColor = CrimsonPrimary),
              modifier = Modifier.testTag("admin_remember_checkbox"),
            )
            Text(text = "મને યાદ રાખો", style = MaterialTheme.typography.bodyMedium)
          }

          if (!validationError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = validationError!!,
              color = CrimsonPrimary,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = performLogin,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("admin_login_button"),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
          ) {
            if (isBusy) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp,
              )
            } else {
              Text(
                text = "લોગિન",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "ફક્ત એડમિન માટે",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDashboardScreen(
  activeAdminMobile: String,
  grainCount: Int,
  loginCount: Int,
  isBusy: Boolean,
  onNavigate: (AdminRoute) -> Unit,
  onSeedDefaults: () -> Unit,
  onLogout: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    TopAppBar(
      title = {
        Text(
          text = "એડમિન પેનલ",
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
      },
      actions = {
        IconButton(
          onClick = onLogout,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_logout_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = "એડમિન લોગઆઉટ",
            tint = Color.White,
          )
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = CrimsonPrimary),
    )

    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = 720.dp)
          .align(Alignment.CenterHorizontally),
    ) {
      item(span = { GridItemSpan(2) }) {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "સ્વાગત છે,",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "Admin ($activeAdminMobile)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "$grainCount અનાજ • $loginCount ગ્રાહક લોગિન",
                style = MaterialTheme.typography.labelMedium,
                color = CrimsonPrimary,
                fontWeight = FontWeight.SemiBold,
              )
            }
            Surface(
              color = SacredGoldLight,
              shape = CircleShape,
              modifier = Modifier.size(52.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.Security,
                  contentDescription = null,
                  tint = SacredGold,
                  modifier = Modifier.size(30.dp),
                )
              }
            }
          }
        }
      }

      item {
        AdminActionTile(
          title = "નવું અનાજ ઉમેરો",
          icon = Icons.Filled.Add,
          color = TileGreen,
          testTag = "admin_tile_add",
          onClick = { onNavigate(AdminRoute.ADD_GRAIN) },
        )
      }
      item {
        AdminActionTile(
          title = "ભાવ અપડેટ કરો",
          icon = Icons.Filled.Edit,
          color = TileOrange,
          testTag = "admin_tile_price",
          onClick = { onNavigate(AdminRoute.UPDATE_PRICE) },
        )
      }
      item {
        AdminActionTile(
          title = "વજન અપડેટ કરો",
          icon = Icons.Filled.Scale,
          color = TileBlue,
          testTag = "admin_tile_weight",
          onClick = { onNavigate(AdminRoute.UPDATE_WEIGHT) },
        )
      }
      item {
        AdminActionTile(
          title = "સ્ટોક અપડેટ કરો",
          icon = Icons.Filled.Inventory2,
          color = TilePurple,
          testTag = "admin_tile_stock",
          onClick = { onNavigate(AdminRoute.UPDATE_STOCK) },
        )
      }
      item {
        AdminActionTile(
          title = "અનાજ ડિલીટ કરો",
          icon = Icons.Filled.Delete,
          color = TileRed,
          testTag = "admin_tile_delete",
          onClick = { onNavigate(AdminRoute.DELETE_GRAIN) },
        )
      }
      item {
        AdminActionTile(
          title = "અનાજ યાદી જુઓ",
          icon = Icons.AutoMirrored.Filled.FormatListBulleted,
          color = TileCyan,
          testTag = "admin_tile_list",
          onClick = { onNavigate(AdminRoute.VIEW_LIST) },
        )
      }

      item(span = { GridItemSpan(2) }) {
        Card(
          modifier =
            Modifier.fillMaxWidth()
              .clip(MaterialTheme.shapes.large)
              .clickable { onNavigate(AdminRoute.CUSTOMER_LOGINS) }
              .testTag("admin_tile_logins"),
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              color = TileTeal,
              shape = CircleShape,
              modifier = Modifier.size(52.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.Group,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(28.dp),
                )
              }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "કોણે એપ લોગિન કરી છે",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "ગ્રાહકોના મોબાઈલ નંબર અને લોગિન સમય જુઓ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Surface(color = SacredGoldLight, shape = RoundedCornerShape(50)) {
              Text(
                text = "$loginCount",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CrimsonPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }
        }
      }

      item(span = { GridItemSpan(2) }) {
        Card(
          modifier =
            Modifier.fillMaxWidth()
              .clip(MaterialTheme.shapes.large)
              .clickable { onNavigate(AdminRoute.APP_QR_SHARE) }
              .testTag("admin_tile_qr_share"),
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              color = CrimsonPrimary,
              shape = CircleShape,
              modifier = Modifier.size(52.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.QrCode2,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(28.dp),
                )
              }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "એપ ડાઉનલોડ લિંક & QR કોડ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "ગ્રાહકોને સ્કેન કરવા માટે QR કોડ અને ડાઉનલોડ લિંક શેર કરો",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      if (grainCount == 0) {
        item(span = { GridItemSpan(2) }) {
          OutlinedButton(
            onClick = onSeedDefaults,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = MaterialTheme.shapes.medium,
          ) {
            Icon(Icons.Filled.CurrencyRupee, contentDescription = null, tint = CrimsonPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("ડિફોલ્ટ 8 અનાજ ઉમેરો (ઘઉં, ચોખા, બાજરી...)", color = CrimsonPrimary)
          }
        }
      }
    }
  }
}

@Composable
private fun AdminActionTile(
  title: String,
  icon: ImageVector,
  color: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Card(
    modifier =
      Modifier.fillMaxWidth()
        .height(132.dp)
        .clip(MaterialTheme.shapes.large)
        .clickable(onClick = onClick)
        .testTag(testTag),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(color = color, shape = CircleShape, modifier = Modifier.size(54.dp)) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
