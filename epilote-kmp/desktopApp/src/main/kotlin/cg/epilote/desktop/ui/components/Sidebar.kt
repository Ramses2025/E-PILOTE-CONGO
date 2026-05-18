package cg.epilote.desktop.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.CategorieWithModulesDto
import cg.epilote.desktop.ui.theme.*
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.shared.domain.model.UserSession

enum class DesktopScreen(
    val label: String,
    val icon: ImageVector,
    val moduleSlug: String?,
    val section: SidebarSection = SidebarSection.PRINCIPAL,
    val requiredRole: String? = null,
    val showInSidebar: Boolean = true
) {
    // ── 1. Pilotage ─────────────────────────────────────────────
    ADMIN_DASHBOARD      ("Tableau de bord",     Icons.Default.Dashboard,          null, SidebarSection.SA_PILOTAGE,       "SUPER_ADMIN"),
    ADMIN_ALERTS         ("Alertes & Seuils",    Icons.Default.Warning,            null, SidebarSection.SA_PILOTAGE,       "SUPER_ADMIN"),

    // ── 2. Organisation ─────────────────────────────────────────
    ADMIN_GROUPES        ("Groupes scolaires",   Icons.Default.Business,           null, SidebarSection.SA_ORGANISATION,   "SUPER_ADMIN"),
    ADMIN_ADMINISTRATORS ("Administrateurs",     Icons.Default.SupervisedUserCircle, null, SidebarSection.SA_ORGANISATION, "SUPER_ADMIN"),

    // ── 3. Configuration plateforme ─────────────────────────────
    ADMIN_CATEGORIES     ("Catégories",          Icons.Default.Category,           null, SidebarSection.SA_CONFIGURATION,  "SUPER_ADMIN"),
    ADMIN_MODULES        ("Modules",             Icons.Default.Extension,          null, SidebarSection.SA_CONFIGURATION,  "SUPER_ADMIN"),

    // ── 4. Monétisation ─────────────────────────────────────────
    ADMIN_PLANS          ("Plans & Tarifs",      Icons.Default.CreditCard,         null, SidebarSection.SA_MONETISATION,   "SUPER_ADMIN"),
    ADMIN_SUBSCRIPTIONS  ("Abonnements",         Icons.Default.Subscriptions,      null, SidebarSection.SA_MONETISATION,   "SUPER_ADMIN"),
    ADMIN_INVOICES       ("Factures",            Icons.Default.Receipt,            null, SidebarSection.SA_MONETISATION,   "SUPER_ADMIN"),
    ADMIN_PAYMENTS       ("Paiements",           Icons.Default.Payments,           null, SidebarSection.SA_MONETISATION,   "SUPER_ADMIN"),

    // ── 5. Communication ────────────────────────────────────────
    ADMIN_NOTIFICATIONS  ("Notifications",       Icons.Default.Notifications,      null, SidebarSection.SA_COMMUNICATION,  "SUPER_ADMIN"),
    ADMIN_MESSAGING      ("Messagerie",          Icons.Default.Forum,              null, SidebarSection.SA_COMMUNICATION,  "SUPER_ADMIN"),
    ADMIN_ANNOUNCEMENTS  ("Annonces",            Icons.Default.Campaign,           null, SidebarSection.SA_COMMUNICATION,  "SUPER_ADMIN", false),

    // ── 6. Support ──────────────────────────────────────────────
    ADMIN_TICKETS        ("Support",             Icons.Default.SupportAgent,       null, SidebarSection.SA_SUPPORT,        "SUPER_ADMIN"),

    // ── 7. Audit ────────────────────────────────────────────────
    ADMIN_AUDIT_LOG      ("Journal d'audit",     Icons.Default.History,            null, SidebarSection.SA_AUDIT,          "SUPER_ADMIN"),

    // ── 8. Paramètres plateforme ────────────────────────────────
    ADMIN_PLATFORM_SETTINGS ("Paramètres",       Icons.Default.Settings,           null, SidebarSection.SA_PARAMETRES,     "SUPER_ADMIN"),

    // ── Fiche groupe (navigation drill-down, non visible en sidebar) ────
    ADMIN_GROUPE_DETAIL  ("Fiche Groupe",        Icons.Default.Business,           null, SidebarSection.SA_ORGANISATION,   "SUPER_ADMIN", false),

    // ── Admin Groupe : Gestion de son tenant ────────────────────
    GROUPE_DASHBOARD         ("Tableau de bord",      Icons.Default.Dashboard,          null, SidebarSection.GESTION_GROUPE,    "ADMIN_GROUPE"),
    GROUPE_ECOLES            ("Mes écoles",           Icons.Default.School,             null, SidebarSection.GESTION_GROUPE,    "ADMIN_GROUPE"),
    GROUPE_UTILISATEURS      ("Utilisateurs",         Icons.Default.People,             null, SidebarSection.GESTION_GROUPE,    "ADMIN_GROUPE"),
    GROUPE_PROFILS           ("Profils d'accès",      Icons.Default.ManageAccounts,     null, SidebarSection.GESTION_GROUPE,    "ADMIN_GROUPE"),
    GROUPE_ANNEES_SCOLAIRES  ("Années scolaires",     Icons.Default.CalendarMonth,      null, SidebarSection.GESTION_GROUPE,    "ADMIN_GROUPE"),
    GROUPE_MODULES_PAR_ECOLE ("Modules par école",    Icons.Default.ViewModule,         null, SidebarSection.GESTION_GROUPE,    "ADMIN_GROUPE"),
    GROUPE_PARAMETRES        ("Paramètres",           Icons.Default.Settings,           null, SidebarSection.GESTION_GROUPE,    "ADMIN_GROUPE"),

    // ── Pédagogie (module-based, visible by ADMIN_GROUPE + USER) ───
    DASHBOARD    ("Tableau de bord",  Icons.Default.Dashboard,      null,                 SidebarSection.PRINCIPAL),
    CLASSES      ("Classes",          Icons.Default.Class,          "classes",            SidebarSection.PRINCIPAL),
    ELEVES       ("Élèves",           Icons.Default.People,         "eleves",             SidebarSection.PRINCIPAL),
    NOTES        ("Notes",            Icons.Default.Grade,          "notes",              SidebarSection.PRINCIPAL),
    ABSENCES     ("Présences",        Icons.Default.CheckBox,       "presences-eleves",   SidebarSection.PRINCIPAL),
    BULLETINS    ("Bulletins",        Icons.Default.Description,    "bulletins",          SidebarSection.PRINCIPAL),
    CAHIER       ("Cahier de textes", Icons.Default.MenuBook,       "cahier-textes",      SidebarSection.PRINCIPAL),

    // ── Gestion (module-based) ──────────────────────────────────
    INSCRIPTIONS ("Inscriptions",     Icons.Default.HowToReg,       "inscriptions",       SidebarSection.GESTION),
    FINANCES     ("Finances",         Icons.Default.AccountBalance,  "finances",          SidebarSection.GESTION),
    PERSONNEL    ("Personnel",        Icons.Default.Badge,           "personnel",         SidebarSection.GESTION),
    DISCIPLINE   ("Discipline",       Icons.Default.Gavel,           "discipline",        SidebarSection.GESTION),
    ANNONCES     ("Annonces",         Icons.Default.Campaign,        "annonces",          SidebarSection.GESTION),

    // ── Système ─────────────────────────────────────────────────
    CONFLICTS    ("Conflits sync",    Icons.Default.SyncProblem,     null,                SidebarSection.SYSTEME),
}

enum class SidebarSection(val label: String) {
    SA_PILOTAGE("PILOTAGE"),
    SA_ORGANISATION("ORGANISATION"),
    SA_CONFIGURATION("CONFIGURATION"),
    SA_MONETISATION("MONÉTISATION"),
    SA_COMMUNICATION("COMMUNICATION"),
    SA_SUPPORT("SUPPORT"),
    SA_AUDIT("AUDIT"),
    SA_PARAMETRES("PARAMÈTRES"),
    GESTION_GROUPE("GESTION GROUPE"),
    PRINCIPAL("PÉDAGOGIE"),
    GESTION("GESTION"),
    SYSTEME("SYSTÈME")
}

@Composable
fun Sidebar(
    session: UserSession,
    currentScreen: DesktopScreen,
    onScreenSelected: (DesktopScreen) -> Unit,
    isExpanded: Boolean = true,
    onToggleExpanded: () -> Unit = {},
    dynamicCategories: List<CategorieWithModulesDto> = emptyList(),
    onLogout: () -> Unit = {}
) {
    val sidebarWidth = if (isExpanded) 220.dp else 64.dp

    val visibleScreens = DesktopScreen.entries.filter { screen ->
        if (!screen.showInSidebar) return@filter false
        // SUPER_ADMIN only sees ADMIN section
        if (session.role == "SUPER_ADMIN") {
            return@filter screen.requiredRole == "SUPER_ADMIN"
        }
        // ADMIN_GROUPE: seuls les écrans de gestion sont en nav principale.
        // Les écrans module (moduleSlug != null) s'affichent EXCLUSIVEMENT dans la section MODULES.
        if (session.role == "ADMIN_GROUPE") {
            if (screen.requiredRole == "ADMIN_GROUPE") return@filter true
            return@filter false
        }
        val roleOk = screen.requiredRole == null || session.role == screen.requiredRole
        val moduleOk = screen.moduleSlug == null || session.hasModule(screen.moduleSlug)
        roleOk && moduleOk
    }
    val screensBySection = visibleScreens.groupBy { it.section }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(sidebarWidth)
            .animateContentSize(animationSpec = tween(250))
            .background(EpiloteSidebar)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // ── Header + Toggle ───────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isExpanded) 16.dp else 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val density = LocalDensity.current
                    val logoPainter = remember {
                        Thread.currentThread().contextClassLoader
                            .getResourceAsStream("logo.svg")
                            ?.let { loadSvgPainter(it, density) }
                    }
                    if (logoPainter != null) {
                        Image(
                            painter = logoPainter,
                            contentDescription = "E-Pilote Congo",
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(36.dp)
                                .background(EpiloteGreen, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("EP", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    if (isExpanded) {
                        Column {
                            Text("E-PILOTE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("CONGO", color = EpiloteTextMuted, fontSize = 10.sp)
                        }
                    }
                }
                // Toggle button
                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.size(28.dp).cursorHand()
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                        contentDescription = if (isExpanded) "Replier" else "Déplier",
                        tint = EpiloteTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            SidebarSection.entries.forEach { section ->
                val items = screensBySection[section] ?: return@forEach
                if (isExpanded) {
                    Text(
                        section.label,
                        color = EpiloteTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                }
                items.forEach { screen ->
                    SidebarItem(
                        screen = screen,
                        isSelected = currentScreen == screen,
                        isExpanded = isExpanded,
                        onClick = { onScreenSelected(screen) }
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            if (session.role == "ADMIN_GROUPE") {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                if (dynamicCategories.isEmpty()) {
                    if (isExpanded) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "MODULES",
                            color = EpiloteTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Extension,
                                contentDescription = null,
                                tint = EpiloteTextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Aucun module disponible\npour votre plan",
                                color = EpiloteTextMuted.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    val allModuleCodes = dynamicCategories
                        .flatMap { cat -> cat.modules.map { it.code } }.toSet()
                    val moduleScreens = DesktopScreen.entries.filter { screen ->
                        screen.moduleSlug != null && screen.moduleSlug in allModuleCodes
                    }

                    if (isExpanded) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "MODULES",
                            color = EpiloteTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        dynamicCategories.forEach { cat ->
                            val catCodes = cat.modules.map { it.code }.toSet()
                            val catScreens = moduleScreens.filter { it.moduleSlug in catCodes }
                            if (catScreens.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        cat.nom.uppercase(),
                                        color = EpiloteTextMuted.copy(alpha = 0.65f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            "${catScreens.size}",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            fontSize = 9.sp,
                                            color = EpiloteTextMuted
                                        )
                                    }
                                }
                                catScreens.forEach { screen ->
                                    SidebarItem(
                                        screen   = screen,
                                        isSelected = currentScreen == screen,
                                        isExpanded = true,
                                        onClick  = { onScreenSelected(screen) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Sidebar repliée : icônes de navigation seulement
                        Spacer(Modifier.height(4.dp))
                        moduleScreens.forEach { screen ->
                            SidebarItem(
                                screen   = screen,
                                isSelected = currentScreen == screen,
                                isExpanded = false,
                                onClick  = { onScreenSelected(screen) }
                            )
                        }
                    }
                }
            }
        }

        // ── Footer : Réduire + Déconnexion + version ──
        SidebarFooter(
            isExpanded = isExpanded,
            onToggleExpanded = onToggleExpanded,
            onLogout = onLogout
        )
    }
}

