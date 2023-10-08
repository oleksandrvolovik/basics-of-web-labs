package web.basics.lab5.model

import io.ktor.server.auth.*

data class UserSession(val name: String) : Principal
