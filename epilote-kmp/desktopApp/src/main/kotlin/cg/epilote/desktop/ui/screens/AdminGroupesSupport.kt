package cg.epilote.desktop.ui.screens
  
  import androidx.compose.foundation.background
  import androidx.compose.foundation.layout.ExperimentalLayoutApi
  import androidx.compose.foundation.layout.FlowRow
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.filled.*
  import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
  import cg.epilote.desktop.ui.screens.superadmin.KpiCard
  import cg.epilote.desktop.ui.theme.cursorHand
  
  @Composable
  internal fun GroupesKpiRow(total: Int, actifs: Int, ecoles: Int, utilisateurs: Int) {
      val pctActifs = if (total > 0) (actifs * 100 / total) else 0
      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
          if (maxWidth < 980.dp) {
              Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                  Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                      KpiCard(
                          modifier = Modifier.weight(1f),
                          icon = Icons.Default.Business,
                          iconBg = Color(0xFFDBEAFE),
                          iconTint = Color(0xFF3B82F6),
                          label = "Total Groupes",
                          value = "$total",
                          trend = if (actifs > 0) "↑" else "",
                          trendLabel = "$actifs actifs"
                      )
                      KpiCard(
                          modifier = Modifier.weight(1f),
                          icon = Icons.Default.CheckCircle,
                          iconBg = Color(0xFFD1FAE5),
                          iconTint = Color(0xFF059669),
                          label = "Groupes Actifs",
                          value = "$actifs",
                          trend = "$pctActifs%",
                          trendLabel = "en activité",
                          trendColor = Color(0xFF059669)
                      )
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                      KpiCard(
                          modifier = Modifier.weight(1f),
                          icon = Icons.Default.School,
                          iconBg = Color(0xFFFEE2E2),
                          iconTint = Color(0xFFE76F51),
                          label = "Total Écoles",
                          value = "$ecoles",
                          trendLabel = "rattachées"
                      )
                      KpiCard(
                          modifier = Modifier.weight(1f),
                          icon = Icons.Default.People,
                          iconBg = Color(0xFFEDE9FE),
                          iconTint = Color(0xFF7C3AED),
                          label = "Utilisateurs",
                          value = "$utilisateurs",
                          trendLabel = "sur la plateforme"
                      )
                  }
              }
          } else {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                  KpiCard(
                      modifier = Modifier.weight(1f),
                      icon = Icons.Default.Business,
                      iconBg = Color(0xFFDBEAFE),
                      iconTint = Color(0xFF3B82F6),
                      label = "Total Groupes",
                      value = "$total",
                      trend = if (actifs > 0) "↑" else "",
                      trendLabel = "$actifs actifs"
                  )
                  KpiCard(
                      modifier = Modifier.weight(1f),
                      icon = Icons.Default.CheckCircle,
                      iconBg = Color(0xFFD1FAE5),
                      iconTint = Color(0xFF059669),
                      label = "Groupes Actifs",
                      value = "$actifs",
                      trend = "$pctActifs%",
                      trendLabel = "en activité",
                      trendColor = Color(0xFF059669)
                  )
                  KpiCard(
                      modifier = Modifier.weight(1f),
                      icon = Icons.Default.School,
                      iconBg = Color(0xFFFEE2E2),
                      iconTint = Color(0xFFE76F51),
                      label = "Total Écoles",
                      value = "$ecoles",
                      trendLabel = "rattachées"
                  )
                  KpiCard(
                      modifier = Modifier.weight(1f),
                      icon = Icons.Default.People,
                      iconBg = Color(0xFFEDE9FE),
                      iconTint = Color(0xFF7C3AED),
                      label = "Utilisateurs",
                      value = "$utilisateurs",
                      trendLabel = "sur la plateforme"
                  )
              }
          }
      }
  }
  
  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  internal fun GroupesToolbar(
      searchQuery: String,
      onSearchChange: (String) -> Unit,
      filterStatus: String,
      onFilterChange: (String) -> Unit,
      sortBy: String,
      onSortChange: (String) -> Unit,
      viewMode: String,
      onViewModeChange: (String) -> Unit,
      onRefresh: () -> Unit,
      onNewGroupe: () -> Unit,
      totalResults: Int
  ) {
      var showFilterMenu by remember { mutableStateOf(false) }
      var showSortMenu by remember { mutableStateOf(false) }

      Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(1.dp)
      ) {
          BoxWithConstraints {
              val availableWidth = maxWidth
              Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                  if (availableWidth < 980.dp) {
                      OutlinedTextField(
                          value = searchQuery,
                          onValueChange = onSearchChange,
                          placeholder = { Text("Rechercher un groupe, département, ville...", fontSize = 13.sp) },
                          leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = Color(0xFF94A3B8)) },
                          modifier = Modifier.fillMaxWidth(),
                          singleLine = true,
                          shape = RoundedCornerShape(12.dp),
                          textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                          colors = OutlinedTextFieldDefaults.colors(
                              unfocusedBorderColor = Color(0xFFE2E8F0),
                              focusedBorderColor = Color(0xFF3B82F6)
                          )
                      )
                      FlowRow(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(10.dp),
                          verticalArrangement = Arrangement.spacedBy(10.dp)
                      ) {
                          Box {
                              OutlinedButton(
                                  onClick = { showFilterMenu = true },
                                  shape = RoundedCornerShape(10.dp),
                                  border = ButtonDefaults.outlinedButtonBorder,
                                  modifier = Modifier.cursorHand()
                              ) {
                                  Text(when (filterStatus) { "actif" -> "Actifs"; "inactif" -> "Inactifs"; else -> "Tous" }, fontSize = 13.sp)
                                  Spacer(Modifier.width(4.dp))
                                  Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp))
                              }
                              DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                                  listOf("all" to "Tous", "actif" to "Actifs", "inactif" to "Inactifs").forEach { (key, label) ->
                                      DropdownMenuItem(text = { Text(label) }, onClick = { onFilterChange(key); showFilterMenu = false })
                                  }
                              }
                          }
                          Box {
                              OutlinedButton(
                                  onClick = { showSortMenu = true },
                                  shape = RoundedCornerShape(10.dp),
                                  border = ButtonDefaults.outlinedButtonBorder,
                                  modifier = Modifier.cursorHand()
                              ) {
                                  Text(when (sortBy) { "nom" -> "Nom"; "ecoles" -> "Écoles"; else -> "Plus récents" }, fontSize = 13.sp)
                                  Spacer(Modifier.width(4.dp))
                                  Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp))
                              }
                              DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                  listOf("recent" to "Plus récents", "nom" to "Par nom", "ecoles" to "Par écoles").forEach { (key, label) ->
                                      DropdownMenuItem(text = { Text(label) }, onClick = { onSortChange(key); showSortMenu = false })
                                  }
                              }
                          }
                          Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
                              Row(Modifier.padding(2.dp)) {
                                  IconButton(onClick = { onViewModeChange("card") }, modifier = Modifier.size(32.dp).cursorHand()) {
                                      Icon(Icons.Default.ViewModule, "Cartes", Modifier.size(18.dp), tint = if (viewMode == "card") Color(0xFF1D3557) else Color(0xFF94A3B8))
                                  }
                                  IconButton(onClick = { onViewModeChange("table") }, modifier = Modifier.size(32.dp).cursorHand()) {
                                      Icon(Icons.Default.TableChart, "Tableau", Modifier.size(18.dp), tint = if (viewMode == "table") Color(0xFF1D3557) else Color(0xFF94A3B8))
                                  }
                              }
                          }
                          IconButton(onClick = onRefresh, modifier = Modifier.cursorHand()) {
                              Icon(Icons.Default.Refresh, "Actualiser", Modifier.size(20.dp), tint = Color(0xFF64748B))
                          }
                          Button(
                              onClick = onNewGroupe,
                              shape = RoundedCornerShape(10.dp),
                              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                              modifier = Modifier.cursorHand()
                          ) {
                              Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                              Spacer(Modifier.width(6.dp))
                              Text("Nouveau", fontSize = 13.sp)
                          }
                      }
                  } else {
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(10.dp),
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                          OutlinedTextField(
                              value = searchQuery,
                              onValueChange = onSearchChange,
                              placeholder = { Text("Rechercher un groupe, département, ville...", fontSize = 13.sp) },
                              leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = Color(0xFF94A3B8)) },
                              modifier = Modifier.weight(1f),
                              singleLine = true,
                              shape = RoundedCornerShape(12.dp),
                              textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                              colors = OutlinedTextFieldDefaults.colors(
                                  unfocusedBorderColor = Color(0xFFE2E8F0),
                                  focusedBorderColor = Color(0xFF3B82F6)
                              )
                          )
                          Box {
                              OutlinedButton(
                                  onClick = { showFilterMenu = true },
                                  shape = RoundedCornerShape(10.dp),
                                  border = ButtonDefaults.outlinedButtonBorder,
                                  modifier = Modifier.cursorHand()
                              ) {
                                  Text(when (filterStatus) { "actif" -> "Actifs"; "inactif" -> "Inactifs"; else -> "Tous" }, fontSize = 13.sp)
                                  Spacer(Modifier.width(4.dp))
                                  Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp))
                              }
                              DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                                  listOf("all" to "Tous", "actif" to "Actifs", "inactif" to "Inactifs").forEach { (key, label) ->
                                      DropdownMenuItem(text = { Text(label) }, onClick = { onFilterChange(key); showFilterMenu = false })
                                  }
                              }
                          }
                          Box {
                              OutlinedButton(
                                  onClick = { showSortMenu = true },
                                  shape = RoundedCornerShape(10.dp),
                                  border = ButtonDefaults.outlinedButtonBorder,
                                  modifier = Modifier.cursorHand()
                              ) {
                                  Text(when (sortBy) { "nom" -> "Nom"; "ecoles" -> "Écoles"; else -> "Plus récents" }, fontSize = 13.sp)
                                  Spacer(Modifier.width(4.dp))
                                  Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp))
                              }
                              DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                  listOf("recent" to "Plus récents", "nom" to "Par nom", "ecoles" to "Par écoles").forEach { (key, label) ->
                                      DropdownMenuItem(text = { Text(label) }, onClick = { onSortChange(key); showSortMenu = false })
                                  }
                              }
                          }
                          Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
                              Row(Modifier.padding(2.dp)) {
                                  IconButton(onClick = { onViewModeChange("card") }, modifier = Modifier.size(32.dp).cursorHand()) {
                                      Icon(Icons.Default.ViewModule, "Cartes", Modifier.size(18.dp), tint = if (viewMode == "card") Color(0xFF1D3557) else Color(0xFF94A3B8))
                                  }
                                  IconButton(onClick = { onViewModeChange("table") }, modifier = Modifier.size(32.dp).cursorHand()) {
                                      Icon(Icons.Default.TableChart, "Tableau", Modifier.size(18.dp), tint = if (viewMode == "table") Color(0xFF1D3557) else Color(0xFF94A3B8))
                                  }
                              }
                          }
                          IconButton(onClick = onRefresh, modifier = Modifier.cursorHand()) {
                              Icon(Icons.Default.Refresh, "Actualiser", Modifier.size(20.dp), tint = Color(0xFF64748B))
                          }
                          Button(
                              onClick = onNewGroupe,
                              shape = RoundedCornerShape(10.dp),
                              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                              modifier = Modifier.cursorHand()
                          ) {
                              Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                              Spacer(Modifier.width(6.dp))
                              Text("Nouveau", fontSize = 13.sp)
                          }
                      }
                  }

                  Text(
                      "${totalResults} groupe${if (totalResults > 1) "s" else ""} trouvé${if (totalResults > 1) "s" else ""}",
                      fontSize = 12.sp,
                      color = Color(0xFF94A3B8)
                  )
              }
          }
      }
  }

@Composable
internal fun EmptyGroupesState(hasSearch: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(64.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    Alignment.Center
                ) {
                    Icon(
                        if (hasSearch) Icons.Default.SearchOff else Icons.Default.Business,
                        null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    if (hasSearch) "Aucun résultat" else "Aucun groupe scolaire",
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF334155)
                )
                Text(
                    if (hasSearch) "Essayez un autre terme de recherche"
                    else "Créez votre premier groupe scolaire pour commencer",
                    fontSize = 13.sp, color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
