package cg.epilote.desktop.data

import cg.epilote.desktop.ui.screens.AdminUserDto
import cg.epilote.desktop.ui.screens.CategorieDto
import cg.epilote.desktop.ui.screens.GroupeDto
import cg.epilote.desktop.ui.screens.ModuleDto
import cg.epilote.desktop.ui.screens.PlanDto
import cg.epilote.desktop.ui.screens.UserDto

internal fun GroupeApiDto.toGroupeDto() = GroupeDto(
    id = id, nom = nom, slug = slug, email = email, phone = phone,
    department = department, city = city, address = address, country = country,
    logo = logo, description = description, foundedYear = foundedYear,
    website = website, planId = planId, ecolesCount = ecolesCount,
    usersCount = usersCount, isActive = isActive, createdAt = createdAt
)

internal fun PlanApiDto.toPlanDto() = PlanDto(
    id = id, nom = nom, type = type, prixXAF = prixXAF,
    currency = currency, maxStudents = maxStudents, maxPersonnel = maxPersonnel,
    modulesIncluded = modulesIncluded, isActive = isActive
)

internal fun ModuleApiDto.toModuleDto() = ModuleDto(
    id, code, nom, categorieCode, description, isCore, requiredPlan, isActive, ordre
)

internal fun CategorieApiDto.toCategorieDto() = CategorieDto(
    code = code, nom = nom, isCore = isCore, ordre = ordre, isActive = isActive
)

internal fun AdminUserApiDto.toAdminUserDto() = AdminUserDto(
    id = id, username = username, firstName = firstName, lastName = lastName,
    email = email, phone = phone, role = role, status = status,
    gender = gender, dateOfBirth = dateOfBirth, groupId = groupId,
    schoolId = schoolId, avatar = avatar, address = address,
    birthPlace = birthPlace, mustChangePassword = mustChangePassword,
    lastLoginAt = lastLoginAt, loginAttempts = loginAttempts,
    isActive = isActive, createdAt = createdAt, updatedAt = updatedAt
)

internal fun UserApiDto.toAdminUserDto() = AdminUserDto(
    id = id, username = username, firstName = firstName, lastName = lastName,
    email = email, phone = null, role = role,
    status = if (isActive) "active" else "inactive",
    gender = null, dateOfBirth = null, groupId = groupId, schoolId = schoolId,
    avatar = null, address = null, birthPlace = null, mustChangePassword = false,
    lastLoginAt = null, loginAttempts = 0, isActive = isActive,
    createdAt = createdAt, updatedAt = createdAt
)

internal fun AdminUserApiDto.toUserDto() = UserDto(
    id = id, username = username, firstName = firstName, lastName = lastName,
    email = email, schoolId = schoolId ?: "", groupId = groupId ?: "",
    profilId = "", role = role, isActive = isActive, createdAt = createdAt
)

internal fun UserApiDto.toUserDto() = UserDto(
    id = id, username = username, firstName = firstName, lastName = lastName,
    email = email, schoolId = schoolId ?: "", groupId = groupId,
    profilId = profilId ?: "", role = role, isActive = isActive, createdAt = createdAt
)

internal fun AdminUserDto.toUserDto() = UserDto(
    id = id, username = username, firstName = firstName, lastName = lastName,
    email = email, schoolId = schoolId ?: "", groupId = groupId ?: "",
    profilId = "", role = role, isActive = isActive, createdAt = createdAt
)
