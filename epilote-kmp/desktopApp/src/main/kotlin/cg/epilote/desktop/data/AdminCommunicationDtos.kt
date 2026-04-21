package cg.epilote.desktop.data

import kotlinx.serialization.Serializable

@Serializable
data class AdminMessageApiDto(
    val id: String = "",
    val sujet: String = "",
    val contenu: String = "",
    val targetType: String = "all_groups",
    val groupId: String? = null,
    val adminId: String? = null,
    val threadKey: String = "",
    val status: String = "sent",
    val createdBy: String = "",
    val createdAt: Long = 0
)

@Serializable
data class CreateAdminMessageDto(
    val sujet: String,
    val contenu: String,
    val targetType: String,
    val groupId: String? = null,
    val adminId: String? = null
)
