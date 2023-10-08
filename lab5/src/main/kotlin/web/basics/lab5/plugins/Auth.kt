package web.basics.lab5.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import org.koin.ktor.ext.inject
import web.basics.lab5.model.UserSession
import web.basics.lab5.service.UserService
import java.util.logging.Level
import java.util.logging.Logger

fun Application.configureAuth() {

    val userService by inject<UserService>()
    val logger = Logger.getLogger("Auth")

    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 600
        }
    }
    install(Authentication) {
        form("auth-form") {
            userParamName = "email"
            passwordParamName = "password"
            validate { credentials ->
                logger.log(Level.INFO, "Form auth request: ${credentials.name} - ${credentials.password}")
                if (userService.login(credentials.name, credentials.password)) {
                    logger.log(Level.INFO, "Form auth success: ${credentials.name} - ${credentials.password}")
                    UserIdPrincipal(credentials.name)
                } else {
                    logger.log(Level.INFO, "Form auth failure: ${credentials.name} - ${credentials.password}")
                    null
                }
            }
            challenge("?loginFailed=true")
        }
        session<UserSession>("auth-session") {
            validate {
                logger.log(Level.INFO, "Session validation: ${it.name}")
                it
            }
            challenge {
                call.respondRedirect("/login")
            }
        }
    }

    routing {
        get("/login") {
            val loginFailed = call.parameters["loginFailed"] == "true"

            call.respond(
                ThymeleafContent("login", mapOf("loginFailed" to loginFailed))
            )
        }

        get("/checkEmailAvailability") {
            val email = call.parameters["email"]

            logger.log(Level.INFO, "Checking email availability: $email")

            email?.let {
                call.respondText {
                    userService.isEmailAvailable(it).toString()
                }
            }
        }

        route("/register") {
            get {
                call.respond(
                    ThymeleafContent("register", emptyMap())
                )
            }

            post {
                val formParameters = call.receiveParameters()

                val email = formParameters["email"] ?: ""
                val passwordHash = formParameters["password"] ?: ""

                val firstName = formParameters["firstName"] ?: ""
                val lastName = formParameters["lastName"] ?: ""

                logger.log(Level.INFO, "New registration request: $email - $passwordHash")

                if (listOf(email, passwordHash, firstName, lastName).none { it.isBlank() }) {
                    if (!userService.isEmailAvailable(email)) {
                        call.respond(HttpStatusCode.BadRequest, "Email already used")
                    }

                    logger.log(Level.INFO, "Registering new user: $email - $passwordHash")
                    userService.register(email, passwordHash, firstName, lastName)
                    call.respondRedirect("/login")
                }
            }
        }

        authenticate("auth-form") {
            post("/login") {
                val userName = call.principal<UserIdPrincipal>()?.name.toString()
                call.sessions.set(UserSession(name = userName))
                call.respondRedirect("/home")
            }
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/login")
        }
    }
}