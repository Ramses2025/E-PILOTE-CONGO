package cg.epilote.desktop.ui.screens

import androidx.compose.runtime.Composable
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.shared.domain.model.UserSession
import kotlinx.coroutines.CoroutineScope

@Composable
fun AdminAnnouncementsScreen(
    session: UserSession,
    groupes: List<GroupeDto>,
    admins: List<AdminUserDto>,
    isLoading: Boolean,
    scope: CoroutineScope,
    client: DesktopAdminClient,
    onRefresh: () -> Unit
) {
    AdminMessagingScreen(
        session = session,
        groupes = groupes,
        admins = admins,
        isLoading = isLoading,
        scope = scope,
        client = client,
        onRefresh = onRefresh,
        initialMailbox = AdminMessagingMailbox.ANNOUNCEMENTS
    )
}
