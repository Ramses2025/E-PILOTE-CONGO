package cg.epilote.backend.admin

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateAnnouncementRequest(
    @field:NotBlank @field:Size(max = 200) val titre: String,
    @field:NotBlank @field:Size(max = 50_000) val contenu: String,
    val cible: String = "all"
)

data class AnnouncementResponse(
    val id: String,
    val titre: String,
    val contenu: String,
    val cible: String,
    val createdBy: String,
    val createdAt: Long
)
