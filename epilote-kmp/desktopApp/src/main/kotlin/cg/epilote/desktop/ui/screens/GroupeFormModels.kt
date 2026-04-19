package cg.epilote.desktop.ui.screens

import cg.epilote.desktop.data.CreateGroupeDto
import cg.epilote.desktop.data.UpdateGroupeDto

internal data class GroupeFormInitialData(
    val nom: String = "",
    val email: String = "",
    val phone: String = "",
    val department: String = "",
    val city: String = "",
    val address: String = "",
    val logo: String? = null,
    val logoFileName: String = "",
    val description: String = "",
    val foundedYear: String = "",
    val website: String = "",
    val planId: String = "",
    val isActive: Boolean = true
)

internal fun GroupeDto.toGroupeFormInitialData(): GroupeFormInitialData = GroupeFormInitialData(
    nom = nom,
    email = email.orEmpty(),
    phone = phone.orEmpty(),
    department = department.orEmpty(),
    city = city.orEmpty(),
    address = address.orEmpty(),
    logo = logo,
    logoFileName = if (logo.isNullOrBlank()) "" else "Logo actuel",
    description = description.orEmpty(),
    foundedYear = foundedYear?.toString().orEmpty(),
    website = website.orEmpty(),
    planId = planId,
    isActive = isActive
)

internal fun CreateGroupeDto.toUpdateGroupeDto(isActive: Boolean? = null): UpdateGroupeDto = UpdateGroupeDto(
    nom = nom,
    email = email,
    phone = phone,
    department = department,
    city = city,
    address = address,
    logo = logo,
    description = description,
    foundedYear = foundedYear,
    website = website,
    planId = planId,
    isActive = isActive ?: this.isActive
)
