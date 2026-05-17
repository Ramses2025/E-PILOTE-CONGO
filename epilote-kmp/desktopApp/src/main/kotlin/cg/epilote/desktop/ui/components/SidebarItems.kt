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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.*

@Composable
internal fun SidebarItem(
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

@Composable
internal fun SidebarFooter(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = if (isExpanded) 8.dp else 4.dp, vertical = 4.dp),
        horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isExpanded) 4.dp else 0.dp, vertical = 2.dp)
                .background(Color.Transparent, RoundedCornerShape(8.dp))
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { onLogout() }
                .padding(horizontal = if (isExpanded) 12.dp else 0.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.spacedBy(10.dp) else Arrangement.Center
        ) {
            Icon(
                Icons.Default.Logout,
                contentDescription = "Déconnexion",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(18.dp)
            )
            if (isExpanded) {
                Text("Déconnexion", color = Color(0xFFEF4444), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(4.dp))
        if (isExpanded) {
            Text("v1.0.0", color = EpiloteTextMuted, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 12.dp))
        } else {
            Text("v1", color = EpiloteTextMuted, fontSize = 9.sp)
        }
    }
}

@Composable
internal fun SidebarModuleCategoryItem(
    categoryName: String,
    moduleCount: Int,
    activeCount: Int
) {
    val allActive = activeCount == moduleCount
    val badgeColor = if (allActive) EpiloteGreen else Color(0xFFF59E0B)
    val badgeText = "$activeCount/$moduleCount"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(badgeColor, RoundedCornerShape(3.dp))
            )
            Text(
                categoryName,
                color = EpiloteTextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeColor.copy(alpha = 0.15f)
        ) {
            Text(
                badgeText,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = badgeColor
            )
        }
    }
}
