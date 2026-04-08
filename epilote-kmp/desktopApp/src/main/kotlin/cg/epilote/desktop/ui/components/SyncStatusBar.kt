package cg.epilote.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.*
import cg.epilote.shared.domain.model.SyncStatus
import cg.epilote.shared.presentation.viewmodel.SyncIndicatorViewModel

@Composable
fun SyncStatusBar(viewModel: SyncIndicatorViewModel) {
    val status by viewModel.syncStatus.collectAsState()
    val pending by viewModel.pendingCount.collectAsState()
    val label by viewModel.label.collectAsState(initial = "Hors ligne")

    val (dotColor, bgColor) = when (status) {
        SyncStatus.SYNCED  -> Pair(Color(0xFF00875A), Color(0xFFE3F7EF))
        SyncStatus.OFFLINE -> Pair(Color(0xFFDE350B), Color(0xFFFFEDEB))
        SyncStatus.PENDING -> Pair(Color(0xFFFF8B00), Color(0xFFFFF0D1))
        else               -> Pair(Color(0xFFDE350B), Color(0xFFFFEDEB))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(bgColor)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Text(label, fontSize = 12.sp, color = Color(0xFF172B4D))

        if (status == SyncStatus.PENDING && pending > 0) {
            Spacer(Modifier.weight(1f))
            Text(
                "$pending modification(s) en attente de sync",
                fontSize = 11.sp,
                color = Color(0xFF856404)
            )
        }
    }
}
