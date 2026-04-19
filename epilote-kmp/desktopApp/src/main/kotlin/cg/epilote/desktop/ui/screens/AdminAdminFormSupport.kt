package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Section Card ──────────────────────────────────────────────────

@Composable
internal fun AdminSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFD), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(28.dp).background(Color(0xFF1D3557).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color(0xFF1D3557), modifier = Modifier.size(16.dp))
            }
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D3557))
        }
        content()
    }
}

// ── Role Selector ──────────────────────────────────────────────────

@Composable
internal fun RoleSelector(
    selectedRole: String,
    onRoleSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        "ADMIN_GROUPE" to ("Admin Groupe" to Color(0xFF6C5CE7)),
        "SUPER_ADMIN" to ("Super Admin" to Color(0xFFE63946))
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, pair) ->
            val (label, color) = pair
            val selected = selectedRole == id
            Surface(
                onClick = { onRoleSelected(id) },
                modifier = Modifier.fillMaxWidth().cursorHand(),
                shape = RoundedCornerShape(14.dp),
                color = if (selected) color.copy(alpha = 0.10f) else Color(0xFFF8FAFD),
                border = if (selected) {
                    androidx.compose.foundation.BorderStroke(2.dp, color)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(14.dp).background(color, RoundedCornerShape(999.dp))
                    )
                    Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold, color = if (selected) color else Color(0xFF1D3557))
                }
            }
        }
    }
}

// ── Gender Selector ────────────────────────────────────────────────

@Composable
internal fun GenderSelector(
    selectedGender: String?,
    onGenderSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(null to "Non précisé", "M" to "Masculin", "F" to "Féminin")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val selected = selectedGender == value
            val color = when (value) {
                "M" -> Color(0xFF3B82F6)
                "F" -> Color(0xFFEC4899)
                else -> Color(0xFF94A3B8)
            }
            Surface(
                onClick = { onGenderSelected(value) },
                modifier = Modifier.cursorHand(),
                shape = RoundedCornerShape(10.dp),
                color = if (selected) color.copy(alpha = 0.12f) else Color.White,
                border = if (selected) {
                    androidx.compose.foundation.BorderStroke(1.5.dp, color)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selected) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = color)
                    }
                    Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) color else Color(0xFF64748B))
                }
            }
        }
    }
}

// ── Group Selector (Dropdown) ──────────────────────────────────────

@Composable
internal fun GroupSelectorField(
    value: String,
    label: String,
    placeholder: String,
    expanded: Boolean,
    enabled: Boolean,
    options: List<Pair<String, String>>,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val displayLabel = options.find { it.first == value }?.second ?: value
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .then(if (enabled) Modifier.cursorHand() else Modifier)
                .clickable(enabled = enabled) { onExpandedChange(true) },
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = {
                IconButton(onClick = { if (enabled) onExpandedChange(true) }, modifier = Modifier.cursorHand()) {
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onOptionSelected(id)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

// ── Date Field ─────────────────────────────────────────────────────

@Composable
internal fun DateField(
    value: String?,
    onValueChange: (String?) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val formatted = value?.let {
        runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull() ?: it
    } ?: ""

    Column(modifier = modifier) {
        OutlinedTextField(
            value = formatted,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().cursorHand().clickable { expanded = true },
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            label = { Text(label) },
            placeholder = { Text("JJ/MM/AAAA") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }, modifier = Modifier.cursorHand()) {
                    Icon(Icons.Default.CalendarMonth, null)
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Text("Saisir une date (AAAA-MM-JJ) :", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 12.sp, color = EpiloteTextMuted)
            var input by remember { mutableStateOf(value ?: "") }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                placeholder = { Text("1990-01-15") }
            )
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                Surface(
                    onClick = {
                        val parsed = runCatching { LocalDate.parse(input) }.getOrNull()
                        if (parsed != null || input.isBlank()) {
                            onValueChange(input.ifBlank { null })
                        }
                        expanded = false
                    },
                    modifier = Modifier.cursorHand(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1D3557)
                ) {
                    Text("OK", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 12.sp)
                }
            }
        }
    }
}

// ── Must Change Password Switch ────────────────────────────────────

@Composable
internal fun MustChangePasswordSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Forcer le changement de mot de passe", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D3557))
            Text("L'admin devra changer son mot de passe à la première connexion", fontSize = 11.sp, color = EpiloteTextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.cursorHand(),
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1D3557))
        )
    }
}

// ── Avatar Picker ──────────────────────────────────────────────────

@Composable
internal fun AdminAvatarPicker(
    avatarData: String?,
    avatarFileName: String?,
    errorMessage: String?,
    onAvatarSelected: () -> Unit,
    onAvatarCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(Color(0xFFE2E8F0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarData != null) {
                    // Show avatar preview via data URL — simplified as initials fallback
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1D3557).copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = Color(0xFF1D3557), modifier = Modifier.size(24.dp))
                    }
                } else {
                    Icon(Icons.Default.Person, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    onClick = onAvatarSelected,
                    modifier = Modifier.cursorHand(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1D3557).copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1D3557).copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(14.dp), tint = Color(0xFF1D3557))
                        Text(if (avatarData != null) "Changer la photo" else "Sélectionner une photo", fontSize = 12.sp, color = Color(0xFF1D3557), fontWeight = FontWeight.Medium)
                    }
                }
                if (avatarData != null) {
                    Text(avatarFileName ?: "Photo sélectionnée", fontSize = 11.sp, color = EpiloteTextMuted)
                    Surface(
                        onClick = onAvatarCleared,
                        modifier = Modifier.cursorHand(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Text("Supprimer", fontSize = 11.sp, color = Color(0xFFB3261E), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
        errorMessage?.let {
            Text(it, fontSize = 11.sp, color = Color(0xFFB3261E))
        }
    }
}
