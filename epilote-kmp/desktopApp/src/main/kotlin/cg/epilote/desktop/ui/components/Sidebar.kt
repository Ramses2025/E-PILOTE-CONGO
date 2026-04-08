package cg.epilote.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.*
import cg.epilote.shared.domain.model.UserSession

enum class DesktopScreen(
    val label: String,
    val icon: ImageVector,
    val moduleSlug: String?,
    val section: SidebarSection = SidebarSection.PRINCIPAL,
    val requiredRole: String? = null
) {
    // ── Super Admin (Groupes, Plans, Modules, Catégories) ────────
    ADMIN_DASHBOARD ("Dashboard Admin",  Icons.Default.AdminPanelSettings, null, SidebarSection.ADMIN, "SUPER_ADMIN"),
    ADMIN_GROUPES   ("Groupes",          Icons.Default.Business,           null, SidebarSection.ADMIN, "SUPER_ADMIN"),
    ADMIN_PLANS     ("Plans",            Icons.Default.CreditCard,         null, SidebarSection.ADMIN, "SUPER_ADMIN"),
    ADMIN_MODULES   ("Modules",          Icons.Default.Extension,          null, SidebarSection.ADMIN, "SUPER_ADMIN"),

    // ── Pédagogie ────────────────────────────────────────────────
    DASHBOARD    ("Tableau de bord",  Icons.Default.Dashboard,      null,                 SidebarSection.PRINCIPAL),
    CLASSES      ("Classes",          Icons.Default.Class,          "classes",            SidebarSection.PRINCIPAL),
    ELEVES       ("Élèves",           Icons.Default.People,         "eleves",             SidebarSection.PRINCIPAL),
    NOTES        ("Notes",            Icons.Default.Grade,          "notes",              SidebarSection.PRINCIPAL),
    ABSENCES     ("Présences",        Icons.Default.CheckBox,       "presences-eleves",   SidebarSection.PRINCIPAL),
    BULLETINS    ("Bulletins",        Icons.Default.Description,    "bulletins",          SidebarSection.PRINCIPAL),
    CAHIER       ("Cahier de textes", Icons.Default.MenuBook,       "cahier-textes",      SidebarSection.PRINCIPAL),

    // ── Gestion ──────────────────────────────────────────────────
    INSCRIPTIONS ("Inscriptions",     Icons.Default.HowToReg,       "inscriptions",       SidebarSection.GESTION),
    FINANCES     ("Finances",         Icons.Default.AccountBalance,  "finances",          SidebarSection.GESTION),
    PERSONNEL    ("Personnel",        Icons.Default.Badge,           "personnel",         SidebarSection.GESTION),
    DISCIPLINE   ("Discipline",       Icons.Default.Gavel,           "discipline",        SidebarSection.GESTION),
    ANNONCES     ("Annonces",         Icons.Default.Campaign,        "annonces",          SidebarSection.GESTION),

    // ── Système ──────────────────────────────────────────────────
    CONFLICTS    ("Conflits sync",    Icons.Default.SyncProblem,     null,                SidebarSection.SYSTEME),
}

enum class SidebarSection(val label: String) {
    ADMIN("ADMINISTRATION"),
    PRINCIPAL("PÉDAGOGIE"),
    GESTION("GESTION"),
    SYSTEME("SYSTÈME")
}

@Composable
fun Sidebar(
    session: UserSession,
    currentScreen: DesktopScreen,
    onScreenSelected: (DesktopScreen) -> Unit,
    onLogout: () -> Unit
) {
    val visibleScreens = DesktopScreen.entries.filter { screen ->
        val roleOk = screen.requiredRole == null || session.role == screen.requiredRole
        val moduleOk = screen.moduleSlug == null || session.hasModule(screen.moduleSlug)
        roleOk && moduleOk
    }
    val screensBySection = visibleScreens.groupBy { it.section }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(220.dp)
            .background(EpiloteSidebar)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(EpiloteGreen, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("EP", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column {
                    Text("E-PILOTE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("CONGO", color = EpiloteTextMuted, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            SidebarSection.entries.forEach { section ->
                val items = screensBySection[section] ?: return@forEach
                Text(
                    section.label,
                    color = EpiloteTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                items.forEach { screen ->
                    SidebarItem(
                        screen = screen,
                        isSelected = currentScreen == screen,
                        onClick = { onScreenSelected(screen) }
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        Column {
            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(EpiloteGreenDark, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        session.firstName.take(1).uppercase() + session.lastName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${session.firstName} ${session.lastName}",
                        color = EpiloteTextOnDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(session.role, color = EpiloteTextMuted, fontSize = 10.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Logout, null, tint = EpiloteTextMuted, modifier = Modifier.size(16.dp))
                Text("Déconnexion", color = EpiloteTextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SidebarItem(
    screen: DesktopScreen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .background(
                if (isSelected) EpiloteSidebarSelected else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
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
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
