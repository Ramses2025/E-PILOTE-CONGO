package cg.epilote.desktop.ui.screens.superadmin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.ProvinceStatsDto

@Composable
internal fun DepartmentDetailPanel(
    selectedDepartment: DepartmentInfo?,
    provinceStats: List<ProvinceStatsDto>,
    totalDepts: Int,
    deptsWithGroupes: Int,
    totalGroupes: Long,
    totalEcoles: Long,
    modifier: Modifier = Modifier
) {
    val uncovered = totalDepts - deptsWithGroupes
    val coveragePct = if (totalDepts > 0) (deptsWithGroupes * 100) / totalDepts else 0
    val statsMap = provinceStats.associateBy { it.province }
    val deptStat = selectedDepartment?.let { statsMap[it.nom] }
    val deptGroupes = deptStat?.groupesCount ?: 0L
    val deptEcoles = deptStat?.ecolesCount ?: 0L
    val deptCovered = deptGroupes > 0

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedDepartment == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Public, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                    Text("Vue nationale", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF3B82F6).copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("$coveragePct%", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3B82F6))
                        Text("couverture nationale", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NationalKpiMini("Groupes", "$totalGroupes", Color(0xFF10B981), Modifier.weight(1f))
                        NationalKpiMini("Écoles", "$totalEcoles", Color(0xFF6366F1), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NationalKpiMini("Dépts couverts", "$deptsWithGroupes / $totalDepts", Color(0xFFF59E0B), Modifier.weight(1f))
                        NationalKpiMini("Zones vides", "$uncovered", if (uncovered > 0) Color(0xFFEF4444) else Color(0xFF10B981), Modifier.weight(1f))
                    }
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Légende", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                    DepartmentTier.entries.forEach { tier ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).background(tier.color, CircleShape))
                            Text(tier.label, fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                }
                Text("‹ Sélectionnez un département", fontSize = 10.sp, color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                Surface(shape = RoundedCornerShape(10.dp), color = selectedDepartment.color.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(selectedDepartment.nom, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = selectedDepartment.color)
                        Text("📍 ${selectedDepartment.chefLieu}", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NationalKpiMini("Groupes scolaires", "$deptGroupes", Color(0xFF10B981), Modifier.fillMaxWidth())
                    NationalKpiMini("Écoles", "$deptEcoles", Color(0xFF6366F1), Modifier.fillMaxWidth())
                }
                Surface(shape = RoundedCornerShape(8.dp),
                    color = if (deptCovered) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(8.dp).background(if (deptCovered) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape))
                        Text(if (deptCovered) "Zone couverte" else "Zone non couverte", fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (deptCovered) Color(0xFF10B981) else Color(0xFFEF4444))
                    }
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Référence nationale", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                    Text("$totalGroupes groupes · $deptsWithGroupes/$totalDepts dépts", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text("Couverture $coveragePct%", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
internal fun NationalKpiMini(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.07f), modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 9.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
internal fun DepartmentProgressBar(deptsWithGroupes: Int, totalDepts: Int) {
    val fraction = if (totalDepts > 0) (deptsWithGroupes.toFloat() / totalDepts).coerceIn(0f, 1f) else 0f
    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color(0xFFE2E8F0), RoundedCornerShape(3.dp))) {
        Box(modifier = Modifier.fillMaxWidth(fraction).height(6.dp).background(
            Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFEC4899))),
            RoundedCornerShape(3.dp)
        ))
    }
}
