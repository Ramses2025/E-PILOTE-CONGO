package cg.epilote.backend.admin

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateAdminMessageRequest(
    @field:NotBlank @field:Size(max = 200) val sujet: String,
    @field:NotBlank @field:Size(max = 50_000) val contenu: String,
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
