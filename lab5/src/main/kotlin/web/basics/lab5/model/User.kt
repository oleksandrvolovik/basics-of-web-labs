package web.basics.lab5.model

data class User(
    val email: String,
    val passwordHash: String,
    val permissions: Set<Permission> = emptySet(),

    val firstName: String,
    val lastName: String
)

fun User.hasPermission(permission: Permission) = this.permissions.contains(permission)