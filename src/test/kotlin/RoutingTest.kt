package com.example

import io.ktor.server.testing.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class RoutingTest {

    // ทดสอบดึงข้อมูลหมวดหมู่ GET /categories
    @Test
    fun testGetAllCategories() = testApplication {
        application { module() }

        val response = client.get("/categories")
        assertEquals(HttpStatusCode.OK, response.status)  // เช็คสถานะตอบกลับเป็น 200 OK

        val json = Json.parseToJsonElement(response.bodyAsText())
        assertTrue(json.jsonArray.isNotEmpty())  // เช็คว่ารายการหมวดหมู่ที่ตอบกลับมาไม่ว่าง
    }

    // ทดสอบเพิ่มรายการ Transaction ใหม่ POST /transactions และดึงรายการ Transaction ทั้งหมด GET /transactions
    @Test
    fun testAddAndGetTransaction() = testApplication {
        application { module() }

        val newTxn = """
            {
                "id": 99,
                "description": "Test Income",
                "amount": 999.0,
                "type": "income",
                "date": "2025-07-11",
                "categoryId": 2
            }
        """

        // ส่งคำขอเพิ่ม Transaction ใหม่
        val postResponse = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody(newTxn)
        }
        assertEquals(HttpStatusCode.Created, postResponse.status)  // ควรได้สถานะ 201 Created

        // ดึงรายการ Transaction ทั้งหมดมาเช็คว่ามีรายการที่เพิ่มเข้ามาหรือไม่
        val getResponse = client.get("/transactions")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        val list = Json.parseToJsonElement(getResponse.bodyAsText()).jsonArray
        assertTrue(list.any { it.jsonObject["id"]?.toString() == "99" })  // ต้องเจอ transaction ที่ id = 99
    }

    // ทดสอบเรียกดูรายงานรายจ่ายรายเดือน (GET /reports?period=monthly&year=2024&month=12&type=expense)
    @Test
    fun testReportMonthlyExpense() = testApplication {
        application { module() }

        val response = client.get("/reports?period=monthly&year=2024&month=12&type=expense")
        println(response.bodyAsText())  // แสดงข้อความ response เพื่อช่วยดีบักถ้ามี error
        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText())
        assertTrue(json.jsonArray.isNotEmpty())  // รายงานต้องไม่ว่าง
        assertEquals("\"Food\"", json.jsonArray[0].jsonObject["category"].toString())  // เช็คหมวดหมู่แรกชื่อ "Food"
    }

    // ทดสอบส่ง parameter period ที่ไม่ถูกต้อง ควรได้สถานะ BadRequest (400)
    @Test
    fun testInvalidPeriodReport() = testApplication {
        application { module() }

        val response = client.get("/reports?period=century")
        assertEquals(HttpStatusCode.BadRequest, response.status)  // period ไม่ถูกต้อง
    }

    // ทดสอบลบ Transaction ที่ id=99 (DELETE /transactions/99)
    @Test
    fun testDeleteTransaction() = testApplication {
        application { module() }

        val deleteResponse = client.delete("/transactions/99")
        // สถานะที่ได้อาจเป็น NoContent (ลบสำเร็จ) หรือ NotFound (ไม่มีรายการนี้)
        assertTrue(deleteResponse.status == HttpStatusCode.NoContent || deleteResponse.status == HttpStatusCode.NotFound)
    }

    @Test
    fun testUpdateTransaction() = testApplication {
        application { module() }

        // สร้าง transaction ก่อน
        val createTxn = """
        {
            "id": 99,
            "description": "Original Income",
            "amount": 1000.0,
            "type": "income",
            "date": "2025-07-11",
            "categoryId": 2
        }
    """
        client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody(createTxn)
        }

        // อัปเดต transaction
        val updatedTxn = """
        {
            "id": 99,
            "description": "Updated Income",
            "amount": 1234.5,
            "type": "income",
            "date": "2025-07-12",
            "categoryId": 2
        }
    """

        val response = client.put("/transactions/99") {
            contentType(ContentType.Application.Json)
            setBody(updatedTxn)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Updated Income", json["description"]?.toString()?.trim('"'))
        assertEquals("1234.5", json["amount"]?.toString())
    }

}