package cg.epilote.desktop.ui.screens

import androidx.compose.runtime.Composable
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.shared.domain.model.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AdminMessagingScreen(
    session: UserSession,
    groupes: List<GroupeDto>,
    admins: List<AdminUserDto>,
    isLoading: Boolean,
    scope: CoroutineScope,
    client: DesktopAdminClient,
    onRefresh: () -> Unit,
    initialMailbox: AdminMessagingMailbox = AdminMessagingMailbox.INBOX,
    sseReloadTick: StateFlow<Int>? = null
) {
    AdminMessagingHub(
        session = session,
        groupes = groupes,
        admins = admins,
        isLoading = isLoading,
        scope = scope,
        client = client,
        onRefresh = onRefresh,
        initialMailbox = initialMailbox,
        sseReloadTick = sseReloadTick
    )
}
