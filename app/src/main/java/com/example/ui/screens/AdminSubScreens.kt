package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppDownloadConfig
import com.example.data.model.CustomerSession
import com.example.data.model.Grain
import com.example.data.model.GrainCategory
import com.example.data.model.GrainIconKey
import com.example.data.model.StockStatus
import com.example.data.model.UiState
import com.example.ui.components.AppShareQrCard
import com.example.ui.components.GrainThumbnail
import com.example.ui.components.ImageUtils
import com.example.ui.components.StockStatusBadge
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.SacredGoldLight
import com.example.ui.theme.StockAvailableGreen
import com.example.ui.theme.TileBlue
import com.example.ui.theme.TileGreen
import com.example.ui.theme.TileOrange
import com.example.ui.theme.TileRed
import com.example.ui.theme.TileTeal
import com.example.ui.theme.WarmOutline
import com.example.ui.viewmodel.formatPrice
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

enum class AdminActionMode {
  PRICE,
  WEIGHT,
  STOCK,
  DELETE,
  VIEW_ALL,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditGrainScreen(
  existingGrain: Grain?,
  isBusy: Boolean,
  onSave:
    (
      name: String,
      category: String,
      iconKey: String,
      pricePerKg: Double,
      weightLabel: String,
      stockStatus: StockStatus,
      notes: String,
      imageBase64: String,
    ) -> Unit,
  onCancel: () -> Unit,
) {
  BackHandler(onBack = onCancel)

  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var name by remember(existingGrain?.id) { mutableStateOf(existingGrain?.name ?: "") }
  var category by remember(existingGrain?.id) { mutableStateOf(existingGrain?.category ?: "અનાજ") }
  var iconKey by remember(existingGrain?.id) { mutableStateOf(existingGrain?.iconKey ?: "wheat") }
  var priceText by
    remember(existingGrain?.id) {
      mutableStateOf(if (existingGrain != null) formatPrice(existingGrain.pricePerKg) else "")
    }
  var weightLabel by
    remember(existingGrain?.id) { mutableStateOf(existingGrain?.weightLabel ?: "50 Kg (બેગ)") }
  var stockStatus by
    remember(existingGrain?.id) {
      mutableStateOf(StockStatus.fromLabel(existingGrain?.stockStatus))
    }
  var notes by remember(existingGrain?.id) { mutableStateOf(existingGrain?.notes ?: "") }
  var imageBase64 by
    remember(existingGrain?.id) { mutableStateOf(existingGrain?.imageBase64 ?: "") }
  var formError by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
      onResult = { uri ->
        if (uri != null) {
          scope.launch {
            val encoded = ImageUtils.compressUriToBase64(context, uri)
            if (encoded.isNotBlank()) {
              imageBase64 = encoded
            }
          }
        }
      },
    )

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    TopAppBar(
      title = {
        Text(
          text = if (existingGrain == null) "નવું અનાજ ઉમેરો" else "અનાજ સુધારો",
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
      },
      navigationIcon = {
        IconButton(onClick = onCancel, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "પાછા જાઓ",
            tint = Color.White,
          )
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = CrimsonPrimary),
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = 600.dp)
          .align(Alignment.CenterHorizontally)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
    ) {
      Surface(
        modifier =
          Modifier.fillMaxWidth()
            .height(140.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            }
            .testTag("pick_grain_photo_button"),
        shape = MaterialTheme.shapes.large,
        color = ParchmentSurface,
        tonalElevation = 1.dp,
      ) {
        Box(contentAlignment = Alignment.Center) {
          if (imageBase64.isNotBlank()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              GrainThumbnail(
                iconKey = iconKey,
                imageBase64 = imageBase64,
                grainName = name,
                size = 104.dp,
              )
              Column {
                Text("ફોટો પસંદ થયેલ છે", fontWeight = FontWeight.Bold)
                TextButton(onClick = { imageBase64 = "" }) {
                  Text("ફોટો દૂર કરો", color = CrimsonPrimary)
                }
              }
            }
          } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Filled.AddAPhoto,
                contentDescription = "ફોટો ઉમેરો",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(38.dp),
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "ફોટો ઉમેરો",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      Text("આઇકોન પસંદ કરો", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(GrainIconKey.entries) { item ->
          val selected = iconKey == item.key
          FilterChip(
            selected = selected,
            onClick = { iconKey = item.key },
            label = { Text(item.labelGu) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = CrimsonPrimary,
                selectedLabelColor = Color.White,
              ),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = name,
        onValueChange = {
          if (it.length <= 80) name = it
          formError = null
        },
        label = { Text("અનાજનું નામ") },
        placeholder = { Text("અહીં નામ લખો (જેમકે ઘઉં)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("grain_name_input"),
      )

      Spacer(modifier = Modifier.height(12.dp))
      Text("કેટેગરી", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(GrainCategory.entries.filter { it != GrainCategory.ALL }) { cat ->
          FilterChip(
            selected = category == cat.labelGu,
            onClick = { category = cat.labelGu },
            label = { Text(cat.labelGu) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = CrimsonPrimary,
                selectedLabelColor = Color.White,
              ),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = priceText,
        onValueChange = { input ->
          priceText = input.filter { it.isDigit() || it == '.' }
          formError = null
        },
        label = { Text("ભાવ (₹ / Kg)") },
        placeholder = { Text("અહીં ભાવ લખો") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("grain_price_input"),
      )

      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = weightLabel,
        onValueChange = {
          if (it.length <= 50) weightLabel = it
          formError = null
        },
        label = { Text("વજન") },
        placeholder = { Text("જેમકે 50 Kg (બેગ)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("grain_weight_input"),
      )

      Spacer(modifier = Modifier.height(12.dp))
      Text("સ્ટોક સ્થિતિ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StockStatus.entries.forEach { st ->
          FilterChip(
            selected = stockStatus == st,
            onClick = { stockStatus = st },
            label = { Text(st.labelGu) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = StockAvailableGreen,
                selectedLabelColor = Color.White,
              ),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = notes,
        onValueChange = { if (it.length <= 250) notes = it },
        label = { Text("વધારાની નોંધ (વૈકલ્પિક)") },
        placeholder = { Text("જેમકે લોકવન ઘઉં • હોલસેલ ભાવ") },
        maxLines = 3,
        modifier = Modifier.fillMaxWidth().testTag("grain_notes_input"),
      )

      if (!formError.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = formError!!, color = CrimsonPrimary, fontWeight = FontWeight.SemiBold)
      }

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = {
          val parsedPrice = priceText.toDoubleOrNull()
          if (name.isBlank()) {
            formError = "કૃપા કરીને અનાજનું નામ લખો."
          } else if (parsedPrice == null || parsedPrice < 0.0) {
            formError = "કૃપા કરીને સાચો ભાવ લખો."
          } else if (weightLabel.isBlank()) {
            formError = "કૃપા કરીને વજન લખો."
          } else {
            onSave(name, category, iconKey, parsedPrice, weightLabel, stockStatus, notes, imageBase64)
          }
        },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_grain_button"),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = TileGreen),
      ) {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.5.dp,
          )
        } else {
          Text("સેવ કરો", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("cancel_grain_button"),
        shape = MaterialTheme.shapes.medium,
      ) {
        Text("રદ કરો", color = CrimsonPrimary, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGrainActionListScreen(
  title: String,
  mode: AdminActionMode,
  grainsState: UiState<List<Grain>>,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  onQuickUpdatePrice: (Grain, Double, () -> Unit) -> Unit,
  onQuickUpdateWeight: (Grain, String, () -> Unit) -> Unit,
  onQuickUpdateStock: (Grain, StockStatus) -> Unit,
  onDeleteGrain: (Grain) -> Unit,
  onEditFullGrain: (Grain) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var priceEditTarget by remember { mutableStateOf<Grain?>(null) }
  var weightEditTarget by remember { mutableStateOf<Grain?>(null) }
  var deleteConfirmTarget by remember { mutableStateOf<Grain?>(null) }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    TopAppBar(
      title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
      navigationIcon = {
        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "પાછા જાઓ",
            tint = Color.White,
          )
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = CrimsonPrimary),
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = 720.dp)
          .align(Alignment.CenterHorizontally)
    ) {
      Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchChange,
          placeholder = { Text("અનાજ શોધો...") },
          leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = "શોધો", tint = CrimsonPrimary)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchChange("") }) {
                Icon(Icons.Filled.Close, contentDescription = "સાફ કરો")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrimsonPrimary,
              unfocusedBorderColor = WarmOutline,
              focusedContainerColor = ParchmentSurface.copy(alpha = 0.45f),
              unfocusedContainerColor = ParchmentSurface.copy(alpha = 0.45f),
            ),
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("admin_grain_search_input"),
        )
      }

      when (grainsState) {
        UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CrimsonPrimary)
          }
        }
        is UiState.Error -> {
          Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Card(colors = CardDefaults.cardColors(containerColor = CrimsonContainer)) {
              Text(
                text = grainsState.message,
                color = CrimsonPrimary,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        }
        is UiState.Success -> {
          val filtered =
            remember(grainsState.data, searchQuery) {
              val q = searchQuery.trim().lowercase()
              grainsState.data.filter { grain ->
                q.isBlank() ||
                  grain.name.lowercase().contains(q) ||
                  grain.category.lowercase().contains(q) ||
                  grain.weightLabel.lowercase().contains(q)
              }
            }

          if (filtered.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize().padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "કોઈ અનાજ મળ્યું નથી.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(filtered, key = { it.id }) { grain ->
                Card(
                  shape = MaterialTheme.shapes.medium,
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                  Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      GrainThumbnail(
                        iconKey = grain.iconKey,
                        imageBase64 = grain.imageBase64,
                        grainName = grain.name,
                        size = 52.dp,
                      )
                      Spacer(modifier = Modifier.width(12.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = grain.name,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                        )
                        Text(
                          text = "₹ ${formatPrice(grain.pricePerKg)} / Kg • ${grain.weightLabel}",
                          style = MaterialTheme.typography.bodyMedium,
                          color = CrimsonPrimary,
                          fontWeight = FontWeight.SemiBold,
                        )
                      }
                      when (mode) {
                        AdminActionMode.PRICE -> {
                          Button(
                            onClick = { priceEditTarget = grain },
                            colors = ButtonDefaults.buttonColors(containerColor = TileOrange),
                            modifier = Modifier.testTag("edit_price_btn_${grain.id}"),
                          ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ભાવ")
                          }
                        }
                        AdminActionMode.WEIGHT -> {
                          Button(
                            onClick = { weightEditTarget = grain },
                            colors = ButtonDefaults.buttonColors(containerColor = TileBlue),
                            modifier = Modifier.testTag("edit_weight_btn_${grain.id}"),
                          ) {
                            Icon(Icons.Filled.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("વજન")
                          }
                        }
                        AdminActionMode.DELETE -> {
                          IconButton(
                            onClick = { deleteConfirmTarget = grain },
                            modifier =
                              Modifier.minimumInteractiveComponentSize()
                                .testTag("delete_grain_btn_${grain.id}"),
                          ) {
                            Icon(Icons.Filled.Delete, contentDescription = "ડિલીટ કરો", tint = TileRed)
                          }
                        }
                        AdminActionMode.VIEW_ALL -> {
                          OutlinedButton(onClick = { onEditFullGrain(grain) }) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("સુધારો")
                          }
                        }
                        AdminActionMode.STOCK -> {
                          StockStatusBadge(stockLabel = grain.stockStatus)
                        }
                      }
                    }

                    if (mode == AdminActionMode.STOCK) {
                      Spacer(modifier = Modifier.height(10.dp))
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StockStatus.entries.forEach { st ->
                          val selected = grain.stockStatus == st.labelGu
                          FilterChip(
                            selected = selected,
                            onClick = { onQuickUpdateStock(grain, st) },
                            label = { Text(st.labelGu) },
                            colors =
                              FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StockAvailableGreen,
                                selectedLabelColor = Color.White,
                              ),
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  priceEditTarget?.let { grain ->
    var priceStr by remember(grain.id) { mutableStateOf(formatPrice(grain.pricePerKg)) }
    AlertDialog(
      onDismissRequest = { priceEditTarget = null },
      title = { Text("${grain.name} - નવો ભાવ (₹ / Kg)") },
      text = {
        OutlinedTextField(
          value = priceStr,
          onValueChange = { priceStr = it.filter { c -> c.isDigit() || c == '.' } },
          label = { Text("ભાવ (₹ / Kg)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("dialog_price_input"),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val newPrice = priceStr.toDoubleOrNull()
            if (newPrice != null && newPrice >= 0.0) {
              onQuickUpdatePrice(grain, newPrice) { priceEditTarget = null }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
          modifier = Modifier.testTag("dialog_price_save"),
        ) {
          Text("અપડેટ કરો")
        }
      },
      dismissButton = { TextButton(onClick = { priceEditTarget = null }) { Text("રદ કરો") } },
    )
  }

  weightEditTarget?.let { grain ->
    var weightStr by remember(grain.id) { mutableStateOf(grain.weightLabel) }
    AlertDialog(
      onDismissRequest = { weightEditTarget = null },
      title = { Text("${grain.name} - વજન બદલો") },
      text = {
        OutlinedTextField(
          value = weightStr,
          onValueChange = { if (it.length <= 50) weightStr = it },
          label = { Text("વજન (જેમકે 50 Kg (બેગ))") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("dialog_weight_input"),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (weightStr.isNotBlank()) {
              onQuickUpdateWeight(grain, weightStr) { weightEditTarget = null }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = TileBlue),
          modifier = Modifier.testTag("dialog_weight_save"),
        ) {
          Text("અપડેટ કરો")
        }
      },
      dismissButton = { TextButton(onClick = { weightEditTarget = null }) { Text("રદ કરો") } },
    )
  }

  deleteConfirmTarget?.let { grain ->
    AlertDialog(
      onDismissRequest = { deleteConfirmTarget = null },
      title = { Text("${grain.name} ડિલીટ કરવું છે?") },
      text = { Text("આ અનાજ ગ્રાહકોની યાદીમાંથી પણ તરત દૂર થઈ જશે.") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteGrain(grain)
            deleteConfirmTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = TileRed),
          modifier = Modifier.testTag("dialog_delete_confirm"),
        ) {
          Text("હા, ડિલીટ કરો")
        }
      },
      dismissButton = { TextButton(onClick = { deleteConfirmTarget = null }) { Text("રદ કરો") } },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomerLoginsScreen(
  sessionsState: UiState<List<CustomerSession>>,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    TopAppBar(
      title = { Text("કોણે એપ લોગિન કરી છે", color = Color.White, fontWeight = FontWeight.Bold) },
      navigationIcon = {
        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "પાછા જાઓ",
            tint = Color.White,
          )
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = CrimsonPrimary),
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = 720.dp)
          .align(Alignment.CenterHorizontally)
    ) {
      Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchChange,
          placeholder = { Text("મોબાઈલ નંબર કે નામ શોધો...") },
          leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = "શોધો", tint = CrimsonPrimary)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchChange("") }) {
                Icon(Icons.Filled.Close, contentDescription = "સાફ કરો")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CrimsonPrimary,
              unfocusedBorderColor = WarmOutline,
              focusedContainerColor = ParchmentSurface.copy(alpha = 0.45f),
              unfocusedContainerColor = ParchmentSurface.copy(alpha = 0.45f),
            ),
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("admin_login_search_input"),
        )
      }

      when (sessionsState) {
        UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CrimsonPrimary)
          }
        }
        is UiState.Error -> {
          Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Card(colors = CardDefaults.cardColors(containerColor = CrimsonContainer)) {
              Text(
                text = sessionsState.message,
                color = CrimsonPrimary,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        }
        is UiState.Success -> {
          val filtered =
            remember(sessionsState.data, searchQuery) {
              val q = searchQuery.trim().lowercase()
              sessionsState.data.filter { s ->
                q.isBlank() ||
                  s.mobileNumber.lowercase().contains(q) ||
                  s.customerName.lowercase().contains(q) ||
                  s.email.lowercase().contains(q)
              }
            }

          if (filtered.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize().padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "હજુ સુધી કોઈ લોગિન નોંધાયેલ નથી.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(filtered, key = { it.id }) { session ->
                val dateStr =
                  remember(session.lastLoginAt) {
                    val ts = session.lastLoginAt ?: session.createdAt ?: Timestamp.now()
                    SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(ts.toDate())
                  }
                Card(
                  shape = MaterialTheme.shapes.medium,
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Surface(
                      color =
                        if (session.roleLabel == "admin") CrimsonPrimary else TileTeal,
                      shape = CircleShape,
                      modifier = Modifier.size(46.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Filled.Person,
                          contentDescription = null,
                          tint = Color.White,
                        )
                      }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = session.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                          imageVector = Icons.Filled.Phone,
                          contentDescription = null,
                          tint = CrimsonPrimary,
                          modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = session.mobileNumber,
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.SemiBold,
                          color = CrimsonPrimary,
                        )
                      }
                      Text(
                        text = "છેલ્લું લોગિન: $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      Surface(color = SacredGoldLight, shape = RoundedCornerShape(50)) {
                        Text(
                          text =
                            if (session.roleLabel == "admin") "એડમિન"
                            else "${session.loginCount} વખત",
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = CrimsonPrimary,
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppDownloadQrScreen(
  downloadConfig: AppDownloadConfig,
  isBusy: Boolean,
  onSaveConfig: (downloadUrl: String, titleGu: String, onDone: () -> Unit) -> Unit,
  onCopiedLink: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var urlText by remember(downloadConfig.downloadUrl) { mutableStateOf(downloadConfig.downloadUrl) }
  var titleText by remember(downloadConfig.titleGu) { mutableStateOf(downloadConfig.titleGu) }
  var errorText by remember { mutableStateOf<String?>(null) }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    TopAppBar(
      title = { Text("એપ ડાઉનલોડ લિંક & QR કોડ", color = Color.White, fontWeight = FontWeight.Bold) },
      navigationIcon = {
        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "પાછા જાઓ",
            tint = Color.White,
          )
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = CrimsonPrimary),
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = 600.dp)
          .align(Alignment.CenterHorizontally)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      AppShareQrCard(
        titleGu = titleText.ifBlank { downloadConfig.titleGu },
        downloadUrl = urlText.trim().ifBlank { downloadConfig.downloadUrl },
        onCopiedLink = onCopiedLink,
      )

      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Text(
            text = "ડાઉનલોડ લિંક બદલો / સેટ કરો",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text =
              "તમારી APK ફાઇલ (Google Drive / Play Store / Website) ની લિંક અહીં સેવ કરો જેથી QR કોડ અને શેર લિંક તરત અપડેટ થઈ જાય.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = titleText,
            onValueChange = { if (it.length <= 80) titleText = it },
            label = { Text("QR કોડ શીર્ષક") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("qr_title_input"),
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = urlText,
            onValueChange = {
              if (it.length <= 500) urlText = it
              errorText = null
            },
            label = { Text("એપ ડાઉનલોડ લિંક (URL)") },
            placeholder = { Text("https://...") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("qr_url_input"),
          )

          if (!errorText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorText!!, color = CrimsonPrimary, style = MaterialTheme.typography.bodySmall)
          }

          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = {
              val clean = urlText.trim()
              if (!clean.startsWith("https://") && !clean.startsWith("http://")) {
                errorText = "કૃપા કરીને https:// અથવા http:// થી શરૂ થતી માન્ય લિંક લખો."
              } else {
                onSaveConfig(clean, titleText.trim()) {}
              }
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_qr_url_button"),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = TileGreen),
          ) {
            if (isBusy) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
              )
            } else {
              Text("લિંક અને QR સેવ કરો", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
