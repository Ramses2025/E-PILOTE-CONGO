package cg.epilote.desktop.ui.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.PaymentReceiptDto
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

// ── Toolbar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaymentToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterMethod: String,
    onFilterMethodChange: (String) -> Unit,
    filterGroupId: String,
    onFilterGroupChange: (String) -> Unit,
    groupOptions: List<Pair<String, String>>,
    onRefresh: () -> Unit,
    totalResults: Int,
    modifier: Modifier = Modifier
) {
    var showGroupMenu by remember { mutableStateOf(false) }
    val selectedGroupLabel = groupOptions.firstOrNull { it.first == filterGroupId }?.second ?: "Tous les groupes"

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                label = { Text("Rechercher un groupe, une référence…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFF1F5F9)) {
                    Text(
                        "$totalResults résultat(s)",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
                FilledTonalButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand()
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Text("Actualiser", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "all" to "Tous",
                "cash" to "Espèces",
                "check" to "Chèque",
                "bank_transfer" to "Virement"
            ).forEach { (value, label) ->
                FilterChip(
                    selected = filterMethod == value,
                    onClick = { onFilterMethodChange(value) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.cursorHand(),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF059669).copy(alpha = 0.12f),
                        selectedLabelColor = Color(0xFF059669)
                    )
                )
            }
            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE2E8F0)) {
                Text("│", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
            Box {
                OutlinedButton(
                    onClick = { showGroupMenu = true },
                    modifier = Modifier.cursorHand(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(selectedGroupLabel, fontSize = 11.sp, maxLines = 1)
                }
                DropdownMenu(expanded = showGroupMenu, onDismissRequest = { showGroupMenu = false }) {
                    groupOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { onFilterGroupChange(value); showGroupMenu = false }
                        )
                    }
                }
            }
        }
    }
}

// ── Clickable Receipt Card ────────────────────────────────────────────────────

@Composable
internal fun PaymentReceiptCard(
    receipt: PaymentReceiptDto,
    groupeNom: String,
    onClick: () -> Unit
) {
    val methodColor = when (receipt.paymentMethod.lowercase()) {
        "cash" -> Color(0xFF059669)
        "check" -> Color(0xFF2563EB)
        "bank_transfer" -> Color(0xFF6D28D9)
        else -> Color(0xFF64748B)
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().cursorHand(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = methodColor.copy(alpha = 0.10f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Payments, null, tint = methodColor, modifier = Modifier.size(22.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                    Text(groupeNom, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1D3557), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(999.dp), color = methodColor.copy(alpha = 0.08f)) {
                            Text(
                                receipt.paymentMethodLabel.ifBlank { receipt.paymentMethod },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp, color = methodColor, fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text("•", fontSize = 10.sp, color = EpiloteTextMuted)
                        Text(formatDate(receipt.receivedAt), fontSize = 11.sp, color = EpiloteTextMuted)
                    }
                    if (!receipt.externalReference.isNullOrBlank()) {
                        Text("Réf. ${receipt.externalReference}", fontSize = 10.sp, color = EpiloteTextMuted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatMoneyXaf(receipt.montantXAF), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF059669))
                Text("${formatDate(receipt.accessStart)} → ${formatDate(receipt.accessEnd)}", fontSize = 10.sp, color = EpiloteTextMuted)
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFD1FAE5)) {
                    Text("Reçu", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
