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

    // ── Admin Groupe : Gestion de son tenant ────────────────────
    GROUPE_DASHBOARD     ("Dashboard Groupe",    Icons.Default.Dashboard,          null, SidebarSection.GESTION_GROUPE,    "ADMIN_GROUPE"),
    GROUPE_ECOLES        ("Écoles",              Icons.Default.School,             null, SidebarSection.GESTION_GROUPE,    "ADMIN_GROUPE"),
    GROUPE_UTILISATEURS  ("Utilisateurs",        Icons.Default.People,             null, SidebarSection.GESTION_GROUPE,    "ADMIN_GROUPE"),
    GROUPE_PROFILS       ("Profils d'accès",     Icons.Default.ManageAccounts,     null, SidebarSection.GESTION_GROUPE,    "ADMIN_GROUPE"),

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
    onToggleExpanded: () -> Unit = {}
) {
    val sidebarWidth = if (isExpanded) 220.dp else 64.dp

    val visibleScreens = DesktopScreen.entries.filter { screen ->
        if (!screen.showInSidebar) return@filter false
        // SUPER_ADMIN only sees ADMIN section
        if (session.role == "SUPER_ADMIN") {
            return@filter screen.requiredRole == "SUPER_ADMIN"
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
        }

        // ── Footer version ──
        Column(
            modifier = Modifier.padding(horizontal = if (isExpanded) 16.dp else 4.dp, vertical = 8.dp),
            horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(Modifier.height(8.dp))
            if (isExpanded) {
                Text("v1.0.0", color = EpiloteTextMuted, fontSize = 10.sp)
            } else {
                Text("v1", color = EpiloteTextMuted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun SidebarItem(
    screen: DesktopScreen,
    isSelected: Boolean,
    isExpanded: Boolean = true,
    onClick: () -> Unit
) {
    if (isExpanded) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .background(
                    if (isSelected) EpiloteSidebarSelected else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(16.dp)
                        .background(EpiloteGreen, RoundedCornerShape(2.dp))
                )
            } else {
                Spacer(Modifier.width(3.dp))
            }
            Icon(
                screen.icon,
                contentDescription = screen.label,
                tint = if (isSelected) EpiloteGreen else EpiloteTextMuted,
                modifier = Modifier.size(18.dp)
            )
            Text(
                screen.label,
                color = if (isSelected) Color.White else EpiloteTextMuted,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        // Collapsed: icon only, centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) EpiloteSidebarSelected else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    screen.icon,
                    contentDescription = screen.label,
                    tint = if (isSelected) EpiloteGreen else EpiloteTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
