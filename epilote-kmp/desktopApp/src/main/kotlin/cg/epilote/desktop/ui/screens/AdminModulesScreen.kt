package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import cg.epilote.desktop.ui.theme.*
import cg.epilote.desktop.ui.theme.cursorHand
import kotlinx.serialization.Serializable

@Serializable
data class ModuleDto(
    val id: String = "",
    val code: String = "",
    val nom: String = "",
    val categorieCode: String = "",
    val description: String = "",
    val isCore: Boolean = false,
    val requiredPlan: String = "gratuit",
    val isActive: Boolean = true
)

private val CATEGORIE_COLORS = mapOf(
    "scolarite"     to Color(0xFF2A9D8F),
    "pedagogie"     to Color(0xFF1D3557),
    "finances"      to Color(0xFFE9C46A),
    "personnel"     to Color(0xFFE63946),
    "vie-scolaire"  to Color(0xFF6C5CE7),
    "communication" to Color(0xFF00875A)
)

private val CATEGORIE_LABELS = mapOf(
    "scolarite"     to "Scolarité",
    "pedagogie"     to "Pédagogie",
    "finances"      to "Finances",
    "personnel"     to "Personnel",
    "vie-scolaire"  to "Vie Scolaire",
    "communication" to "Communication"
)

@Composable
fun AdminModulesScreen(
    modules: List<ModuleDto>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    val grouped = modules.groupBy { it.categorieCode }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Modules & Catégories", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Référentiel des 30 modules réels — 6 catégories", fontSize = 13.sp, color = EpiloteTextMuted)
            }
            FilledTonalButton(onClick = onRefresh, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Actualiser")
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        }

        // Stats
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF2A9D8F).copy(alpha = 0.1f)) {
                Text("Total : ${modules.size} modules", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2A9D8F))
            }
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1D3557).copy(alpha = 0.1f)) {
                Text("Core : ${modules.count { it.isCore }}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D3557))
            }
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE9C46A).copy(alpha = 0.15f)) {
                Text("Gratuit : ${modules.count { it.requiredPlan == "gratuit" }}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB8860B))
            }
        }

        // Catégories avec modules
        val orderedCategories = listOf("scolarite", "pedagogie", "finances", "personnel", "vie-scolaire", "communication")

        orderedCategories.forEach { catCode ->
            val catModules = grouped[catCode] ?: return@forEach
            val catColor = CATEGORIE_COLORS[catCode] ?: Color.Gray
            val catLabel = CATEGORIE_LABELS[catCode] ?: catCode

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(10.dp).background(catColor, CircleShape))
                        Text(catLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = catColor)
                        Surface(shape = RoundedCornerShape(4.dp), color = catColor.copy(alpha = 0.1f)) {
                            Text("${catModules.size} modules", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = catColor)
                        }
                    }

                    catModules.forEach { module ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Extension, null, tint = catColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                Column {
                                    Text(module.nom, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(module.code, fontSize = 11.sp, color = EpiloteTextMuted)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (module.isCore) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2A9D8F).copy(alpha = 0.1f)) {
                                        Text("CORE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A9D8F))
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (module.requiredPlan) {
                                        "gratuit" -> Color(0xFF2A9D8F).copy(alpha = 0.1f)
                                        "premium" -> Color(0xFF1D3557).copy(alpha = 0.1f)
                                        else -> Color(0xFFE9C46A).copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        module.requiredPlan.uppercase(),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (module.requiredPlan) {
                                            "gratuit" -> Color(0xFF2A9D8F)
                                            "premium" -> Color(0xFF1D3557)
                                            else -> Color(0xFFB8860B)
                                        }
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
