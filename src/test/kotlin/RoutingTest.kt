package com.example

import io.ktor.server.testing.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RoutingTest {

    // ===== Category Tests =====

    // 🟩 ทดสอบสร้าง Category ใหม่ และตรวจสอบการดึงหมวดหมู่ทั้งหมด
    @Test
    fun testCreateAndGetCategory() = testApplication {
        application { module() }

        val newCategory = """
            { "id": 3, "name": "Transport" }
        """
        client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody(newCategory)
        }

        val response = client.get("/categories")
        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertTrue(json.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Transport" })
    }

    // 🟩 ทดสอบอัปเดตชื่อ Category
    @Test
    fun testUpdateCategory() = testApplication {
        application { module() }

        val updateBody = """
            { "id": 1, "name": "Updated Food" }
        """
        val response = client.put("/categories/1") {
            contentType(ContentType.Application.Json)
            setBody(updateBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Updated Food", json["name"]?.jsonPrimitive?.content)
    }

    // 🟩 ทดสอบลบ Category ที่มีอยู่
    @Test
    fun testDeleteCategory() = testApplication {
        application { module() }
        val response = client.delete("/categories/1")
        assertTrue(response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.NotFound)
    }

    // ===== Transaction Tests =====

    // 🟦 ทดสอบเพิ่ม Transaction และดึงดูทั้งหมด
    @Test
    fun testCreateAndGetTransaction() = testApplication {
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
        val post = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody(newTxn)
        }
        assertEquals(HttpStatusCode.Created, post.status)

        val get = client.get("/transactions")
        assertEquals(HttpStatusCode.OK, get.status)
        val list = Json.parseToJsonElement(get.bodyAsText()).jsonArray
        assertTrue(list.any { it.jsonObject["id"]?.toString() == "99" })
    }

    // 🟦 ทดสอบอัปเดต Transaction
    @Test
    fun testUpdateTransaction() = testApplication {
        application { module() }

        val createTxn = """
            {
                "id": 99,
                "description": "Original",
                "amount": 100.0,
                "type": "income",
                "date": "2025-07-11",
                "categoryId": 2
            }
        """
        client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody(createTxn)
        }

        val updatedTxn = """
            {
                "id": 99,
                "description": "Updated",
                "amount": 150.0,
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
        assertEquals("Updated", json["description"]?.jsonPrimitive?.content)
        assertEquals("150.0", json["amount"]?.toString())
    }

    // 🟦 ทดสอบลบ Transaction
    @Test
    fun testDeleteTransaction() = testApplication {
        application { module() }
        val response = client.delete("/transactions/99")
        assertTrue(response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.NotFound)
    }

    // ===== Report Tests =====

    // 📊 ทดสอบรายงานรายเดือน
    @Test
    fun testReportMonthly() = testApplication {
        application { module() }
        val response = client.get("/reports?period=monthly&year=2024&month=12&type=expense")
        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertTrue(json.isNotEmpty())
    }

    // 📊 ทดสอบรายงานรายวัน
    @Test
    fun testReportDaily() = testApplication {
        application { module() }
        val response = client.get("/reports?period=daily&year=2024&month=12&day=10&type=expense")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // 📊 ทดสอบรายงานรายสัปดาห์
    @Test
    fun testReportWeekly() = testApplication {
        application { module() }
        val response = client.get("/reports?period=weekly&year=2024&month=12&week=2&type=expense")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // 📊 ทดสอบรายงานรายปี
    @Test
    fun testReportYearly() = testApplication {
        application { module() }
        val response = client.get("/reports?period=yearly&year=2024&type=income")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // ❌ ทดสอบพารามิเตอร์ period ผิดพลาด
    @Test
    fun testInvalidPeriodReport() = testApplication {
        application { module() }
        val response = client.get("/reports?period=century")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ❌ ทดสอบเพิ่ม Category ที่ id ซ้ำ
    @Test
    fun testDuplicateCategoryId() = testApplication {
        application { module() }
        val duplicate = """{ "id": 1, "name": "Duplicate" }"""
        val response = client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody(duplicate)
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    // ❌ ทดสอบเพิ่ม Transaction ที่ id ซ้ำ
    @Test
    fun testDuplicateTransactionId() = testApplication {
        application { module() }
        val duplicate = """
            {
                "id": 1,
                "description": "Dup",
                "amount": 10.0,
                "type": "expense",
                "date": "2025-01-01",
                "categoryId": 1
            }
        """
        val response = client.post("/transactions") {
            contentType(ContentType.Application.Json)
            setBody(duplicate)
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }
}
