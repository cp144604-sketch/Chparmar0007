package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppDownloadConfig
import com.example.data.model.Grain
import com.example.data.model.GrainCategory
import com.example.data.model.UiState
import com.example.ui.components.AppShareQrCard
import com.example.ui.components.GrainThumbnail
import com.example.ui.components.MahadevLogoBadge
import com.example.ui.components.StockStatusBadge
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.SacredGoldLight
import com.example.ui.theme.StockAvailableGreen
import com.example.ui.theme.WarmOutline
import com.example.ui.viewmodel.formatPrice
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  grainsState: UiState<List<Grain>>,
  downloadConfig: AppDownloadConfig,
  searchQuery: String,
  selectedCategory: GrainCategory,
  savedGrainIds: Set<String>,
  customerMobile: String,
  onSearchChange: (String) -> Unit,
  onSelectCategory: (GrainCategory) -> Unit,
  onGrainClick: (Grain) -> Unit,
  onToggleSave: (Grain, Boolean) -> Unit,
  onCopiedDownloadLink: () -> Unit,
  onCustomerLogout: () -> Unit,
) {
  var showQrDialog by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    TopAppBar(
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          MahadevLogoBadge(size = 36.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "મહાદેવ ટ્રેડર્સ",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier =
                  Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4ADE80))
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text =
                  if (customerMobile.isNotBlank()) "હળવદ • $customerMobile"
                  else "હળવદ • લાઈવ ભાવ",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.88f),
              )
            }
          }
        }
      },
      actions = {
        IconButton(
          onClick = { showQrDialog = true },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("home_qr_share_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.QrCode2,
            contentDescription = "એપ ડાઉનલોડ QR કોડ",
            tint = Color.White,
          )
        }
        IconButton(
          onClick = onCustomerLogout,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("customer_logout_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = "ગ્રાહક લોગઆઉટ",
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
      // Search & Category Bar
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("અનાજ શોધો... (જેમકે ઘઉં, ચોખા, દાળ)") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "અનાજ શોધો",
                tint = CrimsonPrimary,
              )
            },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchChange("") }) {
                  Icon(Icons.Filled.Close, contentDescription = "સર્ચ સાફ કરો")
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
            modifier = Modifier.fillMaxWidth().testTag("home_search_input"),
          )

          Spacer(modifier = Modifier.height(10.dp))

          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GrainCategory.entries) { category ->
              val isSelected = selectedCategory == category
              FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(category) },
                label = {
                  Text(
                    text = category.labelGu,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  )
                },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CrimsonPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = ParchmentSurface,
                  ),
                modifier = Modifier.testTag("category_chip_${category.name}"),
              )
            }
          }
        }
      }

      when (grainsState) {
        UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = CrimsonPrimary)
              Spacer(modifier = Modifier.height(12.dp))
              Text("તાજા ભાવ લોડ થઈ રહ્યા છે...", style = MaterialTheme.typography.bodyMedium)
            }
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
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        }
        is UiState.Success -> {
          val filtered =
            remember(grainsState.data, searchQuery, selectedCategory) {
              val q = searchQuery.trim().lowercase()
              grainsState.data.filter { grain ->
                val matchesCat =
                  selectedCategory == GrainCategory.ALL ||
                    grain.category == selectedCategory.labelGu
                val matchesQuery =
                  q.isBlank() ||
                    grain.name.lowercase().contains(q) ||
                    grain.category.lowercase().contains(q) ||
                    grain.weightLabel.lowercase().contains(q) ||
                    grain.stockStatus.lowercase().contains(q)
                matchesCat && matchesQuery
              }
            }

          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "અનાજની યાદી",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Surface(color = SacredGoldLight, shape = RoundedCornerShape(50)) {
              Text(
                text = "${filtered.size} વસ્તુઓ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }

          if (filtered.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize().padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Filled.Grain,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text =
                    if (grainsState.data.isEmpty())
                      "હાલમાં કોઈ અનાજ ઉમેરાયેલ નથી.\nએડમિન દ્વારા અનાજ ઉમેરતા અહીં તરત દેખાશે."
                    else "‘$searchQuery’ માટે કોઈ અનાજ મળ્યું નથી.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(filtered, key = { it.id }) { grain ->
                val isSaved = grain.id in savedGrainIds
                GrainListCard(
                  grain = grain,
                  isSaved = isSaved,
                  onClick = { onGrainClick(grain) },
                  onToggleSave = { onToggleSave(grain, isSaved) },
                )
              }
            }
          }
        }
      }
    }
  }

  if (showQrDialog) {
    AlertDialog(
      onDismissRequest = { showQrDialog = false },
      confirmButton = {
        Button(
          onClick = { showQrDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
        ) {
          Text("બંધ કરો")
        }
      },
      text = {
        AppShareQrCard(
          titleGu = downloadConfig.titleGu,
          downloadUrl = downloadConfig.downloadUrl,
          onCopiedLink = onCopiedDownloadLink,
        )
      },
    )
  }
}

@Composable
private fun GrainListCard(
  grain: Grain,
  isSaved: Boolean,
  onClick: () -> Unit,
  onToggleSave: () -> Unit,
) {
  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .testTag("grain_card_${grain.id}"),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      GrainThumbnail(
        iconKey = grain.iconKey,
        imageBase64 = grain.imageBase64,
        grainName = grain.name,
        size = 60.dp,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = grain.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "₹ ${formatPrice(grain.pricePerKg)} / Kg",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = CrimsonPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          StockStatusBadge(stockLabel = grain.stockStatus)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = grain.weightLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      IconButton(
        onClick = onToggleSave,
        modifier =
          Modifier.minimumInteractiveComponentSize().testTag("bookmark_grain_${grain.id}"),
      ) {
        Icon(
          imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
          contentDescription = if (isSaved) "યાદીમાંથી દૂર કરો" else "મારી યાદીમાં ઉમેરો",
          tint = if (isSaved) CrimsonPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "વિગત જુઓ",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrainDetailScreen(
  grain: Grain,
  isSaved: Boolean,
  onToggleSave: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val formattedDate =
    remember(grain.updatedAt, grain.createdAt) {
      val ts = grain.updatedAt ?: grain.createdAt ?: Timestamp.now()
      SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(ts.toDate())
    }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    TopAppBar(
      title = { Text("ઉત્પાદન વિગત", color = Color.White, fontWeight = FontWeight.Bold) },
      navigationIcon = {
        IconButton(
          onClick = onBack,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_top_back_button"),
        ) {
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
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier = Modifier.fillMaxWidth().padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          GrainThumbnail(
            iconKey = grain.iconKey,
            imageBase64 = grain.imageBase64,
            grainName = grain.name,
            size = 180.dp,
            cornerRadius = 24.dp,
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          DetailRow(label = "નામ", value = grain.name, isLargeTitle = true)
          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WarmOutline)

          Text(
            text = "ભાવ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "₹ ${formatPrice(grain.pricePerKg)} / Kg",
            style = MaterialTheme.typography.headlineMedium,
            color = CrimsonPrimary,
            fontWeight = FontWeight.ExtraBold,
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WarmOutline)
          DetailRow(label = "વજન", value = grain.weightLabel)

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WarmOutline)
          DetailRow(label = "કેટેગરી", value = grain.category)

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WarmOutline)
          Text(
            text = "સ્ટોક",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(6.dp))
          StockStatusBadge(stockLabel = grain.stockStatus)

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WarmOutline)
          DetailRow(label = "છેલ્લું અપડેટ", value = formattedDate)

          if (grain.notes.isNotBlank()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WarmOutline)
            DetailRow(label = "નોંધ", value = grain.notes)
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      OutlinedButton(
        onClick = onToggleSave,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("detail_mylist_button"),
        shape = MaterialTheme.shapes.medium,
      ) {
        Icon(
          imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
          contentDescription = null,
          tint = if (isSaved) StockAvailableGreen else CrimsonPrimary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isSaved) "મારી યાદીમાં ઉમેરેલ છે (દૂર કરો)" else "મારી યાદીમાં ઉમેરો",
          color = if (isSaved) StockAvailableGreen else CrimsonPrimary,
          fontWeight = FontWeight.Bold,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Button(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("detail_back_button"),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
      ) {
        Text("પાછા જાઓ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun DetailRow(label: String, value: String, isLargeTitle: Boolean = false) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = value,
      style =
        if (isLargeTitle) MaterialTheme.typography.headlineSmall
        else MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
