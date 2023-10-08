package web.basics.lab5

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.thymeleaf.*
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import web.basics.lab5.plugins.configureAuth
import web.basics.lab5.plugins.home.configureHome
import web.basics.lab5.plugins.configureRouting
import web.basics.lab5.repository.UserRepository
import web.basics.lab5.repository.UserRepositoryImpl
import web.basics.lab5.service.UserService

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }

    install(ContentNegotiation) {
        json()
    }

    configureAuth()
    configureRouting()
    configureHome()
}

val appModule = module {
    singleOf(::UserRepositoryImpl) { bind<UserRepository>() }
    singleOf(::UserService)
}