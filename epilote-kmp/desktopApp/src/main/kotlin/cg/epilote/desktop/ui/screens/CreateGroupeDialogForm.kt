package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.EpiloteTextMuted

@Composable
internal fun GroupeFormFields(
    nom: String, onNomChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit, emailError: String?,
    phone: String, onPhoneChange: (String) -> Unit,
    department: String, onDepartmentChange: (String) -> Unit,
    city: String, onCityChange: (String) -> Unit,
    cities: List<String>,
    showDeptMenu: Boolean, onShowDeptMenuChange: (Boolean) -> Unit,
    showCityMenu: Boolean, onShowCityMenuChange: (Boolean) -> Unit,
    address: String, onAddressChange: (String) -> Unit,
    foundedYear: String, onFoundedYearChange: (String) -> Unit, foundedYearError: String?,
    website: String, onWebsiteChange: (String) -> Unit, websiteError: String?,
    logoData: String?, logoFileName: String, logoError: String?,
    onLogoSelected: (String, String) -> Unit,
    onLogoSelectionError: (String) -> Unit,
    onLogoCleared: () -> Unit,
    planOptions: List<GroupePlanOption>, selectedPlan: String, onPlanSelected: (String) -> Unit,
    selectedPlanLabel: String,
    includeBlankOptionalFields: Boolean,
    isActive: Boolean, onIsActiveChange: (Boolean) -> Unit,
    description: String, onDescriptionChange: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        // ── Left column ──
        Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GroupeSectionCard(icon = Icons.Default.Business, title = "Informations de base") {
                OutlinedTextField(
                    value = nom, onValueChange = onNomChange,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("Nom du groupe *") },
                    placeholder = { Text("Ex: Groupe Scolaire Excellence") },
                    supportingText = { Text("Nom public utilisé dans tout le système.", color = EpiloteTextMuted) }
                )
                OutlinedTextField(
                    value = "Généré automatiquement après la création",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(), readOnly = true, singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("Code unique") },
                    trailingIcon = { Icon(Icons.Default.Tag, null) },
                    supportingText = { Text("Attribué côté serveur pour garantir l'unicité.", color = EpiloteTextMuted) }
                )
            }

            GroupeSectionCard(icon = Icons.Default.Email, title = "Contact") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = email, onValueChange = onEmailChange,
                        modifier = Modifier.weight(1f), singleLine = true,
                        isError = emailError != null,
                        shape = RoundedCornerShape(14.dp),
                        label = { Text("Email de contact") },
                        placeholder = { Text("contact@groupe.com") },
                        supportingText = { Text(emailError ?: "Optionnel", color = if (emailError != null) Color(0xFFB3261E) else EpiloteTextMuted) }
                    )
                    OutlinedTextField(
                        value = phone, onValueChange = onPhoneChange,
                        modifier = Modifier.weight(1f), singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        label = { Text("Téléphone") },
                        placeholder = { Text("+242 06 XXX XX XX") },
                        supportingText = { Text("Optionnel", color = EpiloteTextMuted) }
                    )
                }
            }

            GroupeSectionCard(icon = Icons.Default.LocationOn, title = "Localisation") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GroupeSelectionField(
                        value = department, label = "Département",
                        placeholder = "Sélectionnez un département",
                        expanded = showDeptMenu, enabled = true,
                        options = DEPARTMENTS_CONGO,
                        onExpandedChange = onShowDeptMenuChange,
                        onOptionSelected = onDepartmentChange,
                        modifier = Modifier.weight(1f)
                    )
                    GroupeSelectionField(
                        value = city, label = "Ville",
                        placeholder = if (department.isBlank()) "Choisissez d'abord le département" else "Sélectionnez une ville",
                        expanded = showCityMenu, enabled = cities.isNotEmpty(),
                        options = cities,
                        onExpandedChange = onShowCityMenuChange,
                        onOptionSelected = onCityChange,
                        modifier = Modifier.weight(1f),
                        supportingText = if (department.isBlank()) "Disponible après sélection du département." else null
                    )
                }
                OutlinedTextField(
                    value = address, onValueChange = onAddressChange,
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("Adresse complète") },
                    placeholder = { Text("Rue, quartier, arrondissement...") },
                    supportingText = { Text("Optionnel", color = EpiloteTextMuted) }
                )
            }

            GroupeSectionCard(icon = Icons.Default.Info, title = "Détails supplémentaires") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = foundedYear,
                        onValueChange = { onFoundedYearChange(it.filter { char -> char.isDigit() }.take(4)) },
                        modifier = Modifier.weight(1f), singleLine = true,
                        isError = foundedYearError != null,
                        shape = RoundedCornerShape(14.dp),
                        label = { Text("Année de création") },
                        placeholder = { Text("Ex: 2010") },
                        supportingText = { Text(foundedYearError ?: "Optionnel", color = if (foundedYearError != null) Color(0xFFB3261E) else EpiloteTextMuted) }
                    )
                    OutlinedTextField(
                        value = website, onValueChange = onWebsiteChange,
                        modifier = Modifier.weight(1f), singleLine = true,
                        isError = websiteError != null,
                        shape = RoundedCornerShape(14.dp),
                        label = { Text("Site web") },
                        placeholder = { Text("www.exemple.cg") },
                        supportingText = { Text(websiteError ?: "Optionnel", color = if (websiteError != null) Color(0xFFB3261E) else EpiloteTextMuted) }
                    )
                }
            }
        }

        // ── Right column ──
        Column(modifier = Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GroupeSectionCard(icon = Icons.Default.Image, title = "Logo du groupe") {
                GroupeLogoPicker(
                    logoData = logoData, logoFileName = logoFileName, errorMessage = logoError,
                    onLogoSelected = onLogoSelected,
                    onLogoSelectionError = onLogoSelectionError,
                    onLogoCleared = onLogoCleared
                )
            }

            GroupeSectionCard(icon = Icons.Default.CalendarMonth, title = "Plan d'abonnement") {
                PlanSelector(options = planOptions, selectedPlanId = selectedPlan, onPlanSelected = onPlanSelected)
                Text(
                    text = if (selectedPlanLabel.isBlank()) "Aucun plan sélectionné" else "Plan sélectionné : $selectedPlanLabel",
                    fontSize = 12.sp, color = EpiloteTextMuted
                )
            }

            if (includeBlankOptionalFields) {
                GroupeSectionCard(icon = Icons.Default.PowerSettingsNew, title = "État du groupe") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isActive) "Groupe actif" else "Groupe désactivé",
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) Color(0xFF2E7D32) else Color(0xFFB3261E)
                            )
                            Text(
                                text = if (isActive) "Le groupe et ses écoles sont accessibles." else "Le groupe et ses écoles sont suspendus.",
                                fontSize = 12.sp, color = EpiloteTextMuted
                            )
                        }
                        Switch(
                            checked = isActive, onCheckedChange = onIsActiveChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2E7D32),
                                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFBDBDBD)
                            )
                        )
                    }
                }
            }

            GroupeSectionCard(icon = Icons.Default.Description, title = "Présentation") {
                OutlinedTextField(
                    value = description, onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    minLines = 5, maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                    label = { Text("Description du groupe") },
                    placeholder = { Text("Présentez rapidement le groupe scolaire, son positionnement et ses spécificités.") },
                    supportingText = { Text("Optionnel", color = EpiloteTextMuted) }
                )
            }
        }
    }
}
