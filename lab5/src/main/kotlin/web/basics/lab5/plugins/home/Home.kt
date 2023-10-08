package web.basics.lab5.plugins.home

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import org.koin.ktor.ext.inject
import web.basics.lab5.model.Permission
import web.basics.lab5.model.hasPermission
import web.basics.lab5.model.requirePermission
import web.basics.lab5.plugins.home.entity.EmailChangeRequest
import web.basics.lab5.plugins.home.entity.PasswordChangeRequest
import web.basics.lab5.service.UserService

fun Application.configureHome() {

    val userService by inject<UserService>()
    val logger = java.util.logging.Logger.getLogger("Home")

    routing {
        authenticate("auth-session", strategy = AuthenticationStrategy.Required) {

            route("/home") {
                get {
                    requirePermission(
                        userService, Permission.GET_INFO_ABOUT_OWN_ACCOUNT
                    ) { _, user ->
                        call.respond(
                            ThymeleafContent(
                                "home",
                                mapOf(
                                    "user" to user,
                                    "userCanGetInfoAboutOtherAccounts" to user.hasPermission(Permission.GET_INFO_ABOUT_OTHER_ACCOUNTS),
                                    "userCanChangeOwnPassword" to user.hasPermission(Permission.CHANGE_OWN_PASSWORD),
                                    "userCanChangeOwnEmail" to user.hasPermission(Permission.CHANGE_OWN_EMAIL),
                                    "userCanChangeOwnName" to user.hasPermission(Permission.CHANGE_OWN_NAME)
                                )
                            )
                        )
                    }
                }

                put("/email") {
                    requirePermission(userService, Permission.CHANGE_OWN_EMAIL) { _, user ->
                        val emailChangeRequest = call.receive<EmailChangeRequest>()
                        val changed = userService.changeEmail(
                            user,
                            emailChangeRequest.hashedPassword,
                            emailChangeRequest.newEmail
                        )

                        if (changed) {
                            call.respond("Success")
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "Invalid password")
                        }
                    }
                }

                put("/password") {
                    requirePermission(userService, Permission.CHANGE_OWN_PASSWORD) { _, user ->
                        val passwordChangeRequest = call.receive<PasswordChangeRequest>()
                        val changed = userService.changePassword(
                            user,
                            passwordChangeRequest.hashedOldPassword,
                            passwordChangeRequest.hashedNewPassword
                        )

                        if (changed) {
                            call.respond("Success")
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "Invalid password")
                        }
                    }
                }

                put("/first-name") {
                    requirePermission(userService, Permission.CHANGE_OWN_NAME) { _, user ->
                        userService.changeFirstName(
                            user,
                            call.receiveText()
                        )

                        call.respond("Success")
                    }
                }

                put("/last-name") {
                    requirePermission(userService, Permission.CHANGE_OWN_NAME) { _, user ->
                        userService.changeLastName(
                            user,
                            call.receiveText()
                        )

                        call.respond("Success")
                    }
                }
            }

            route("/accounts") {
                get {
                    requirePermission(
                        userService, Permission.GET_INFO_ABOUT_OTHER_ACCOUNTS
                    ) { _, user ->
                        call.respond(
                            ThymeleafContent(
                                "accounts",
                                mapOf(
                                    "users" to userService.getAllUsers(),
                                    "permissions" to Permission.values(),
                                    "userCanChangeEmails" to user.hasPermission(Permission.CHANGE_OTHER_EMAILS),
                                    "userCanChangePasswords" to user.hasPermission(Permission.CHANGE_OTHER_PASSWORDS),
                                    "userCanChangePermissions" to user.hasPermission(Permission.CHANGE_PERMISSIONS),
                                    "userCanChangeNames" to user.hasPermission(Permission.CHANGE_OTHER_NAMES)
                                )
                            )
                        )
                    }
                }

                // Change users email
                put("/{email}") {
                    val email = call.parameters["email"]!!
                    val newEmail = call.receiveText()

                    requirePermission(userService, Permission.CHANGE_OTHER_EMAILS) { _, user ->
                        val editedUser = userService.getUserByEmail(email)
                        if (editedUser == null) {
                            call.respond(HttpStatusCode.NotFound, "User with email $email not found")
                        } else {
                            userService.changeEmail(user, user.passwordHash, newEmail)
                            call.respond("Successfully changed user email $email to $newEmail")
                        }
                    }
                }

                // Change users first name
                put("/{email}/first-name") {
                    val email = call.parameters["email"]!!
                    val newFirstName = call.receiveText()

                    requirePermission(userService, Permission.CHANGE_OTHER_NAMES) { _, user ->
                        val editedUser = userService.getUserByEmail(email)
                        if (editedUser == null) {
                            call.respond(HttpStatusCode.NotFound, "User with email $email not found")
                        } else {
                            userService.changeFirstName(editedUser, newFirstName)
                            call.respond("Successfully changed user $email first name to $newFirstName")
                        }
                    }
                }

                // Change users last name
                put("/{email}/last-name") {
                    val email = call.parameters["email"]!!
                    val newLastName = call.receiveText()

                    requirePermission(userService, Permission.CHANGE_OTHER_NAMES) { _, user ->
                        val editedUser = userService.getUserByEmail(email)
                        if (editedUser == null) {
                            call.respond(HttpStatusCode.NotFound, "User with email $email not found")
                        } else {
                            userService.changeLastName(editedUser, newLastName)
                            call.respond("Successfully changed user $email first name to $newLastName")
                        }
                    }
                }

                // Change users password
                put("/{email}/password") {
                    val email = call.parameters["email"]!!
                    val newPassword = call.receiveText()

                    requirePermission(userService, Permission.CHANGE_OTHER_PASSWORDS) { _, user ->
                        val editedUser = userService.getUserByEmail(email)
                        if (editedUser == null) {
                            call.respond(HttpStatusCode.NotFound, "User with email $email not found")
                        } else {
                            userService.changePassword(editedUser, editedUser.passwordHash, newPassword)
                            call.respond("Successfully changed user $email password to $newPassword")
                        }
                    }
                }

                // Add new permission to user by email
                post("/{email}/permissions/{permission}") {
                    val email = call.parameters["email"]!!
                    val permission = Permission.valueOf(call.parameters["permission"]!!)
                    requirePermission(
                        userService, Permission.CHANGE_PERMISSIONS
                    ) { _, _ ->
                        val editedUser = userService.getUserByEmail(email)
                        if (editedUser == null) {
                            call.respond(HttpStatusCode.NotFound, "User with email $email not found")
                        } else {
                            userService.grantPermission(editedUser, permission)
                            call.respond("Successfully granted $permission to user with email $email")
                        }
                    }
                }

                // Remove permission from user by email
                delete("/{email}/permissions/{permission}") {
                    val email = call.parameters["email"]!!
                    val permission = Permission.valueOf(call.parameters["permission"]!!)
                    requirePermission(
                        userService, Permission.CHANGE_PERMISSIONS
                    ) { _, _ ->
                        val editedUser = userService.getUserByEmail(email)
                        if (editedUser == null) {
                            call.respond(HttpStatusCode.NotFound, "User with email $email not found")
                        } else {
                            userService.revokePermission(editedUser, permission)
                            call.respond("Successfully revoked $permission from user with email $email")
                        }
                    }
                }
            }
        }
    }
}
