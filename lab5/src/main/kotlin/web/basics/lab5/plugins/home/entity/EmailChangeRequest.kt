package web.basics.lab5.plugins.home.entity

import kotlinx.serialization.Serializable

@Serializable
data class EmailChangeRequest(
    val hashedPassword: String,
    val newEmail: String
)