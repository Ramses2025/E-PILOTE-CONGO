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
    var logoFileName by remember { mutableStateOf("") }
    var logoError by remember { mutableStateOf<String?>(null) }

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
        logoFileName = ""
        logoError = null
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
                    onClick = {},
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
                SectionCard("Identité juridique") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = raisonSociale,
                            onValueChange = { raisonSociale = it },
                            label = { Text("Raison sociale") },
                            placeholder = { Text("Ex. E-PILOTE CONGO SARL") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rccm,
                            onValueChange = { rccm = it },
                            label = { Text("RCCM") },
                            placeholder = { Text("Ex. CG-BZV-01-2024-B12-00123") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = niu,
                            onValueChange = { niu = it },
                            label = { Text("NIU / Identifiant fiscal") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = competentCourt,
                            onValueChange = { competentCourt = it },
                            label = { Text("Tribunal compétent") },
                            placeholder = { Text("Ex. Tribunal de Commerce de Brazzaville") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SectionCard("Siège & contacts") {
                    OutlinedTextField(
                        value = siege,
                        onValueChange = { siege = it },
                        label = { Text("Adresse du siège (rue, quartier, BP)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("Ville") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = country,
                            onValueChange = { country = it },
                            label = { Text("Pays") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Téléphone") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = website,
                            onValueChange = { website = it },
                            label = { Text("Site web") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SectionCard("Logo de la plateforme") {
                    Text(
                        "Affiché en en-tête de chaque facture émise. Si vide, le logo E-PILOTE par défaut est utilisé.",
                        fontSize = 11.sp,
                        color = Color(0xFF667085)
                    )
                    GroupeLogoPicker(
                        logoData = logoBase64.ifBlank { null },
                        logoFileName = logoFileName,
                        errorMessage = logoError,
                        onLogoSelected = { dataUrl, fileName ->
                            logoBase64 = dataUrl
                            logoFileName = fileName
                            logoError = null
                        },
                        onLogoSelectionError = { logoError = it },
                        onLogoCleared = {
                            logoBase64 = ""
                            logoFileName = ""
                            logoError = null
                        }
                    )
                }

                SectionCard("TVA & conditions de paiement") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Plateforme exonérée de TVA :", fontSize = 13.sp)
                        Switch(checked = tvaExempted, onCheckedChange = { tvaExempted = it })
                        if (!tvaExempted) {
                            OutlinedTextField(
                                value = tvaRateText,
                                onValueChange = { tvaRateText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                                label = { Text("Taux TVA (%)") },
                                modifier = Modifier.width(180.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = paymentTerms,
                        onValueChange = { paymentTerms = it },
                        label = { Text("Conditions de paiement") },
                        placeholder = { Text("Ex. Paiement à réception — espèces, chèque ou virement") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SectionCard("Coordonnées bancaires") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Nom banque") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = iban,
                            onValueChange = { iban = it },
                            label = { Text("IBAN / N° compte") },
                            modifier = Modifier.weight(2f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = mtnMomoNumber,
                            onValueChange = { mtnMomoNumber = it },
                            label = { Text("N° MTN Mobile Money (futur)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = airtelMoneyNumber,
                            onValueChange = { airtelMoneyNumber = it },
                            label = { Text("N° Airtel Money (futur)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        "Les numéros Mobile Money sont enregistrés pour affichage sur facture. Les paiements " +
                            "électroniques seront câblés dans une phase ultérieure.",
                        fontSize = 11.sp,
                        color = Color(0xFF667085)
                    )
                }

                SectionCard("Numérotation & mentions légales") {
                    OutlinedTextField(
                        value = invoiceNumberFormat,
                        onValueChange = { invoiceNumberFormat = it },
                        label = { Text("Format numéro de facture") },
                        placeholder = { Text("FAC-{YYYY}-{NNNNNN}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Variables disponibles : {YYYY} = année, {NNNNNN} = séquence annuelle zéro-paddée.",
                        fontSize = 11.sp,
                        color = Color(0xFF667085)
                    )
                    OutlinedTextField(
                        value = legalMentions,
                        onValueChange = { legalMentions = it },
                        label = { Text("Mentions légales (pied de page)") },
                        placeholder = { Text("Ex. En cas de retard de paiement, des pénalités seront appliquées conformément à la loi.") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        maxLines = 6
                    )
                }

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
                                tvaRate = tvaRateText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0,
                                tvaExempted = tvaExempted,
                                paymentTerms = paymentTerms.trim(),
                                competentCourt = competentCourt.trim(),
                                iban = iban.trim(),
                                bankName = bankName.trim(),
                                mtnMomoNumber = mtnMomoNumber.trim(),
                                airtelMoneyNumber = airtelMoneyNumber.trim(),
                                invoiceNumberFormat = invoiceNumberFormat.trim().ifBlank { "FAC-{YYYY}-{NNNNNN}" },
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

@Composable
private fun SectionCard(
    title: String,
    content: @Composable (androidx.compose.foundation.layout.ColumnScope.() -> Unit)
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF101828))
            HorizontalDivider(color = Color(0xFFEAECF0))
            content()
        }
    }
}
