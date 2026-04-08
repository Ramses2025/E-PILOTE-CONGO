package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlinx.serialization.Serializable

@Serializable
data class PlanDto(
    val id: String = "",
    val nom: String = "",
    val maxEcoles: Int = 0,
    val maxUtilisateurs: Int = 0,
    val modulesIncluded: List<String> = emptyList(),
    val categoriesIncluded: List<String> = emptyList(),
    val dureeJours: Int = 365
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPlansScreen(
    plans: List<PlanDto>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()

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
                Text("Plans d'abonnement", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("3 niveaux : Gratuit, Premium, Pro", fontSize = 13.sp, color = EpiloteTextMuted)
            }
            FilledTonalButton(onClick = onRefresh, shape = RoundedCornerShape(10.dp)) {
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val planColors = listOf(Color(0xFF2A9D8F), Color(0xFF1D3557), Color(0xFFE9C46A))

            plans.forEachIndexed { index, plan ->
                val color = planColors.getOrElse(index) { Color(0xFF2A9D8F) }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(color.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when (index) {
                                    0 -> Icons.Default.Star
                                    1 -> Icons.Default.StarHalf
                                    else -> Icons.Default.StarRate
                                },
                                null, tint = color, modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(plan.nom, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)

                        Divider(color = color.copy(alpha = 0.2f))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PlanFeatureRow("Max écoles", "${plan.maxEcoles}", color)
                            PlanFeatureRow("Max utilisateurs", "${plan.maxUtilisateurs}", color)
                            PlanFeatureRow("Modules inclus", "${plan.modulesIncluded.size}", color)
                            PlanFeatureRow("Durée", "${plan.dureeJours} jours", color)
                        }

                        Divider(color = color.copy(alpha = 0.2f))

                        Text("Modules :", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = EpiloteTextMuted)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            plan.modulesIncluded.forEach { slug ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = color.copy(alpha = 0.08f)
                                ) {
                                    Text(
                                        slug,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        color = color,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (plans.size < 3) {
                repeat(3 - plans.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PlanFeatureRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = EpiloteTextMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
