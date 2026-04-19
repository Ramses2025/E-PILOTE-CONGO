package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import cg.epilote.desktop.data.CreateGroupeDto
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

@Composable
internal fun CreateGroupeDialog(
    onDismiss: () -> Unit,
    onSubmit: (CreateGroupeDto) -> Unit,
    availablePlans: List<PlanDto> = emptyList(),
    initialData: GroupeFormInitialData = GroupeFormInitialData(),
    title: String = "Nouveau groupe",
    subtitle: String = "Créez un groupe scolaire avec une fiche propre et complète",
    submitLabel: String = "Créer le groupe",
    includeBlankOptionalFields: Boolean = false,
    isSubmitting: Boolean = false,
    submitError: String? = null
) {
    var nom by rememberSaveable { mutableStateOf(initialData.nom) }
    var email by rememberSaveable { mutableStateOf(initialData.email) }
    var phone by rememberSaveable { mutableStateOf(initialData.phone) }
    var department by rememberSaveable { mutableStateOf(initialData.department) }
    var city by rememberSaveable { mutableStateOf(initialData.city) }
    var address by rememberSaveable { mutableStateOf(initialData.address) }
    var foundedYear by rememberSaveable { mutableStateOf(initialData.foundedYear) }
    var website by rememberSaveable { mutableStateOf(initialData.website) }
    var description by rememberSaveable { mutableStateOf(initialData.description) }
    var selectedPlan by rememberSaveable { mutableStateOf(initialData.planId) }
    var logoData by remember { mutableStateOf(initialData.logo) }
    var logoFileName by rememberSaveable { mutableStateOf(initialData.logoFileName) }
    var logoError by rememberSaveable { mutableStateOf<String?>(null) }
    var isActive by rememberSaveable { mutableStateOf(initialData.isActive) }
    var showDeptMenu by remember { mutableStateOf(false) }
    var showCityMenu by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val planOptions = remember(availablePlans) {
        availablePlans.toPlanOptions().ifEmpty { DEFAULT_PLAN_OPTIONS }
    }

    LaunchedEffect(planOptions) {
        if (planOptions.none { it.id == selectedPlan }) {
            selectedPlan = planOptions.firstOrNull()?.id.orEmpty()
        }
    }

    val cities = remember(department) { CITIES_BY_DEPARTMENT[department].orEmpty() }
    val trimmedNom = nom.trim()
    val trimmedEmail = email.trim()
    val trimmedPhone = phone.trim()
    val trimmedAddress = address.trim()
    val trimmedDescription = description.trim()
    val normalizedWebsite = normalizeWebsite(website)
    val foundedYearValue = foundedYear.toIntOrNull()
    val selectedPlanLabel = planOptions.firstOrNull { it.id == selectedPlan }?.label.orEmpty()

    val emailError = validateEmail(trimmedEmail)
    val websiteError = validateWebsite(normalizedWebsite)
    val foundedYearError = validateFoundedYear(foundedYear)

    val isValid = trimmedNom.isNotBlank() &&
        selectedPlan.isNotBlank() &&
        emailError == null &&
        websiteError == null &&
        foundedYearError == null &&
        (logoError == null || logoData != null)

    val dialogState = rememberDialogState(size = DpSize(1100.dp, 880.dp))

    DialogWindow(
        onCloseRequest = onDismiss, title = title,
        state = dialogState,
        undecorated = true, resizable = false
    ) {
        WindowDraggableArea(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(14.dp), color = Color.White) {
                Column(Modifier.fillMaxSize()) {
                    GroupeDialogHeader(onDismiss, title, subtitle)
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF7FAFE),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6ECF4))
                        ) {
                            Text(
                                text = "Seul le nom du groupe est obligatoire. Le code unique est généré automatiquement et le site web reçoit https:// automatiquement si nécessaire.",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                fontSize = 12.sp,
                                color = EpiloteTextMuted
                            )
                        }

                        GroupeFormFields(
                            nom = nom, onNomChange = { nom = it },
                            email = email, onEmailChange = { email = it }, emailError = emailError,
                            phone = phone, onPhoneChange = { phone = it },
                            department = department, onDepartmentChange = { department = it; city = "" },
                            city = city, onCityChange = { city = it },
                            cities = cities,
                            showDeptMenu = showDeptMenu, onShowDeptMenuChange = { showDeptMenu = it },
                            showCityMenu = showCityMenu, onShowCityMenuChange = { showCityMenu = it },
                            address = address, onAddressChange = { address = it },
                            foundedYear = foundedYear, onFoundedYearChange = { foundedYear = it }, foundedYearError = foundedYearError,
                            website = website, onWebsiteChange = { website = it }, websiteError = websiteError,
                            logoData = logoData, logoFileName = logoFileName, logoError = logoError,
                            onLogoSelected = { dataUrl, fileName -> logoData = dataUrl; logoFileName = fileName; logoError = null },
                            onLogoSelectionError = { logoError = it },
                            onLogoCleared = { logoData = null; logoFileName = ""; logoError = null },
                            planOptions = planOptions, selectedPlan = selectedPlan, onPlanSelected = { selectedPlan = it },
                            selectedPlanLabel = selectedPlanLabel,
                            includeBlankOptionalFields = includeBlankOptionalFields,
                            isActive = isActive, onIsActiveChange = { isActive = it },
                            description = description, onDescriptionChange = { description = it }
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE9EEF5))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isValid) "Formulaire prêt à être créé." else "Corrigez les champs signalés avant de créer le groupe.",
                            fontSize = 12.sp,
                            color = if (isValid) EpiloteTextMuted else Color(0xFFB3261E)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.cursorHand()) { Text("Annuler") }
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (isValid) {
                                        val emailValue = if (includeBlankOptionalFields) trimmedEmail else trimmedEmail.takeIf { it.isNotBlank() }
                                        val phoneValue = if (includeBlankOptionalFields) trimmedPhone else trimmedPhone.takeIf { it.isNotBlank() }
                                        val departmentValue = if (includeBlankOptionalFields) department.trim() else department.takeIf { it.isNotBlank() }
                                        val cityValue = if (includeBlankOptionalFields) city.trim() else city.takeIf { it.isNotBlank() }
                                        val addressValue = if (includeBlankOptionalFields) trimmedAddress else trimmedAddress.takeIf { it.isNotBlank() }
                                        val logoValue = logoData?.takeIf { it.isNotBlank() } ?: if (includeBlankOptionalFields) "" else null
                                        val descriptionValue = if (includeBlankOptionalFields) trimmedDescription else trimmedDescription.takeIf { it.isNotBlank() }
                                        val websiteValue = if (includeBlankOptionalFields) normalizedWebsite.orEmpty() else normalizedWebsite
                                        onSubmit(
                                            CreateGroupeDto(
                                                nom = trimmedNom,
                                                planId = selectedPlan,
                                                email = emailValue,
                                                phone = phoneValue,
                                                department = departmentValue,
                                                city = cityValue,
                                                address = addressValue,
                                                logo = logoValue,
                                                description = descriptionValue,
                                                foundedYear = foundedYearValue,
                                                website = websiteValue,
                                                isActive = if (includeBlankOptionalFields) isActive else null
                                            )
                                        )
                                    }
                                },
                                enabled = isValid && !isSubmitting,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                                modifier = Modifier.cursorHand()
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(if (isSubmitting) "Envoi en cours…" else submitLabel)
                            }
                        }
                    }
                    if (submitError != null) {
                        Text(submitError, Modifier.padding(horizontal = 24.dp), color = Color(0xFFB3261E), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
