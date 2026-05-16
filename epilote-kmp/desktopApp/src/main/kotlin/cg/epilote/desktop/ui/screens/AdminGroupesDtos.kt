package cg.epilote.desktop.ui.screens

import kotlinx.serialization.Serializable

@Serializable
data class GroupeDto(
    val id: String = "",
    val nom: String = "",
    val slug: String = "",
    val email: String? = null,
    val phone: String? = null,
    val department: String? = null,
    val city: String? = null,
    val address: String? = null,
    val country: String = "Congo",
    val logo: String? = null,
    val description: String? = null,
    val foundedYear: Int? = null,
    val website: String? = null,
    val planId: String = "",
    val ecolesCount: Int = 0,
    val usersCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = 0
)
