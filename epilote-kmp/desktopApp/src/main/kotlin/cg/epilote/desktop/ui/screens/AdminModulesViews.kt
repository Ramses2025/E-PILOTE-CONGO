package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

@Composable
internal fun ModuleTableView(
    modules: List<ModuleDto>,
    categoriesByCode: Map<String, CategorieDto>,
    onViewDetail: (ModuleDto) -> Unit,
    onEdit: (ModuleDto) -> Unit,
    onToggleStatus: (ModuleDto) -> Unit
) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            modules.forEachIndexed { index, module ->
                val accent = categoryColor(module.categorieCode)
                val categoryLabel = categoriesByCode[module.categorieCode]?.nom ?: module.categorieCode.ifBlank { "Sans catégorie" }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(accent, CircleShape))
                        Column {
                            Text(module.nom, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${module.code} • $categoryLabel • ordre ${module.ordre}", fontSize = 11.sp, color = EpiloteTextMuted)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ModuleStatusBadges(module = module, categoryLabel = categoryLabel, accentColor = accent)
                        OutlinedButton(onClick = { onViewDetail(module) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Text("Détails")
                        }
                        OutlinedButton(onClick = { onEdit(module) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Text("Modifier")
                        }
                        FilledTonalButton(onClick = { onToggleStatus(module) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Text(if (module.isActive) "Suspendre" else "Réactiver")
                        }
                    }
                }
                if (index < modules.lastIndex) {
                    Spacer(Modifier.height(2.dp))
                    Surface(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color(0xFFE2E8F0)) {}
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ModuleStatusBadges(module: ModuleDto, categoryLabel: String, accentColor: Color) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = RoundedCornerShape(999.dp), color = accentColor.copy(alpha = 0.1f)) {
            Text(categoryLabel, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
        }
        if (module.isCore) {
            Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFEDE9FE)) {
                Text("CORE", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.SemiBold)
            }
        }
        Surface(shape = RoundedCornerShape(999.dp), color = modulePlanAccent(module.requiredPlan).copy(alpha = 0.1f)) {
            Text(modulePlanLabel(module.requiredPlan), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = modulePlanAccent(module.requiredPlan), fontWeight = FontWeight.SemiBold)
        }
        Surface(shape = RoundedCornerShape(999.dp), color = if (module.isActive) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)) {
            Text(if (module.isActive) "ACTIVE" else "INACTIVE", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = if (module.isActive) Color(0xFF166534) else Color(0xFFB91C1C), fontWeight = FontWeight.SemiBold)
        }
    }
}
