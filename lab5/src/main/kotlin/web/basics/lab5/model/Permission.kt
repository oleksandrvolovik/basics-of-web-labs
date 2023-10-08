package web.basics.lab5.model

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import web.basics.lab5.service.UserService

enum class Permission {
    GET_INFO_ABOUT_OWN_ACCOUNT,
    CHANGE_OWN_EMAIL,
    CHANGE_OWN_PASSWORD,
    CHANGE_OWN_NAME,

    GET_INFO_ABOUT_OTHER_ACCOUNTS,
    CHANGE_OTHER_EMAILS,
    CHANGE_OTHER_PASSWORDS,
    CHANGE_OTHER_NAMES,

    CHANGE_PERMISSIONS
}

suspend inline fun PipelineContext<*, ApplicationCall>.requirePermission(
    userService: UserService,
    permission: Permission,
    onPermissionGranted: (userSession: UserSession, user: User) -> Unit
) {
    val userSession = call.principal<UserSession>()
    val user = userSession?.let { userService.getUserByEmail(it.name) }
    if (user != null) {
        if (!user.permissions.contains(permission)) {
            call.respond(HttpStatusCode.Forbidden, "User must have $permission permission")
        } else {
            onPermissionGranted.invoke(userSession, user)
        }
    } else {
        call.respond(HttpStatusCode.Unauthorized)
    }
}

suspend inline fun PipelineContext<*, ApplicationCall>.requirePermissions(
    userService: UserService,
    permissions: Collection<Permission>,
    onPermissionsGranted: (userSession: UserSession, user: User) -> Unit
) {
    val userSession = call.principal<UserSession>()
    val user = userSession?.let { userService.getUserByEmail(it.name) }
    if (user != null) {
        if (!user.permissions.containsAll(permissions)) {
            call.respond(HttpStatusCode.Forbidden, "User must have $permissions permissions")
        } else {
            onPermissionsGranted.invoke(userSession, user)
        }
    } else {
        call.respond(HttpStatusCode.Unauthorized)
    }
}