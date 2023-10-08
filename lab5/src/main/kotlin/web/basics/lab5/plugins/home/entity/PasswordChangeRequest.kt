package web.basics.lab5.plugins.home.entity

import kotlinx.serialization.Serializable

@Serializable
data class PasswordChangeRequest(
    val hashedOldPassword: String,
    val hashedNewPassword: String
)