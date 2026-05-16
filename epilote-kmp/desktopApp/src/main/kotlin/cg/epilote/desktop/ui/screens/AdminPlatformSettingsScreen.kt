package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.PlatformIdentityDto
import cg.epilote.desktop.data.UpdatePlatformIdentityDto
import cg.epilote.desktop.data.*
import kotlinx.coroutines.launch

/**
 * Page « Paramètres plateforme » (Super Admin).
 *
 * Permet de saisir l'identité juridique de la plateforme — raison sociale, RCCM, NIU,
 * siège, coordonnées bancaires, mentions légales — qui sont ensuite repris au moment
 * de l'émission de chaque facture (snapshot figé dans le document `invoice`).
 *
 * Tant qu'un champ n'est pas rempli, le PDF affiche un placeholder explicite
 * « À compléter dans Paramètres plateforme ».
 */
@Composable
fun AdminPlatformSettingsScreen(
    client: DesktopAdminClient
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }
    var current by remember { mutableStateOf(PlatformIdentityDto()) }

    var raisonSociale by remember { mutableStateOf("") }
    var rccm by remember { mutableStateOf("") }
    var niu by remember { mutableStateOf("") }
    var siege by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Congo") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var tvaRateText by remember { mutableStateOf("") }
    var tvaExempted by remember { mutableStateOf(true) }
    var paymentTerms by remember { mutableStateOf("") }
    var competentCourt by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var mtnMomoNumber by remember { mutableStateOf("") }
    var airtelMoneyNumber by remember { mutableStateOf("") }
    var invoiceNumberFormat by remember { mutableStateOf("FAC-{YYYY}-{NNNNNN}") }
    var legalMentions by remember { mutableStateOf("") }
    var logoBase64 by remember { mutableStateOf("") }

    fun applyDto(dto: PlatformIdentityDto) {
        current = dto
        raisonSociale = dto.raisonSociale
        rccm = dto.rccm
        niu = dto.niu
        siege = dto.siege
        city = dto.city
        country = dto.country.ifBlank { "Congo" }
        phone = dto.phone
        email = dto.email
        website = dto.website
        tvaRateText = if (dto.tvaRate > 0.0) dto.tvaRate.toString() else ""
        tvaExempted = dto.tvaExempted
        paymentTerms = dto.paymentTerms
        competentCourt = dto.competentCourt
        iban = dto.iban
        bankName = dto.bankName
        mtnMomoNumber = dto.mtnMomoNumber
        airtelMoneyNumber = dto.airtelMoneyNumber
        invoiceNumberFormat = dto.invoiceNumberFormat.ifBlank { "FAC-{YYYY}-{NNNNNN}" }
        legalMentions = dto.legalMentions
        logoBase64 = dto.logoBase64
    }

    LaunchedEffect(Unit) {
        loading = true
        val dto = runCatching { client.getPlatformIdentity() }.getOrNull()
        if (dto != null) applyDto(dto) else feedback = AdminFeedbackMessage(
            "Impossible de charger les paramètres actuels. Vous pouvez commencer à les saisir ci-dessous.",
            isError = true
        )
        loading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F8FA))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column {
                Text(
                    "Paramètres plateforme",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101828)
                )
                Text(
                    "Identité juridique utilisée sur chaque facture émise. Les champs non renseignés apparaissent " +
                        "comme « À compléter » sur les PDF.",
                    fontSize = 13.sp,
                    color = Color(0xFF667085)
                )
            }

            feedback?.let { fb ->
                AssistChip(
                    onClick = { feedback = null },
                    label = { Text(fb.message) },
                    colors = if (fb.isError)
                        AssistChipDefaults.assistChipColors(labelColor = Color(0xFFB42318))
                    else AssistChipDefaults.assistChipColors(labelColor = Color(0xFF027A48))
                )
            }

            if (loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp))
                    Text("Chargement des paramètres…", fontSize = 13.sp, color = Color(0xFF667085))
                }
            } else {
                PlatformIdentitySection(
                    raisonSociale = raisonSociale, onRaisonSocialeChange = { raisonSociale = it },
                    rccm = rccm, onRccmChange = { rccm = it },
                    niu = niu, onNiuChange = { niu = it },
                    competentCourt = competentCourt, onCompetentCourtChange = { competentCourt = it }
                )
                PlatformSiegeSection(
                    siege = siege, onSiegeChange = { siege = it },
                    city = city, onCityChange = { city = it },
                    country = country, onCountryChange = { country = it },
                    phone = phone, onPhoneChange = { phone = it },
                    email = email, onEmailChange = { email = it },
                    website = website, onWebsiteChange = { website = it }
                )
                PlatformLogoSection(logoBase64 = logoBase64, onLogoChange = { logoBase64 = it })
                PlatformTvaSection(
                    tvaExempted = tvaExempted, onTvaExemptedChange = { tvaExempted = it },
                    tvaRateText = tvaRateText, onTvaRateChange = { tvaRateText = it },
                    paymentTerms = paymentTerms, onPaymentTermsChange = { paymentTerms = it }
                )
                PlatformBankSection(
                    bankName = bankName, onBankNameChange = { bankName = it },
                    iban = iban, onIbanChange = { iban = it },
                    mtnMomoNumber = mtnMomoNumber, onMtnChange = { mtnMomoNumber = it },
                    airtelMoneyNumber = airtelMoneyNumber, onAirtelChange = { airtelMoneyNumber = it }
                )
                PlatformLegalSection(
                    invoiceNumberFormat = invoiceNumberFormat, onFormatChange = { invoiceNumberFormat = it },
                    legalMentions = legalMentions, onLegalChange = { legalMentions = it }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { applyDto(current) },
                        enabled = !saving
                    ) { Text("Annuler les modifications") }
                    Spacer(Modifier.weight(1f))
                    Button(
                        enabled = !saving,
                        onClick = {
                            val trimmedFormat = invoiceNumberFormat.trim().ifBlank { "FAC-{YYYY}-{NNNNNN}" }
                            val sequencePlaceholder = Regex("\\{N+}")
                            if (!sequencePlaceholder.containsMatchIn(trimmedFormat)) {
                                feedback = AdminFeedbackMessage(
                                    "Le format de numéro de facture doit contenir au moins un bloc {N+} (ex. {NNNNNN}) pour la séquence.",
                                    isError = true
                                )
                                return@Button
                            }
                            val payload = UpdatePlatformIdentityDto(
                                raisonSociale = raisonSociale.trim(),
                                rccm = rccm.trim(),
                                niu = niu.trim(),
                                siege = siege.trim(),
                                city = city.trim(),
                                country = country.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                website = website.trim(),
                                logoBase64 = logoBase64.trim(),
                                tvaRate = if (tvaExempted) null else (tvaRateText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0),
                                tvaExempted = tvaExempted,
                                paymentTerms = paymentTerms.trim(),
                                competentCourt = competentCourt.trim(),
                                iban = iban.trim(),
                                bankName = bankName.trim(),
                                mtnMomoNumber = mtnMomoNumber.trim(),
                                airtelMoneyNumber = airtelMoneyNumber.trim(),
                                invoiceNumberFormat = trimmedFormat,
                                legalMentions = legalMentions.trim()
                            )
                            scope.launch {
                                saving = true
                                feedback = null
                                val updated = runCatching { client.updatePlatformIdentity(payload) }.getOrNull()
                                saving = false
                                if (updated != null) {
                                    applyDto(updated)
                                    feedback = AdminFeedbackMessage("Paramètres enregistrés. Les prochaines factures intégreront ces données.")
                                } else {
                                    feedback = AdminFeedbackMessage("Échec de l'enregistrement. Vérifiez votre connexion et réessayez.", isError = true)
                                }
                            }
                        }
                    ) { Text(if (saving) "Enregistrement…" else "Enregistrer") }
                }
            }
        }
    }
}

