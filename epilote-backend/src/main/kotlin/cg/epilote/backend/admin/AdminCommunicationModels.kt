package cg.epilote.backend.admin

import jakarta.validation.constraints.NotBlank

data class CreateAdminMessageRequest(
    @field:NotBlank val sujet: String,
    @field:NotBlank val contenu: String,
    @field:NotBlank val targetType: String,
    val groupId: String? = null,
    val adminId: String? = null
)

data class AdminMessageResponse(
    val id: String,
    val sujet: String,
    val contenu: String,
    val targetType: String,
    val groupId: String?,
    val adminId: String?,
    val threadKey: String,
    val status: String,
    val createdBy: String,
    val createdAt: Long
)
