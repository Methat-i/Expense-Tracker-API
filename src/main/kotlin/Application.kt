package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    install(ContentNegotiation) {
        json()
    }
    // ติดตั้ง plugin สำหรับจัดการ error และสถานะ HTTP
    install(StatusPages) {
        // กำหนดการจัดการกรณีเกิด NoSuchElementException เช่น หา item ไม่เจอใน list
        exception<NoSuchElementException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, cause.message ?: "Not Found")
        }
        exception<Throwable> { call, cause ->
            cause.printStackTrace()  // แสดงรายละเอียดของข้อผิดพลาดใน console (เพื่อให้ developer ดู debug ได้)
            call.respond(HttpStatusCode.InternalServerError, "Unexpected error: ${cause.localizedMessage}")  // ส่ง response กลับไปยัง client ว่าเกิด error 500 พร้อมข้อความ
        }
    }
    configureRouting()
}
