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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Grain
import com.example.data.model.MyListItem
import com.example.data.model.UiState
import com.example.ui.components.GrainThumbnail
import com.example.ui.components.StockStatusBadge
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.WarmOutline
import com.example.ui.viewmodel.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
  myListState: UiState<List<MyListItem>>,
  liveGrainsState: UiState<List<Grain>>,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  onOpenGrainDetail: (Grain) -> Unit,
  onUpdateNote: (Grain, String) -> Unit,
  onRemoveItem: (String, String) -> Unit,
) {
  val liveGrainsById =
    remember(liveGrainsState) {
      (liveGrainsState as? UiState.Success)?.data?.associateBy { it.id } ?: emptyMap()
    }

  var editingItem by remember { mutableStateOf<Pair<MyListItem, Grain?>?>(null) }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    TopAppBar(
      title = {
        Text(
          text = "મારી યાદી",
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
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
          placeholder = { Text("મારી યાદીમાં શોધો...") },
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
              .testTag("mylist_search_input"),
        )
      }

      when (myListState) {
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
                text = myListState.message,
                color = CrimsonPrimary,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        }
        is UiState.Success -> {
          val filtered =
            remember(myListState.data, liveGrainsById, searchQuery) {
              val q = searchQuery.trim().lowercase()
              myListState.data.filter { item ->
                val live = liveGrainsById[item.grainId]
                val name = live?.name ?: item.grainName
                val note = item.quantityNote
                q.isBlank() ||
                  name.lowercase().contains(q) ||
                  note.lowercase().contains(q)
              }
            }

          if (filtered.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize().padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Filled.PlaylistAddCheck,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(60.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text =
                    if (myListState.data.isEmpty())
                      "તમારી યાદી ખાલી છે.\nહોમ પેજ પરથી પસંદગીના અનાજ ઉમેરો!"
                    else "‘$searchQuery’ માટે કોઈ વસ્તુ મળી નથી.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(filtered, key = { it.id }) { item ->
                val liveGrain = liveGrainsById[item.grainId]
                val displayName = liveGrain?.name ?: item.grainName
                val displayPrice = liveGrain?.pricePerKg ?: item.pricePerKg
                val displayWeight = liveGrain?.weightLabel ?: item.weightLabel

                Card(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(MaterialTheme.shapes.medium)
                      .clickable { if (liveGrain != null) onOpenGrainDetail(liveGrain) }
                      .testTag("mylist_card_${item.grainId}"),
                  shape = MaterialTheme.shapes.medium,
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    GrainThumbnail(
                      iconKey = liveGrain?.iconKey ?: "grocery",
                      imageBase64 = liveGrain?.imageBase64 ?: "",
                      grainName = displayName,
                      size = 56.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = "₹ ${formatPrice(displayPrice)} / Kg • $displayWeight",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.Bold,
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        if (liveGrain != null) {
                          StockStatusBadge(stockLabel = liveGrain.stockStatus)
                          Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (item.quantityNote.isNotBlank()) {
                          Surface(color = ParchmentSurface, shape = RoundedCornerShape(50)) {
                            Text(
                              text = item.quantityNote,
                              style = MaterialTheme.typography.labelSmall,
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                          }
                        }
                      }
                    }
                    IconButton(
                      onClick = { editingItem = item to liveGrain },
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.EditNote,
                        contentDescription = "જથ્થો બદલો",
                        tint = MaterialTheme.colorScheme.primary,
                      )
                    }
                    IconButton(
                      onClick = { onRemoveItem(item.grainId, displayName) },
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .testTag("remove_mylist_${item.grainId}"),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.BookmarkRemove,
                        contentDescription = "દૂર કરો",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

  editingItem?.let { (item, liveGrain) ->
    var noteText by remember(item.grainId) { mutableStateOf(item.quantityNote) }
    AlertDialog(
      onDismissRequest = { editingItem = null },
      title = { Text("${liveGrain?.name ?: item.grainName} - જથ્થાની નોંધ") },
      text = {
        OutlinedTextField(
          value = noteText,
          onValueChange = { if (it.length <= 50) noteText = it },
          label = { Text("જથ્થો (જેમકે 2 બેગ / 25 Kg)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (liveGrain != null) {
              onUpdateNote(liveGrain, noteText)
            }
            editingItem = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
        ) {
          Text("સેવ કરો")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingItem = null }) { Text("રદ કરો") }
      },
    )
  }
}
