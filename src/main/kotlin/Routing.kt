package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.time.LocalDate

// ข้อมูลหมวดหมู่ (Category) สำหรับแยกประเภทรายรับ-รายจ่าย
@Serializable
data class Category(val id: Int, val name: String)

// ข้อมูลรายการ Transaction (รายรับ-รายจ่าย) แต่ละรายการมีรายละเอียดต่าง ๆ
@Serializable
data class Transaction(
    val id: Int,             // รหัสรายการ
    val description: String, // คำอธิบาย
    val amount: Double,      // จำนวน
    val type: String,        // "income" "expense"
    val date: String,        // "yyyy-MM-dd"
    val categoryId: Int      // หมวดหมู่เชื่อมกับ Category
)

// คลาสเก็บข้อมูลสรุปรายงาน: หมวดหมู่และยอดรวม
@Serializable
data class ReportDetail(
    val category: String,           // ชื่อหมวดหมู่
    val totalAmount: Double,        // ยอดรวมของหมวดหมู่นั้น
    val transactions: List<TransactionSummary> // รายการ transaction ย่อ (แค่ description กับ date)
)

@Serializable
data class TransactionSummary(
    val description: String,
    val amount: Double,
    val date: String,
    val category: String
)


// เก็บข้อมูลแบบ in-memory จำลองฐานข้อมูล
object DataStorage {
    val categories = mutableListOf<Category>(
        Category(1, "Food"),
        Category(2, "Salary"),
    )

    val transactions = mutableListOf<Transaction>(
        Transaction(2, "Ryota", 358.0, "expense", "2024-12-10", 1),
        Transaction(3, "Major F1 ", 320.0, "expense", "2024-12-10", 1),
        Transaction(1, "July Salary", 3000.0, "income", "2024-12-01", 2),
    )
}

// ฟังก์ชันหลักสำหรับตั้งค่า routing API
fun Application.configureRouting() {
    routing {
        // API สำหรับจัดการหมวดหมู่
        route("/categories") {
            // ดึงข้อมูลหมวดหมู่ทั้งหมด
            get {
                call.respond(DataStorage.categories)
            }

            // สร้างหมวดหมู่ใหม่
            post {
                val category = call.receive<Category>()
                if (DataStorage.categories.any { it.id == category.id }) {
                    call.respond(HttpStatusCode.Conflict, "Category with this ID already exists")
                    return@post
                }
                DataStorage.categories.add(category)
                call.respond(HttpStatusCode.Created, category)
            }

            // แก้ไขหมวดตาม ID
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                val category = DataStorage.categories.find { it.id == id }
                    ?: return@put call.respond(HttpStatusCode.NotFound, "Category not found")
                val newData = call.receive<Category>()
                val updated = category.copy(name = newData.name)
                DataStorage.categories[DataStorage.categories.indexOf(category)] = updated
                call.respond(updated)
            }

            // ลบหมวดตาม ID
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                val removed = DataStorage.categories.removeIf { it.id == id }
                if (removed) call.respond(HttpStatusCode.NoContent)
                else call.respond(HttpStatusCode.NotFound, "Category not found")
            }
        }

        // API สำหรับจัดการรายการ Transaction
        route("/transactions") {
            // ดึงรายการทั้งหมด
            get {
                call.respond(DataStorage.transactions)
            }
            // สร้างรายการใหม่
            post {
                val transaction = call.receive<Transaction>()
                if (DataStorage.transactions.any { it.id == transaction.id }) {
                    call.respond(HttpStatusCode.Conflict, "Transaction with this ID already exists")
                    return@post
                }
                DataStorage.transactions.add(transaction)
                call.respond(HttpStatusCode.Created, transaction)
            }
            // แก้ไขรายการตาม ID
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                val transaction = DataStorage.transactions.find { it.id == id }
                    ?: return@put call.respond(HttpStatusCode.NotFound, "Transaction not found")
                val newData = call.receive<Transaction>()
                val updated = transaction.copy(
                    description = newData.description,
                    amount = newData.amount,
                    type = newData.type,
                    date = newData.date,
                    categoryId = newData.categoryId
                )
                DataStorage.transactions[DataStorage.transactions.indexOf(transaction)] = updated
                call.respond(updated)
            }
            // ลบรายการตาม ID
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                val removed = DataStorage.transactions.removeIf { it.id == id }
                if (removed) call.respond(HttpStatusCode.NoContent)
                else call.respond(HttpStatusCode.NotFound, "Transaction not found")
            }
        }

        // รายงานสรุปรายจ่ายรายเดือน (ตัวอย่างเฉพาะรายจ่าย)
        get("/reports/monthly") {
            val year = call.request.queryParameters["year"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid year")
            val month = call.request.queryParameters["month"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid month")

            // กรองรายการตามปี, เดือน และประเภท expense
            val filtered = DataStorage.transactions.filter {
                val dt = LocalDate.parse(it.date)
                dt.year == year && dt.monthValue == month && it.type == "expense"
            }

            // รวมยอดเงินแยกตามหมวด
            val report = filtered.groupBy { it.categoryId }
                .map { (categoryId, txns) ->
                    val categoryName = DataStorage.categories.find { it.id == categoryId }?.name ?: "Unknown"
                    val totalAmount = txns.sumOf { it.amount }
                    val txnSummaries = txns.map {
                        TransactionSummary(
                            description = it.description,
                            amount = it.amount,
                            date = it.date,
                            category = categoryName
                        )
                    }
                    ReportDetail(
                        category = categoryName,
                        totalAmount = totalAmount,
                        transactions = txnSummaries
                    )
                }

            call.respond(report)
        }


        //รายงานสรุปรายรับรายจ่ายรายวัน,สัปดาห์,เดือน,ปี
        //ใช้ query parameter period เพื่อระบุช่วงเวลา และ type เพื่อเลือก income หรือ expense
        //Query parameters:
        //- period: daily | weekly | monthly | yearly  (บังคับต้องใส่)
        //- type: income | expense (ถ้าไม่กำหนดจะเป็น expense)
        //- สำหรับ period ต่าง ๆ ต้องระบุพารามิเตอร์ให้ครับดังนี้
        //  * daily: ต้องระบุ year, month, day
        //  * weekly: ต้องระบุ year, month และ week (1 ถึง 4)
        //  * monthly: ต้องระบุ year, month
        //  * yearly: ต้องระบุ year
        //ตัวอย่าง:
        //  GET /reports?period=daily&year=2025&month=7&day=11&type=expense
        //  GET /reports?period=weekly&year=2025&month=7&week=2&type=expense
        //   GET /reports?period=monthly&year=2025&month=7&type=expense
        //  GET /reports?period=yearly&year=2025&type=income

        get("/reports") {
            val period = call.request.queryParameters["period"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, "Missing period parameter (daily, weekly, monthly, yearly)"
            )
            val type = call.request.queryParameters["type"] ?: "expense"

            val filtered = when (period.lowercase()) {
                "daily" -> {
                    val year = call.request.queryParameters["year"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid year")
                    val month = call.request.queryParameters["month"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid month")
                    val day = call.request.queryParameters["day"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid day")
                    DataStorage.transactions.filter {
                        val dt = try {
                            LocalDate.parse(it.date)
                        } catch (e: Exception) {
                            null
                        } ?: return@filter false
                        dt.year == year && dt.monthValue == month && dt.dayOfMonth == day && it.type == type
                    }
                }

                //รายวัน
                ///reports?period=daily&year=2025&month=7&day=11&type=expense


                "weekly" -> {
                    val year = call.request.queryParameters["year"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid year")
                    val month = call.request.queryParameters["month"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid month")
                    val week = call.request.queryParameters["week"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid week number (1-4)")

                    // ตรวจสอบว่า week อยู่ในช่วงที่ (1 ถึง 4)
                    if (week !in 1..4) return@get call.respond(HttpStatusCode.BadRequest, "Week number must be 1 to 4")

                    // // กำหนดวันที่เริ่มต้นของแต่ละสัปดาห์
                    val startDay = when (week) {
                        1 -> 1       // สัปดาห์ที่ 1: วันที่ 1 - 7
                        2 -> 8       // สัปดาห์ที่ 2: วันที่ 8 - 14
                        3 -> 15      // สัปดาห์ที่ 3: วันที่ 15 - 21
                        4 -> 22      // สัปดาห์ที่ 4: วันที่ 22 ถึงสิ้นเดือน
                        else -> 1 // ไม่เกิดขึ้นเพราะเช็คด้านบนแล้ว
                    }

                    // กำหนดวันที่สิ้นสุดของแต่ละสัปดาห์
                    val endDay = when (week) {
                        1 -> 7
                        2 -> 14
                        3 -> 21
                        4 -> {
                            //สำหรับสัปดาห์ที่ 4: หาวันสุดท้ายของเดือนโดยอิงจากปีและเดือน
                            val lastDay = LocalDate.of(year, month, 1).lengthOfMonth()
                            lastDay
                        }
                        else -> 7
                    }
                    // กรอง transactions ตามปี, เดือน, วันที่ที่กำหนด income กับ expense ด้วย
                    DataStorage.transactions.filter {
                        val dt = try {
                            LocalDate.parse(it.date)
                        } catch (e: Exception) {
                            null
                        } ?: return@filter false
                        dt.year == year && dt.monthValue == month &&
                                dt.dayOfMonth in startDay..endDay && it.type == type
                    }
                }
//รายสัปดาห์
//GET /reports?period=weekly&year=2025&month=7&week=2&type=expense

                "monthly" -> {
                    val year = call.request.queryParameters["year"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid year")
                    val month = call.request.queryParameters["month"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid month")
                    DataStorage.transactions.filter {
                        val dt = try {
                            LocalDate.parse(it.date)
                        } catch (e: Exception) {
                            null
                        } ?: return@filter false
                        dt.year == year && dt.monthValue == month && it.type == type
                    }
                }
//รายเดือน
///reports?period=monthly&year=2025&month=7&type=expense

                "yearly" -> {
                    val year = call.request.queryParameters["year"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid year")
                    DataStorage.transactions.filter {
                        val dt = try {
                            LocalDate.parse(it.date)
                        } catch (e: Exception) {
                            null
                        } ?: return@filter false
                        dt.year == year && it.type == type
                    }
                }
//รายปี
///reports?period=yearly&year=2025&type=income

                else -> return@get call.respond(HttpStatusCode.BadRequest, "Invalid period parameter")
            }

            val report = filtered.groupBy { it.categoryId }
                .map { (categoryId, txns) ->
                    val categoryName = DataStorage.categories.find { it.id == categoryId }?.name ?: "Unknown"
                    val totalAmount = txns.sumOf { it.amount }
                    val txnSummaries = txns.map {
                        TransactionSummary(
                            description = it.description,
                            amount = it.amount,
                            date = it.date,
                            category = categoryName
                        )
                    }
                    ReportDetail(
                        category = categoryName,
                        totalAmount = totalAmount,
                        transactions = txnSummaries
                    )
                }

            call.respond(report)
        }



    }
}

//รายสัปดาห์
//get("/reports/weekly") {
//    val year = call.request.queryParameters["year"]?.toIntOrNull()
//        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid year")
//    val week = call.request.queryParameters["week"]?.toIntOrNull()
//        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid week number")
//    val type = call.request.queryParameters["type"] ?: "expense"
//
//    val filtered = DataStorage.transactions.filter {
//        val dt = LocalDate.parse(it.date)
//        val weekOfYear = dt.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
//        dt.year == year && weekOfYear == week && it.type == type
//    }
//
//    val report = filtered.groupBy { it.categoryId }
//        .map { (categoryId, txns) ->
//            val categoryName = DataStorage.categories.find { it.id == categoryId }?.name ?: "Unknown"
//            val totalAmount = txns.sumOf { it.amount }
//            mapOf("category" to categoryName, "totalAmount" to totalAmount)
//        }
//
//    call.respond(report)
//}

//รายวัน
//get("/reports/daily") {
//    val year = call.request.queryParameters["year"]?.toIntOrNull()
//        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid year")
//    val month = call.request.queryParameters["month"]?.toIntOrNull()
//        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid month")
//    val day = call.request.queryParameters["day"]?.toIntOrNull()
//        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid day")
//    val type = call.request.queryParameters["type"] ?: "expense"
//
//    val filtered = DataStorage.transactions.filter {
//        val dt = LocalDate.parse(it.date)
//        dt.year == year && dt.monthValue == month && dt.dayOfMonth == day && it.type == type
//    }
//
//    val report = filtered.groupBy { it.categoryId }
//        .map { (categoryId, txns) ->
//            val categoryName = DataStorage.categories.find { it.id == categoryId }?.name ?: "Unknown"
//            val totalAmount = txns.sumOf { it.amount }
//            mapOf("category" to categoryName, "totalAmount" to totalAmount)
//        }
//
//    call.respond(report)
//}

//รายงานสรุปรายรับรายจ่ายรายปี รองรับการดึงรายรับ
//        get("/reports/yearly") {
//            รายจ่ายรายปี:GET /reports/yearly?year=2025&type=expense
//            รายรับรายปี:GET /reports/yearly?year=2025&type=income
//            val year = call.request.queryParameters["year"]?.toIntOrNull()
//                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid year")
//            val type = call.request.queryParameters["type"] ?: "expense" // กำหนด default
//
//            val filtered = DataStorage.transactions.filter {
//                val dt = LocalDate.parse(it.date)
//                dt.year == year && it.type == type
//            }
//
//            val report = filtered.groupBy { it.categoryId }
//                .map { (categoryId, txns) ->
//                    val categoryName = DataStorage.categories.find { it.id == categoryId }?.name ?: "Unknown"
//                    val totalAmount = txns.sumOf { it.amount }
//                    mapOf("category" to categoryName, "totalAmount" to totalAmount)
//                }
//
//            call.respond(report)
//        }

//รายงานสรุปรายรับรายจ่ายรายเดือน รองรับการดึงรายรับ
//        get("/reports/monthly2") {
//            รายจ่าย: /reports/monthly?year=2024&month=12&type=expense
//            รายรับ: /reports/monthly?year=2024&month=12&type=income
//            val year = call.request.queryParameters["year"]?.toIntOrNull()
//                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid year")
//            val month = call.request.queryParameters["month"]?.toIntOrNull()
//                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid month")
//            val type = call.request.queryParameters["type"] ?: "expense" // default เป็น expense
//
//            val filtered = DataStorage.transactions.filter {
//                val dt = LocalDate.parse(it.date)
//                dt.year == year && dt.monthValue == month && it.type == type
//            }
//
//            val report = filtered.groupBy { it.categoryId }
//                .map { (categoryId, txns) ->
//                    val categoryName = DataStorage.categories.find { it.id == categoryId }?.name ?: "Unknown"
//                    val totalAmount = txns.sumOf { it.amount }
//                    mapOf("category" to categoryName, "totalAmount" to totalAmount)
//                }
//
//            call.respond(report)
//        }