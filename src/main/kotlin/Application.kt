package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.*

//import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureSerialization()
    configureRouting()
    install(StatusPages) {
        exception<NoSuchElementException> { call: ApplicationCall, cause: NoSuchElementException ->
            call.respond(HttpStatusCode.NotFound, cause.message ?: "Not Found")
        }

        exception<Throwable> { call: ApplicationCall, cause: Throwable ->
            call.respond(HttpStatusCode.InternalServerError, "Unexpected error: ${cause.localizedMessage}")
        }
    }

}

fun Application.configureSerialization(){
    install(ContentNegotiation){
        json()
    }
}