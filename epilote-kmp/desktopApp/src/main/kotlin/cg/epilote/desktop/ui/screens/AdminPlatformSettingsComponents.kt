package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SectionCard(
    title: String,
    content: @Composable (androidx.compose.foundation.layout.ColumnScope.() -> Unit)
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF101828))
            HorizontalDivider(color = Color(0xFFEAECF0))
            content()
        }
    }
}

@Composable
internal fun PlatformIdentitySection(
    raisonSociale: String, onRaisonSocialeChange: (String) -> Unit,
    rccm: String, onRccmChange: (String) -> Unit,
    niu: String, onNiuChange: (String) -> Unit,
    competentCourt: String, onCompetentCourtChange: (String) -> Unit
) {
    SectionCard("Identité juridique") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = raisonSociale, onValueChange = onRaisonSocialeChange,
                label = { Text("Raison sociale") }, placeholder = { Text("Ex. E-PILOTE CONGO SARL") },
                modifier = Modifier.weight(1f))
            OutlinedTextField(value = rccm, onValueChange = onRccmChange,
                label = { Text("RCCM") }, placeholder = { Text("Ex. CG-BZV-01-2024-B12-00123") },
                modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = niu, onValueChange = onNiuChange,
                label = { Text("NIU / Identifiant fiscal") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = competentCourt, onValueChange = onCompetentCourtChange,
                label = { Text("Tribunal compétent") },
                placeholder = { Text("Ex. Tribunal de Commerce de Brazzaville") },
                modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun PlatformSiegeSection(
    siege: String, onSiegeChange: (String) -> Unit,
    city: String, onCityChange: (String) -> Unit,
    country: String, onCountryChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    website: String, onWebsiteChange: (String) -> Unit
) {
    SectionCard("Siège & contacts") {
        OutlinedTextField(value = siege, onValueChange = onSiegeChange,
            label = { Text("Adresse du siège (rue, quartier, BP)") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = city, onValueChange = onCityChange,
                label = { Text("Ville") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = country, onValueChange = onCountryChange,
                label = { Text("Pays") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = phone, onValueChange = onPhoneChange,
                label = { Text("Téléphone") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = email, onValueChange = onEmailChange,
                label = { Text("Email") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = website, onValueChange = onWebsiteChange,
                label = { Text("Site web") }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun PlatformLogoSection(logoBase64: String, onLogoChange: (String) -> Unit) {
    SectionCard("Logo (données base64, PNG/JPG)") {
        OutlinedTextField(
            value = logoBase64,
            onValueChange = { onLogoChange(it.take(3_000_000)) },
            label = { Text("Contenu base64 — data:image/png;base64,…") },
            modifier = Modifier.fillMaxWidth().height(110.dp),
            maxLines = 4
        )
        Text("Astuce : convertis ton logo en base64 (copier-coller). Taille max ~3 Mo.",
            fontSize = 11.sp, color = Color(0xFF667085))
    }
}

@Composable
internal fun PlatformTvaSection(
    tvaExempted: Boolean, onTvaExemptedChange: (Boolean) -> Unit,
    tvaRateText: String, onTvaRateChange: (String) -> Unit,
    paymentTerms: String, onPaymentTermsChange: (String) -> Unit
) {
    SectionCard("TVA & conditions de paiement") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Plateforme exonérée de TVA :", fontSize = 13.sp)
            Switch(checked = tvaExempted, onCheckedChange = onTvaExemptedChange)
            if (!tvaExempted) {
                OutlinedTextField(
                    value = tvaRateText,
                    onValueChange = { onTvaRateChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
                    label = { Text("Taux TVA (%)") },
                    modifier = Modifier.width(180.dp)
                )
            }
        }
        OutlinedTextField(value = paymentTerms, onValueChange = onPaymentTermsChange,
            label = { Text("Conditions de paiement") },
            placeholder = { Text("Ex. Paiement à réception — espèces, chèque ou virement") },
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun PlatformBankSection(
    bankName: String, onBankNameChange: (String) -> Unit,
    iban: String, onIbanChange: (String) -> Unit,
    mtnMomoNumber: String, onMtnChange: (String) -> Unit,
    airtelMoneyNumber: String, onAirtelChange: (String) -> Unit
) {
    SectionCard("Coordonnées bancaires") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = bankName, onValueChange = onBankNameChange,
                label = { Text("Nom banque") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = iban, onValueChange = onIbanChange,
                label = { Text("IBAN / N° compte") }, modifier = Modifier.weight(2f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = mtnMomoNumber, onValueChange = onMtnChange,
                label = { Text("N° MTN Mobile Money (futur)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = airtelMoneyNumber, onValueChange = onAirtelChange,
                label = { Text("N° Airtel Money (futur)") }, modifier = Modifier.weight(1f))
        }
        Text("Les numéros Mobile Money sont enregistrés pour affichage sur facture. Les paiements " +
            "électroniques seront câblés dans une phase ultérieure.", fontSize = 11.sp, color = Color(0xFF667085))
    }
}

@Composable
internal fun PlatformLegalSection(
    invoiceNumberFormat: String, onFormatChange: (String) -> Unit,
    legalMentions: String, onLegalChange: (String) -> Unit
) {
    SectionCard("Numérotation & mentions légales") {
        OutlinedTextField(value = invoiceNumberFormat, onValueChange = onFormatChange,
            label = { Text("Format numéro de facture") }, placeholder = { Text("FAC-{YYYY}-{NNNNNN}") },
            modifier = Modifier.fillMaxWidth())
        Text("Variables disponibles : {YYYY} = année, {NNNNNN} = séquence annuelle zéro-paddée.",
            fontSize = 11.sp, color = Color(0xFF667085))
        OutlinedTextField(value = legalMentions, onValueChange = onLegalChange,
            label = { Text("Mentions légales (pied de page)") },
            placeholder = { Text("Ex. En cas de retard de paiement, des pénalités seront appliquées conformément à la loi.") },
            modifier = Modifier.fillMaxWidth().height(110.dp), maxLines = 6)
    }
}
